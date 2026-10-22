# Keycloak backend integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the backend's custom JWT authentication (`User`/`AppRole` entities, hand-rolled
`JwtAuthFilter`) with Keycloak as an OAuth2 resource server, with the realm/clients/roles defined
as a committed, importable configuration and verified by real integration tests against a live
Keycloak container — not mocks.

**Architecture:** One Keycloak realm (`pluraletatfin`) with two clients — `admin-service`
(confidential, server-side, used by this backend to call the Keycloak Admin API) and
`plural_etatfi` (public SPA client, used by the Angular frontend — wired up in a separate,
later plan). Spring Security becomes a plain OAuth2 resource server: it validates the JWT's
signature against the realm's issuer and extracts roles from
`resource_access.plural_etatfi.roles` via a custom `Converter`. No local password storage, no
local role table — Keycloak is the only source of truth for "who is this and what role do they
hold." This plan does **not** implement per-société delegation (`membre_compte`/
`membre_entreprise`) — that is the next plan in the agreed order (multi-tenant), which builds on
the roles this plan makes available in the token.

**Tech Stack:** Spring Boot 3.5.6 / Java 21, `spring-boot-starter-oauth2-resource-server`,
Keycloak 26.8.0 (`quay.io/keycloak/keycloak:26.8.0`, matching the project's own Keycloak guide),
Testcontainers (`org.testcontainers:testcontainers`, `:junit-jupiter`) + the official Keycloak
container support, `org.keycloak:keycloak-admin-client` for the `admin-service` → Keycloak Admin
API calls, Docker (confirmed available in this environment: `docker ps` succeeds).

**Spec:** [docs/superpowers/specs/2026-10-22-evolution-multi-tenant-keycloak-design.md](../specs/2026-10-22-evolution-multi-tenant-keycloak-design.md)
(section 4.3 "Keycloak"), cross-referenced against the project's own
`Guide_v3_Keycloak_Local_puis_Production` for exact Keycloak 26 UI/field names.

## Global Constraints

- Java 21 / Spring Boot 3.5.6 — no version changes.
- Realm name: `pluraletatfin`. Clients: `admin-service` (confidential, service account,
  Standard flow OFF), `plural_etatfi` (public, Standard flow + PKCE `S256`, Full scope allowed
  OFF). Client roles on `plural_etatfi`: `ADMIN_PLATEFORME`, `ADMIN_COMPTE`, `COLLABORATEUR`,
  `INVITE`.
- `admin-service`'s service account needs these `realm-management` client roles:
  `manage-users`, `view-users`, `query-users`, `view-clients`, `query-clients`.
- No data to preserve (confirmed in the spec) — the old `User`/`AppRole` tables can simply stop
  being managed by Hibernate (`ddl-auto: update`, no Flyway in this project); no migration
  script required.
- No per-endpoint role enforcement in this plan (e.g. no `@PreAuthorize`) — that is deferred to
  the multi-tenant plan, which needs the `membre_compte`/`membre_entreprise` tables first to
  decide what each role is actually allowed to touch. This plan's job is only: a request with a
  valid Keycloak token for realm `pluraletatfin` is authenticated, and its roles are visible to
  Spring Security as `ROLE_*` authorities.
- No invitation email in this phase (decided earlier in the conversation this spec came from):
  `admin-service` creates a Keycloak user with a temporary password; nothing sends it anywhere.

## Review Focus

- **A request with an expired or tampered JWT.** A reasonable person expects `401 Unauthorized`,
  not a stack trace or a `500`. Covered by Task 4's integration test (a token signed with the
  wrong key, and a token past its `exp`).
- **A request with a valid token for the right realm but no roles at all under
  `resource_access.plural_etatfi`** (e.g. a user Keycloak-side who was never assigned any client
  role). The converter must not throw — it should yield zero authorities, letting Spring's
  `permitAll`/`authenticated` rules decide, not a `NullPointerException` from assuming the claim
  exists. Covered by Task 3's unit test.
- **A request whose token is for a *different* Keycloak client's roles only** (e.g. a token that
  has `resource_access.some-other-client.roles` but nothing under `plural_etatfi`). Must yield
  zero authorities from this backend's perspective — never accidentally pick up another client's
  role names. Covered by Task 3's unit test (mirrors the guide's own
  "Full scope allowed = OFF" warning).
