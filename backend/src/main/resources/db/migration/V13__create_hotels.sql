CREATE TABLE hotels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    destination_id UUID REFERENCES travel_destinations(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    rating NUMERIC(2,1),
    location TEXT,
    nearby TEXT,
    facilities TEXT,
    description TEXT,
    overview TEXT,
    rooms_count INTEGER DEFAULT 0,
    floors_count INTEGER DEFAULT 0,
    image1 TEXT,
    image2 TEXT,
    image3 TEXT,
    image4 TEXT,
    image5 TEXT,
    gallery TEXT
);

CREATE INDEX idx_hotels_destination ON hotels(destination_id);