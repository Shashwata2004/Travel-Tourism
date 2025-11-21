-- Add up to 5 image URL slots per package and backfill from existing columns
ALTER TABLE travel_packages
    ADD COLUMN IF NOT EXISTS image1 TEXT,
    ADD COLUMN IF NOT EXISTS image2 TEXT,
    ADD COLUMN IF NOT EXISTS image3 TEXT,
    ADD COLUMN IF NOT EXISTS image4 TEXT,
    ADD COLUMN IF NOT EXISTS image5 TEXT;

-- Backfill first two slots from previous columns so existing data still shows
UPDATE travel_packages
SET image1 = COALESCE(image1, dest_image_url),
    image2 = COALESCE(image2, hotel_image_url);