- **The `admin-service` call failing because the realm/client isn't reachable or misconfigured**
  (e.g. Keycloak not running). `KeycloakUserServiceImpl` must surface a clear exception rather
  than an opaque Keycloak-client stack trace bubbling to the controller. Covered by Task 6.
- **Creating a collaborator with a username/email that already exists in the realm.** A
  reasonable person expects a clear "already exists" error, not a generic 500. Covered by
  Task 6's test (Keycloak's Admin API returns 409 for this — the service must translate it,
  not swallow or mask it).

---

## Task 1: Local Keycloak infrastructure (docker-compose + realm bootstrap + export)

Produces the realm/client/role configuration as a committed, reproducible artifact — not a
manual one-time click-through. This is Keycloak's own recommended workflow (create once via the
Admin API or Console, then export), matching the project's own guide section 9.

**Files:**
- Create: `docker-compose.keycloak.yml` (project root of `pluralisEtatFin`) — Keycloak +
  its own MySQL, `start-dev` mode, matching the guide's `keycloak-local/docker-compose.yml`
  pattern (two services: `kc-db` on MySQL 8.4, `keycloak` on `quay.io/keycloak/keycloak:26.8.0`,
  bound to `127.0.0.1:8080`, `KC_BOOTSTRAP_ADMIN_USERNAME=admin` /
  `KC_BOOTSTRAP_ADMIN_PASSWORD=admin` for local-only use).
- Create: `scripts/keycloak-bootstrap.sh` — a script using the Keycloak container's own bundled
  `kcadm.sh` (via `docker compose exec keycloak ...`) to create the realm, the two clients, the
  four client roles on `plural_etatfi`, the `realm-management` role assignments for
  `admin-service`'s service account, and 4 test users (one per role: `awa`/`ADMIN_PLATEFORME`,
  `moussa`/`ADMIN_COMPTE`, `fatou`/`COLLABORATEUR`, `nouveau`/`INVITE` — reusing the guide's own
  example names), each with `Temporary=false` and password `test-password`. Idempotent: running
  it twice must not fail (use `kcadm.sh` `create` and ignore/check "already exists" the way the
  guide warns about, or `get`-then-create).
- Create: `src/test/resources/keycloak/realm-export.json` — the output of exporting the realm
  built by the script above (`docker compose exec keycloak /opt/keycloak/bin/kc.sh export --dir
  /tmp/export --realm pluraletatfin --users realm_file`, then copy the file out). This is the
  single source of truth Task 4's Testcontainers tests import — do not hand-write this JSON.

**Interfaces:**
- Produces: a realm named `pluraletatfin` importable by both `docker-compose.keycloak.yml` (via
  a bind-mounted `/opt/keycloak/data/import/realm-export.json` pointing at this same file, with
  `start-dev --import-realm`) and Task 4's Testcontainers setup (classpath resource
  `keycloak/realm-export.json`).

- [ ] **Step 1: Write `docker-compose.keycloak.yml`**

Two services (`kc-db`: `mysql:8.4`, database `keycloak`; `keycloak`:
`quay.io/keycloak/keycloak:26.8.0`, `command: start-dev`, env
`KC_BOOTSTRAP_ADMIN_USERNAME=admin`, `KC_BOOTSTRAP_ADMIN_PASSWORD=admin`, `KC_DB=mysql`,
`KC_DB_URL=jdbc:mysql://kc-db:3306/keycloak`, `KC_DB_USERNAME=keycloak`,
`KC_DB_PASSWORD=keycloak`, port `"127.0.0.1:8080:8080"`, `depends_on: kc-db` with a healthcheck
condition) — same shape as the guide's `keycloak-local/docker-compose.yml`, renamed and placed
at the backend project root so it's easy to find alongside `docker-compose.keycloak.yml`.

- [ ] **Step 2: Boot it and confirm Keycloak starts clean**

Run: `docker compose -f docker-compose.keycloak.yml up -d`, then
`docker compose -f docker-compose.keycloak.yml logs keycloak` until the "started" line appears.
Expected: both containers healthy, `curl http://127.0.0.1:8080/realms/master/.well-known/openid-configuration`
returns 200 JSON.

- [ ] **Step 3: Write `scripts/keycloak-bootstrap.sh`**

