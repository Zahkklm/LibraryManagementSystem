-- Create schemas for each microservice
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS users;

-- Create roles with password authentication
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
    
    -- Keycloak User
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'keycloak') THEN
        CREATE ROLE keycloak WITH LOGIN PASSWORD 'keycloak' SUPERUSER;
    END IF;
END $$;

-- Grant privileges for auth schema
GRANT ALL PRIVILEGES ON SCHEMA auth TO auth_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA auth TO auth_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA auth GRANT ALL PRIVILEGES ON TABLES TO auth_service;
GRANT CREATE ON SCHEMA auth TO auth_service;
GRANT USAGE ON SCHEMA auth TO auth_service;

-- Grant privileges for users schema
GRANT ALL PRIVILEGES ON SCHEMA users TO user_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA users TO user_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA users GRANT ALL PRIVILEGES ON TABLES TO user_service;
GRANT CREATE ON SCHEMA users TO user_service;
GRANT USAGE ON SCHEMA users TO user_service;

-- Set search paths
ALTER ROLE auth_service SET search_path TO auth;
ALTER ROLE user_service SET search_path TO users;

-- Grant privileges to admin user for all schemas
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA users TO admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA users TO admin;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA users TO admin;
GRANT ALL PRIVILEGES ON SCHEMA users TO admin;
GRANT CREATE ON SCHEMA users TO admin;
GRANT USAGE ON SCHEMA users TO admin;

-- Allow user_service to create flyway history table
ALTER DEFAULT PRIVILEGES FOR ROLE admin IN SCHEMA users GRANT ALL PRIVILEGES ON TABLES TO user_service;

-- Create Keycloak database
CREATE DATABASE keycloak;

-- Grant ownership and privileges to keycloak user for its database
ALTER DATABASE keycloak OWNER TO keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;