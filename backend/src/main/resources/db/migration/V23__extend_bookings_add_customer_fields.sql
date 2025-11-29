ALTER TABLE hotel_room_bookings
    ADD COLUMN IF NOT EXISTS customer_name TEXT,
    ADD COLUMN IF NOT EXISTS id_type TEXT,
    ADD COLUMN IF NOT EXISTS id_number TEXT;
