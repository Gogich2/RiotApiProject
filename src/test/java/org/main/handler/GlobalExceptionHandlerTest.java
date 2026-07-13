package org.main.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.main.dto.ApiErrorResponse;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    @Test
    void handlesRequestsWithoutQueryString() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("error.internal", Locale.ENGLISH, "Internal error");
        GlobalExceptionHandler handler = new GlobalExceptionHandler(messages);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");

        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(
                new Exception("failure"),
                request,
                Locale.ENGLISH
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().context()).doesNotContainKey("query");
    }
}
