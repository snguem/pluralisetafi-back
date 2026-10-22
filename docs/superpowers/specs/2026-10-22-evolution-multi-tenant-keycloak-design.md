# Évolution PluralEtatFin — multi-tenant, Keycloak, refactor, import simplifié

Date : 2026-10-22
Statut : à valider

## 1. Intention et pourquoi

PluralEtatFin gère aujourd'hui plusieurs sociétés clientes (`Entreprise`) mais pour **un seul
client final** : il n'y a aucune notion de compte/tenant au-dessus, l'authentification est un
JWT maison, et une partie du code (import des balances notamment) a grossi au point de devenir
difficile à faire évoluer. Le produit doit devenir un SaaS multi-cabinets : chaque cabinet
comptable ou entreprise de services doit pouvoir avoir son propre espace, avec ses propres
sociétés clientes, ses collaborateurs et ses droits, **sans que les cabinets se voient entre
eux**.

Cette phase pose les quatre fondations nécessaires avant de construire les fonctionnalités
métier du cahier des charges complet (périodes de mission, facturation Paytech, documents,
notifications, tableaux de bord) :

1. Nettoyer l'architecture existante (MVC / SOLID / segmentation des god classes).
2. Remplacer l'authentification maison par Keycloak.
3. Introduire la notion de compte (tenant) et d'abonnement multi-société.
4. Simplifier radicalement l'import des balances.

## 2. Objectifs, non-objectifs, anti-objectifs

### Objectifs
- Un cabinet (compte) peut avoir plusieurs sociétés clientes (`Entreprise`), isolées des
  autres comptes, dans la même base et la même instance applicative.
- L'authentification passe entièrement par Keycloak, à la fois pour le frontend Angular
  (`plural_etatfi`) et pour les appels serveur-à-serveur de gestion des utilisateurs
  (`admin-service`).
- Le code suit une architecture MVC propre : les controllers orchestrent, les services
  portent la logique, les god classes actuelles sont décomposées en collaborateurs à
  responsabilité unique.
- L'import d'une balance devient : on remplace la feuille par le fichier déposé, on complète
  les colonnes Ext 1 à Ext 5 si absentes, point.

### Non-objectifs (pas dans cette phase)
- Périodes de mission (activation, expiration, suppression irréversible des données).
- Facturation / abonnement payant via Paytech, simulateur de prix.
- Documents et ressources attachés à une société/période.
- Notifications (email, in-app).
- Tableaux de bord multi-sociétés, exports de suivi.
- Historique/traçabilité fine (journal des accès).

Ces points restent décrits dans le cahier des charges d'origine et seront repris en phase 2,
une fois les fondations ci-dessus en place.

### Anti-objectifs (ce qui ferait échouer cette phase même si "ça marche")
- Un découpage multi-tenant qui oblige à dupliquer la base ou l'instance par cabinet (on a
  choisi l'isolation logique justement pour l'éviter).
