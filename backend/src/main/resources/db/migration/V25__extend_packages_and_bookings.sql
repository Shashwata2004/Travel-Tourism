-- Safety net migration to ensure booking deadline + audit fields exist even if V24 differed
ALTER TABLE travel_packages
    ADD COLUMN IF NOT EXISTS booking_deadline DATE;

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS user_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS id_type VARCHAR(64);

ALTER TABLE hotel_room_bookings
    ADD COLUMN IF NOT EXISTS user_email VARCHAR(255);
