ALTER TABLE hotels
    ADD COLUMN minimal_price NUMERIC(10,2);

ALTER TABLE hotels
    DROP COLUMN IF EXISTS overview;
