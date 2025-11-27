CREATE TABLE hotel_room_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES hotel_rooms(id) ON DELETE CASCADE,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    rooms_booked INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_room_bookings_room ON hotel_room_bookings(room_id);
CREATE INDEX idx_room_bookings_dates ON hotel_room_bookings(room_id, check_in, check_out);