- Une intégration Keycloak qui redonne la main à l'application sur la gestion des mots de
  passe (Keycloak doit rester la seule source de vérité pour l'authentification).
- Un "refactor" qui renomme des classes sans réduire réellement la taille/la
  responsabilité des god classes identifiées plus bas.
- Un nouvel import de balance qui réintroduit une étape de configuration/mapping manuelle :
  l'objectif est justement de supprimer cette complexité.

## 3. Contraintes

- Stack actuelle : Spring Boot 3.5.6 / Java 21 côté `pluralisEtatFin`, Angular 19 côté
  `etat-fin-app-front`. Le guide Keycloak fourni est écrit avec Spring Boot 4.1 / Angular 22,
  mais le **modèle** (OAuth2 resource server, `keycloak-js` + `keycloak-angular`, un realm,
  des rôles de client) s'applique tel quel aux versions actuelles — aucune migration de
  version n'est requise par ce chantier.
- Le produit doit pouvoir tourner en ligne (SaaS partagé, plusieurs comptes) et installé
  chez un client (Docker, un seul compte). L'isolation logique retenue couvre les deux cas
  sans configuration différente : une installation "chez le client" est juste un déploiement
  avec un seul `Compte` actif.
- Pas de données existantes à préserver (confirmé) : les scripts de migration de données ne
  sont pas nécessaires, on peut repartir d'un schéma neuf. Le code doit quand même passer par
  des migrations versionnées (Flyway ou équivalent) pour rester cohérent avec le guide
  Keycloak et faciliter la prod plus tard — **à vérifier par le builder** : le projet n'a pas
  Flyway aujourd'hui (pas de dépendance trouvée), c'est donc un ajout, pas une migration.
- Aucune échéance imposée : la qualité prime sur la vitesse.

## 4. Décisions d'architecture (actées avec le porteur du projet)

### 4.1 Compte (tenant) et abonnements

Nouvelle entité `Compte`, au-dessus de l'`Entreprise` existante :

- `id`, `nom`, `type_abonnement` (enum `SOLO`, `MULTI_SOCIETE`, `CABINET`), `active`,
  `createAt`/`updatedAt`.
- Règles de quota par palier (remplacent la grille Starter/Essentiel/Pro/Business/Sur-mesure
  du cahier des charges d'origine pour cette phase) :

  | Palier | Sociétés incluses | Particularité |
  |---|---|---|
  | `SOLO` | 1 | Une seule société, pas de réimport possible au-delà |
  | `MULTI_SOCIETE` | 2 à 4 | — |
  | `CABINET` | 5 à 10 | — |

  Le nombre d'utilisateurs inclus par palier n'a pas été fixé par le porteur du projet.
  **Hypothèse à valider** : reprendre une grille simple (solo = 2, multi = 5, cabinet = 10),
  configurable plutôt que codée en dur, puisque la facturation/les quotas précis seront revus
  en phase 2 avec Paytech.
- `Entreprise` reçoit une FK `compte_id` (non nullable). Toute requête de lecture/écriture sur
  `Entreprise` et ses sous-entités (`ActiviteEntreprise`, `ActionaireEntreprise`,
  `DirigentEntreprise`, `GerantEntreprise`, `BanqueEntreprise`) doit être filtrée par le
  `compte_id` de l'utilisateur courant (sauf pour l'administrateur plateforme).
- **Correction issue de la relecture** : `EtatFinancier.entreprise` n'est aujourd'hui *pas*
  une relation JPA vers `Entreprise` mais un `@Embedded EntrepriseDatasSubstitute` — une copie
  figée des données de la société au moment de la génération de l'état financier (utile pour
  ne pas réécrire l'historique si la société est modifiée ensuite). Le filtrage par
  `compte_id` ne peut pas s'appuyer sur cette copie. **Décision** : `EtatFinancier` reçoit en
  plus une vraie relation `@ManyToOne Entreprise entreprise` (et donc accès à
  `entreprise.compte_id` par jointure), tout en gardant l'embarqué `EntrepriseDatasSubstitute`
  pour l'instantané historique affiché/exporté. Les deux coexistent, chacun avec son rôle :
  la relation pour le filtrage/la sécurité, l'embarqué pour le contenu figé du document.
- Le verrouillage des champs d'identification légale (`registre_commerce`, `caisse_sociale`,
  etc. dans `EntrepriseDatasSubstitute`) après création est une **règle générale**,
  indépendante du palier — confirmé par le porteur du projet. Le palier `SOLO` n'ajoute pas de
  règle supplémentaire sur ce point ; sa seule particularité est le plafond à 1 société.
- Dépassement de quota : bloqué à la création d'une nouvelle `Entreprise` tant que le compte
  n'a pas changé de palier (pas de logique "société en plus au tarif unitaire" tant que la
  facturation n'existe pas).
- `ModelExcel` (bibliothèque de modèles) reste une ressource globale, partagée entre tous les
  comptes — cohérent avec le cahier des charges ("bibliothèque de modèles maintenue
  indépendamment des cycles de release").

### 4.2 Isolation multi-tenant : logique, schéma unique

- Une seule base, un seul schéma, une colonne `compte_id` sur les tables qui en ont besoin.
- Un seul realm Keycloak pour toute la plateforme (pas de realm par tenant).
- Toute la vérification d'appartenance (quel compte, quelle société) se fait côté Spring, à
  chaque requête — même mécanique que le modèle `membre_societe` du guide Keycloak fourni,
  étendu d'un niveau (société appartient à un compte, utilisateur est rattaché à un compte et,
  pour les collaborateurs, à des sociétés précises de ce compte).
