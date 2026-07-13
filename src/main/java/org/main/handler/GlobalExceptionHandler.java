package org.main.handler;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.main.dto.ApiErrorResponse;
import org.main.exception.ExternalServiceException;
import org.main.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex,
                                                             HttpServletRequest request,
                                                             Locale locale) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "error.bad_request",
                "BAD_REQUEST",
                ex,
                request,
                locale,
                queryContext(request)
        );
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex,
                                                           HttpServletRequest request,
                                                           Locale locale) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "error.not_found",
                "NOT_FOUND",
                ex,
                request,
                locale,
                queryContext(request)
        );
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleExternalService(ExternalServiceException ex,
                                                                  HttpServletRequest request,
                                                                  Locale locale) {
        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                "error.external_service",
                "EXTERNAL_SERVICE_ERROR",
                ex,
                request,
                locale,
                queryContext(request)
        );
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleNotImplemented(UnsupportedOperationException ex,
                                                                 HttpServletRequest request,
                                                                 Locale locale) {
        return buildResponse(
                HttpStatus.NOT_IMPLEMENTED,
                "error.not_implemented",
                "NOT_IMPLEMENTED",
                ex,
                request,
                locale,
                queryContext(request)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request,
            Locale locale
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "error.bad_request",
                "VALIDATION_ERROR",
                ex,
                request,
                locale,
                queryContext(request)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex,
                                                          HttpServletRequest request,
                                                          Locale locale) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "error.internal",
                "INTERNAL_ERROR",
                ex,
                request,
                locale,
                queryContext(request)
        );
    }

    private Map<String, Object> queryContext(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? Map.of() : Map.of("query", query);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status,
                                                           String messageKey,
                                                           String code,
                                                           Exception ex,
                                                           HttpServletRequest request,
                                                           Locale locale,
                                                           Map<String, Object> extraContext) {
        String errorId = UUID.randomUUID().toString();
        String requestId = MDC.get("requestId");
        String localizedMessage = messageSource.getMessage(messageKey, null, locale);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("requestId", requestId);
        context.put("method", request.getMethod());
        context.putAll(extraContext);

        if (status.is5xxServerError()) {
            log.error("Handled exception: errorId={}, code={}, path={}, context={}",
                    errorId, code, request.getRequestURI(), context, ex);
        } else {
            log.warn("Handled exception: errorId={}, code={}, path={}, context={}, message={}",
                    errorId, code, request.getRequestURI(), context, ex.getMessage());
        }

        ApiErrorResponse body = new ApiErrorResponse(
                errorId,
                code,
                localizedMessage,
                request.getRequestURI(),
                OffsetDateTime.now(),
                context
        );

        return ResponseEntity.status(status).body(body);
    }
}
