CREATE TABLE IF NOT EXISTS public.travel_packages (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    base_price NUMERIC(12,2) NOT NULL,
    dest_image_url VARCHAR(512),
    hotel_image_url VARCHAR(512),
    overview TEXT,
    location_points TEXT,
    timing TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

