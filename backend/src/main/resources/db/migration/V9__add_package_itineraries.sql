CREATE TABLE IF NOT EXISTS travel_package_itineraries (
    id BIGSERIAL PRIMARY KEY,
    package_id UUID NOT NULL REFERENCES travel_packages(id) ON DELETE CASCADE,
    day_number INTEGER NOT NULL,
    title TEXT NOT NULL,
    subtitle TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_itinerary_package_day ON travel_package_itineraries(package_id, day_number);
