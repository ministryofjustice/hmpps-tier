ALTER TABLE TIER_CALCULATION
    ADD COLUMN EVENT TIMESTAMP;

UPDATE TIER_CALCULATION
    SET EVENT = CREATED
    WHERE CRN = CRN AND CREATED = CREATED AND DATA = DATA;

ALTER TABLE TIER_CALCULATION
    ALTER COLUMN EVENT SET NOT NULL;

DROP
INDEX TIER_CALCULATION_CRN_CREATED_ON;

CREATE
INDEX TIER_CALCULATION_CRN_EVENT on TIER_CALCULATION(CRN,EVENT);