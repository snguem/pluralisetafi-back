# Multi-société Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce a Société (tenant) layer above the existing Entreprise model, so multiple
client organizations can use the same PluralEtatFin instance with isolated users, isolated
Entreprises, and subscription-based (Abonnement) access limits, without seeing each other's
data.

**Architecture:** Two new top-level entities (`Societe`, `UserSociete`) resolve "which tenant is
this request for," reusing the existing Keycloak client roles
(`COLLABORATEUR`/`INVITE`/`ADMIN_COMPTE`/`ADMIN_PLATEFORME`) for *what kind* of user this is.
`AbonnementPlan`/`Abonnement` carry the subscription data (quota, hybride flag, dates) that
gates Entreprise creation/replacement. `Entreprise` gets a `societe` relation; `EtatFinancier`
gets a *new*, separate, real `@ManyToOne` relation to `Entreprise` (named `entrepriseRef`, to
avoid colliding with its existing `@Embedded EntrepriseDatasSubstitute entreprise` field) so
société-ownership can be checked via a real join instead of the frozen embedded snapshot.

**Tech Stack:** Spring Boot 3.5.6 / Java 21, Spring Data JPA + QueryDSL, MySQL, Lombok, MapStruct
(existing project stack — no new dependencies).

**Spec:** `docs/superpowers/specs/2026-10-28-multi-societe-design.md`

## Global Constraints

- Every Keycloak user except `ADMIN_PLATEFORME` belongs to exactly one `Societe` (unique
  constraint on `UserSociete.keycloakUserId`).
- A creator can only grant a role ≤ their own role. Role order (highest to lowest):
  `ADMIN_PLATEFORME` > `ADMIN_COMPTE` > `COLLABORATEUR` = `INVITE`.
- Storage stays relational — no JSON blob, no new encryption in this plan (explicitly deferred
  by the spec).
- `UserSociete.role` is a synced copy of the Keycloak role, written by our own code at the same
  time the Keycloak role is granted — never read from Keycloak on the hot path.
- A Société on a non-hybride plan is locked to its first-registered Entreprise for the
  Société's lifetime; the lock is derived from the *current* active Abonnement's `hybride` flag
  at write time, not stored as a separate flag (per spec §6.3).
- Export and "delete except nom/code" operations on an Entreprise are `ADMIN_COMPTE`-only (spec
  §6.6). Export format is a JSON dump of the Entreprise + its 5 child collections, not the
  PDF/Excel pipeline.
- New `record` types go in `api/records/` (new package), never nested in a controller or
  interface — project convention agreed for this effort.
- `SecurityConfig.java`, `KeycloakAdminClientConfig.java`, and `KeycloakClientRoleConverter.java`
  may have uncommitted changes from outside this plan's execution at any time (the repo owner
  edits them directly). Any task touching `SecurityConfig.java` must re-read its current content
  immediately before editing — never assume it still matches what an earlier task or this plan
  saw — and must only *add* a new `.requestMatchers(...)` rule, never modify or reorder the
  existing ones.

## Review Focus

- Cross-société leak: a `COLLABORATEUR` of Société A requests an Entreprise (or État Financier)
  belonging to Société B by guessing its id — must get a 404/empty result, never the data.
- Role escalation: an `ADMIN_COMPTE` attempts to create a user with role `ADMIN_COMPTE` or
  `ADMIN_PLATEFORME` via the new société-scoped creation endpoint — must be rejected.
- Quota ceiling: a Société whose plan allows only 1 Entreprise attempts to register a second
  one — must be rejected, with a clear reason distinguishing "quota reached" from "locked,
  no hybride".
- Lock persistence across renewal: a Société that registered its one Entreprise under a
  non-hybride plan, then renews under another non-hybride plan (never having had hybride),
  attempts to replace its Entreprise — must still be rejected (the lock is not reset by
  renewal).
- Stale-snapshot filtering: querying États Financiers "by entreprise" or "by société" must use
  the new `entrepriseRef` relation, not the embedded `EntrepriseDatasSubstitute` copy — proven
  by a test where the embedded snapshot's `id_` and the real `entrepriseRef.id` could in
  principle disagree (e.g. after the historical "wipe except nom/code" operation) and the
  société-scoped query must still return the correct, current ownership answer.

