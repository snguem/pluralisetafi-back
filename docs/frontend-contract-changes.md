# Contrat API du backend (branche `keycloak-backend`)

Ce que le frontend (`snguem/pluralisetafi`, Angular) doit savoir de l'API de la branche
`keycloak-backend` : authentification, rôles, règles métier et endpoints.

Convention de réponse à connaître : les endpoints qui renvoient `Response<Object>` signalent
l'erreur dans le **corps** (`status` = `UNAUTHORIZED`, `BAD_REQUEST`, …) ; le code HTTP reste celui
de `@ResponseStatus` (200 ou 201). Les endpoints `void` ou `ResponseEntity` (suppressions,
téléchargement Excel, PDF) renvoient un vrai code HTTP (401, 403, 404…). Un jeton absent ou
invalide donne un vrai 401.

## 1. Authentification

L'authentification est faite par Keycloak (realm `pluraletatfin`, client `plural_etatfi`, flux
« authorization code » avec PKCE) : jeton `Bearer` sur chaque appel. `GET /me` renvoie le nom
d'utilisateur et les rôles lus dans le jeton. Les comptes se créent par `POST /admin/users`
(ADMIN_PLATEFORME) et `POST /societe/users` (ADMIN_COMPTE, dans sa société).

Supprimés : `/json/**`, `/public/**` en fichiers statiques (les fichiers se téléchargent par
`GET /etats-fin/{id}/excel`), l'import « état financier N-1 » et le tableau de bord `/stats/...`
d'origine (remplacé, voir la section 8).

## 2. Import des balances

Deux endpoints restent, la même règle pour toutes les sources de balance (norme OHADA, plus de
notion « sage »).

- `PUT /etats-fin/{id}/annees-balances` — deux fichiers `balance_n` et `balance_n_1` : la
  **première feuille** de chacun remplace entièrement `BAL N` et `BAL N-1` du modèle.
- `PUT /etats-fin/{id}/annees-balances-n` — **un seul fichier** `balance_n` qui doit contenir
  **au moins 2 feuilles** (1re = N, 2e = N-1). Sinon : erreur dans le corps (`BAD_REQUEST`,
  « doit contenir 2 feuilles »), et l'état financier créé pour l'import est supprimé comme pour
  tout échec d'import. Le champ s'appelle toujours `balance_n`, mais l'écran doit annoncer que
  le fichier couvre N **et** N-1.

Les feuilles de balance sont reprises telles quelles (en-têtes, styles, fusions, largeurs). Les
colonnes I à M reçoivent les en-têtes `Ext 2`, `Ext 3`, `Ext 4`, `Ext 5`, `Ext 1` et les formules
`GAUCHE(A,n)` (n = 2, 3, 4, 5, 1) sur chaque ligne dont la colonne A est un numéro de compte.
Conséquence : la mise en page de la balance importée détermine les cellules lues par les
formules du modèle.

## 3. Création d'un état financier

`POST /etats-fin` exige maintenant `entreprise.id_` : l'identifiant d'une **entreprise déjà
enregistrée de la société de l'appelant**. Sans lui, ou avec l'entreprise d'une autre société,
la réponse est `UNAUTHORIZED` (dans le corps). « Créer l'entreprise à la volée » doit donc passer
par `POST /entreprises` d'abord.

## 4. Rôles : qui peut quoi

| Action | INVITE | COLLABORATEUR | ADMIN_COMPTE |
|---|---|---|---|
| Lire, exporter Excel/PDF, lister | oui | oui | oui |
| Créer/modifier/supprimer un état financier, importer des balances | non | oui | oui |
| Dirigeants, banques, actionnaires, activités, gérants (créer/modifier/supprimer) | non | oui | oui |
| Modifier une entreprise | non | oui | oui |
| **Créer / supprimer** une entreprise | non | non | oui |
| Exporter les données d'une entreprise, la vider (`wipe`), supprimer tous les états financiers | non | non | oui |
| Ajouter des membres (`/societe/users`) | non | non | oui |
| Écrire sur `/models` et `/config-model-excel` | non | non | non — **ADMIN_PLATEFORME uniquement** |

Le front doit masquer les boutons d'écriture selon le rôle. Les routes `/admin/**` sont réservées
à `ADMIN_PLATEFORME`, `/societe/**` à `ADMIN_COMPTE`. Les modèles et leur configuration sont
**en lecture seule** pour toutes les sociétés.

## 5. Isolation par société

Toute ressource d'une entreprise (dirigeants, banques, actionnaires, activités, gérants) est
inaccessible hors de la société propriétaire. Les listes `/all` ne renvoient que la société de
l'appelant ; `/all-entreprise?entreprise_id=` est refusé (`UNAUTHORIZED`) pour une entreprise
d'une autre société. Un enregistrement ne peut pas être déplacé vers une autre entreprise (le
champ `entrepriseId` est ignoré en modification). Le nom d'une entreprise est unique **par
société** (le doublon est détecté dans la société seulement).

## 6. Abonnement et société

