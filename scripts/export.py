#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.14,<4"
# dependencies = [
#     "boto3>=1.34,<2",
#     "psycopg>=3.2,<4",
#     "psycopg[binary]>=3.2,<4",
# ]
# ///
import argparse
import csv
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import LiteralString

import boto3
import psycopg

EXPORT_SQL: LiteralString = """
                            with latest_calculation as (select distinct on (crn) id,
                                                                                 crn,
                                                                                 created,
                                                                                 data
                                                        from tier_calculation
                                                        where data -> 'oasysInputs' is not null
                                                          and data ->> 'calculationVersion' = '3'
                                                          and created >= localtimestamp - (%(lookback_days)s * interval '1 day')
                                                          and id >= %(start_id)s
                                                        order by crn, created desc, id desc)
                            select crn,
                                   data ->> 'tier'                                                                       as tier,
                                   nullif(data ->> 'provisional', '')::boolean                                           as provisional,

                                   -- tier metadata
                                   id                                                                                    as tier_calculation_id,
                                   created                                                                               as tier_calculated_at,

                                   -- riskPredictors metadata
                                   make_timestamp(
                                           nullif(data #>> '{oasysInputs,predictors,completedDate,0}', '')::int,
                                           nullif(data #>> '{oasysInputs,predictors,completedDate,1}', '')::int,
                                           nullif(data #>> '{oasysInputs,predictors,completedDate,2}', '')::int,
                                           coalesce(
                                                   nullif(data #>> '{oasysInputs,predictors,completedDate,3}', '')::int,
                                                   0),
                                           coalesce(
                                                   nullif(data #>> '{oasysInputs,predictors,completedDate,4}', '')::int,
                                                   0),
                                           coalesce(
                                                   nullif(data #>> '{oasysInputs,predictors,completedDate,5}', '')::double precision,
                                                   0)
                                   )                                                                                     as assessment_completed_at,

                                   -- allReoffendingPredictor
                                   nullif(data #>> '{oasysInputs,predictors,output,allReoffendingPredictor,score}',
                                          '')::numeric                                                                   as arp_score,
                                   data #>> '{oasysInputs,predictors,output,allReoffendingPredictor,band}'               as arp_band,
                                   data #>>
                                   '{oasysInputs,predictors,output,allReoffendingPredictor,staticOrDynamic}'             as arp_static_or_dynamic,

                                   -- combinedSeriousReoffendingPredictor
                                   nullif(data #>>
                                          '{oasysInputs,predictors,output,combinedSeriousReoffendingPredictor,score}',
                                          '')::numeric                                                                   as csrp_score,
                                   data #>>
                                   '{oasysInputs,predictors,output,combinedSeriousReoffendingPredictor,band}'            as csrp_band,
                                   data #>>
                                   '{oasysInputs,predictors,output,combinedSeriousReoffendingPredictor,staticOrDynamic}' as csrp_static_or_dynamic,

                                   -- directContactSexualReoffendingPredictor
                                   nullif(data #>>
                                          '{oasysInputs,predictors,output,directContactSexualReoffendingPredictor,score}',
                                          '')::numeric                                                                   as dcsrp_score,
                                   data #>>
                                   '{oasysInputs,predictors,output,directContactSexualReoffendingPredictor,band}'        as dcsrp_band,

                                   -- indirectImageContactSexualReoffendingPredictor
                                   nullif(data #>>
                                          '{oasysInputs,predictors,output,indirectImageContactSexualReoffendingPredictor,score}',
                                          '')::numeric
                                                                                                                         as iicsrp_score,
                                   data #>>
                                   '{oasysInputs,predictors,output,indirectImageContactSexualReoffendingPredictor,band}' as iicsrp_band,

                                   -- everCommittedSexualOffence
                                   nullif(data #>> '{oasysInputs,everCommittedSexualOffence}', '')::boolean              as ever_committed_sexual_offence,

                                   -- deliusInputs
                                   nullif(data #>> '{deliusInputs,hasActiveEvent}', '')::boolean                         as has_active_event,

                                   -- deliusInputs.registrations
                                   data #>> '{deliusInputs,registrations,rosh}'                                          as rosh,
                                   data #>> '{deliusInputs,registrations,mappaLevel}'                                    as mappa_level,
                                   data #>> '{deliusInputs,registrations,mappaCategory}'                                 as mappa_category,
                                   nullif(data #>> '{deliusInputs,registrations,hasLiferIpp}', '')::boolean              as lifer_ipp,
                                   nullif(data #>> '{deliusInputs,latestReleaseDate}', '')::timestamp                    as latest_release_date,
                                   nullif(data #>> '{deliusInputs,registrations,hasStalking}', '')::boolean              as stalking,
                                   nullif(data #>> '{deliusInputs,registrations,hasDomesticAbuse}', '')::boolean         as domestic_abuse,
                                   nullif(data #>> '{deliusInputs,registrations,hasChildProtection}', '')::boolean       as child_protection

                            from latest_calculation
                            order by crn \
                            """


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Export the latest v3 tier calculation values from the last 7 days to S3."
    )
    parser.add_argument("--dsn", required=True, help="Postgres DSN")
    parser.add_argument("--bucket-name", required=True, help="S3 bucket to upload the CSV file to")
    parser.add_argument("--s3-key", default="tier/export.csv", help="S3 key for exported CSV file")
    parser.add_argument("--lookback-days", type=int, default=7)
    parser.add_argument("--search-window-size", type=int, default=100_000)
    return parser.parse_args()


