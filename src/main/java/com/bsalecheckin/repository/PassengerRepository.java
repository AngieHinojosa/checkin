package com.bsalecheckin.repository;

import com.bsalecheckin.dto.PassengerDto;
import com.bsalecheckin.exception.DatabaseUnavailableException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PassengerRepository {
  @PersistenceContext
  private EntityManager em;

  public List<PassengerDto> findByFlight(Long flightId) {
    try {
      var q = em.createNativeQuery("""
        SELECT
          p.`passenger_id`, p.`dni`, p.`name`, p.`age`, p.`country`,
          bp.`boarding_pass_id`, bp.`purchase_id`, bp.`seat_type_id`, bp.`seat_id`
        FROM `boarding_pass` bp
        JOIN `passenger` p ON p.`passenger_id` = bp.`passenger_id`
        WHERE bp.`flight_id` = ?
        ORDER BY bp.`purchase_id`, bp.`boarding_pass_id`
      """);
      q.setParameter(1, flightId);

      var out = new ArrayList<PassengerDto>();
      for (var o : q.getResultList()) {
        var r = (Object[]) o;
        out.add(new PassengerDto(
          ((Number) r[0]).longValue(),
          (String) r[1],
          (String) r[2],
          r[3] == null ? null : ((Number) r[3]).intValue(),
          (String) r[4],
          ((Number) r[5]).longValue(),
          ((Number) r[6]).longValue(),
          ((Number) r[7]).intValue(),
          r[8] == null ? null : ((Number) r[8]).longValue()
        ));
      }
      return out;
    } catch (PersistenceException e) {
      throw new DatabaseUnavailableException();
    }
  }
}
