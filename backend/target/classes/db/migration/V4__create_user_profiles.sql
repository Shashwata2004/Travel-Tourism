CREATE TABLE IF NOT EXISTS public.user_profiles (
    user_id UUID PRIMARY KEY REFERENCES public.app_users(id) ON DELETE CASCADE,
    full_name VARCHAR(255),
    id_type VARCHAR(32),           -- NID | BIRTH_CERTIFICATE | PASSPORT
    id_number VARCHAR(64),
    gender VARCHAR(16)             -- MALE | FEMALE
);

