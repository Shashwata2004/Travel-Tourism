CREATE TABLE hotel_rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    price NUMERIC(10,2),
    max_guests INTEGER,
    available_rooms INTEGER,
    description TEXT
);

CREATE INDEX idx_hotel_rooms_hotel ON hotel_rooms(hotel_id);
