#!/usr/bin/env bash
set -euo pipefail

cleanup() {
  docker compose -f docker-compose.e2e.yml down --volumes
}

trap cleanup EXIT

echo "[1/3] Building backend image..."
./backend/gradlew -p backend bootBuildImage --imageName=kir-pay-backend:e2e

echo "[2/3] Building frontend image..."
docker build -t kir-pay-frontend:e2e frontend/

echo "[3/3] Starting e2e environment and running tests..."
docker compose -f docker-compose.e2e.yml down --volumes 2>/dev/null || true
docker compose -f docker-compose.e2e.yml up -d --wait

export CI=true
cd frontend
npx playwright test --config=e2e/playwright.config.ts "$@"
cd ..
