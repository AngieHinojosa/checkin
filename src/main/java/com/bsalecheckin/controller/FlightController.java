package com.bsalecheckin.controller;

import com.bsalecheckin.exception.DatabaseUnavailableException;
import com.bsalecheckin.exception.FlightNotFoundException;
import com.bsalecheckin.service.FlightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class FlightController {
  private final FlightService service;

  public FlightController(FlightService service) {
    this.service = service;
  }

  @GetMapping("/status")
  public Map<String,Object> status() {
    return Map.of("status","ok");
  }

  @GetMapping("/flights/{id}/passengers")
  public ResponseEntity<Map<String,Object>> get(@PathVariable Long id) {
    try {
      var data = service.getFlightData(id);
      var body = new LinkedHashMap<String,Object>();
      body.put("code", 200);
      body.put("data", data);
      return ResponseEntity.ok(body);
    } catch (FlightNotFoundException e) {
      var body = new LinkedHashMap<String,Object>();
      body.put("code", 404);
      body.put("data", Map.of());
      return ResponseEntity.status(404).body(body);
    } catch (DatabaseUnavailableException e) {
      var body = new LinkedHashMap<String,Object>();
      body.put("code", 400);
      body.put("errors", "could not connect to db");
      return ResponseEntity.badRequest().body(body);
    } catch (Exception e) {
      var body = new LinkedHashMap<String,Object>();
      body.put("code", 500);
      body.put("data", Map.of());
      body.put("errors", "internal error");
      return ResponseEntity.status(500).body(body);
    }
  }
}
