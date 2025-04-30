#!/bin/bash

services=(
  "discovery-service"
  "config-server"
  "api-gateway"
  "book-service"
  "user-service"
  "borrow-service"
  "auth-service"
  "saga-service"
  "notification-service"
)

for service in "${services[@]}"; do
  echo "Building $service..."
  cd "$service"
  mvn clean package -DskipTests
  cd ..
done

echo "Starting containers..."
docker-compose up --build
