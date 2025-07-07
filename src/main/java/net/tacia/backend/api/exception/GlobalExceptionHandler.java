package net.tacia.backend.api.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    @ExceptionHandler(ContentNotFoundException.class)
    protected ResponseEntity<Object> handleContentNotFound(ContentNotFoundException ex, WebRequest request) {
        return handleExceptionInternal(ex, 
            Map.of("error", ex.getMessage()),
            new HttpHeaders(),
            HttpStatus.NOT_FOUND,
            request);
    }
    
    @ExceptionHandler(SecurityException.class)
    protected ResponseEntity<Object> handleSecurityException(SecurityException ex, WebRequest request) {
        return handleExceptionInternal(ex,
            Map.of("error", "Access denied: " + ex.getMessage()),
            new HttpHeaders(),
            HttpStatus.FORBIDDEN,
            request);
    }
    
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleGeneralException(Exception ex, WebRequest request) {
        return handleExceptionInternal(ex,
            Map.of("error", "An unexpected error occurred"),
            new HttpHeaders(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            request);
    }
}
