package dev.koski.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import dev.koski.urlshortener.repository.ClickRepository;
import dev.koski.urlshortener.repository.LinkRepository;
import dev.koski.urlshortener.repository.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AuthFlowTests {

    private static final Pattern TOKEN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired LinkRepository links;
    @Autowired ClickRepository clicks;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        links.deleteAll();
        clicks.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String credentials(String username, String password) {
        return "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
    }

    private void register(String username, String password) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(username, password))).andExpect(status().isCreated());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(username, password))).andExpect(status().isOk()).andReturn();
        Matcher matcher = TOKEN.matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        return "Bearer " + matcher.group(1);
    }

    private void createLink(String bearer, String slug) throws Exception {
        mvc.perform(post("/api/links").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\",\"slug\":\"%s\"}".formatted(slug)))
                .andExpect(status().isCreated());
    }

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mvc.perform(get("/api/links")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        register("martin", "password-lunga");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials("martin", "sbagliata"))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials("inesistente", "password-lunga"))).andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationIsConflictEvenWithDifferentCase() throws Exception {
        register("martin", "password-lunga");

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(credentials("MARTIN", "password-lunga"))).andExpect(status().isConflict());
    }

    @Test
    void shortPasswordIsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(credentials("martin", "corta"))).andExpect(status().isBadRequest());
    }

    @Test
    void tokenGrantsAccessToOwnLinks() throws Exception {
        register("martin", "password-lunga");
        String bearer = login("martin", "password-lunga");

        createLink(bearer, "ciao");

        mvc.perform(get("/api/links").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("ciao"));
    }

    @Test
    void usersCannotTouchEachOthersLinksAndOwnerHeaderIsIgnored() throws Exception {
        register("martin", "password-lunga");
        register("altro", "password-lunga");
        String martin = login("martin", "password-lunga");
        String altro = login("altro", "password-lunga");
        createLink(martin, "ciao");

        mvc.perform(get("/api/links").header("Authorization", altro)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/links/ciao/stats").header("Authorization", altro).header("X-Owner", "martin"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/links/ciao").header("Authorization", altro).header("X-Owner", "martin"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/links/ciao").header("Authorization", martin)).andExpect(status().isNoContent());
    }

    @Test
    void redirectIsPublic() throws Exception {
        register("martin", "password-lunga");
        createLink(login("martin", "password-lunga"), "pubblico");

        mvc.perform(get("/pubblico")).andExpect(status().isFound());
    }
}
