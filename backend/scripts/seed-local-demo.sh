#!/usr/bin/env bash
set -euo pipefail

# Default parameters
DATABASE="rtms_db"
NEW_GROOM_PASSWORD="RtmsDemo@2026"
NEW_OWNER_PASSWORD="RtmsDemo@2026"
VERIFY_ONLY=false
ADD_OWNERS=false
ASSETS_ROOT=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    -AssetsRoot|--assets-root)
      ASSETS_ROOT="$2"
      shift 2
      ;;
    -Database|--database)
      DATABASE="$2"
      shift 2
      ;;
    -NewGroomPassword|--new-groom-password)
      NEW_GROOM_PASSWORD="$2"
      shift 2
      ;;
    -NewOwnerPassword|--new-owner-password)
      NEW_OWNER_PASSWORD="$2"
      shift 2
      ;;
    -VerifyOnly|--verify-only)
      VERIFY_ONLY=true
      shift
      ;;
    -AddOwners|--add-owners)
      ADD_OWNERS=true
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

if [[ -z "$ASSETS_ROOT" ]]; then
  echo "Error: -AssetsRoot is mandatory." >&2
  exit 1
fi

if [[ "$VERIFY_ONLY" = true && "$ADD_OWNERS" = true ]]; then
  echo "Error: Choose either VerifyOnly or AddOwners." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ASSET_PATH="$(cd "$ASSETS_ROOT" 2>/dev/null && pwd || echo "$ASSETS_ROOT")"

if [[ ! -f "$ASSET_PATH/seed-preview.json" ]]; then
  echo "Missing seed-preview.json; supply the reviewed admission pack directory." >&2
  exit 1
fi

REPORT_DIRECTORY="$BACKEND_ROOT/target/demo-seed"
mkdir -p "$REPORT_DIRECTORY"

BACKUP_NAME="before-${DATABASE}-$(date +%Y%m%d%H%M%S).dump"
CONTAINER_BACKUP="/tmp/$BACKUP_NAME"
BACKUP_PATH="$REPORT_DIRECTORY/$BACKUP_NAME"

RUNNING_CONTAINER=$(sudo docker ps --filter "name=rtms-postgres" --format "{{.Names}}" 2>/dev/null || true)
if [[ "$RUNNING_CONTAINER" =~ rtms-postgres ]]; then
  sudo docker exec rtms-postgres pg_dump -h /var/run/postgresql -U rtms_user -d "$DATABASE" --format=custom --file="$CONTAINER_BACKUP" > /dev/null 2>&1 || true
  sudo docker cp "rtms-postgres:$CONTAINER_BACKUP" "$BACKUP_PATH" > /dev/null 2>&1 || true
fi

if [[ "$VERIFY_ONLY" = false && "$ADD_OWNERS" = false ]]; then
  UPLOAD_DIR="$BACKEND_ROOT/uploads/admissions"
  if [[ -d "$UPLOAD_DIR" ]]; then
    echo "Cleaning uploads/admissions directory..."
    rm -rf "$UPLOAD_DIR"/* 2>/dev/null || true
  fi

  COMPOSE_FILE="$BACKEND_ROOT/../docker-compose.yml"
  if [[ -f "$COMPOSE_FILE" ]]; then
    echo "Resetting local Postgres container via docker-compose..."
    sudo docker compose -f "$COMPOSE_FILE" down -v > /dev/null 2>&1 || true
    sudo docker compose -f "$COMPOSE_FILE" up -d > /dev/null 2>&1 || true

    echo "Waiting for Postgres to accept connections..."
    RETRIES=30
    while [[ $RETRIES -gt 0 ]]; do
      if sudo docker exec rtms-postgres pg_isready -U rtms_user -d "$DATABASE" > /dev/null 2>&1; then
        break
      fi
      sleep 1
      ((RETRIES--))
    done
    sleep 0.5
  fi
fi

PREV_PASSWORD="${RTMS_SEED_PASSWORD:-}"
PREV_DATABASE="${RTMS_DEMO_SEED_DATABASE:-}"
PREV_OWNER_PASSWORD="${RTMS_OWNER_SEED_PASSWORD:-}"

export RTMS_SEED_PASSWORD="$NEW_GROOM_PASSWORD"
export RTMS_OWNER_SEED_PASSWORD="$NEW_OWNER_PASSWORD"
export RTMS_DEMO_SEED_DATABASE="$DATABASE"

RUN_LOG_PATH="$REPORT_DIRECTORY/run.log"

cd "$BACKEND_ROOT"
SEED_ARGS=(test -Dtest=LocalDemoSeedToolTest -Drtms.seed.local=true "-Drtms.seed.assetsRoot=$ASSET_PATH")

if [[ "$VERIFY_ONLY" = true ]]; then
  SEED_ARGS+=("-Drtms.seed.verifyOnly=true")
fi
if [[ "$ADD_OWNERS" = true ]]; then
  SEED_ARGS+=("-Drtms.seed.addOwners=true")
fi

echo "Running Maven seed workflow (this may take 1-2 minutes)..."
chmod +x ./mvnw 2>/dev/null || true

if ! ./mvnw "${SEED_ARGS[@]}" > "$RUN_LOG_PATH" 2>&1; then
  tail -n 65 "$RUN_LOG_PATH"
  echo "Seed stopped. Local backup: $BACKUP_PATH. Check run.log before retrying; partial commits may exist." >&2
  exit 1
fi

cat "$REPORT_DIRECTORY/result.json" 2>/dev/null || true
echo "Local backup: $BACKUP_PATH"

export RTMS_SEED_PASSWORD="$PREV_PASSWORD"
export RTMS_DEMO_SEED_DATABASE="$PREV_DATABASE"
export RTMS_OWNER_SEED_PASSWORD="$PREV_OWNER_PASSWORD"
