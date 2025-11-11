CREATE TABLE IF NOT EXISTS public.app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    location VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_users_email ON public.app_users(email);