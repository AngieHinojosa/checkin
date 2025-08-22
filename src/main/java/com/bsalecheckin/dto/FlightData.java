package com.bsalecheckin.dto;

import java.util.List;

public record FlightData(
  Long flightId,
  Integer takeoffDateTime,
  String takeoffAirport,
  Integer landingDateTime,
  String landingAirport,
  Long airplaneId,
  List<PassengerDto> passengers
) {}
