package com.bsalecheckin.service;

import com.bsalecheckin.dto.FlightData;
import com.bsalecheckin.dto.PassengerDto;
import com.bsalecheckin.exception.FlightNotFoundException;
import com.bsalecheckin.repository.FlightRepository;
import com.bsalecheckin.repository.PassengerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlightService {
  private final FlightRepository flights;
  private final PassengerRepository passengers;

  public FlightService(FlightRepository flights, PassengerRepository passengers) {
    this.flights = flights;
    this.passengers = passengers;
  }

  public FlightData getFlightData(Long flightId) {
    var row = flights.findRowById(flightId).orElseThrow(FlightNotFoundException::new);
    var pax = passengers.findByFlight(flightId);
    return toFlightData(row, pax);
  }

  private FlightData toFlightData(Object[] r, List<PassengerDto> pax) {
    return new FlightData(
      ((Number) r[0]).longValue(),
      ((Number) r[1]).intValue(),
      (String) r[2],
      ((Number) r[3]).intValue(),
      (String) r[4],
      ((Number) r[5]).longValue(),
      pax
    );
  }
}
