package com.bsalecheckin.dto;

public record FlightResponse(
  Integer code,
  Object data,
  String errors
) {}
