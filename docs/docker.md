# Docker : backend + LibreOffice + MySQL

Le frontend n'est pas dans Docker pour l'instant : on le lance à côté avec `ng serve` (son
environnement de développement appelle `http://localhost:3302/api/v1/`, le backend autorise déjà
ces appels).

## Lancer

```bash
cp .env.example .env     # puis changer les mots de passe
docker compose up -d --build
```

| Service | Image | Rôle | Accès |
|---|---|---|---|
| `backend` | Spring Boot + **LibreOffice** (une seule image) | API, recalcul des formules et PDF | `127.0.0.1:3302` (Swagger : `/api/v1/swagger-ui.html`) |
| `db` | mysql:8.4 | base de l'application | interne |

**Keycloak n'est pas dans ce compose** : le backend utilise ton Keycloak partagé (realm
voir plus bas). Volumes : `db-data`, `backend-public` (modèles Excel, états financiers générés,
fichiers temporaires : à sauvegarder).

## Comment les conteneurs se parlent

- `backend` → `db` : `SPRING_DATASOURCE_URL` (profil `prod`, qui pointe LibreOffice vers
  `/usr/lib/libreoffice`, installé dans l'image).
- `backend` → Keycloak (celui que tu lances déjà, sur la machine hôte par défaut) :
  - le jeton contient l'adresse que le **navigateur** a utilisée pour Keycloak
    (`KEYCLOAK_PUBLIC_URL`, `http://localhost:8080` par défaut) : c'est l'`iss` que le backend vérifie ;
  - un conteneur ne peut pas joindre le `localhost:8080` de l'hôte : les clés de signature et
    l'API d'administration passent par `KEYCLOAK_INTERNAL_URL` (`http://host.docker.internal:8080`
    par défaut ; `extra_hosts` le rend valable aussi sous Linux). Si ton Keycloak est ailleurs
    (autre conteneur, autre machine), change cette variable.

## Ce que le realm doit contenir

Le realm de ton Keycloak (variable `KEYCLOAK_REALM`) doit avoir : le client public `KEYCLOAK_CLIENT_ID` (PKCE,
redirect URI `http://localhost:4200/*`, web origin `http://localhost:4200`), les rôles client
`ADMIN_PLATEFORME`, `ADMIN_COMPTE`, `COLLABORATEUR`, `INVITE`, et le client `KEYCLOAK_ADMIN_CLIENT_ID`
(service account autorisé à gérer les utilisateurs) dont le secret est `KEYCLOAK_ADMIN_SERVICE_SECRET`.
`src/test/resources/keycloak/realm-export.json` en est un export de référence à importer.

## Vérifié, non vérifié

Vérifié dans la session de rédaction :
- dans une image Ubuntu 22.04 + Java 21 + `libreoffice-calc` (mêmes paquets que le Dockerfile), le
  jar du backend démarre LibreOffice via JODConverter 4.4.2 : recalcul de formules (`SUM`, `LEFT`)
  et export PDF fonctionnent, en utilisateur non-root ;
- le jar Maven est unique dans `target/` (le `COPY` du Dockerfile) ; `docker compose config` est valide.

**Non vérifié** (Maven Central n'était pas joignable depuis un build Docker dans cette session) :
1. `docker compose build` en entier (étape Maven) ;
2. le démarrage du backend avec MySQL et un Keycloak externe ;
3. l'acceptation d'un jeton Keycloak par le backend (`iss` public, clés internes).

## À savoir

- `.env` ne doit jamais être commité (il est dans `.gitignore`).

## Variables Keycloak (aucun nom de realm ou de client n'est écrit dans le code)

| Variable | Rôle |
|---|---|
| `KEYCLOAK_REALM` | realm qui contient les utilisateurs |
| `KEYCLOAK_CLIENT_ID` | client public du frontend, dont les rôles client sont les rôles de l'application |
| `KEYCLOAK_ADMIN_CLIENT_ID` / `KEYCLOAK_ADMIN_SERVICE_SECRET` | client « service account » (création, blocage, liste des comptes) et son secret |
| `KEYCLOAK_PUBLIC_URL` | adresse utilisée par le navigateur (champ `iss` des jetons) |
| `KEYCLOAK_INTERNAL_URL` | adresse utilisée par le conteneur backend (clés de signature, API d'administration) |

Hors Docker (backend lancé depuis l'IDE), exporte les mêmes variables ainsi que `KEYCLOAK_ISSUER_URI`
(`<KEYCLOAK_PUBLIC_URL>/realms/<KEYCLOAK_REALM>`).

## Déployer sur un VPS (backend + MySQL)

Sur le VPS : Docker + le plugin Compose, et le dépôt (branche `keycloak-backend`). Keycloak reste
externe : il n'est pas dans ce compose.

```bash
cp .env.example .env     # mots de passe, KEYCLOAK_*, et pour un Keycloak public :
                         # KEYCLOAK_PUBLIC_URL = KEYCLOAK_INTERNAL_URL = https://keycloak.exemple.com
docker compose up -d --build
docker compose logs -f backend
curl http://127.0.0.1:3302/api/v1/public/plans
```

Le backend n'écoute que sur `127.0.0.1:3302` : un Nginx de l'hôte fait le HTTPS et proxifie `/api/v1`
(`client_max_body_size 25m;`). MySQL n'est pas exposé.

**Mise à jour** : `git pull && docker compose up -d --build backend` (les volumes sont conservés).

**Reprendre les données d'une ancienne installation**

```bash
# base (le conteneur db doit tourner)
docker compose exec -T db sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" etat_fin_app' < sauvegarde.sql
# modèles Excel et états générés
docker compose cp ./public/. backend:/app/public/
docker compose exec -u root backend chown -R app:app /app/public
docker compose restart backend
```

**Sauvegarder**

```bash
docker compose exec -T db sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" etat_fin_app' > sauvegarde.sql
docker compose cp backend:/app/public ./public-sauvegarde
```

Prévoir au moins 2 Go de RAM (JVM + LibreOffice). `JAVA_OPTS` et `TZ` se règlent dans `.env`.
