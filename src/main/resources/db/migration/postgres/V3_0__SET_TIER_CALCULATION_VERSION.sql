UPDATE TIER_CALCULATION
SET DATA = jsonb_set(DATA, '{calculationVersion}', '"1"', true);
