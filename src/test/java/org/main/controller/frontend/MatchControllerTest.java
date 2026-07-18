package org.main.controller.frontend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.main.dto.frontend.MatchDetailsDto;
import org.main.service.frontend.MatchDetailsService;

class MatchControllerTest {

    @Test
    void delegatesDetailsDirectlyToMatchDetailsService() {
        MatchDetailsService service = mock(MatchDetailsService.class);
        MatchDetailsDto expected = mock(MatchDetailsDto.class);
        when(service.getMatchDetails("EUW1_1", "puuid")).thenReturn(expected);
        MatchController controller = new MatchController(service);

        assertThat(controller.details("EUW1_1", "puuid")).isSameAs(expected);
        verify(service).getMatchDetails("EUW1_1", "puuid");
    }
}
