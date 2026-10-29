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
`pluraletatfin`, voir plus bas). Volumes : `db-data`, `backend-public` (modèles Excel, états financiers générés,
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

Le realm `pluraletatfin` de ton Keycloak doit avoir : le client public `plural_etatfi` (PKCE,
redirect URI `http://localhost:4200/*`, web origin `http://localhost:4200`), les rôles client
`ADMIN_PLATEFORME`, `ADMIN_COMPTE`, `COLLABORATEUR`, `INVITE`, et le client `admin-service`
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
