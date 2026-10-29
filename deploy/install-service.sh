#!/usr/bin/env bash
# Installe et démarre le service systemd « pluralis-back » (backend + MySQL).
# À lancer en root depuis n'importe où : le dossier du dépôt est déduit de l'emplacement du script.
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UNIT=/etc/systemd/system/pluralis-back.service

if [[ $EUID -ne 0 ]]; then
  echo "À lancer en root : sudo $0" >&2
  exit 1
fi
if ! command -v docker >/dev/null || ! docker compose version >/dev/null 2>&1; then
  echo "Docker et le plugin « docker compose » sont requis." >&2
  exit 1
fi
if [[ ! -f "$APP_DIR/.env" ]]; then
  echo "Fichier $APP_DIR/.env manquant : cp .env.example .env, puis renseignez-le." >&2
  exit 1
fi
DOCKER_BIN="$(command -v docker)"

sed -e "s#__APP_DIR__#$APP_DIR#g" -e "s#/usr/bin/docker#$DOCKER_BIN#g" "$APP_DIR/deploy/pluralis-back.service" > "$UNIT"
chmod 644 "$UNIT"

systemctl daemon-reload
systemctl enable pluralis-back   # démarre aussi au boot
systemctl start pluralis-back

echo
systemctl --no-pager --lines=0 status pluralis-back || true
echo
echo "Suivre le backend :  docker compose -f $APP_DIR/docker-compose.yml logs -f backend"
echo "Gérer le service  :  systemctl status|stop|start|reload pluralis-back"
