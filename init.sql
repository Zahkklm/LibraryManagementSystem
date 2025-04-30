-- init.sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Create schemas for each microservice
CREATE SCHEMA IF NOT EXISTS books;
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS borrows;
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS saga;
CREATE SCHEMA IF NOT EXISTS notifications;

-- Create roles with password authentication
DO $$
BEGIN
  -- Book Service
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'book_service') THEN
CREATE ROLE book_service WITH LOGIN PASSWORD 'bookpass';
END IF;

  -- User Service
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'user_service') THEN
CREATE ROLE user_service WITH LOGIN PASSWORD 'userpass';
END IF;

  -- Borrow Service
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'borrow_service') THEN
CREATE ROLE borrow_service WITH LOGIN PASSWORD 'borrowpass';
END IF;

  -- Auth Service
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'auth_service') THEN
CREATE ROLE auth_service WITH LOGIN PASSWORD 'authpass';
END IF;

  -- Saga Service
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'saga_service') THEN
CREATE ROLE saga_service WITH LOGIN PASSWORD 'sagapass';
END IF;

  -- Notification Service
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'notification_service') THEN
CREATE ROLE notification_service WITH LOGIN PASSWORD 'notificationpass';
END IF;
END $$;

-- Grant schema privileges
GRANT USAGE, CREATE ON SCHEMA
  books TO book_service,
  users TO user_service,
  borrows TO borrow_service,
  auth TO auth_service,
  saga TO saga_service,
  notifications TO notification_service;

-- Set default search paths
ALTER ROLE book_service SET search_path = books;
ALTER ROLE user_service SET search_path = users;
ALTER ROLE borrow_service SET search_path = borrows;
ALTER ROLE auth_service SET search_path = auth;
ALTER ROLE saga_service SET search_path = saga;
ALTER ROLE notification_service SET search_path = notifications;

-- Revoke public privileges for security
REVOKE ALL ON DATABASE library FROM PUBLIC;
REVOKE ALL ON SCHEMA
    public,
    books,
    users,
    borrows,
    auth,
    saga,
    notifications
    FROM PUBLIC;

-- Enable logical replication for Kafka Connect
ALTER SYSTEM SET wal_level = logical;
