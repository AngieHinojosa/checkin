package com.bsalecheckin.repository;

import com.bsalecheckin.exception.DatabaseUnavailableException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class FlightRepository {
  @PersistenceContext
  private EntityManager em;

  public Optional<Object[]> findRowById(Long id) {
    try {
      var q = em.createNativeQuery("""
        SELECT `flight_id`, `takeoff_date_time`, `takeoff_airport`,
               `landing_date_time`, `landing_airport`, `airplane_id`
        FROM `flight`
        WHERE `flight_id` = ?
      """);
      q.setParameter(1, id);
      List<?> rows = q.getResultList();
      if (rows.isEmpty()) return Optional.empty();
      return Optional.of((Object[]) rows.get(0));
    } catch (PersistenceException e) {
      throw new DatabaseUnavailableException();
    }
  }
}
