CREATE TABLE IF NOT EXISTS public.travel_destinations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    region VARCHAR(255) NOT NULL,
    tags TEXT,
    best_season VARCHAR(255),
    image_url VARCHAR(512),
    hotels_count INTEGER DEFAULT 0 NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