- Une installation "chez le client" est un déploiement Docker normal avec un seul `Compte` ;
  aucune branche de code spécifique n'est nécessaire.

### 4.3 Keycloak

- **Un realm** pour la plateforme (nom proposé : `pluraletatfin`, à créer en suivant les
  sections 4-5 du guide fourni).
- **Deux clients**, conformes au guide :
  - `admin-service` — un client Keycloak, pas un service séparé à déployer : c'est un bean de
    configuration (identifiants *client credentials*) dans le monolithe `pluralisEtatFin`
    existant, qui l'utilise pour appeler l'API Admin de Keycloak en interne (création de
    comptes lors de l'invitation d'un collaborateur, attribution des rôles). Client
    confidentiel, *Service accounts roles* activé, *Standard flow* désactivé. Rôles de client
    `realm-management` à assigner à son compte de service : `manage-users`, `view-users`,
    `query-users`, `view-clients`, `query-clients`.
  - `plural_etatfi` — client public (SPA), *Standard flow* + PKCE (`S256`), pas de secret.
    C'est le client utilisé par Angular (`keycloak-js` / `keycloak-angular`) pour la connexion
    interactive des utilisateurs.
- **Rôles de client sur `plural_etatfi`** (un seul jeu de rôles, pas de realm par type
  d'utilisateur) :
  - `ADMIN_PLATEFORME` — accès transverse à tous les comptes (équipe PluralEtatFin).
  - `ADMIN_COMPTE` — administre un compte : crée des sociétés, invite des collaborateurs,
    gère les délégations.
  - `COLLABORATEUR` — rôle de base ; la société précise à laquelle il a accès est déterminée
    en base, pas par Keycloak.
  - `INVITE` — lecture seule, même principe de délégation que `COLLABORATEUR`.
- Convertisseur de rôles : reprendre tel quel le pattern `KeycloakClientRoleConverter` du
  guide (lit uniquement `resource_access.plural_etatfi.roles`, préfixe `ROLE_`,
  `Full scope allowed` = OFF sur le client).
- `SecurityConfig` ct Spring : remplacer l'actuel `JwtAuthFilter` +
  `UserAuthenticationProvider` par `oauth2ResourceServer().jwt(...)` standard, avec
  `issuer-uri` pointant sur le realm Keycloak (variable d'environnement en prod, comme dans le
  guide).
- Suppression complète côté backend : `User` (entité + table), `AppRole`, `UserServiceImpl`
  (logique de login), `UserAuthenticationProvider`, `UserAuthenticationEntryPoint`,
  `JwtAuthFilter`, `PasswordConfig`. `RoleRestController`/`RoleServiceImpl` disparaissent ou
  sont remplacés par un contrôleur qui appelle l'API Admin Keycloak via `admin-service`
  (invitation, changement de rôle) plutôt que de gérer une table `roles` locale.
- **Invitation d'un collaborateur (décidé avec le porteur du projet)** : pas d'envoi d'email
  automatique dans cette phase. `admin-service` crée le compte Keycloak avec un mot de passe
  temporaire via l'API Admin ; l'admin-compte le communique lui-même en dehors de l'outil.
  L'envoi automatique d'email sera traité avec les Notifications en phase 2 (configuration
  SMTP du realm ou e-mail applicatif, à trancher à ce moment-là).
- Côté Angular : remplacer `AuthService`/`jwt-interceptor`/`AuthGuard` maison par
  `keycloak-angular` + `keycloak-js`, selon la section 6 du guide (client `plural_etatfi`,
  PKCE, redirection gérée par Angular, 401/403 traités comme décrit dans le guide — 401 =
  relance la connexion, 403 = "accès refusé" sans redirection).

### 4.4 Rôles et délégation (4 niveaux d'utilisateurs)

Un rôle de client Keycloak (`ADMIN_COMPTE`, `COLLABORATEUR`...) dit *quel type* d'utilisateur
on a, jamais *de quel compte* il s'agit — ça, Keycloak ne le sait pas. **Décision de
conception (point soulevé en relecture, tranché ici)** : deux tables en base, jamais de
dépendance à un attribut/claim Keycloak personnalisé pour cette résolution :

- `membre_compte` (`keycloak_user_id`, `compte_id`) : toute personne non `ADMIN_PLATEFORME`
  a exactement une ligne ici. C'est elle qui répond à "cet utilisateur appartient à quel
  compte ?" à chaque requête (résolue une fois par requête, par ex. dans un composant qui
  enrichit le contexte de sécurité Spring). `ADMIN_COMPTE` et `INVITE` s'arrêtent à ce
  niveau : ils voient toutes les sociétés du compte résolu par cette table, pas besoin d'aller
  plus loin.
