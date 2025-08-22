package com.bsalecheckin.dto;

public record PassengerDto(
  Long passengerId,
  String dni,
  String name,
  Integer age,
  String country,
  Long boardingPassId,
  Long purchaseId,
  Integer seatTypeId,
  Long seatId
) {}
