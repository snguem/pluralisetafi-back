# Multi-société (multi-tenant) — conception

Date : 2026-10-28
Statut : à valider

> Un document antérieur (`2026-10-22-evolution-multi-tenant-keycloak-design.md`) couvrait un
> sujet voisin sous un vocabulaire différent (`Compte` au lieu de `Société`, une distinction
> `membre_compte`/`membre_entreprise` à deux niveaux, pas de notion de durée/mois ni d'option
> "hybride"). Le porteur du projet a demandé d'ignorer ce document et de repartir des décisions
> prises dans la conversation qui a produit celui-ci. Ce document est autonome ; il ne suppose
> aucune lecture préalable de l'ancien.

## 1. Intention et pourquoi

PluralEtatFin gère aujourd'hui une base d'utilisateurs plate, sans frontière d'organisation :
tout le monde voit tout. Pour que le produit grandisse au-delà d'un client unique, il doit
devenir multi-société : plusieurs organisations clientes (cabinets, entreprises de services)
doivent pouvoir utiliser la même instance, chacune avec ses propres utilisateurs et ses propres
sociétés clientes (pour lesquelles elle génère des états financiers), sans qu'aucune ne voie les
données d'une autre.

La demande de pouvoir supprimer/exporter les données d'une société cliente est née d'une
réflexion du porteur du projet lui-même (les balances exposent le chiffre d'affaires et
d'autres informations sensibles), pas d'une obligation réglementaire externe — ça oriente vers
une fonctionnalité de confiance honnête plutôt que vers un système de conformité formel.

## 2. Vocabulaire (fixé dans cette conversation)

- **Société** : l'organisation-tenant. Un ensemble d'utilisateurs ayant des accès dans
  l'organisation. C'est elle qui souscrit un abonnement.
- **Entreprise** (entité déjà existante dans le code) : la société *cliente*, virtuelle, pour
  laquelle on crée des états financiers. Distincte de la Société elle-même. Une Société
  enregistre une ou plusieurs Entreprises (selon son abonnement).
- **user_societe** : la table de rattachement entre un utilisateur Keycloak et sa Société.

## 3. Objectifs

- Chaque utilisateur Keycloak, sauf `ADMIN_PLATEFORME`, appartient à exactement une Société
  (table `user_societe`, contrainte d'unicité sur l'identifiant Keycloak).
- `ADMIN_PLATEFORME` crée une Société, définit son premier administrateur (`ADMIN_COMPTE`) et
  active son abonnement (manuellement — le paiement est hors scope, voir §4).
- `ADMIN_COMPTE` peut ajouter d'autres utilisateurs, mais seulement dans sa propre Société, et
  seulement avec un rôle inférieur ou égal au sien (donc `COLLABORATEUR`/`INVITE`, jamais
  `ADMIN_COMPTE` ni `ADMIN_PLATEFORME`). Règle générale : un créateur ne peut jamais attribuer
  un rôle supérieur au sien.
- `ADMIN_COMPTE` peut enregistrer une ou plusieurs Entreprises, dans la limite du quota de son
  abonnement.
- `ADMIN_COMPTE` peut créer un état financier pour une Entreprise déjà enregistrée, ou en créer
  une à la volée au moment de créer l'état financier.
- L'export PDF/Excel avec sélection de pages existe déjà (`EtatFinancierRestController`) ; il
  doit rester fonctionnel mais être filtré par l'appartenance à la Société de l'appelant.
- `ADMIN_PLATEFORME` configure les modèles Excel (`ConfigModelExcel`), y compris les listes
  (dirigeants, etc.) — ressource globale, partagée entre toutes les Sociétés, non affectée par
  ce chantier au-delà de rien (déjà comme ça).
- Abonnement : `ADMIN_PLATEFORME` définit des formules (nom, quota d'Entreprises, prix/mois,
  option "hybride" incluse ou payante en supplément) et des variantes de durée en mois (ex.
  1/2/3 mois) ; la Société choisit une formule et une durée. Le paiement réel (Paytech) est
  branché plus tard — pour l'instant, `ADMIN_PLATEFORME` active l'abonnement manuellement
  (dates de début/fin posées à la main).
  - **Formule à 1 Entreprise, sans hybride** : la Société reste verrouillée sur la première
    Entreprise enregistrée, **pour toute la durée de vie de la Société** — pas seulement pour la
    période d'abonnement en cours. Un renouvellement sous une formule sans hybride ne lève
    jamais ce verrou. Objectif explicite : empêcher une Société de faire passer les données
    d'une autre société sous son propre abonnement en remplaçant l'Entreprise suivie.
  - **Formule à 1 Entreprise, hybride** : pendant toute période où l'abonnement actif de la
    Société a l'option hybride, la Société peut remplacer entièrement son Entreprise —
    **y compris le nom et le code** (changement réel de société cliente suivie), pas seulement
    les données secondaires (dirigeants, banques, etc.) — ou tout supprimer, librement. Hybride
    est donc la contrepartie payante de cette souplesse, pas une simple option de nettoyage.
    Si la Société renouvelle ensuite sous une formule sans hybride, le verrou "toute la durée de
    vie" ci-dessus s'applique à partir de ce moment, sur l'Entreprise alors suivie — hybride
    n'a pas d'effet rétroactif sur une période d'abonnement antérieure ou future sans hybride.
    À l'expiration de la période hybride (comportement exact — blocage, lecture seule, autre —
    laissé au builder, voir §8).
  - **Formule à plusieurs Entreprises (5 à 10)** : hybride disponible en option, avec un
    supplément de prix.