def log(message: str) -> None:
    now = datetime.now(timezone.utc).isoformat()
    print(f"[{now}] {message}", flush=True)


def get_max_id(conn: psycopg.Connection) -> int | None:
    with conn.cursor() as cur:
        cur.execute("select max(id) from tier_calculation")
        row = cur.fetchone()
        return row[0] if row and row[0] is not None else None


def find_starting_id(
        conn: psycopg.Connection,
        max_id: int,
        search_window_size: int,
        lookback_days: int,
) -> int | None:
    log(
        "Searching backwards for earliest v3 row "
        f"from the last {lookback_days} days using window size {search_window_size}"
    )

    sql: LiteralString = """
                         select min(id)
                         from tier_calculation
                         where data ->> 'calculationVersion' = '3'
                           and created >= localtimestamp - (%s * interval '1 day')
                           and id between %s
                             and %s \
                         """

    earliest_seen: int | None = None
    upper = max_id

    with conn.cursor() as cur:
        while upper >= 1:
            lower = max(1, upper - search_window_size + 1)
            cur.execute(sql, (lookback_days, lower, upper))
            row = cur.fetchone()
            min_id_in_window = int(row[0]) if row and row[0] else None

            log(f"Back-search window [{lower}, {upper}] -> min_id_in_window={min_id_in_window}")

            if min_id_in_window is not None:
                earliest_seen = min_id_in_window
                if min_id_in_window > lower:
                    return min_id_in_window
            elif earliest_seen is not None:
                return earliest_seen

            upper = lower - 1

    return earliest_seen


def export_to_csv(conn: psycopg.Connection, csv_path: Path, start_id: int, lookback_days: int) -> int:
    row_count = 0

    with csv_path.open("w", encoding="utf-8", newline="") as csv_file:
        writer = csv.writer(csv_file)

        with conn.cursor(name="tier_export") as cur:
            cur.itersize = 5_000
            cur.execute(EXPORT_SQL, {"start_id": start_id, "lookback_days": lookback_days})

            writer.writerow([column.name for column in cur.description])
            for row in cur:
                writer.writerow(row)
                row_count += 1

    return row_count


def upload_to_s3(csv_path: Path, bucket_name: str, s3_key: str) -> None:
    log(f"Uploading {csv_path.name} to s3://{bucket_name}/{s3_key}")
    boto3.client("s3").upload_file(
        str(csv_path),
        bucket_name,
        s3_key,
        ExtraArgs={"ContentType": "text/csv"},
    )


def main() -> int:
    args = parse_args()

    if args.search_window_size <= 0:
        print("search-window-size must be > 0", file=sys.stderr)
        return 2
    if args.lookback_days <= 0:
        print("lookback-days must be > 0", file=sys.stderr)
        return 2

    log("Starting export...")
    log(
        "Configuration: "
        f"lookback_days={args.lookback_days}, "
        f"search_window_size={args.search_window_size}, "
        f"bucket_name={args.bucket_name}, "
        f"s3_key={args.s3_key}"
    )

    with psycopg.connect(args.dsn) as conn:
        conn.autocommit = False

        max_id = get_max_id(conn)
        if max_id is None:
            log("Table tier_calculation is empty. Skipping export.")
            return 0

        log(f"Overall max(id) = {max_id}")

        start_id = find_starting_id(
            conn=conn,
            max_id=max_id,
            search_window_size=args.search_window_size,
            lookback_days=args.lookback_days,
        )
        if start_id is None:
            log(f"No v3 tier_calculation rows found in the last {args.lookback_days} days. Skipping export.")
            return 0

        log(f"Starting export from id = {start_id}")
        with tempfile.TemporaryDirectory() as temp_dir:
            csv_path = Path(temp_dir) / args.s3_key.rsplit("/", maxsplit=1)[-1]
            row_count = export_to_csv(conn, csv_path, start_id, args.lookback_days)
            file_size = csv_path.stat().st_size
            log(f"Exported {row_count} rows to {csv_path.name} ({file_size} bytes)")
            upload_to_s3(csv_path, args.bucket_name, args.s3_key)

    log("Finished.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
