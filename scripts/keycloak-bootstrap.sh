#!/usr/bin/env bash
# Bootstraps the pluraletatfin Keycloak realm: realm, two clients (admin-service,
# plural_etatfi), the four plural_etatfi client roles, the realm-management role
# assignments on admin-service's service account, and four test users (one per role).
#
# Idempotent: safe to run against a stack that already has some/all of this configured.
# Uses the Keycloak container's own bundled kcadm.sh via `docker compose exec`.
#
# Usage:
#   docker compose -f docker-compose.keycloak.yml up -d
#   bash scripts/keycloak-bootstrap.sh

set -euo pipefail

# On Windows/Git Bash, MSYS rewrites leading-/ paths (like /opt/keycloak/...) into Windows
# paths before they reach the container; MSYS_NO_PATHCONV disables that rewriting. It is a
# no-op on Linux/macOS.
export MSYS_NO_PATHCONV=1

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.keycloak.yml}"
REALM="pluraletatfin"
KCADM="/opt/keycloak/bin/kcadm.sh"

compose() {
  # Redirect stdin from /dev/null: `docker compose exec -T` still attaches stdin, and a
  # non-interactive caller with no stdin to send (e.g. a CI runner or an agent shell) makes
  # kcadm.sh block forever waiting for input that will never arrive.
  docker compose -f "$COMPOSE_FILE" exec -T keycloak "$@" < /dev/null
}

kcadm() {
  compose "$KCADM" "$@"
}

echo "== Authenticating kcadm.sh against master realm =="
kcadm config credentials --server http://localhost:8080 --realm master --user admin --password admin

echo "== Realm: $REALM =="
if kcadm get "realms/$REALM" >/dev/null 2>&1; then
  echo "realm $REALM already exists, skipping creation"
else
  kcadm create realms -s realm="$REALM" -s enabled=true
fi

echo "== Client: admin-service (confidential, service account, Standard flow OFF) =="
ADMIN_SERVICE_ID="$(kcadm get clients -r "$REALM" -q clientId=admin-service --fields id --format csv --noquotes || true)"
if [ -z "${ADMIN_SERVICE_ID:-}" ]; then
  kcadm create clients -r "$REALM" \
    -s clientId=admin-service \
    -s publicClient=false \
    -s standardFlowEnabled=false \
    -s directAccessGrantsEnabled=false \
    -s serviceAccountsEnabled=true \
    -s enabled=true
  ADMIN_SERVICE_ID="$(kcadm get clients -r "$REALM" -q clientId=admin-service --fields id --format csv --noquotes)"
else
  echo "client admin-service already exists (id=$ADMIN_SERVICE_ID), skipping creation"
fi

echo "== Client: plural_etatfi (public, Standard flow + PKCE S256, Full scope allowed OFF, direct access grants ON) =="
PLURAL_ETATFI_ID="$(kcadm get clients -r "$REALM" -q clientId=plural_etatfi --fields id --format csv --noquotes || true)"
if [ -z "${PLURAL_ETATFI_ID:-}" ]; then
  kcadm create clients -r "$REALM" \
    -s clientId=plural_etatfi \
    -s publicClient=true \
    -s standardFlowEnabled=true \
    -s fullScopeAllowed=false \
    -s directAccessGrantsEnabled=true \
    -s enabled=true
  PLURAL_ETATFI_ID="$(kcadm get clients -r "$REALM" -q clientId=plural_etatfi --fields id --format csv --noquotes)"
  # PKCE must be set via a follow-up update: kcadm's nested-attribute syntax for a dotted
  # key (attributes."pkce.code.challenge.method") is only honored on `update`, not on the
  # initial `create` call.
  kcadm update "clients/$PLURAL_ETATFI_ID" -r "$REALM" -s "attributes.\"pkce.code.challenge.method\"=S256"
  echo "NOTE: directAccessGrantsEnabled=true on plural_etatfi is for test/dev use only"
  echo "      (lets Task 4's tests obtain tokens via the password grant)."
  echo "      Production MUST disable direct access grants on this public client."
else
  echo "client plural_etatfi already exists (id=$PLURAL_ETATFI_ID), skipping creation"
  # Still ensure PKCE is set even if the client already existed from a previous partial run.
  kcadm update "clients/$PLURAL_ETATFI_ID" -r "$REALM" -s "attributes.\"pkce.code.challenge.method\"=S256"
fi

echo "== Client roles on plural_etatfi =="
for ROLE in ADMIN_PLATEFORME ADMIN_COMPTE COLLABORATEUR INVITE; do
  if kcadm get "clients/$PLURAL_ETATFI_ID/roles/$ROLE" -r "$REALM" >/dev/null 2>&1; then
    echo "role $ROLE already exists on plural_etatfi, skipping"
  else
    kcadm create "clients/$PLURAL_ETATFI_ID/roles" -r "$REALM" -s name="$ROLE"
  fi
done

echo "== realm-management roles -> admin-service service account =="
REALM_MGMT_ID="$(kcadm get clients -r "$REALM" -q clientId=realm-management --fields id --format csv --noquotes)"
SERVICE_ACCOUNT_USERNAME="service-account-admin-service"
SERVICE_ACCOUNT_ID="$(kcadm get users -r "$REALM" -q username="$SERVICE_ACCOUNT_USERNAME" --fields id --format csv --noquotes)"

for ROLE in manage-users view-users query-users view-clients query-clients; do
  ASSIGNED="$(kcadm get "users/$SERVICE_ACCOUNT_ID/role-mappings/clients/$REALM_MGMT_ID" -r "$REALM" --fields name --format csv --noquotes || true)"
  if echo "$ASSIGNED" | grep -qx "$ROLE"; then
    echo "role $ROLE already assigned to admin-service service account, skipping"
  else
    kcadm add-roles -r "$REALM" --uusername "$SERVICE_ACCOUNT_USERNAME" --cclientid realm-management --rolename "$ROLE"
  fi
done

echo "== Test users =="
create_user_with_role() {
  local username="$1"
  local role="$2"
  local user_id
  user_id="$(kcadm get users -r "$REALM" -q username="$username" --fields id --format csv --noquotes || true)"
  if [ -z "${user_id:-}" ]; then
    # firstName/lastName/email are required here, not just cosmetic: Keycloak 26's default
    # User Profile validation flags an incomplete profile as a pending "verify profile"
    # required action, which makes the password grant fail with "Account is not fully set
    # up" even though kcadm shows an empty requiredActions list.
    kcadm create users -r "$REALM" \
      -s username="$username" \
      -s enabled=true \
      -s emailVerified=true \
      -s email="${username}@pluraletatfin.local" \
      -s firstName="$username" \
      -s lastName="Test"
    user_id="$(kcadm get users -r "$REALM" -q username="$username" --fields id --format csv --noquotes)"
    kcadm set-password -r "$REALM" --userid "$user_id" --new-password test-password --temporary=false
  else
    echo "user $username already exists (id=$user_id), skipping creation"
  fi

  local assigned
  assigned="$(kcadm get "users/$user_id/role-mappings/clients/$PLURAL_ETATFI_ID" -r "$REALM" --fields name --format csv --noquotes || true)"
  if echo "$assigned" | grep -qx "$role"; then
    echo "role $role already assigned to $username, skipping"
  else
    kcadm add-roles -r "$REALM" --uusername "$username" --cclientid plural_etatfi --rolename "$role"
  fi
}

create_user_with_role awa ADMIN_PLATEFORME
create_user_with_role moussa ADMIN_COMPTE
create_user_with_role fatou COLLABORATEUR
create_user_with_role nouveau INVITE

echo "== Bootstrap complete =="