- Suppression/export des données d'une Entreprise (à la demande de la Société, pas d'un
  déclencheur réglementaire) :
  - Export des données de l'Entreprise (dirigeants, banques, actionnaires, activités, gérants,
    substitut) à tout moment.
  - Suppression totale de ces données, à l'exception du nom et du code de l'Entreprise (qui
    restent comme identifiant résiduel).
  - Suppression, séparément, de tous les états financiers de la Société (fichiers + lignes).
    Les balances brutes ne sont jamais stockées ailleurs que dans ces fichiers — les supprimer
    élimine donc aussi les balances.

## 4. Non-objectifs (hors de ce chantier)

- Traitement réel du paiement (Paytech), facturation, renouvellement automatique.
- Chiffrement au repos des champs/fichiers sensibles — option de durcissement future,
  explicitement non bloquante pour ce chantier (voir §5, décision stockage).
- Création ou mise à niveau d'abonnement en libre-service par la Société elle-même.
- Restriction fine d'un utilisateur à certaines Entreprises précises à l'intérieur de sa
  Société : un `COLLABORATEUR` voit toutes les Entreprises de sa Société, pas un sous-ensemble.
- Le refactor plus large des classes volumineuses historiques (`ExcelService`,
  `EtatFinancierRestController`, simplification de l'import de balance) décrit dans l'ancien
  document — hors scope ici, à traiter séparément si besoin.

## 5. Anti-objectifs

*Hypothèse non confirmée par le porteur du projet — posée par défaut, à corriger si besoin* :
- Une Société qui peut voir ou modifier les données d'une autre Société (fuite entre tenants).
- Un utilisateur qui obtient, par quelque chemin que ce soit, un rôle supérieur à celui de la
  personne qui l'a créé (contournement de la hiérarchie de délégation).

## 6. Décisions actées

### 6.1 Stockage : relationnel, pas de JSON

Décision explicite : on garde les tables relationnelles existantes (`Entreprise` et ses
entités liées) plutôt que de migrer vers des fichiers JSON de configuration.

Raisons : l'export et la suppression ne demandent pas de JSON — un export est juste une lecture
sérialisée, une suppression "sauf nom et code" est une suppression ciblée sur les tables
enfants. `Entreprise.java` a déjà `cascade = CascadeType.ALL, orphanRemoval = true` sur ses 5
listes enfants, donc la cascade de suppression est déjà configurée. Passer en JSON casserait les
écrans CRUD existants et la configuration des modèles Excel (qui mappe des champs précis vers
des cellules). Si le besoin réel est de protéger les données contre un accès brut à la base, la
bonne réponse est un chiffrement ciblé des champs/fichiers sensibles plus tard — pas un
changement de modèle de données maintenant.

### 6.2 Rôles et rattachement : une seule table, pas de niveau intermédiaire

Un rôle Keycloak (`COLLABORATEUR`, `INVITE`, `ADMIN_COMPTE`, `ADMIN_PLATEFORME`) est rattaché
au user, pas à une Société — mais comme chaque utilisateur (hors `ADMIN_PLATEFORME`) n'appartient
qu'à une seule Société, le rôle Keycloak de l'utilisateur *est* son rôle dans cette Société, sans
ambiguïté possible.

Décision : `user_societe` **duplique quand même** le rôle (colonne `role`), pour pouvoir
répondre localement à "qui est admin de telle Société" sans appeler l'API Keycloak à chaque
requête. Keycloak reste la source de vérité pour l'authentification ; `user_societe.role` est
une copie locale synchronisée à chaque changement de rôle — synchronisation possible parce que
tout changement de rôle passe déjà par notre API (`AdminUserController`/`KeycloakUserService`).

### 6.3 Entités (nouvelles)

- **`Societe`** : `id`, `nom`, `code`, `active`, `createAt`, `updateAt`.
- **`UserSociete`** : `id`, `keycloakUserId` (String, le `sub` du JWT), `societeId` (FK →
  `Societe`), `role` (String, copie synchronisée du rôle Keycloak), `createAt`, `updateAt`.
  Contrainte d'unicité sur `keycloakUserId`.
- **`AbonnementPlan`** (catalogue, configuré par `ADMIN_PLATEFORME`) : `id`, `nom`,
  `entrepriseQuota` (int), `hybrideInclus` (boolean), `hybrideDisponibleEnOption` (boolean),
  `hybrideSupplementPrix` (decimal, si option), `prixParMois` (decimal), `active` (boolean).
- **`Abonnement`** (souscription d'une Société à un plan) : `id`, `societeId` (FK), `planId`
  (FK → `AbonnementPlan`), `nbMois` (int, choisi par la Société), `hybride` (boolean — vaut
  `hybrideInclus` du plan, ou le choix de la Société si l'option est payante), `dateDebut`,
  `dateFin`, `active` (boolean).
- **`Entreprise`** (existante) : ajoute `societeId` (FK → `Societe`). Le verrouillage "pas de
  remplacement" (§3) n'a pas besoin d'un champ dédié : il se dérive à la lecture de
  `abonnement.hybride` de l'abonnement actif de la Société au moment de la tentative de
  remplacement — hybride actif autorise, sinon la Société a déjà une Entreprise et la demande
  est refusée.
- **`EtatFinancier`** (existante) : ajoute une vraie relation `@ManyToOne Entreprise entreprise`
  (en plus de l'existant `@Embedded EntrepriseDatasSubstitute entreprise`, conservé tel quel).
  **Constat vérifié dans le code actuel** : le champ `entreprise` d'`EtatFinancier` n'est
  aujourd'hui qu'un embarqué (`EntrepriseDatasSubstitute`, une copie figée au moment de la
  génération, utile pour ne pas réécrire l'historique si l'Entreprise change ensuite) — il n'y a
  aucune vraie clé étrangère vers `Entreprise`. Le filtrage par Société a besoin d'une jointure
  réelle, que l'embarqué ne permet pas ; d'où l'ajout de cette relation, qui coexiste avec
  l'embarqué (chacun son rôle : la relation pour le filtrage/la sécurité, l'embarqué pour le
  contenu figé du document).
- Entités enfants existantes (`DirigentEntreprise`, `BanqueEntreprise`, `ActionaireEntreprise`,
  `ActiviteEntreprise`, `GerantEntreprise`) : inchangées, toujours rattachées via
  `entreprise_id` ; leur appartenance à une Société se vérifie par jointure sur l'`Entreprise`
  possédante (déjà consolidé dans `AbstractEntrepriseChildService`/`AbstractEntrepriseChildController`,
  voir §6.5).

### 6.4 Services / contrôleurs

- **`SocieteService`/`SocieteRestController`** (`ADMIN_PLATEFORME` uniquement) : créer une
  Société (+ son premier `ADMIN_COMPTE` + son abonnement), lister, consulter, suspendre/activer.
- **`UserSocieteService`** : résoudre la Société d'un utilisateur courant à partir de son JWT ;
  lister les membres d'une Société. Branché sur le flux existant de création de collaborateur
  (`KeycloakUserService.createCollaborator`) pour : (a) poser la ligne `user_societe` du
  nouvel utilisateur avec la Société de l'appelant, et (b) faire respecter la règle "un rôle ne
  peut être attribué que si inférieur ou égal à celui de l'appelant" — remplace la règle
  actuelle, trop restrictive, qui limite `POST /admin/users` à `ADMIN_PLATEFORME` seul (posée
  comme mesure provisoire lors de la revue finale du chantier Keycloak, en attendant précisément
  cette conception).
- **`AbonnementPlanService`/`AbonnementPlanRestController`** (`ADMIN_PLATEFORME`) : CRUD sur le
  catalogue de formules.
- **`AbonnementService`/`AbonnementRestController`** (`ADMIN_PLATEFORME`) : activer/renouveler
  l'abonnement d'une Société ; expose aussi les vérifications de quota et de droit "hybride"
  utilisées par `EntrepriseService` au moment de créer/remplacer/supprimer une Entreprise.
- **`EntrepriseService`** (existant, à étendre) : vérifier l'appartenance à la Société de
  l'appelant sur toute lecture/écriture ; appliquer la règle de quota et de verrouillage/hybride
  à la création d'une nouvelle Entreprise ; nouvelles opérations "exporter" et "supprimer sauf
  nom et code".
- **`EtatFinancierService`** (existant, à étendre) : filtrer par Société de l'appelant (via la
  nouvelle relation `entreprise`, §6.3) ; nouvelle opération "supprimer tous les états financiers
  de la Société".

### 6.5 Où s'applique le filtrage par Société

Le filtrage "cet enregistrement appartient-il à la Société de l'appelant" doit s'écrire une
seule fois, pas une fois par entité — même logique que le refactor déjà fait sur les 5 entités
enfants d'Entreprise (`AbstractEntrepriseChildService`). Le builder décide du point d'accroche
exact (intercepteur, aspect, ou vérification explicite dans chaque service), mais ne doit pas la
dupliquer service par service.

### 6.6 Précisions issues de la relecture technique (choix du rédacteur, sans conséquence de conception)

- **Qui peut exporter/supprimer les données d'une Entreprise (§3)** : réservé à `ADMIN_COMPTE`,
  jamais `COLLABORATEUR`/`INVITE` — cohérent avec le reste du document, où toutes les actions
  administratives sur les Entreprises (ajout d'utilisateur, enregistrement d'Entreprise,
  création d'état financier) sont déjà posées comme des actions d'`ADMIN_COMPTE`. `COLLABORATEUR`
  reste un rôle d'usage courant, pas un rôle d'administration ; `INVITE` est lecture seule.
- **Format de l'export des données d'une Entreprise (§3)** : un export structuré (JSON — un
  objet par Entreprise avec ses 5 collections enfants), pas une réutilisation du pipeline
  PDF/Excel des états financiers. Ce pipeline est spécifique à la génération de documents à
  partir d'un modèle Excel configuré ; l'export dont il est question ici est un simple
  déversement des données brutes de l'Entreprise, pas un document mis en forme.

## 7. Contraintes

- S'appuie sur l'intégration Keycloak déjà faite (branche `keycloak-backend`) : client
  `plural_etatfi`, rôles `COLLABORATEUR`/`INVITE`/`ADMIN_COMPTE`/`ADMIN_PLATEFORME`,
  `KeycloakClientRoleConverter`, endpoints `/admin/users` (création, liste, liste des rôles,
  copie d'accès) actuellement tous réservés à `ADMIN_PLATEFORME` par mesure provisoire.
- Les 5 entités enfants d'Entreprise viennent d'être consolidées sur une base commune
  (`AbstractEntrepriseChildService`/`AbstractEntrepriseChildController`,
  `EntrepriseChildRepository`) — toute nouvelle vérification de Société doit s'y greffer, pas
  recréer une duplication.
- Stack inchangée : Spring Boot / Java 21, JPA/Hibernate, QueryDSL, MySQL.
- Convention déjà en place dans le projet pour les nouvelles entités : une interface de service
  par entité + une impl + repository + mapper + controller.
- Convention de code décidée dans cette même conversation : tout `record` Java va dans un
  dossier `api/records/`, au même niveau que `api/models/` pour les DTOs — pas de record
  imbriqué dans une interface ou un contrôleur.

## 8. Ce qui reste au builder

- Stratégie de migration pour les `Entreprise` déjà existantes sans `societe_id` (créer une
  Société "legacy" par défaut, ou autre — à trancher selon qu'il y a des données à préserver en
  pratique).
- Comportement exact à l'expiration d'un abonnement (hybride ou non) : lecture seule, blocage
  total, ou autre — non précisé par le porteur du projet au-delà de "il faudra qu'il paie à
  nouveau".
- Détail du point d'accroche technique du filtrage par Société (§6.5) : intercepteur Spring,
  aspect, ou vérification explicite — à choisir selon ce qui s'intègre le mieux avec
  `SecurityConfig` et les contrôleurs existants.
- Mécanisme exact de synchronisation de `user_societe.role` avec le rôle Keycloak (où
  l'écriture a lieu, gestion des échecs partiels entre l'appel Keycloak et l'écriture locale).
- Schéma de migration (le projet n'a pas d'outil de migration versionnée aujourd'hui — à
  confirmer/ajouter si on veut des migrations traçables plutôt qu'un simple
  `ddl-auto: update`).
- Plan de tests : couverture a minima du filtrage par Société (isolation entre tenants, le
  risque le plus sensible identifié) et de la hiérarchie de délégation de rôles.
