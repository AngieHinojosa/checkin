package com.bsalecheckin.service;

import com.bsalecheckin.dto.PassengerDto;
import com.bsalecheckin.dto.SeatSlot;

import java.util.*;
import java.util.stream.Collectors;

public class SeatAssigner {
  private final List<SeatSlot> seats;
  private final List<PassengerDto> passengers;

  public SeatAssigner(List<SeatSlot> seats, List<PassengerDto> passengers) {
    this.seats = seats;
    this.passengers = passengers;
  }

  public List<PassengerDto> assignAll() {
    var occupied = passengers.stream()
        .map(PassengerDto::seatId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    var freeSeats = seats.stream()
        .filter(s -> !occupied.contains(s.seatId()))
        .collect(Collectors.toList());

    var seatsByType = freeSeats.stream()
        .collect(Collectors.groupingBy(SeatSlot::seatTypeId));

    var groups = passengers.stream()
        .collect(Collectors.groupingBy(PassengerDto::purchaseId, LinkedHashMap::new, Collectors.toList()));

    var resultSeatByBp = new HashMap<Long, Long>();

    for (var samePurchase : groups.values()) {
      var byType = samePurchase.stream()
          .collect(Collectors.groupingBy(PassengerDto::seatTypeId, LinkedHashMap::new, Collectors.toList()));

      for (var e : byType.entrySet()) {
        var type = e.getKey();
        var pax = new ArrayList<>(e.getValue());
        var freeRows = buildFreeRows(seatsByType.getOrDefault(type, List.of()));

        var minors = pax.stream()
            .filter(p -> p.age() != null && p.age() < 18)
            .collect(Collectors.toCollection(LinkedList::new));
        var adults = pax.stream()
            .filter(p -> p.age() == null || p.age() >= 18)
            .collect(Collectors.toCollection(LinkedList::new));

        while (minors.size() >= 2 && !adults.isEmpty() && takeTriad(freeRows, resultSeatByBp, adults, minors)) {}
        while (!minors.isEmpty() && !adults.isEmpty() && takePair(freeRows, resultSeatByBp, adults, minors)) {}
        while (!adults.isEmpty() && takeSingle(freeRows, resultSeatByBp, adults.removeFirst().boardingPassId())) {}
        while (!minors.isEmpty() && takeSingle(freeRows, resultSeatByBp, minors.removeFirst().boardingPassId())) {}
      }
    }

    var out = new ArrayList<PassengerDto>(passengers.size());
    for (var p : passengers) {
      Long sid = p.seatId() != null ? p.seatId() : resultSeatByBp.get(p.boardingPassId());
      out.add(new PassengerDto(
          p.passengerId(), p.dni(), p.name(), p.age(), p.country(),
          p.boardingPassId(), p.purchaseId(), p.seatTypeId(), sid
      ));
    }
    return out;
  }

  private static TreeMap<Integer, List<SeatSlot>> buildFreeRows(List<SeatSlot> list) {
    var map = new TreeMap<Integer, List<SeatSlot>>();
    for (var s : list) map.computeIfAbsent(s.row(), k -> new ArrayList<>()).add(s);
    for (var row : map.values()) row.sort(Comparator.comparingInt(a -> "ABCDEF".indexOf(a.col())));
    return map;
  }

  private static boolean takeTriad(TreeMap<Integer, List<SeatSlot>> freeRows, Map<Long,Long> out, Deque<PassengerDto> adults, Deque<PassengerDto> minors) {
    for (var e : freeRows.entrySet()) {
      var row = e.getValue();

      SeatSlot A = find(row,"A");
      SeatSlot B = find(row,"B");
      SeatSlot C = find(row,"C");
      if (A!=null && B!=null && C!=null) {
        var adult = adults.removeFirst();
        var m1 = minors.removeFirst();
        var m2 = minors.removeFirst();
        out.put(adult.boardingPassId(), B.seatId());
        out.put(m1.boardingPassId(), A.seatId());
        out.put(m2.boardingPassId(), C.seatId());
        row.remove(A); row.remove(B); row.remove(C);
        return true;
      }

      SeatSlot D = find(row,"D");
      SeatSlot E = find(row,"E");
      SeatSlot F = find(row,"F");
      if (D!=null && E!=null && F!=null) {
        var adult = adults.removeFirst();
        var m1 = minors.removeFirst();
        var m2 = minors.removeFirst();
        out.put(adult.boardingPassId(), E.seatId());
        out.put(m1.boardingPassId(), D.seatId());
        out.put(m2.boardingPassId(), F.seatId());
        row.remove(D); row.remove(E); row.remove(F);
        return true;
      }
    }
    return false;
  }

  private static boolean takePair(TreeMap<Integer, List<SeatSlot>> freeRows, Map<Long,Long> out, Deque<PassengerDto> adults, Deque<PassengerDto> minors) {
    for (var e : freeRows.entrySet()) {
      var row = e.getValue();

      Pair AB = pair(row,"A","B");
      if (AB!=null) {
        var a = adults.removeFirst();
        var m = minors.removeFirst();
        out.put(a.boardingPassId(), AB.first.seatId());
        out.put(m.boardingPassId(), AB.second.seatId());
        row.remove(AB.first); row.remove(AB.second);
        return true;
      }

      Pair BC = pair(row,"B","C");
      if (BC!=null) {
        var a = adults.removeFirst();
        var m = minors.removeFirst();
        out.put(a.boardingPassId(), BC.first.seatId());
        out.put(m.boardingPassId(), BC.second.seatId());
        row.remove(BC.first); row.remove(BC.second);
        return true;
      }

      Pair DE = pair(row,"D","E");
      if (DE!=null) {
        var a = adults.removeFirst();
        var m = minors.removeFirst();
        out.put(a.boardingPassId(), DE.first.seatId());
        out.put(m.boardingPassId(), DE.second.seatId());
        row.remove(DE.first); row.remove(DE.second);
        return true;
      }

      Pair EF = pair(row,"E","F");
      if (EF!=null) {
        var a = adults.removeFirst();
        var m = minors.removeFirst();
        out.put(a.boardingPassId(), EF.first.seatId());
        out.put(m.boardingPassId(), EF.second.seatId());
        row.remove(EF.first); row.remove(EF.second);
        return true;
      }
    }
    return false;
  }

  private static boolean takeSingle(TreeMap<Integer, List<SeatSlot>> freeRows, Map<Long,Long> out, Long bpId) {
    for (var e : freeRows.entrySet()) {
      var row = e.getValue();
      if (!row.isEmpty()) {
        var s = row.remove(0);
        out.put(bpId, s.seatId());
        return true;
      }
    }
    return false;
  }

  private static SeatSlot find(List<SeatSlot> row, String col) {
    for (var s : row) if (s.col().equals(col)) return s;
    return null;
  }

  private static Pair pair(List<SeatSlot> row, String c1, String c2) {
    var a = find(row,c1);
    var b = find(row,c2);
    if (a!=null && b!=null) return new Pair(a,b);
    return null;
  }

  private record Pair(SeatSlot first, SeatSlot second) {}
}
