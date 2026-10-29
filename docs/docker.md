# Docker : backend + LibreOffice + frontend + Keycloak

## Lancer

Le dépôt du frontend (`snguem/pluralisetafi`) doit être à côté de celui-ci (`../pluralisetafi`),
ou `FRONTEND_CONTEXT` doit pointer dessus.

```bash
cp .env.example .env     # puis changer les mots de passe
docker compose up -d --build
```

| Service | Image | Rôle | Accès |
|---|---|---|---|
| `frontend` | nginx (Angular compilé) | sert l'app et proxifie `/api/v1` vers le backend | `http://localhost:4200` |
| `backend` | Spring Boot + **LibreOffice** (une seule image) | API, recalcul des formules et PDF | `127.0.0.1:3302` |
| `db` | mysql:8.4 | base de l'application | interne |
| `keycloak` + `kc-db` | keycloak 26.8 + mysql:8.4 | authentification (realm importé de `src/test/resources/keycloak/realm-export.json`) | `127.0.0.1:8080` |

Volumes : `db-data`, `kc-db-data`, `backend-public` (modèles Excel, états financiers générés,
fichiers temporaires : à sauvegarder).

## Comment les conteneurs se parlent

- Navigateur → `frontend` (nginx) → `backend:3302/api/v1` : même origine, pas de CORS à gérer.
  Le build « docker » du frontend utilise `baseUrl: '/api/v1/'`.
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
- le frontend se compile avec `ng build --configuration docker`, sans trace d'adresse en dur (seul `/api/v1/` reste) ;
- `nginx.conf` : syntaxe, proxy vers le backend, repli SPA, en-têtes de cache, refus à 413 au-dessus de 10 Mo ;
- le jar Maven est unique dans `target/` (le `COPY` du Dockerfile) ; `docker compose config` est valide.

**Non vérifié : les images n'ont pas pu être construites** (Docker Hub renvoyait 429 depuis cette
session). Premières choses à contrôler sur ta machine :
1. `docker compose build` passe (paquets LibreOffice et polices sous Ubuntu 22.04) ;
2. un import de balance puis un export PDF fonctionnent dans le conteneur (JODConverter 4.4.2 avec
   le LibreOffice 7.3 d'Ubuntu 22.04) ;
3. un jeton Keycloak est accepté par le backend (voir ci-dessus, `iss` et clés).

## À savoir

- **Keycloak en `start-dev`** : pour un test ou une préproduction seulement. En production : TLS,
  `KC_HOSTNAME`, base dédiée, mots de passe réels, realm exporté sans comptes de test.
- Le realm importé impose HTTPS hors `localhost` (`sslRequired: external`) : tester depuis une autre
  machine en http ne marchera pas sans changer ce réglage.
- Le client `plural_etatfi` du realm n'a **aucune URI de redirection** : le frontend n'utilise pas
  encore Keycloak (il a toujours ses écrans de connexion locale). À ajouter quand on migrera le front.
- `.env` ne doit jamais être commité (il est dans `.gitignore`).
