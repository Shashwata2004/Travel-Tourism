CREATE TABLE IF NOT EXISTS public.admin_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'ADMIN'
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_admin_users_email ON public.admin_users(email);

