ALTER TABLE hotel_rooms
    ADD COLUMN IF NOT EXISTS total_rooms INTEGER,
    ADD COLUMN IF NOT EXISTS bed_type TEXT,
    ADD COLUMN IF NOT EXISTS facilities TEXT,
    ADD COLUMN IF NOT EXISTS real_price NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS current_price NUMERIC(10,2);

-- Backfill total_rooms from available_rooms when missing so existing data keeps working
UPDATE hotel_rooms
SET total_rooms = COALESCE(total_rooms, available_rooms)
WHERE total_rooms IS NULL;

CREATE INDEX IF NOT EXISTS idx_hotel_rooms_hotel_total ON hotel_rooms(hotel_id, total_rooms);
