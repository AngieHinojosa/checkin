package com.bsalecheckin.repository;

import com.bsalecheckin.dto.SeatSlot;
import com.bsalecheckin.exception.DatabaseUnavailableException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SeatRepository {
  @PersistenceContext
  private EntityManager em;

  public List<SeatSlot> findByAirplane(Long airplaneId) {
    try {
      var q = em.createNativeQuery("""
        SELECT `seat_id`, `seat_row`, `seat_column`, `seat_type_id`
        FROM `seat`
        WHERE `airplane_id` = ?
      """);
      q.setParameter(1, airplaneId);
      var out = new ArrayList<SeatSlot>();
      for (var o : q.getResultList()) {
        var r = (Object[]) o;
        out.add(new SeatSlot(
          ((Number) r[0]).longValue(),
          ((Number) r[1]).intValue(),
          ((String) r[2]).toUpperCase(),
          ((Number) r[3]).intValue()
        ));
      }
      return out;
    } catch (PersistenceException e) {
      throw new DatabaseUnavailableException();
    }
  }
}
