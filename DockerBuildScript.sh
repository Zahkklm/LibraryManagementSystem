#!/bin/bash
docker build -t user-service ./user-service
docker build -t book-service ./book-service
docker build -t borrow-service ./borrow-service
docker build -t auth-service ./auth-service
docker build -t api-gateway ./api-gateway
docker build -t discovery-service ./discovery-service