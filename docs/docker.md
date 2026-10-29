# Docker : backend + LibreOffice + Keycloak

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
| `keycloak` + `kc-db` | keycloak 26.8 + mysql:8.4 | authentification (realm importé de `src/test/resources/keycloak/realm-export.json`) | `127.0.0.1:8080` |

Volumes : `db-data`, `kc-db-data`, `backend-public` (modèles Excel, états financiers générés,
fichiers temporaires : à sauvegarder).

## Comment les conteneurs se parlent

- `backend` → `db` : `SPRING_DATASOURCE_URL` (profil `prod`, qui pointe LibreOffice vers
  `/usr/lib/libreoffice`, installé dans l'image).
- `backend` → `keycloak` :
  - le jeton contient l'adresse que le **navigateur** a utilisée pour Keycloak
    (`KEYCLOAK_PUBLIC_URL`, `http://localhost:8080` par défaut) : c'est l'`iss` que le backend vérifie ;
  - un conteneur ne peut pas joindre `localhost:8080`, donc les clés de signature sont lues à
    `http://keycloak:8080` (`..._JWT_JWK_SET_URI`) ; Spring Boot ne s'en sert que pour les clés et
    garde l'`iss` public pour la validation ;
  - le client `admin-service` (création et blocage de comptes) utilise `http://keycloak:8080`.

## Vérifié, non vérifié

Vérifié dans la session de rédaction :
- dans une image Ubuntu 22.04 + Java 21 + `libreoffice-calc` (mêmes paquets que le Dockerfile), le
  jar du backend démarre LibreOffice via JODConverter 4.4.2 : recalcul de formules (`SUM`, `LEFT`)
  et export PDF fonctionnent, en utilisateur non-root ;
- le jar Maven est unique dans `target/` (le `COPY` du Dockerfile) ; `docker compose config` est valide.

**Non vérifié** (Maven Central n'était pas joignable depuis un build Docker dans cette session) :
1. `docker compose build` en entier (étape Maven) ;
2. le démarrage du backend avec MySQL et Keycloak ;
3. l'acceptation d'un jeton Keycloak par le backend (`iss` public, clés internes).

## À savoir

- **Keycloak en `start-dev`** : pour un test ou une préproduction seulement. En production : TLS,
  `KC_HOSTNAME`, base dédiée, mots de passe réels, realm exporté sans comptes de test.
- Le realm importé impose HTTPS hors `localhost` (`sslRequired: external`).
- Le client `plural_etatfi` du realm n'a **aucune URI de redirection** : le frontend n'utilise pas
  encore Keycloak (il a toujours ses écrans de connexion locale). À ajouter quand on migrera le front.
- `.env` ne doit jamais être commité (il est dans `.gitignore`).