- **Lecture seule à l'expiration.** Sans abonnement actif couvrant la date du jour (expiré, pas
  commencé, ou aucun), toute écriture est refusée, même pour l'`ADMIN_COMPTE` ; la lecture et les
  exports restent possibles, ainsi que `wipe` et la suppression des états financiers. L'état est
  donné par le champ `readOnly` de `GET /me/societe` (section 8), qui sert au bandeau « abonnement
  expiré ».
- **Société désactivée** : tous ses membres reçoivent `UNAUTHORIZED` ; message « société
  désactivée ». L'écran doit l'expliquer.
- **Verrou « une seule entreprise »** (plan à 1 entreprise sans hybride, ou sans abonnement
  actif) : l'entreprise ne peut être ni supprimée (`DELETE /entreprises/{id}` → vrai **403**) ni
  renommée / re-codée (`PUT` → `BAD_REQUEST` dans le corps, message explicite). Les autres champs
  restent modifiables. Avec l'option hybride ou un plan multi-entreprises, pas de verrou.
- **Création d'entreprise** : quota et verrou toujours contrôlés (`BAD_REQUEST` avec message).

## 7. Nouveaux endpoints d'administration (ADMIN_PLATEFORME)

| Endpoint | Rôle |
|---|---|
| `PATCH /admin/societes/{id}/deactivate` / `/activate` | Désactiver / réactiver une société (jamais supprimée) |
| `PATCH /admin/users/{id}/block` / `/unblock` | Bloquer / débloquer un compte (Keycloak) ; on ne peut pas bloquer son propre compte |
| `POST /admin/abonnements/activate` | Activer un abonnement (refusé si la société est désactivée) |
| `GET /admin/abonnement-plans/all`, `/{id}` | Catalogue des formules |
| `POST /admin/societes`, `GET /admin/societes/all`, `/{id}` | Sociétés avec leur premier admin |
| `GET /admin/users` | Tous les comptes : `id`, `username`, `email`, `firstName`, `lastName`, `enabled`, `roles`, `societeId`, `societeNom` (vides pour un admin plateforme) |
| `GET /admin/users/roles`, `POST /admin/users/{target}/copy-access/{source}` | Rôles disponibles ; copie des accès d'un compte vers un autre |
| `GET /admin/societes/overview` | Toutes les sociétés, la plus récente d'abord : `id`, `nom`, `code`, `active`, `planNom`, `dateFin`, `abonnementCurrent`, `entreprises`, `membres` |
| `PATCH /admin/abonnement-plans/{id}/deactivate` / `/activate` | Retirer une formule du catalogue public (les abonnements en cours continuent) / la remettre |

Blocage d'un compte : la session Keycloak est fermée, mais un jeton déjà émis reste valide
jusqu'à son expiration (durée de vie courte à configurer côté Keycloak).

## 8. Endpoints pour la refonte du frontend

| Endpoint | Qui | Contenu |
|---|---|---|
| `GET /public/plans` | **tout le monde, sans jeton** | Formules actives, de la moins chère à la plus chère : `id`, `nom`, `entrepriseQuota`, `prixParMois`, `hybrideInclus`, `hybrideDisponibleEnOption`, `hybrideSupplementPrix`. Sert la page d'accueil. La monnaie n'est pas stockée : le front affiche FCFA. |
| `GET /me/societe` | membres d'une société | `societeId`, `societeNom`, `role`, `readOnly`, `abonnement` (`planNom`, `dateDebut`, `dateFin`, `hybride`, `current`). Pour l'ADMIN_PLATEFORME (sans société) : `UNAUTHORIZED` dans le corps, ne pas l'appeler. |
| `GET /stats/societe` | membres d'une société | Tableau de bord société : `entreprises`, `entrepriseQuota`, `etatsFinanciers`, `membres`, `abonnement`, `derniersEtatsFinanciers` (5 derniers), `parAnnee` et `parEntreprise` (listes `{label, count}` pour les graphiques ; `parEntreprise` : 4 premières entreprises puis « Autres »). |
| `GET /admin/stats` | ADMIN_PLATEFORME | Tableau de bord plateforme : `societes`, `societesActives`, `abonnementsEnCours`, `abonnementsExpirantSous30Jours`, `utilisateurs`, `entreprises`, `etatsFinanciers`, `etatsParMois` (6 derniers mois), `repartitionFormules` (abonnements en cours par formule), `prochainesExpirations` (5 prochaines, sous 30 jours : `societeNom`, `planNom`, `dateFin`, `joursRestants`), `dernieresSocietes`. |

Remplace `GET /stats/entreprises-etats-fin-users` (supprimé).

`GET /societe/users` (ADMIN_COMPTE) renvoie maintenant les membres **avec leur nom** :
`id`, `keycloakUserId`, `username`, `email`, `firstName`, `lastName`, `role`, `enabled`. Les comptes
disparus de Keycloak n'apparaissent pas. `POST /societe/users` crée un membre (`COLLABORATEUR` ou
`INVITE`).

## 9. À trancher avec le backend

1. Les données existantes sont fictives : aucune migration n'est faite ; les enregistrements sans
   société sont inaccessibles.
