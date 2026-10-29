package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** A 409 from Keycloak must say whether the username or the email is the one already taken. */
class KeycloakConflictMessageTest {

    @Test
    void anEmailClashNamesTheEmail() {
        var message = KeycloakUserServiceImpl.conflictMessage("{\"errorMessage\":\"User exists with same email\"}", "chamen", "a@b.sn");
        assertThat(message).contains("e-mail").contains("a@b.sn").doesNotContain("identifiant");
    }

    @Test
    void aUsernameClashNamesTheUsername() {
        var message = KeycloakUserServiceImpl.conflictMessage("{\"errorMessage\":\"User exists with same username\"}", "chamen", "a@b.sn");
        assertThat(message).contains("identifiant").contains("chamen");
    }

    @Test
    void anUnknownBodyFallsBackToAGenericMessage() {
        assertThat(KeycloakUserServiceImpl.conflictMessage("", "chamen", "a@b.sn")).contains("identifiant").contains("e-mail");
    }
}
