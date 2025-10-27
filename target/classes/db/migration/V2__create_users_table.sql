CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- create admin user
INSERT INTO users (username, password_hash, role)
VALUES ('admin', '$2y$12$l1.kuAydIVvWMRpelEcJ2eBlEtHNAHBWxhtyj8PatIbAKR0eeS5wG', 'ADMIN');