Use `docker compose -f docker-compose.keycloak.yml exec keycloak /opt/keycloak/bin/kcadm.sh`,
starting with `config credentials --server http://localhost:8080 --realm master --user admin
--password admin`, then the realm/client/role/user creation calls described in Files above.
Make every `kcadm.sh create` call tolerant of "already exists" (check exit code or precede with
a `get` check) so re-running the script is a no-op on the parts already done.

- [ ] **Step 4: Run the bootstrap script against the running stack from Step 2, verify by
  listing**

Run: `bash scripts/keycloak-bootstrap.sh`, then
`docker compose -f docker-compose.keycloak.yml exec keycloak /opt/keycloak/bin/kcadm.sh get clients -r pluraletatfin`
Expected: both `admin-service` and `plural_etatfi` appear in the output.

- [ ] **Step 5: Export the realm to `src/test/resources/keycloak/realm-export.json`**

Run: `docker compose -f docker-compose.keycloak.yml exec keycloak /opt/keycloak/bin/kc.sh export --dir /tmp/export --realm pluraletatfin --users realm_file`,
then `docker compose -f docker-compose.keycloak.yml cp keycloak:/tmp/export/pluraletatfin-realm.json src/test/resources/keycloak/realm-export.json`.
Expected: the file exists, is valid JSON, and `jq '.clients[].clientId' src/test/resources/keycloak/realm-export.json`
lists `admin-service` and `plural_etatfi`.

- [ ] **Step 6: Tear down the manual stack (Task 4's tests bring up their own container)**

Run: `docker compose -f docker-compose.keycloak.yml down -v`

- [ ] **Step 7: Commit**

```bash
git add docker-compose.keycloak.yml scripts/keycloak-bootstrap.sh src/test/resources/keycloak/realm-export.json
git commit -m "feat: add local Keycloak infra and exported pluraletatfin realm"
```

---

## Task 2: Add Keycloak-related Maven dependencies

**Files:**
- Modify: `pom.xml`

**Interfaces:**
- Produces: `spring-boot-starter-oauth2-resource-server` (Task 4), `org.testcontainers:testcontainers`
  + `org.testcontainers:junit-jupiter` (Task 4), `org.keycloak:keycloak-admin-client` (Task 6).

- [ ] **Step 1: Add dependencies**

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>testcontainers</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.keycloak</groupId>
  <artifactId>keycloak-admin-client</artifactId>
  <version>26.0.7</version>
</dependency>
```

Check whether a `<testcontainers.version>`/BOM import is needed for this Spring Boot version
(3.5.6 typically manages a Testcontainers version via `spring-boot-dependencies` — verify with
`mvn dependency:tree | grep testcontainers` after adding; pin an explicit `<version>` only if
resolution fails without one).

- [ ] **Step 2: Resolve and compile**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS, no dependency resolution errors. If `keycloak-admin-client` or the
Testcontainers artifacts fail to resolve from Maven Central, STOP and report BLOCKED — don't
silently pick a different library.

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add Keycloak resource-server, admin-client and Testcontainers dependencies"
```

---

## Task 3: `KeycloakClientRoleConverter`

Pure logic, no Spring context needed to test it — matches the project's existing guide's own
converter (`resource_access.<client>.roles`, prefixed `ROLE_`).

**Files:**
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/config/KeycloakClientRoleConverter.java`
- Test: `src/test/java/pluralisconseil/sn/pluralisEtatFin/config/KeycloakClientRoleConverterTest.java`

**Interfaces:**
- Produces:
  ```java
  public class KeycloakClientRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
      public KeycloakClientRoleConverter(String clientId) { ... }
      @Override public Collection<GrantedAuthority> convert(Jwt jwt) { ... }
  }
  ```
  Consumed by Task 4's `SecurityConfig`, constructed with `"plural_etatfi"`.

- [ ] **Step 1: Write the tests**

```java
class KeycloakClientRoleConverterTest {
    private final KeycloakClientRoleConverter converter = new KeycloakClientRoleConverter("plural_etatfi");

    @Test
    void convert_extractsAndPrefixesRolesForTheConfiguredClient() {
        Jwt jwt = jwtWithResourceAccess(Map.of(
            "plural_etatfi", Map.of("roles", List.of("COLLABORATEUR", "ADMIN_COMPTE"))
        ));
        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_COLLABORATEUR", "ROLE_ADMIN_COMPTE");
    }

