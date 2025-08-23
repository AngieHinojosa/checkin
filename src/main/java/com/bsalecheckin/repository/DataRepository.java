package com.bsalecheckin.repository;

import com.bsalecheckin.dto.PassengerDto;
import com.bsalecheckin.dto.SeatSlot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class DataRepository {
  private final JdbcTemplate jdbc;

  public DataRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<Map<String,Object>> findFlightInfo(Long flightId) {
    var sql = """
      select f.flight_id,
             f.takeoff_date_time,
             f.landing_date_time,
             f.airplane_id,
             f.takeoff_airport as takeoff_airport,
             f.landing_airport as landing_airport
      from flight f
      where f.flight_id = ?
    """;
    var list = jdbc.query(sql, (rs, n) -> {
      var m = new HashMap<String,Object>();
      m.put("flightId", rs.getLong("flight_id"));
      m.put("takeoffDateTime", rs.getLong("takeoff_date_time"));
      m.put("landingDateTime", rs.getLong("landing_date_time"));
      m.put("airplaneId", rs.getLong("airplane_id"));
      m.put("takeoffAirport", rs.getString("takeoff_airport"));
      m.put("landingAirport", rs.getString("landing_airport"));
      return m;
    }, flightId);
    if (list.isEmpty()) return Optional.empty();
    return Optional.of(list.get(0));
  }

  public List<PassengerDto> findPassengersByFlight(Long flightId) {
    var sql = """
      select p.passenger_id, p.dni, p.name, p.age, p.country,
             bp.boarding_pass_id, bp.purchase_id, bp.seat_type_id, bp.seat_id
      from boarding_pass bp
      join passenger p on p.passenger_id = bp.passenger_id
      where bp.flight_id = ?
      order by bp.boarding_pass_id
    """;
    return jdbc.query(sql, (rs, n) -> {
      Object seatObj = rs.getObject("seat_id");
      Long seatId = (seatObj == null) ? null : ((Number) seatObj).longValue();
      Integer age = (Integer) rs.getObject("age");
      return new PassengerDto(
        rs.getLong("passenger_id"),
        rs.getString("dni"),
        rs.getString("name"),
        age,
        rs.getString("country"),
        rs.getLong("boarding_pass_id"),
        rs.getInt("purchase_id"),
        rs.getInt("seat_type_id"),
        seatId
      );
    }, flightId);
  }

  public List<SeatSlot> findSeatsByAirplane(Long airplaneId) {
    var sql = """
      select seat_id, seat_row, seat_column, seat_type_id
      from seat
      where airplane_id = ?
      order by seat_row, seat_column
    """;
    return jdbc.query(sql, (rs, n) -> new SeatSlot(
      rs.getLong("seat_id"),
      rs.getInt("seat_row"),
      rs.getString("seat_column"),
      rs.getInt("seat_type_id")
    ), airplaneId);
  }
}
