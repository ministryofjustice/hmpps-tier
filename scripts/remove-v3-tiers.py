#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.14,<4"
# dependencies = [
#     "psycopg>=3.2,<4",
#     "psycopg[binary]>=3.2,<4",
# ]
# ///
import argparse
import psycopg
import sys
import time
from datetime import datetime, timezone


def log(message: str) -> None:
    now = datetime.now(timezone.utc).isoformat()
    print(f"[{now}] {message}", flush=True)


def get_max_id(conn: psycopg.Connection) -> int | None:
    with conn.cursor() as cur:
        cur.execute("select max(id) from tier_calculation")
        row = cur.fetchone()
        return row[0] if row and row[0] is not None else None


def find_starting_id(conn: psycopg.Connection, max_id: int, search_window_size: int) -> int | None:
    log(f"Searching backwards for earliest v3 row using window size {search_window_size}")

    sql = """
          select min(id)
          from tier_calculation
          where data ->>'calculationVersion' = '3'
            and id between %s
            and %s \
          """

    earliest_seen: int | None = None
    upper = max_id

    with conn.cursor() as cur:
        while upper >= 1:
            lower = max(1, upper - search_window_size + 1)
            cur.execute(sql, (lower, upper))
            row = cur.fetchone()
            min_id_in_window = row[0] if row else None

            log(f"Back-search window [{lower}, {upper}] -> min_id_in_window={min_id_in_window}")

            if min_id_in_window is not None:
                earliest_seen = min_id_in_window
                if min_id_in_window > lower:
                    return min_id_in_window
            elif earliest_seen is not None:
                return earliest_seen

            upper = lower - 1

    return earliest_seen


def count_batch(conn: psycopg.Connection, start_id: int, end_id_exclusive: int) -> int:
    sql = """
          select count(*)
          from tier_calculation
          where id >= %s
            and id < %s
            and data ->>'calculationVersion' = '3' \
          """
    with conn.cursor() as cur:
        cur.execute(sql, (start_id, end_id_exclusive))
        row = cur.fetchone()
        return int(row[0]) if row else 0


def update_batch(conn: psycopg.Connection, start_id: int, end_id_exclusive: int) -> int:
    sql = """
          update tier_calculation
          set data = jsonb_set(data - 'tier', '{calculationVersion}', '"2"'::jsonb)
          where id >= %s
            and id < %s
            and data ->>'calculationVersion' = '3' \
          """
    with conn.cursor() as cur:
        cur.execute(sql, (start_id, end_id_exclusive))
        return cur.rowcount


def count_tier_summary_updates(conn: psycopg.Connection) -> int:
    sql = """
          select count(*)
          from tier_summary
          where tier is not null \
          """
    with conn.cursor() as cur:
        cur.execute(sql)
        row = cur.fetchone()
        return int(row[0]) if row else 0


def update_tier_summary(conn: psycopg.Connection) -> int:
    sql = """
          update tier_summary
          set tier = null
          where tier is not null \
          """
    with conn.cursor() as cur:
        cur.execute(sql)
        return cur.rowcount


def run_vacuum_analyze(dsn: str) -> None:
    log("Running VACUUM (ANALYZE) tier_calculation")
    with psycopg.connect(dsn, autocommit=True) as conn:
        with conn.cursor() as cur:
            cur.execute("VACUUM (ANALYZE) tier_calculation")
    log("VACUUM (ANALYZE) complete")