- `membre_entreprise` (`keycloak_user_id`, `entreprise_id`) : restriction fine, uniquement
  pour `COLLABORATEUR` (et `INVITE` si le porteur du projet veut aussi le restreindre par
  société plus tard). Un collaborateur ne voit que les sociétés où une ligne existe pour lui,
  toujours à l'intérieur du compte résolu par `membre_compte`. Vérification systématique côté
  Spring (pas de confiance aveugle au token, comme le recommande le guide).
- `ADMIN_PLATEFORME` n'a de ligne dans aucune des deux tables : son rôle Keycloak seul suffit
  à autoriser l'accès transverse à tous les comptes.
- **Décision de conception (à valider en revue)** : cette phase implémente la délégation
  directe utilisateur ↔ société, **sans** introduire l'entité "Équipe" du cahier des charges
  (section 2, "Organisation en équipes"). Une équipe n'est qu'un regroupement nommé de
  délégations ; l'ajouter plus tard (phase 2) n'impose pas de revoir ce modèle, juste
  d'ajouter une table `equipe` et une colonne `equipe_id` optionnelle sur la délégation.

### 4.5 Refactor : MVC / SOLID / segmentation des god classes

Classes identifiées comme surdimensionnées (par nombre de lignes et de responsabilités) :

| Classe | Lignes | Problème |
|---|---|---|
| `helpers/ExcelService.java` | 640 | Fait à la fois : fusion de balance, configuration du modèle, nettoyage de fichier, calcul de complémentarité Sage/non-Sage. |
| `api/controllers/EtatFinancierRestController.java` | 373 | 8 dépendances injectées directement (`ExcelService`, `AposePdfExcelService`, `JobConverterExcelPdflService`, `HelperService`...) ; le controller orchestre lui-même l'import au lieu de déléguer à un service. |
| `helpers/AposePdfExcelService.java` | 285 | Mélange calcul de formules Excel et génération. |
| `helpers/JobConverterExcelPdflService.java` | 238 | Conversion Excel→PDF, à isoler derrière une interface. |

Proposition de découpage (le builder peut ajuster les noms, le principe reste) :

- `ExcelService` → éclaté en :
  - `BalanceImportService` (nouveau, remplace `addBalanceToModel` / `fuseBalanceToExcel` /
    `configExcelFile` / `complement(...)` — voir § 4.6, logique radicalement plus simple).
  - `ExcelTemplateService` (noms de pages, découpage en parties — `getExcelPageNames`,
    `setPartModelExcel`).
  - `ExcelCleanupService` (`nettoyerEtRecupererExcel`).
  - Suppression pure et simple de `complement(...)` et de la distinction Sage/non-Sage.
