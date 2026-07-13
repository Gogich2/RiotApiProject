package org.main.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class StaticAccountFrontendTest {

    private static final Path STATIC_ROOT = Path.of("src", "main", "resources", "static");

    @Test
    void accountPageExposesAccessibleAuthenticationAndSavedProfileControls() throws IOException {
        String html = read("account.html");

        assertThat(html).contains("for=\"loginEmail\"");
        assertThat(html).contains("autocomplete=\"email\"");
        assertThat(html).contains("autocomplete=\"current-password\"");
        assertThat(html).contains("autocomplete=\"new-password\"");
        assertThat(html).contains("/oauth2/authorization/discord");
        assertThat(html).contains("id=\"savedProfilesList\"");
        assertThat(html).contains("name=\"personalLabel\"");
        assertThat(html).contains("aria-live=\"polite\"");
    }

    @Test
    void everyPublicPageHasSameOptionalAccountEntryPoint() throws IOException {
        for (String page : List.of(
                "index.html",
                "players.html",
                "player.html",
                "champions.html",
                "champion.html",
                "match.html",
                "account.html"
        )) {
            String html = read(page);
            assertThat(html).as(page).contains("data-account-entry");
            assertThat(html).as(page).contains("js/account.js");
        }
    }

    @Test
    void headerBootstrapNeverGatesPublicContent() throws IOException {
        String js = read("js/account.js");

        assertThat(js).contains("api.getCurrentUser()");
        assertThat(js).contains("Sign in");
        assertThat(js).contains("Saved profiles");
        assertThat(js).doesNotContain("window.location.replace");
        assertThat(js).doesNotContain("requireAuthentication");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(STATIC_ROOT.resolve(relativePath));
    }
}
