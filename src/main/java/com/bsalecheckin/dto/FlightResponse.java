package com.bsalecheckin.dto;

import java.util.Map;

public record FlightResponse(int code, Map<String,Object> data, String errors) {}
