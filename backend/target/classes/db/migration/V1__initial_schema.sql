CREATE TABLE public."app_users" (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL
);
