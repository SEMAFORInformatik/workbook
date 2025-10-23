package ch.semafor.intens.ws.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import io.swagger.v3.oas.annotations.Hidden;

@ControllerAdvice
@Hidden
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
  Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);
  @ExceptionHandler(ResourceNotFoundException.class)
  protected ResponseEntity<Object> handleNotFound(
      RuntimeException ex, WebRequest request) {
    return handleExceptionInternal(ex, new ErrorInfo("not found", ex),
        new HttpHeaders(), HttpStatus.NOT_FOUND, request);
  }
  @ExceptionHandler(TypeCreationException.class)
  protected ResponseEntity<Object> handleTypeCreationError(
          RuntimeException ex, WebRequest request) {
    return handleExceptionInternal(ex, new ErrorInfo(ex.getMessage(), ex),
            new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
  }
}
