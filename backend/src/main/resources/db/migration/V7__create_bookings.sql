CREATE TABLE IF NOT EXISTS public.bookings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES public.app_users(id) ON DELETE CASCADE,
    package_id UUID NOT NULL REFERENCES public.travel_packages(id) ON DELETE RESTRICT,
    total_persons INT NOT NULL,
    price_total NUMERIC(12,2) NOT NULL,
    customer_name VARCHAR(255),
    id_number VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_bookings_user ON public.bookings(user_id);
CREATE INDEX IF NOT EXISTS ix_bookings_package ON public.bookings(package_id);