def process_batches(
        conn: psycopg.Connection,
        start_id: int,
        max_id: int,
        batch_size: int,
        dry_run: bool,
        sleep_millis: int,
) -> int:
    batch_number = 0
    total_matched = 0
    total_updated = 0

    current_start = start_id

    while current_start <= max_id:
        current_end_exclusive = min(current_start + batch_size, max_id + 1)
        batch_number += 1

        try:
            batch_count = count_batch(conn, current_start, current_end_exclusive)

            if dry_run:
                if batch_count > 0:
                    total_matched += batch_count
                    log(
                        f"[DRY RUN] batch={batch_number} "
                        f"ids=[{current_start}, {current_end_exclusive}) "
                        f"matches={batch_count} cumulative_matches={total_matched}"
                    )
                conn.rollback()
            else:
                if batch_count > 0:
                    updated = update_batch(conn, current_start, current_end_exclusive)
                    conn.commit()

                    total_matched += batch_count
                    total_updated += updated

                    log(
                        f"[UPDATE] batch={batch_number} "
                        f"ids=[{current_start}, {current_end_exclusive}) "
                        f"matched={batch_count} updated={updated} cumulative_updated={total_updated}"
                    )
                else:
                    conn.rollback()

        except Exception:
            conn.rollback()
            raise

        current_start = current_end_exclusive

        if sleep_millis > 0:
            time.sleep(sleep_millis / 1000.0)

    if dry_run:
        log(f"[DRY RUN] Complete: tier_calculation - total_matching_rows={total_matched}")
    else:
        log(f"Complete: tier_calculation - total_updated_rows={total_updated}")

    return total_updated


def process_tier_summary(conn: psycopg.Connection, dry_run: bool) -> int:
    try:
        possible_updates = count_tier_summary_updates(conn)

        if dry_run:
            log(f"[DRY RUN] Complete: tier_summary - total_matching_rows={possible_updates}")
            conn.rollback()
            return 0

        if possible_updates == 0:
            conn.rollback()
            log("Complete: tier_summary updated=0")
            return 0

        updated = update_tier_summary(conn)
        conn.commit()
        log(f"Complete: tier_summary - total_updated_rows={possible_updates}")
        return updated
    except Exception:
        conn.rollback()
        raise


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="One-off updater for tier_calculation calculationVersion 3 -> 2 in batches and tier_summary tier -> null."
    )
    parser.add_argument("--dsn", required=True, help="Postgres DSN")
    parser.add_argument("--batch-size", type=int, default=10_000)
    parser.add_argument("--search-window-size", type=int, default=100_000)
    parser.add_argument("--sleep-millis", type=int, default=50)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--vacuum-analyze", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    if args.batch_size <= 0:
        print("batch-size must be > 0", file=sys.stderr)
        return 2
    if args.search_window_size <= 0:
        print("search-window-size must be > 0", file=sys.stderr)
        return 2
    if args.sleep_millis < 0:
        print("sleep-millis must be >= 0", file=sys.stderr)
        return 2

    log("Starting...")
    log(
        "Configuration: "
        f"dry_run={args.dry_run}, "
        f"batch_size={args.batch_size}, "
        f"search_window_size={args.search_window_size}, "
        f"sleep_millis={args.sleep_millis}, "
        f"vacuum_analyze={args.vacuum_analyze}"
    )

    with psycopg.connect(args.dsn) as conn:
        conn.autocommit = False
        tier_calculation_updates = 0

        max_id = get_max_id(conn)
        if max_id is None:
            log("Table tier_calculation is empty. Skipping tier_calculation update.")
        else:
            log(f"Overall max(id) = {max_id}")

            start_id = find_starting_id(conn, max_id, args.search_window_size)
            if start_id is None:
                log("No rows found with calculationVersion = 3. Skipping tier_calculation update.")
            else:
                log(f"Starting from id = {start_id}")

                tier_calculation_updates = process_batches(
                    conn=conn,
                    start_id=start_id,
                    max_id=max_id,
                    batch_size=args.batch_size,
                    dry_run=args.dry_run,
                    sleep_millis=args.sleep_millis,
                )

        process_tier_summary(
            conn=conn,
            dry_run=args.dry_run
        )

    if not args.dry_run and args.vacuum_analyze and tier_calculation_updates > 0:
        run_vacuum_analyze(args.dsn)

    log("Finished.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
