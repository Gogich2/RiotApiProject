package org.main.builds.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.main.builds.model.BuildConfidence;
import org.main.builds.model.BuildRole;
import org.main.builds.model.BuildScope;
import org.main.exception.NotFoundException;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChampionBuildControllerTest {

    private final ChampionBuildService service = mock(ChampionBuildService.class);

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ChampionBuildController(service)).
                setControllerAdvice(new org.main.handler.GlobalExceptionHandler(messages())).
                build();
    }

    @Test
    void exposesPublicExactBuildMetadataWithoutRawRiotPayloads() throws Exception {
        ChampionBuildResponse response = new ChampionBuildResponse(
                true, new RequestedFilters(420, "16.13", BuildRole.BOTTOM, 55),
                new ResolvedFilters(420, "16.13", "16.12", BuildRole.BOTTOM, 55),
                BuildScope.EXACT_MATCHUP, BuildConfidence.LOW, 12, 7, 7.0 / 12.0,
                false, false, BuildFallbackReason.NONE, "12 games",
                "Exact matchup evidence", null, null,
                new DisplayBuildPayload(List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of(), List.of(), List.of()));
        when(service.builds(22, 420, "16.13", BuildRole.BOTTOM, 55)).
                thenReturn(response);

        mvc.perform(get("/api/champions/22/builds").param("queueId", "420").
                        param("patch", "16.13").param("role", "BOTTOM").
                        param("opponentId", "55")).
                andExpect(status().isOk()).
                andExpect(jsonPath("$.resultScope").value("EXACT_MATCHUP")).
                andExpect(jsonPath("$.wins").value(7)).
                andExpect(jsonPath("$.resolved.comparisonPatch").value("16.12")).
                andExpect(jsonPath("$.rawJson").doesNotExist()).
                andExpect(jsonPath("$.raw_json").doesNotExist());
    }

    @Test
    void returnsFilteredOptionsAndFlexAvailability() throws Exception {
        ChampionBuildOptionsResponse response = new ChampionBuildOptionsResponse(
                22,
                List.of(new QueueOption(420, "Ranked Solo/Duo", true),
                        new QueueOption(440, "Ranked Flex", false)),
                List.of(new PatchOption("16.13")),
                List.of(new RoleOption(BuildRole.BOTTOM, 80, true)),
                List.of(new OpponentOption(55, "Katarina", "/champion/Katarina.png", 12)),
                new RequestedFilters(420, "16.13", BuildRole.BOTTOM, null));
        when(service.options(22, 420, "16.13", BuildRole.BOTTOM)).thenReturn(response);

        mvc.perform(get("/api/champions/22/builds/options").param("queueId", "420").
                        param("patch", "16.13").param("role", "BOTTOM")).
                andExpect(status().isOk()).
                andExpect(jsonPath("$.defaults.role").value("BOTTOM")).
                andExpect(jsonPath("$.roles[0].games").value(80)).
                andExpect(jsonPath("$.opponents[0].games").value(12)).
                andExpect(jsonPath("$.queues[1].available").value(false));
    }

    @Test
    void rejectsInvalidQueryValuesBeforeServiceAccess() throws Exception {
        mvc.perform(get("/api/champions/22/builds").param("queueId", "420").
                        param("patch", "16.13").param("role", "INVALID")).
                andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void mapsUnknownChampionToNotFound() throws Exception {
        when(service.options(999, null, null, null)).
                thenThrow(new NotFoundException("Champion not found: 999"));

        mvc.perform(get("/api/champions/999/builds/options")).
                andExpect(status().isNotFound());
    }

    private StaticMessageSource messages() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("error.bad_request", java.util.Locale.ENGLISH, "Bad request");
        messages.addMessage("error.not_found", java.util.Locale.ENGLISH, "Not found");
        messages.addMessage("error.internal", java.util.Locale.ENGLISH, "Internal error");
        return messages;
    }
}