---

## Task 1: `Societe` + `UserSociete` — tenant and membership

**Files:**
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/Societe.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/UserSociete.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/SocieteRepository.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/UserSocieteRepository.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/models/SocieteDto.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/models/UserSocieteDto.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/mappers/SocieteMapper.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/mappers/UserSocieteMapper.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/SocieteService.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/UserSocieteService.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/SocieteServiceImpl.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/UserSocieteServiceImpl.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/SocieteRestController.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/exceptions/SocieteMembershipNotFoundException.java`
- Test: `src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/SocieteServiceImplTest.java`

**Interfaces:**
- Produces:
  ```java
  public interface SocieteService {
      SocieteDto create(String nom, String code, String adminUsername, String adminEmail,
                         String adminFirstName, String adminLastName, String adminTemporaryPassword);
      SocieteDto get(Long id);
      Page<SocieteDto> getAll(Map<String, String> searchParams, Pageable pageable);
  }

  public interface UserSocieteService {
      UserSociete attach(String keycloakUserId, Long societeId, String role); // no hierarchy check — trusted internal call
      UserSocieteDto resolveByKeycloakUserId(String keycloakUserId) throws SocieteMembershipNotFoundException;
      UserSocieteDto resolveCurrent(Authentication authentication) throws SocieteMembershipNotFoundException;
      List<UserSocieteDto> listMembers(Long societeId);
  }
  ```
  `resolveCurrent(authentication)` extracts the JWT `sub` the same way `MeRestController.java`
  already does, then calls `resolveByKeycloakUserId`. **Every controller added or modified from
  Task 3 onward must call `resolveCurrent(authentication)` to get the caller's société/role —
  never re-extract the JWT `sub` inline in a controller.** This is the single shared filtering
  hook the spec requires (§6.5) instead of duplicating "resolve the caller's société" in each of
  `SocieteUserRestController`, `EntrepriseRestController`, and `EtatFinancierRestController`.
  `SocieteService.create(...)` calls `keycloakUserService.createCollaborator(adminUsername, adminEmail, adminFirstName, adminLastName, adminTemporaryPassword, "ADMIN_COMPTE")` (existing method, signature confirmed in `services/interfaces/KeycloakUserService.java`) to create the société's first admin in Keycloak, then `userSocieteService.attach(newUserId, societe.getId(), "ADMIN_COMPTE")` to link it.
- Consumes: `KeycloakUserService.createCollaborator(String, String, String, String, String, String)` (existing, returns the new user's Keycloak id, throws `KeycloakUserAlreadyExistsException`).

- [ ] **Step 1: Write the failing test for `Societe` creation + admin attachment**

```java
@Test
void create_createsSocieteAndAttachesAdminAsUserSociete() {
    var dto = societeService.create("Cabinet Fatou", "CAB-001",
        "admin.cabinet", "admin@cabinet.sn", "Admin", "Cabinet", "Temp1234!");

    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getNom()).isEqualTo("Cabinet Fatou");

    var membership = userSocieteService.resolveByKeycloakUserId(/* the id returned by the stubbed/real Keycloak call */);
    assertThat(membership.getSocieteId()).isEqualTo(dto.getId());
    assertThat(membership.getRole()).isEqualTo("ADMIN_COMPTE");
}
```
Use the existing `KeycloakTestSupport.KEYCLOAK` singleton container pattern (see
`src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/KeycloakUserServiceImplTest.java`
for the exact setup) to get a real `Keycloak` admin client for `KeycloakUserServiceImpl`, wired
into a real `SocieteServiceImpl`. Do not mock Keycloak.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=SocieteServiceImplTest test`
Expected: FAIL (classes don't exist yet)

- [ ] **Step 3: Implement the entities, repositories, mapper, service, controller**

`Societe`: `id`, `nom` (String, not null), `code` (String, not null, unique), `active`
(Boolean, default true), `createAt`/`updateAt` (same `@CreationTimestamp`/`@UpdateTimestamp`
pattern as `Entreprise.java`).

`UserSociete`: `id`, `keycloakUserId` (String, not null, `@Column(unique = true)`), `societeId`
(Long, not null — plain FK column, not a `@ManyToOne`, since nothing currently needs to
navigate from `UserSociete` to a loaded `Societe` object), `role` (String, not null),
`createAt`/`updateAt`.

`SocieteRestController`: `@RequestMapping("societe")`, gated by `SecurityConfig`'s existing
`.requestMatchers("/admin/**").hasRole("ADMIN_PLATEFORME")` is NOT reusable here (different
path) — add `POST /admin/societes`, `GET /admin/societes/{id}`, `GET /admin/societes/all`
instead, under the existing `/admin/**` prefix so no new `SecurityConfig` rule is needed for
this task (defer the new `/societe/**` member-facing prefix to Task 3). Follow the existing
`Response<Object>` + try/catch pattern from `AdminUserController.java`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=SocieteServiceImplTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/Societe.java src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/UserSociete.java src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/SocieteRepository.java src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/UserSocieteRepository.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/models/SocieteDto.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/models/UserSocieteDto.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/mappers/SocieteMapper.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/mappers/UserSocieteMapper.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/SocieteService.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/UserSocieteService.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/SocieteServiceImpl.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/UserSocieteServiceImpl.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/SocieteRestController.java src/main/java/pluralisconseil/sn/pluralisEtatFin/exceptions/SocieteMembershipNotFoundException.java src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/SocieteServiceImplTest.java
git commit -m "feat: add Societe and UserSociete, admin-created societe with its first admin"
```

---

## Task 2: `AbonnementPlan` + `Abonnement` — subscription catalog and activation

**Files:**
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/AbonnementPlan.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/Abonnement.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/AbonnementPlanRepository.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/AbonnementRepository.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/models/AbonnementPlanDto.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/models/AbonnementDto.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/mappers/AbonnementPlanMapper.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/mappers/AbonnementMapper.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/AbonnementPlanService.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/AbonnementService.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/AbonnementPlanServiceImpl.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/AbonnementServiceImpl.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/AbonnementPlanRestController.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/AbonnementRestController.java`
- Test: `src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/AbonnementServiceImplTest.java`

**Interfaces:**
- Consumes: `Societe` (Task 1), `SocieteRepository` (Task 1, to verify a société exists before
  activating an abonnement for it).
- Produces:
  ```java
  public interface AbonnementService {
      AbonnementDto activate(Long societeId, Long planId, int nbMois, LocalDate dateDebut);
      Abonnement getActiveEntity(Long societeId); // null if none active — used by Task 4
  }
  ```
  `Abonnement.hybride` is copied from `AbonnementPlan.hybrideInclus` at activation time, unless
  the plan has `hybrideDisponibleEnOption = true`, in which case `activate(...)` takes the
  caller's explicit choice — add a `boolean hybrideChoisi` parameter to `activate(...)` for that
  case (ignored, always `false`, when the plan doesn't offer the option; required `true` when
  the plan has `hybrideInclus = true`, since it's not optional then).

- [ ] **Step 1: Write the failing test for plan quota/hybride resolution**

```java
@Test
void activate_setsHybrideFromPlanWhenIncluded() {
    var plan = planRepository.save(AbonnementPlan.builder().nom("SOLO_HYBRIDE")
        .entrepriseQuota(1).hybrideInclus(true).hybrideDisponibleEnOption(false)
        .prixParMois(new BigDecimal("50000")).active(true).build());
    var societe = societeRepository.save(Societe.builder().nom("X").code("X-1").active(true).build());

    var dto = abonnementService.activate(societe.getId(), plan.getId(), 1, LocalDate.now(), false);

    assertThat(dto.isHybride()).isTrue();
    assertThat(dto.getDateFin()).isEqualTo(LocalDate.now().plusMonths(1));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=AbonnementServiceImplTest test`
Expected: FAIL (classes don't exist yet)

- [ ] **Step 3: Implement the entities, repositories, mappers, services, controllers**

`AbonnementPlan`: `id`, `nom`, `entrepriseQuota` (int), `hybrideInclus` (boolean),
`hybrideDisponibleEnOption` (boolean), `hybrideSupplementPrix` (BigDecimal, nullable),
`prixParMois` (BigDecimal), `active` (boolean).

`Abonnement`: `id`, `societeId` (Long, plain FK column, same reasoning as `UserSociete`),
`planId` (Long, plain FK column), `nbMois` (int), `hybride` (boolean), `dateDebut` (LocalDate),
`dateFin` (LocalDate, computed as `dateDebut.plusMonths(nbMois)`), `active` (boolean).

`AbonnementPlanRestController`/`AbonnementRestController`: under `/admin/**`
(`ADMIN_PLATEFORME`-only, same reasoning as Task 1's `SocieteRestController` — no new
`SecurityConfig` rule needed).

`AbonnementService.getActiveEntity(societeId)`: `abonnementRepository.findBySocieteIdAndActiveTrue(societeId)`
(add this derived-query method to `AbonnementRepository`), returning the entity (not DTO) for
internal use by Task 4/5.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=AbonnementServiceImplTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/AbonnementPlan.java src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/Abonnement.java src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/AbonnementPlanRepository.java src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/AbonnementRepository.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/models/AbonnementPlanDto.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/models/AbonnementDto.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/mappers/AbonnementPlanMapper.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/mappers/AbonnementMapper.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/AbonnementPlanService.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/AbonnementService.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/AbonnementPlanServiceImpl.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/AbonnementServiceImpl.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/AbonnementPlanRestController.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/AbonnementRestController.java src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/AbonnementServiceImplTest.java
git commit -m "feat: add AbonnementPlan catalog and per-societe Abonnement activation"
```

---

## Task 3: Role-hierarchy-aware société member creation

**Files:**
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/UserSocieteService.java` (Task 1)
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/UserSocieteServiceImpl.java` (Task 1)
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/exceptions/InsufficientRoleException.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/records/CreateSocieteUserRequest.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/SocieteUserRestController.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/config/SecurityConfig.java` (re-read
  current content first — see Global Constraints)
- Test: `src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/UserSocieteServiceImplTest.java`

**Interfaces:**
- Consumes: `UserSocieteService.resolveByKeycloakUserId` (Task 1), `KeycloakUserService.createCollaborator` (existing).
- Produces:
  ```java
  public interface UserSocieteService {
      // ...Task 1 methods...
      UserSocieteDto createMember(String callerKeycloakUserId, String username, String email,
                                  String firstName, String lastName, String temporaryPassword,
                                  String requestedRole) throws InsufficientRoleException;
  }
  ```

- [ ] **Step 1: Write the failing tests for the role hierarchy**

```java
@Test
void createMember_whenCallerIsAdminCompte_canCreateCollaborateur() {
    var dto = userSocieteService.createMember(adminCompteKeycloakId, "nouveau.collab",
        "nc@x.sn", "Nouveau", "Collab", "Temp1234!", "COLLABORATEUR");
    assertThat(dto.getRole()).isEqualTo("COLLABORATEUR");
    assertThat(dto.getSocieteId()).isEqualTo(adminCompteSocieteId);
}

@Test
void createMember_whenCallerIsAdminCompte_cannotCreateAdminCompte() {
    assertThatThrownBy(() -> userSocieteService.createMember(adminCompteKeycloakId, "x", "x@x.sn",
        "X", "X", "Temp1234!", "ADMIN_COMPTE"))
        .isInstanceOf(InsufficientRoleException.class);
}
```
Reuse `KeycloakTestSupport.KEYCLOAK`; seed an `ADMIN_COMPTE` test user the same way
`KeycloakUserServiceImplTest` already does.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -Dtest=UserSocieteServiceImplTest test`
Expected: FAIL (`createMember` doesn't exist)

- [ ] **Step 3: Implement `createMember` and the role-hierarchy check**

Role order constant (highest to lowest): `ADMIN_PLATEFORME`, `ADMIN_COMPTE`, `COLLABORATEUR`,
`INVITE` (`COLLABORATEUR` and `INVITE` are equal rank — a caller with either can only create
the other or themselves-rank, never `ADMIN_COMPTE`). `createMember` resolves the caller's
`UserSociete` row, rejects if caller's role isn't `ADMIN_COMPTE` (only an `ADMIN_COMPTE` calls
this method — `ADMIN_PLATEFORME` creates société admins via `SocieteService.create`, Task 1),
rejects if `requestedRole` is `ADMIN_COMPTE` or `ADMIN_PLATEFORME`, otherwise calls
`keycloakUserService.createCollaborator(...)` then `attach(newUserId, caller's societeId, requestedRole)`.

`SocieteUserRestController`: `@RequestMapping("societe/users")`, `POST` (create, resolves the
caller via `userSocieteService.resolveCurrent(authentication)` to get their Keycloak id, then
delegates to `createMember`) and `GET` (delegates to `listMembers` using the société from the
same `resolveCurrent(authentication)` call). Maps `InsufficientRoleException` to
`Response.unauthorized()`.

In `SecurityConfig.java`: **re-read the file fresh before editing.** Add exactly one new line —
`.requestMatchers("/societe/**").hasRole("ADMIN_COMPTE")` — placed before `.anyRequest()`,
alongside (not replacing) the existing `/admin/**` rule. Do not touch any other line.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -Dtest=UserSocieteServiceImplTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/UserSocieteService.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/UserSocieteServiceImpl.java src/main/java/pluralisconseil/sn/pluralisEtatFin/exceptions/InsufficientRoleException.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/records/CreateSocieteUserRequest.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/SocieteUserRestController.java src/main/java/pluralisconseil/sn/pluralisEtatFin/config/SecurityConfig.java src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/UserSocieteServiceImplTest.java
git commit -m "feat: let ADMIN_COMPTE create societe-scoped collaborators within role hierarchy"
```

---

## Task 4: `Entreprise` société-scoping, quota, and lock/hybride enforcement

**Files:**
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/Entreprise.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/EntrepriseRepository.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/EntrepriseService.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EntrepriseServiceImpl.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/EntrepriseRestController.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/exceptions/EntrepriseQuotaExceededException.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/exceptions/EntrepriseLockedException.java`
- Test: `src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EntrepriseServiceImplTest.java`

**Interfaces:**
- Consumes: `AbonnementService.getActiveEntity(Long societeId)` (Task 2, returns `Abonnement`
  with `.getHybride()` and `.getPlanId()` → load `AbonnementPlan.getEntrepriseQuota()`),
  `UserSocieteService.resolveByKeycloakUserId` (Task 1).
- Produces:
  ```java
  public interface EntrepriseService extends Service<EntrepriseDto> {
      EntrepriseDto getName(String name);
      Entreprise getEntity(Long id);
      EntrepriseDto create(EntrepriseDto dto, Long societeId) throws EntrepriseQuotaExceededException, EntrepriseLockedException;
      Page<EntrepriseDto> getAllBySociete(Long societeId, Map<String, String> searchParams, Pageable pageable);
  }
  ```
  The existing no-arg `create(EntrepriseDto dto)` from `Service<EntrepriseDto>` stays for
  internal/test callers that don't need société checks; the controller only ever calls the new
  `create(dto, societeId)` overload.

- [ ] **Step 1: Write the failing tests for quota and lock**

```java
@Test
void create_secondEntreprise_onSoloNonHybridePlan_throwsQuotaExceeded() {
    // societe has an active Abonnement on a plan with entrepriseQuota=1, hybride=false,
    // and already has one Entreprise.
    assertThatThrownBy(() -> entrepriseService.create(new EntrepriseDto(), societeId))
        .isInstanceOf(EntrepriseQuotaExceededException.class);
}

@Test
void getAllBySociete_doesNotReturnOtherSocietesEntreprises() {
    var page = entrepriseService.getAllBySociete(societeA.getId(), Map.of(), Pageable.unpaged());
    assertThat(page.getContent()).extracting(EntrepriseDto::getId).doesNotContain(entrepriseOfSocieteB.getId());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -Dtest=EntrepriseServiceImplTest test`
Expected: FAIL

- [ ] **Step 3: Implement the `societe` relation and the checks**

`Entreprise.java`: add `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "societe_id") private Societe societe;`
(same style as the existing children's `@JoinColumn(name = "entreprise_id")`).

`EntrepriseServiceImpl.create(dto, societeId)`: count existing Entreprises for `societeId`
(`entrepriseRepository.countBySociete_Id(societeId)` — add this derived method to
`EntrepriseRepository`). If count `>= quota`: throw `EntrepriseQuotaExceededException`. If
count `== quota` is impossible to reach for a quota-1 plan without the count check already
catching it, so the separate "locked" case is: count `>= 1` AND quota `== 1` AND the active
abonnement's `hybride` is `false` → throw `EntrepriseLockedException` (distinct exception from
quota, per Global Constraints — the UI needs to tell these apart). Otherwise delegate to the
existing `create(EntrepriseDto)` logic, then set `entity.setSociete(...)`.

`EntrepriseRestController`: every method resolves the caller via
`userSocieteService.resolveCurrent(authentication)` (never re-extract the JWT `sub` inline, per
Task 1's note). `createEntreprise` calls the new `create(dto, societeId)` overload with the
resolved société id; `getAllEntreprises` calls `getAllBySociete`; `getEntreprise`/
`updateEntreprise`/`deleteEntreprise` reject with `Response.unauthorized()` if the fetched
Entreprise's `societeId` doesn't match the caller's.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -Dtest=EntrepriseServiceImplTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/Entreprise.java src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/EntrepriseRepository.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/EntrepriseService.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EntrepriseServiceImpl.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/EntrepriseRestController.java src/main/java/pluralisconseil/sn/pluralisEtatFin/exceptions/EntrepriseQuotaExceededException.java src/main/java/pluralisconseil/sn/pluralisEtatFin/exceptions/EntrepriseLockedException.java src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EntrepriseServiceImplTest.java
git commit -m "feat: scope Entreprise by societe and enforce quota/lock on creation"
```

---

## Task 5: Entreprise export and "delete except nom/code"

**Files:**
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/EntrepriseService.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EntrepriseServiceImpl.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/EntrepriseRestController.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/records/EntrepriseExportDto.java`
- Test: `src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EntrepriseServiceImplTest.java` (extend)

**Interfaces:**
- Consumes: `DirigeantEntrepriseService.getListByEntreprise`, `BanqueEntrepriseService.getListByEntreprise`,
  `ActionaireEntrepriseService.getListByEntreprise`, `ActiviteEntrepriseService.getListByEntreprise`,
  `GerantEntrepriseService.getListByEntreprise` (all existing, from `EntrepriseChildService<D>`,
  confirmed signature `List<D> getListByEntreprise(Long entrepriseId)`).
- Produces:
  ```java
  public record EntrepriseExportDto(
      EntrepriseDto entreprise,
      List<DirigeantEntrepriseDto> dirigeants,
      List<BanqueEntrepriseDto> banques,
      List<ActionaireEntrepriseDto> actionaires,
      List<ActiviteEntrepriseDto> activites,
      List<GerantEntrepriseDto> gerants
  ) {}

  // on EntrepriseService:
  EntrepriseExportDto export(Long entrepriseId);
  EntrepriseDto wipeExceptIdentity(Long entrepriseId);
  ```

- [ ] **Step 1: Write the failing tests**

```java
@Test
void export_returnsEntrepriseWithAllChildCollections() {
    var export = entrepriseService.export(entrepriseId);
    assertThat(export.entreprise().getId()).isEqualTo(entrepriseId);
    assertThat(export.dirigeants()).hasSize(expectedDirigeantsCount);
}

@Test
void wipeExceptIdentity_keepsNameAndCode_deletesEverythingElse() {
    var before = entrepriseService.get(entrepriseId);
    var after = entrepriseService.wipeExceptIdentity(entrepriseId);

    assertThat(after.getSubstitute().getNameEntreprise()).isEqualTo(before.getSubstitute().getNameEntreprise());
    assertThat(after.getSubstitute().getCode_importeur()).isEqualTo(before.getSubstitute().getCode_importeur());
    assertThat(dirigeantService.getListByEntreprise(entrepriseId)).isEmpty();
    assertThat(banqueService.getListByEntreprise(entrepriseId)).isEmpty();
}
```
`code_importeur` is the field this plan treats as "le code" of the Entreprise (the only
company-level identifying code on `EntrepriseDatasSubstitute`; its other field literally named
`code` sits under a "boîte postale" comment and is a postal address component, not an identity
code) — this is the plan author's call, not pinned by the spec; flag it for the reviewer if it
turns out wrong once someone who knows the business meaning of these fields checks it.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -Dtest=EntrepriseServiceImplTest test`
Expected: FAIL

- [ ] **Step 3: Implement `export` and `wipeExceptIdentity`**

`export(entrepriseId)`: load the `Entreprise`, call each of the 5 child services'
`getListByEntreprise(entrepriseId)`, assemble the record.

`wipeExceptIdentity(entrepriseId)`: load the entity, clear its 5 `@OneToMany` collections
(JPA's `orphanRemoval = true` on `Entreprise`'s collections, already present, deletes the
orphaned rows on save when the collection is cleared and re-saved — no manual per-child delete
call needed), replace `substitute` with a new `EntrepriseDatasSubstitute` that copies only
`nameEntreprise` and `code_importeur` from the original and leaves every other field `null`,
save, return the DTO.

`EntrepriseRestController`: add `GET /entreprises/{id}/export` and
`DELETE /entreprises/{id}/wipe`, both resolving the caller via
`userSocieteService.resolveCurrent(authentication)`, checking the resolved role is
`ADMIN_COMPTE` (reject with `Response.unauthorized()` otherwise) and that the resolved société
owns the entreprise (same check as Task 4).

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -Dtest=EntrepriseServiceImplTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/EntrepriseService.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EntrepriseServiceImpl.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/EntrepriseRestController.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/records/EntrepriseExportDto.java src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EntrepriseServiceImplTest.java
git commit -m "feat: add Entreprise data export and wipe-except-identity for societe admins"
```

---

## Task 6: `EtatFinancier` société-scoping and bulk deletion

**Files:**
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/EtatFinancier.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/EtatFinancierRepository.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/EtatFinancierService.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EtatFinancierServiceImpl.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/EtatFinancierRestController.java`
- Test: `src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EtatFinancierServiceImplTest.java`

**Interfaces:**
- Consumes: `EntrepriseService.getEntity(Long id)` (existing), `UserSocieteService.resolveByKeycloakUserId` (Task 1).
- Produces:
  ```java
  public interface EtatFinancierService extends Service<EtatFinancierDto> {
      Page<EtatFinancierDto> getAllByEntreprise(long entreprise_id, Map<String, String> searchParams, Pageable pageable);
      Page<EtatFinancierDto> getAllBySociete(Long societeId, Map<String, String> searchParams, Pageable pageable);
      void deleteAllBySociete(Long societeId);
  }
  ```

- [ ] **Step 1: Write the failing tests**

```java
@Test
void getAllBySociete_usesRealRelation_notEmbeddedSnapshot() {
    // etatFinancier created with entrepriseRef pointing at an Entreprise of societeA;
    // its embedded EntrepriseDatasSubstitute.id_ is deliberately set to an id belonging to societeB
    // (simulating a stale snapshot after an entreprise was later wiped/reassigned).
    var pageA = etatFinancierService.getAllBySociete(societeA.getId(), Map.of(), Pageable.unpaged());
    var pageB = etatFinancierService.getAllBySociete(societeB.getId(), Map.of(), Pageable.unpaged());

    assertThat(pageA.getContent()).extracting(EtatFinancierDto::getId).contains(etatFinancierId);
    assertThat(pageB.getContent()).extracting(EtatFinancierDto::getId).doesNotContain(etatFinancierId);
}

@Test
void deleteAllBySociete_removesEveryEtatFinancierOfThatSociete_andNoOthers() {
    etatFinancierService.deleteAllBySociete(societeA.getId());
    assertThat(etatFinancierRepository.existsById(etatFinancierOfSocieteAId)).isFalse();
    assertThat(etatFinancierRepository.existsById(etatFinancierOfSocieteBId)).isTrue();
}
```

Also add an integration test (in `SecurityConfigIntegrationTest.java` or a focused new test
class, implementer's call, same reasoning as Task 3's step 3) asserting that a `COLLABORATEUR`
token of Société B gets 403/401 from `GET /etats-fin/{id}/pdf` (or whichever PDF-export path
this controller actually exposes) when `{id}` belongs to Société A.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -Dtest=EtatFinancierServiceImplTest test`
Expected: FAIL

- [ ] **Step 3: Implement the `entrepriseRef` relation and société-scoped queries**

`EtatFinancier.java`: add
`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "entreprise_ref_id") private Entreprise entrepriseRef;`
— named `entrepriseRef`, not `entreprise`, because the existing
`@Embedded EntrepriseDatasSubstitute entreprise` field already owns that name and must not be
renamed (it's referenced by QueryDSL paths like `QEtatFinancier.etatFinancier.entreprise.id_`
elsewhere — leave it exactly as is).

`EtatFinancierServiceImpl.create`: in addition to the existing mapping, set
`entity.setEntrepriseRef(entrepriseService.getEntity(dto.getEntrepriseId()))` (or whatever the
mapper already resolves the entreprise id from — check `EtatFinancierMapper.java`'s current
`@Mapping` for the embedded `entreprise.id_` and add a second one targeting `entrepriseRef.id`
sourced from the same DTO field).

`getAllBySociete(societeId, ...)`: QueryDSL predicate
`QEtatFinancier.etatFinancier.entrepriseRef.societe.id.eq(societeId)`.

`deleteAllBySociete(societeId)`: `etatFinancierRepository.deleteAll(etatFinancierRepository.findAll(predicate))`
using the same predicate (or a derived `deleteByEntrepriseRef_Societe_Id(Long societeId)` on
the repository — plan author's call, either is fine).

`EtatFinancierRestController`: add `DELETE /etats-fin/societe`, resolving the caller via
`userSocieteService.resolveCurrent(authentication)`, checking the resolved role is
`ADMIN_COMPTE` (same pattern as Task 5), calling `deleteAllBySociete`.

This controller (325 lines today) also already exposes several endpoints that take an
`EtatFinancier` id directly (get, update, delete, PDF export, Excel export, page-names,
correction, balance updates — enumerate every `@PathVariable` id-based method actually present
in the file, which this plan does not re-list in full). **Add a single private helper**,
`requireOwnership(Long etatFinancierId, Authentication authentication)` (throws, e.g.,
`Response.unauthorized()`-mapped exception, or returns normally if the caller's société owns
the entreprise behind that état financier via `entrepriseRef.societe.id`), and call it at the
top of every one of those existing id-based methods. This is the Review Focus item: a
`COLLABORATEUR` of Société B must not be able to download Société A's PDF/Excel just by
guessing an id.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -Dtest=EtatFinancierServiceImplTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/EtatFinancier.java src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/EtatFinancierRepository.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/EtatFinancierService.java src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EtatFinancierServiceImpl.java src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/EtatFinancierRestController.java src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EtatFinancierServiceImplTest.java
git commit -m "feat: add real Entreprise relation to EtatFinancier for societe-scoped queries"
```

---

## Task 7: Final regression

**Files:**
- Verify only.

- [ ] **Step 1: Run `mvn test` (full suite)**

Expected: all tests pass, including every test added in Tasks 1-6. Known, pre-existing,
environment-caused failures to disregard if seen again (not introduced by this plan):
`EtatFinAppBackApplicationTests.contextLoads` (needs a live local MySQL), and
`SecurityConfigIntegrationTest`'s tests (sandbox-specific embedded-Tomcat loopback-bind issue
seen repeatedly on this machine across the whole Keycloak effort).

- [ ] **Step 2: Confirm the role-hierarchy rule end to end**

Run a `SecurityConfigIntegrationTest`-style check (or extend that file) that an `ADMIN_COMPTE`
token gets a non-403 from `POST /societe/users` with `requestedRole=COLLABORATEUR`, and a 403
(mapped from `InsufficientRoleException`) with `requestedRole=ADMIN_COMPTE`.

- [ ] **Step 3: Confirm `SecurityConfig.java`'s existing rules are untouched beyond the one added line**

Run: `git diff <task-3-base-commit>..HEAD -- src/main/java/pluralisconseil/sn/pluralisEtatFin/config/SecurityConfig.java`
Expected: the diff shows exactly one added `.requestMatchers(...)` line; nothing else changed.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: final regression check for multi-societe"
```