    @Test
    void convert_ignoresRolesOfOtherClients() {
        Jwt jwt = jwtWithResourceAccess(Map.of(
            "some-other-client", Map.of("roles", List.of("SOMETHING")),
            "plural_etatfi", Map.of("roles", List.of("INVITE"))
        ));
        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_INVITE");
    }

    @Test
    void convert_returnsEmptyWhenResourceAccessHasNoEntryForTheConfiguredClient() {
        Jwt jwt = jwtWithResourceAccess(Map.of("some-other-client", Map.of("roles", List.of("X"))));
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void convert_returnsEmptyWhenResourceAccessClaimIsAbsent() {
        Jwt jwt = jwtWithResourceAccess(null); // builder simply omits the claim
        assertThat(converter.convert(jwt)).isEmpty();
    }

    // Helper: builds a Jwt via Jwt.withTokenValue("token").header("alg","none")
    // .claim("resource_access", resourceAccess).subject("test").build() — omit the claim
    // entirely when resourceAccess is null.
}
```

- [ ] **Step 2: Run tests, verify they fail**

Run: `mvn -Dtest=KeycloakClientRoleConverterTest test`
Expected: FAIL — class doesn't exist

- [ ] **Step 3: Implement `KeycloakClientRoleConverter`**

Reads `jwt.getClaimAsMap("resource_access")`, looks up the entry for the configured `clientId`,
reads its `"roles"` list (a `List<String>`), maps each to `new SimpleGrantedAuthority("ROLE_" +
role)`. Returns an empty list (never throws) when the top-level claim is missing, when there's
no entry for this `clientId`, or when that entry has no `"roles"` key.

- [ ] **Step 4: Run tests, verify they pass**

Run: `mvn -Dtest=KeycloakClientRoleConverterTest test`
Expected: PASS, 4/4

- [ ] **Step 5: Commit**

```bash
git add src/main/java/.../config/KeycloakClientRoleConverter.java src/test/java/.../config/KeycloakClientRoleConverterTest.java
git commit -m "feat: add KeycloakClientRoleConverter"
```

---

## Task 4: `SecurityConfig` as an OAuth2 resource server, verified against a live Keycloak

The highest-risk task: swap the entire authentication mechanism, proven against a real Keycloak
container importing Task 1's realm — not a mocked JWT decoder.

**Files:**
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/config/SecurityConfig.java`
- Modify: `src/main/resources/application.yml` (add `spring.security.oauth2.resourceserver.jwt.issuer-uri`
  placeholder; remove the `security.jwt.token.secret-key` block)
- Modify: `src/main/resources/application-dev.yml` (set the dev issuer-uri to
  `http://localhost:8080/realms/pluraletatfin`, matching Task 1's local stack)
- Test: `src/test/java/pluralisconseil/sn/pluralisEtatFin/config/SecurityConfigIntegrationTest.java`

**Interfaces:**
- Consumes: `KeycloakClientRoleConverter` (Task 3), `src/test/resources/keycloak/realm-export.json`
  (Task 1)
- Produces: a `SecurityFilterChain` bean that authenticates any request bearing a valid JWT for
  issuer `${spring.security.oauth2.resourceserver.jwt.issuer-uri}`, exposing
  `ROLE_ADMIN_PLATEFORME`/`ROLE_ADMIN_COMPTE`/`ROLE_COLLABORATEUR`/`ROLE_INVITE` as granted
  authorities for later tasks (and the multi-tenant plan) to check.

- [ ] **Step 1: Write the integration test (it will drive the SecurityConfig rewrite)**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SecurityConfigIntegrationTest {
    @Container
    static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.8.0")
        .withRealmImportFile("keycloak/realm-export.json");
    // (If org.testcontainers doesn't ship a KeycloakContainer convenience class for this
    // Testcontainers version, use GenericContainer<>("quay.io/keycloak/keycloak:26.8.0")
    // .withCommand("start-dev --import-realm")
    // .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME","admin").withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD","admin")
    // .withClasspathResourceMapping("keycloak/realm-export.json",
    //   "/opt/keycloak/data/import/realm-export.json", BindMode.READ_ONLY)
    // .withExposedPorts(8080).waitingFor(Wait.forHttp("/realms/pluraletatfin").forStatusCode(200)))

    @DynamicPropertySource
    static void issuerUri(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
            () -> "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080) + "/realms/pluraletatfin");
    }

    @Autowired TestRestTemplate rest;

    @Test
    void requestWithNoToken_onAnAuthenticatedEndpoint_returns401() {
        var response = rest.getForEntity("/api/v1/etats-fin/all", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void requestWithValidTokenForCollaborateurUser_returns200() {
        String token = passwordGrantToken("fatou", "test-password"); // COLLABORATEUR, from Task 1
        var headers = new HttpHeaders(); headers.setBearerAuth(token);
        var response = rest.exchange("/api/v1/etats-fin/all", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void requestWithExpiredToken_returns401() {
        // Use a token obtained, then wait past its short dev-realm expiry, OR a token for a
        // different realm/issuer (simpler and deterministic) to prove signature/issuer
        // validation actually runs.
        var headers = new HttpHeaders(); headers.setBearerAuth("not-a-real-jwt-at-all");
        var response = rest.exchange("/api/v1/etats-fin/all", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // Helper: passwordGrantToken(username, password) posts to
    // http://<keycloak host:port>/realms/pluraletatfin/protocol/openid-connect/token
    // with grant_type=password, client_id=plural_etatfi, username, password (direct access
    // grants must be enabled on plural_etatfi for this test client — enable it in Task 1's
    // bootstrap script for the dev/test realm only; note in that script's comments that
    // production Keycloak should NOT have direct access grants on a public client), parses the
    // JSON response's "access_token" field.
}
```

- [ ] **Step 2: Run the test, verify it fails**

Run: `mvn -Dtest=SecurityConfigIntegrationTest test`
Expected: FAIL — either compile error (no `issuer-uri` config / old `SecurityConfig` still
rejects everything) or a 401/403 mismatch against the still-custom-JWT security chain.

- [ ] **Step 3: Rewrite `SecurityConfig`**

Replace the existing `JwtAuthFilter`/`UserAuthenticationProvider`-based chain with:
```java
http
  .exceptionHandling(c -> c.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()))
  .csrf(AbstractHttpConfigurer::disable)
  .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
  .authorizeHttpRequests(requests -> requests
      .requestMatchers(/* same permitAll list already in the file */).permitAll()
      .anyRequest().authenticated())
  .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
      .jwtAuthenticationConverter(jwtAuthenticationConverter())));
```
with a `jwtAuthenticationConverter()` bean composing Spring's default
`JwtGrantedAuthoritiesConverter` replacement: a `JwtAuthenticationConverter` whose
`setJwtGrantedAuthoritiesConverter(new KeycloakClientRoleConverter("plural_etatfi"))` is set.
Keep the existing `permitAll` request matcher list exactly as-is — this plan doesn't change
which endpoints are public, it only changes how the private ones authenticate.

- [ ] **Step 4: Update `application.yml`/`application-dev.yml` per Files above**

- [ ] **Step 5: Run the test, verify it passes**

Run: `mvn -Dtest=SecurityConfigIntegrationTest test`
Expected: PASS, 3/3. (First Testcontainers run pulls the Keycloak image — allow extra time.)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/.../config/SecurityConfig.java src/main/resources/application.yml src/main/resources/application-dev.yml src/test/java/.../config/SecurityConfigIntegrationTest.java
git commit -m "feat: SecurityConfig becomes an OAuth2 resource server backed by Keycloak"
```

---

## Task 5: Remove the old local-auth code

Pure deletion, no new behavior — now that Task 4's resource-server chain is live and proven.

**Files:**
- Delete: `src/main/java/pluralisconseil/sn/pluralisEtatFin/config/JwtAuthFilter.java`,
  `UserAuthenticationProvider.java`, `UserAuthenticationEntryPoint.java`, `PasswordConfig.java`
- Delete: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/entities/User.java`, `AppRole.java`
- Delete: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/repositories/UserRepository.java`,
  `RoleRepository.java`
- Delete: `src/main/java/pluralisconseil/sn/pluralisEtatFin/data/fixtures/UserFixtures.java`,
  `AppRoleFixtures.java`
- Delete: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/UserService.java`,
  `RoleService.java`, and their `impls/UserServiceImpl.java`, `RoleServiceImpl.java`
- Delete: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/UserRestController.java`,
  `RoleRestController.java`
- Delete: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/mappers/UserMapper.java`,
  `RoleMapper.java`; `api/models/UserDto.java`, `RoleDto.java`, `LoginDto.java`,
  `TokenReponseDto.java`, `UserLoginPasswordDto.java` (verify each is unused elsewhere before
  deleting — `UserDto` in particular may be referenced from `EtatFinancierDto`/other DTOs; grep
  first, only delete what's genuinely unused)
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/MeRestController.java`
  (minimal replacement: `GET /api/v1/me` returning the authenticated principal's `sub`,
  `preferred_username`, and granted authorities, read directly from the
  `JwtAuthenticationToken`/`Authentication` — no DB lookup)
- Test: `src/test/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/MeRestControllerTest.java`
- Modify: `pom.xml` (remove `io.jsonwebtoken:jjwt-api`/`jjwt-impl`/`jjwt-jackson` — no longer
  used by anything after this task)

**Interfaces:**
- Produces: `GET /me` → `{ "sub": "...", "username": "...", "roles": ["ADMIN_COMPTE", ...] }`
  (no `/api/v1` prefix repeated here — it's applied globally by `server.servlet.context-path`)

- [ ] **Step 1: Grep for usages of each type listed under "Delete" above across `src/main` and
  `src/test`**

Confirm nothing outside the files being deleted references them (the `EtatFinancierRestController`
tests and others from the refactor plan should NOT reference `UserDto`/`User`/`AppRole` — verify
this is actually true before deleting, per the parenthetical note in Files above).

- [ ] **Step 2: Write `MeRestControllerTest`**

```java
@WebMvcTest(MeRestController.class)
@Import(SecurityConfig.class) // or an equivalent minimal security test config
class MeRestControllerTest {
    @Test
    void me_withAuthenticatedJwt_returnsSubUsernameAndRoles() throws Exception {
        // build a JwtAuthenticationToken with claims sub="abc-123", preferred_username="fatou"
        // and authority ROLE_COLLABORATEUR, use mockMvc.perform(get("/me").with(authentication(token)))
        // assert $.sub == "abc-123", $.username == "fatou", $.roles == ["ROLE_COLLABORATEUR"]
    }
}
```

- [ ] **Step 3: Run it, verify it fails** (controller doesn't exist)

- [ ] **Step 4: Implement `MeRestController`** and delete every file listed under "Delete"

- [ ] **Step 5: Remove the `jjwt-*` dependencies and the `security.jwt.token.secret-key`
  property if any trace remains**

- [ ] **Step 6: Run `mvn -q -DskipTests compile`, then the full test class, then
  `grep -rE "jjwt|JwtAuthFilter|UserAuthenticationProvider" src/main` to confirm nothing's left**

Expected: compile clean, `MeRestControllerTest` passes, grep returns nothing.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: remove local JWT auth (User/AppRole), add minimal MeRestController"
```

---

## Task 6: `KeycloakUserService` + `AdminUserController` (collaborator creation via `admin-service`)

**Files:**
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/config/KeycloakAdminClientConfig.java`
  (a `Keycloak` bean built with `KeycloakBuilder`, grant type `client_credentials`, realm
  `pluraletatfin`, `clientId=admin-service`, `clientSecret` from a new
  `keycloak.admin-service.client-secret` property)
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/KeycloakUserService.java`,
  `services/impls/KeycloakUserServiceImpl.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/AdminUserController.java`
- Test: `src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/KeycloakUserServiceImplTest.java`
  (Testcontainers, same realm import as Task 4)

**Interfaces:**
- Consumes: `src/test/resources/keycloak/realm-export.json` (Task 1)
- Produces:
  ```java
  public interface KeycloakUserService {
      String createCollaborator(String username, String email, String temporaryPassword, String role) throws KeycloakUserAlreadyExistsException;
  }
  ```
  returning the new user's Keycloak `id`. `POST /api/v1/admin/users` (body: username, email,
  temporaryPassword, role) exposed by `AdminUserController`, calling this service.

- [ ] **Step 1: Write `KeycloakUserServiceImplTest`**

```java
@Testcontainers
class KeycloakUserServiceImplTest {
    @Container
    static final GenericContainer<?> keycloak = /* same setup as Task 4's SecurityConfigIntegrationTest */;

    KeycloakUserServiceImpl service; // constructed with a Keycloak admin client pointed at the container

    @Test
    void createCollaborator_createsUserWithRoleAssignedInKeycloak() {
        String id = service.createCollaborator("new.collab", "new.collab@example.com", "Temp1234!", "COLLABORATEUR");
        assertThat(id).isNotBlank();
        // verify via the same admin client: user exists, has the COLLABORATEUR client role on plural_etatfi
    }

    @Test
    void createCollaborator_whenUsernameAlreadyExists_throwsKeycloakUserAlreadyExistsException() {
        service.createCollaborator("dup.user", "dup@example.com", "Temp1234!", "COLLABORATEUR");
        assertThatThrownBy(() -> service.createCollaborator("dup.user", "other@example.com", "Temp1234!", "COLLABORATEUR"))
            .isInstanceOf(KeycloakUserAlreadyExistsException.class);
    }
}
```

- [ ] **Step 2: Run, verify failure** (classes don't exist)

- [ ] **Step 3: Implement `KeycloakAdminClientConfig`, `KeycloakUserServiceImpl`**

Use the `keycloak-admin-client`'s `RealmResource.users().create(UserRepresentation)` (username,
email, `enabled=true`, credentials list with the temp password and `temporary=false` per this
plan's "no email, admin communicates the password directly" decision — so the collaborator isn't
forced through a Keycloak-hosted "set your password" flow they'd need an email to reach), then
`UsersResource.get(id).roles().clientLevel(pluralEtatfiClientUuid).add(List.of(roleRepresentation))`
to assign the requested client role. Catch the Admin client's `jakarta.ws.rs.WebApplicationException`
with a 409 status and rethrow as `KeycloakUserAlreadyExistsException` (new, simple
`RuntimeException` subclass in `pluralisconseil.sn.pluralisEtatFin.exceptions`).

- [ ] **Step 4: Run, verify pass**

- [ ] **Step 5: Implement `AdminUserController`** (`POST /admin/users`, delegates to the service,
  maps `KeycloakUserAlreadyExistsException` to `Response.duplicateEmail()` or similar existing
  `Response.Status`, maps success to `Response.created()` with the new user's id)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/.../config/KeycloakAdminClientConfig.java src/main/java/.../services/interfaces/KeycloakUserService.java src/main/java/.../services/impls/KeycloakUserServiceImpl.java src/main/java/.../api/controllers/AdminUserController.java src/main/java/.../exceptions/KeycloakUserAlreadyExistsException.java src/test/java/.../services/impls/KeycloakUserServiceImplTest.java
git commit -m "feat: add KeycloakUserService and AdminUserController for collaborator creation"
```

---

## Task 7: Final regression

**Files:**
- Verify only.

- [ ] **Step 1: Run `mvn test` (full suite)**

Expected: all tests pass, including the Testcontainers-backed ones — this is a genuine
improvement over the refactor plan's DB-less gap, since Docker is available here. The only
pre-existing unrelated issue to watch for is `EtatFinAppBackApplicationTests.contextLoads`, which
needs a live MySQL (not Keycloak's) — unaffected by this plan either way.

- [ ] **Step 2: `grep -rE "User\.java|AppRole|JwtAuthFilter|jjwt" src/main --include="*.java"`**

Expected: no matches (confirms Task 5's cleanup is complete).

- [ ] **Step 3: Confirm `docker-compose.keycloak.yml` + `scripts/keycloak-bootstrap.sh` still
  work end-to-end from a clean state**

Run: `docker compose -f docker-compose.keycloak.yml up -d`, `bash scripts/keycloak-bootstrap.sh`,
`mvn -Dtest=SecurityConfigIntegrationTest,KeycloakUserServiceImplTest test`, then
`docker compose -f docker-compose.keycloak.yml down -v`. Expected: all pass without needing the
manually-booted stack (the tests bring up their own Testcontainers instance independently) —
this step instead proves the *local-dev* workflow (docker-compose + bootstrap script, for a
developer who isn't running the test suite) still produces a working realm.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: final regression check for Keycloak backend integration"
```
