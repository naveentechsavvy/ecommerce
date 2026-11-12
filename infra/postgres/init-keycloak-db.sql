-- Keycloak gets its own database and role on the same Postgres instance (dev only).
CREATE ROLE keycloak WITH LOGIN PASSWORD 'keycloak';
CREATE DATABASE keycloak OWNER keycloak;
