package com.bsalecheckin.service;

import com.bsalecheckin.dto.PassengerDto;
import com.bsalecheckin.dto.SeatSlot;
import com.bsalecheckin.exception.DatabaseUnavailableException;
import com.bsalecheckin.exception.FlightNotFoundException;
import com.bsalecheckin.repository.DataRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FlightService {
  private final DataRepository repo;

  public FlightService(DataRepository repo) {
    this.repo = repo;
  }

  public Map<String,Object> getFlightData(Long flightId) {
    try {
      var infoOpt = repo.findFlightInfo(flightId);
      if (infoOpt.isEmpty()) throw new FlightNotFoundException();
      var info = new HashMap<>(infoOpt.get());
      var passengers = repo.findPassengersByFlight(flightId);
      var airplaneId = (Long) info.get("airplaneId");
      var seats = repo.findSeatsByAirplane(airplaneId);
      var assigned = assign(passengers, seats);
      info.put("passengers", assigned);
      return info;
    } catch (DataAccessException e) {
      throw new DatabaseUnavailableException(e);
    }
  }

  private List<PassengerDto> assign(List<PassengerDto> passengers, List<SeatSlot> seats) {
    var assigner = new SeatAssigner(seats, passengers);
    return assigner.assignAll();
  }
}