- `EtatFinancierRestController` → reste un controller fin : délègue l'import à
  `BalanceImportService`/`EtatFinancierImportOrchestrator`, et l'export PDF à un service dédié
  (`AposePdfExcelService`/`JobConverterExcelPdflService` restent des collaborateurs internes,
  mais ne sont plus injectés dans le controller — ils sont cachés derrière l'orchestrateur).
- Garder la convention déjà en place dans le projet (une interface de service par entité +
  une impl + repository + mapper + controller) pour les nouvelles entités (`Compte`,
  délégation) : c'est la segmentation déjà établie, pas besoin d'inventer un style CQRS.

### 4.6 Import des balances — nouveau flux

Compris grâce à l'exemple fourni : les colonnes "Ext 1" à "Ext 5" ne sont pas un calcul
métier — ce sont des formules Excel `GAUCHE(compte;1)` … `GAUCHE(compte;5)` (préfixes du
numéro de compte, pour les niveaux de regroupement du plan comptable OHADA).

Nouveau flux (remplace `addBalanceToModel`, `fuseBalanceToExcel`, `configExcelFile`,
`complement(...)`) :

1. L'utilisateur dépose son fichier de balance (BAL N, et éventuellement BAL N-1) — n'importe
   quel export, sans format imposé au-delà d'une colonne "numéro de compte" identifiable.
2. Le backend **remplace** la feuille "BAL N" (resp. "BAL N-1") du classeur modèle par les
   données du fichier déposé, telle quelle (plus d'injection cellule par cellule, plus de
   fusion avec un modèle intermédiaire).
3. Si les colonnes Ext 1 à Ext 5 n'existent pas déjà dans le fichier déposé, le backend les
   calcule en Java (`substring` sur la colonne du numéro de compte, longueurs 1 à 5) et les
   ajoute à côté des colonnes importées.
4. La distinction Sage / non-Sage disparaît : un seul chemin de traitement, quel que soit
   l'outil comptable d'origine de la balance.
5. Le reste du pipeline (calcul des formules du classeur via `AposePdfExcelService`,
   génération PDF/Excel) ne change pas : il consomme toujours un classeur dont les feuilles
   BAL N / BAL N-1 sont à jour.

Points laissés au builder (voir § 5) : le détail exact de détection de la colonne "numéro de
compte" dans le fichier déposé (position fixe vs en-tête reconnu), et la gestion du cas où le
fichier déposé a déjà certaines colonnes Ext mais pas toutes.

### 4.7 Ordre d'exécution

1. **Refactor** de l'existant (découpage des god classes, nettoyage MVC) — avant d'ajouter de
   nouvelles couches, pour ne pas empiler la dette.
2. **Keycloak** (realm, clients, `SecurityConfig`, Angular) — fondation de sécurité avant le
   modèle de comptes.
3. **Multi-tenant / abonnements** (`Compte`, quotas, délégation) — s'appuie sur les rôles
   Keycloak posés à l'étape précédente.
4. **Import des balances simplifié** — indépendant du reste, fait en dernier.

## 5. Ce qui reste au builder

- Fixer les valeurs exactes de quota utilisateurs par palier (hypothèse § 4.1 à confirmer ou
  ajuster, idéalement dans un fichier de config plutôt que codées en dur).
- Décider du nom technique définitif du realm et des rôles si différent de la proposition
  (`pluraletatfin`, `ADMIN_PLATEFORME`/`ADMIN_COMPTE`/`COLLABORATEUR`/`INVITE`).
- Détail du schéma de la table de délégation (`membre_entreprise` ou autre nom), et du champ
  utilisé pour relier un utilisateur Keycloak à cette table (`keycloak_user_id` en
  `VARCHAR(36)`, comme dans le guide).
- Détail de détection de la colonne "numéro de compte" dans un fichier de balance déposé par
  l'utilisateur (position, en-tête), et comportement si une seule des deux balances (N ou
  N-1) est fournie.
- Stratégie de migration de schéma (Flyway recommandé, à confirmer puisqu'absent du projet
  aujourd'hui) et nommage des migrations pour les nouvelles tables (`compte`, délégation,
  `entreprise.compte_id`).
- Plan de tests : le projet n'a pas de tests Java significatifs aujourd'hui (dossier
  `src/test` quasi vide) ; prévoir au moins des tests sur `BalanceImportService` (le nouveau
  flux d'import) et sur le filtrage par `compte_id`, vu leur caractère critique pour
  l'isolation des données entre cabinets.
