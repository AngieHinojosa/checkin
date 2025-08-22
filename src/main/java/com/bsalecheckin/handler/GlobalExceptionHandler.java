package com.bsalecheckin.handler;

import com.bsalecheckin.dto.FlightResponse;
import com.bsalecheckin.exception.DatabaseUnavailableException;
import com.bsalecheckin.exception.FlightNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(FlightNotFoundException.class)
  public ResponseEntity<FlightResponse> notFound() {
    return ResponseEntity.status(404).body(new FlightResponse(404, Map.of(), null));
  }

  @ExceptionHandler({DatabaseUnavailableException.class, DataAccessException.class})
  public ResponseEntity<FlightResponse> dbError() {
    return ResponseEntity.badRequest().body(new FlightResponse(400, null, "could not connect to db"));
  }
}
