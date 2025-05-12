-- Create schemas for each microservice (in the main 'library' database)
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS books;

-- Create roles with password authentication (for main DB services)
DO $$
BEGIN
    -- Auth Service
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'auth_service') THEN
        CREATE ROLE auth_service WITH LOGIN PASSWORD 'authpass';
    END IF;
    
    -- User Service
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'user_service') THEN
        CREATE ROLE user_service WITH LOGIN PASSWORD 'userpass';
    END IF;
    
    -- Book Service 
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'book_service_user') THEN
        CREATE ROLE book_service_user WITH LOGIN PASSWORD 'bookpass';
    END IF;
    
    -- Keycloak User
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'keycloak') THEN
        CREATE ROLE keycloak WITH LOGIN PASSWORD 'keycloak' SUPERUSER;
    END IF;
END $$;

-- Grant privileges for auth schema
GRANT CREATE, USAGE ON SCHEMA auth TO auth_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA auth TO auth_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA auth GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO auth_service;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA auth TO auth_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA auth GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO auth_service;

-- Grant privileges for users schema
GRANT CREATE, USAGE ON SCHEMA users TO user_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA users TO user_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA users GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO user_service;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA users TO user_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA users GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO user_service;

-- Grant privileges for books schema
GRANT CREATE, USAGE ON SCHEMA books TO book_service_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA books TO book_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA books GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO book_service_user;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA books TO book_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA books GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO book_service_user;

-- Set search paths
ALTER ROLE auth_service SET search_path TO auth;
ALTER ROLE user_service SET search_path TO users;
ALTER ROLE book_service_user SET search_path TO books;

-- Grant privileges to admin user for all schemas
GRANT ALL PRIVILEGES ON SCHEMA auth TO admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA auth TO admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA auth TO admin;

GRANT ALL PRIVILEGES ON SCHEMA users TO admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA users TO admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA users TO admin;

GRANT ALL PRIVILEGES ON SCHEMA books TO admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA books TO admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA books TO admin;

-- Allow service users to create flyway history tables if needed
ALTER DEFAULT PRIVILEGES FOR ROLE admin IN SCHEMA auth GRANT ALL PRIVILEGES ON TABLES TO auth_service;
ALTER DEFAULT PRIVILEGES FOR ROLE admin IN SCHEMA users GRANT ALL PRIVILEGES ON TABLES TO user_service;
ALTER DEFAULT PRIVILEGES FOR ROLE admin IN SCHEMA books GRANT ALL PRIVILEGES ON TABLES TO book_service_user;

-- Create Keycloak database
CREATE DATABASE keycloak;
ALTER DATABASE keycloak OWNER TO keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;