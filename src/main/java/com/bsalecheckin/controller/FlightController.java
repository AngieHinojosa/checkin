package com.bsalecheckin.controller;

import com.bsalecheckin.dto.FlightResponse;
import com.bsalecheckin.exception.DatabaseUnavailableException;
import com.bsalecheckin.exception.FlightNotFoundException;
import com.bsalecheckin.service.FlightService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/flights")
public class FlightController {
  private final FlightService service;

  public FlightController(FlightService service) {
    this.service = service;
  }

  @GetMapping("/{id}/passengers")
  public ResponseEntity<FlightResponse> get(@PathVariable("id") Long id) {
    try {
      var data = service.getFlightData(id);
      return ResponseEntity.ok(new FlightResponse(200, data, null));
    } catch (FlightNotFoundException e) {
      return ResponseEntity.status(404).body(new FlightResponse(404, Map.of(), null));
    } catch (DatabaseUnavailableException | DataAccessException e) {
      return ResponseEntity.badRequest().body(new FlightResponse(400, null, "could not connect to db"));
    } catch (Exception e) {
      return ResponseEntity.status(500).body(new FlightResponse(500, Map.of(), "internal error"));
    }
  }
}
