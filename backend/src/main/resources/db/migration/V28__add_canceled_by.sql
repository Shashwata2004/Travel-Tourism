-- Track who canceled the booking (USER or ADMIN)
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS canceled_by VARCHAR(32);

ALTER TABLE hotel_room_bookings
    ADD COLUMN IF NOT EXISTS canceled_by VARCHAR(32);
