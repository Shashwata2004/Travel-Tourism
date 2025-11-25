ALTER TABLE public.travel_packages
    ADD COLUMN IF NOT EXISTS package_available BOOLEAN NOT NULL DEFAULT FALSE;
