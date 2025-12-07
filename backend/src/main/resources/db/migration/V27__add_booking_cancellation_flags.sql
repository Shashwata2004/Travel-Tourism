-- Add cancellation tracking to package bookings
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
    ADD COLUMN IF NOT EXISTS canceled_at TIMESTAMPTZ;

-- Add cancellation tracking to hotel room bookings
ALTER TABLE hotel_room_bookings
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
    ADD COLUMN IF NOT EXISTS canceled_at TIMESTAMPTZ;
