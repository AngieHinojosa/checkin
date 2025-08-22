package com.bsalecheckin.service;

import com.bsalecheckin.dto.PassengerDto;
import com.bsalecheckin.dto.SeatSlot;

import java.util.*;
import java.util.stream.Collectors;

public class SeatAssigner {
  private final List<SeatSlot> allSeats;
  private final List<PassengerDto> passengers;

  public SeatAssigner(List<SeatSlot> allSeats, List<PassengerDto> passengers) {
    this.allSeats = allSeats;
    this.passengers = passengers;
  }

  public List<PassengerDto> assign() {
    var takenSeatIds = passengers.stream().map(PassengerDto::seatId).filter(Objects::nonNull).collect(Collectors.toSet());
    var seatById = allSeats.stream().collect(Collectors.toMap(SeatSlot::seatId, s -> s));
    var freeByTypeByRow = new HashMap<Integer, TreeMap<Integer, List<SeatSlot>>>();
    for (var s : allSeats) {
      if (takenSeatIds.contains(s.seatId())) continue;
      freeByTypeByRow.computeIfAbsent(s.seatTypeId(), k -> new TreeMap<>())
        .computeIfAbsent(s.row(), k -> new ArrayList<>()).add(s);
    }
    for (var map : freeByTypeByRow.values()) for (var list : map.values())
      list.sort(Comparator.comparingInt(a -> colIndex(a.col())));

    var assignedByBp = new HashMap<Long, Long>();
    var groups = passengers.stream().collect(Collectors.groupingBy(PassengerDto::purchaseId, LinkedHashMap::new, Collectors.toList()));

    for (var group : groups.values()) {
      var byType = group.stream().collect(Collectors.groupingBy(PassengerDto::seatTypeId, LinkedHashMap::new, Collectors.toList()));
      for (var e : byType.entrySet()) {
        int seatType = e.getKey();
        var pax = e.getValue();
        for (var p : pax) if (p.seatId() != null) assignedByBp.put(p.boardingPassId(), p.seatId());

        var needing = pax.stream().filter(p -> p.seatId() == null).collect(Collectors.toList());
        if (needing.isEmpty()) continue;

        var minors = needing.stream().filter(p -> p.age() != null && p.age() < 18).collect(Collectors.toCollection(LinkedList::new));
        var adults = needing.stream().filter(p -> p.age() == null || p.age() >= 18).collect(Collectors.toCollection(LinkedList::new));
        var fixedAdults = pax.stream().filter(p -> p.seatId() != null && (p.age() == null || p.age() >= 18)).toList();

        var freeRows = freeByTypeByRow.getOrDefault(seatType, new TreeMap<>());
        var usedNow = new HashSet<Long>();

        for (var fa : fixedAdults) {
          if (minors.isEmpty()) break;
          var aSeat = seatById.get(fa.seatId());
          if (aSeat == null) continue;
          for (var nc : neighborColumns(aSeat.col())) {
            var neigh = findSeat(freeRows, aSeat.row(), nc);
            if (neigh != null) {
              var m = minors.removeFirst();
              assignedByBp.put(m.boardingPassId(), neigh.seatId());
              markTaken(freeRows, neigh);
              usedNow.add(neigh.seatId());
              break;
            }
          }
        }

        if (!minors.isEmpty()) {
          var rows = new ArrayList<>(freeRows.keySet());
          boolean placed = false;
          outer:
          for (int i = 0; i < rows.size(); i++) {
            var tmpAssigned = new HashMap<Long, Long>();
            var tmpUsed = new HashSet<Long>();
            var tmpFree = deepCopy(freeRows);

            var minorsCopy = new LinkedList<>(minors);
            var adultsCopy = new LinkedList<>(adults);

            for (int j = i; j < rows.size() && (!minorsCopy.isEmpty() || !adultsCopy.isEmpty()); j++) {
              int row = rows.get(j);
              var pairs = listPairs(tmpFree.get(row));
              while (!pairs.isEmpty() && !minorsCopy.isEmpty()) {
                var pr = pairs.removeFirst();
                if (!adultsCopy.isEmpty()) {
                  var a = adultsCopy.removeFirst();
                  tmpAssigned.put(a.boardingPassId(), pr.adultSeat.seatId());
                  tmpUsed.add(pr.adultSeat.seatId());
                  markTaken(tmpFree, pr.adultSeat);
                }
                var m = minorsCopy.removeFirst();
                tmpAssigned.put(m.boardingPassId(), pr.minorSeat.seatId());
                tmpUsed.add(pr.minorSeat.seatId());
                markTaken(tmpFree, pr.minorSeat);
              }
              var list = tmpFree.getOrDefault(row, new ArrayList<>());
              var it = list.iterator();
              while (it.hasNext() && !adultsCopy.isEmpty()) {
                var s = it.next();
                if (tmpUsed.contains(s.seatId())) { it.remove(); continue; }
                var a = adultsCopy.removeFirst();
                tmpAssigned.put(a.boardingPassId(), s.seatId());
                tmpUsed.add(s.seatId());
                it.remove();
              }
            }

            if (minorsCopy.isEmpty()) {
              assignedByBp.putAll(tmpAssigned);
              freeRows.clear(); freeRows.putAll(tmpFree);
              usedNow.addAll(tmpUsed);
              placed = true;
              break outer;
            }
          }

          if (!placed) {
            while (!minors.isEmpty() && (!adults.isEmpty() || !fixedAdults.isEmpty())) {
              var m = minors.removeFirst();
              Long adultSeatId = null;
              if (!adults.isEmpty() && assignedByBp.containsKey(adults.peekFirst().boardingPassId()))
                adultSeatId = assignedByBp.get(adults.peekFirst().boardingPassId());
              if (adultSeatId == null && !fixedAdults.isEmpty())
                adultSeatId = fixedAdults.get(0).seatId();
              boolean seated = false;
              if (adultSeatId != null) {
                var a = seatById.get(adultSeatId);
                for (var nc : neighborColumns(a.col())) {
                  var neigh = findSeat(freeRows, a.row(), nc);
                  if (neigh != null) {
                    assignedByBp.put(m.boardingPassId(), neigh.seatId());
                    markTaken(freeRows, neigh);
                    usedNow.add(neigh.seatId());
                    seated = true;
                    break;
                  }
                }
              }
              if (!seated) {
                var any = takeAny(freeRows);
                if (any != null) {
                  assignedByBp.put(m.boardingPassId(), any.seatId());
                  markTaken(freeRows, any);
                  usedNow.add(any.seatId());
                } else {
                  break;
                }
              }
            }
            while (!adults.isEmpty()) {
              var a = adults.removeFirst();
              var any = takeAny(freeRows);
              if (any == null) break;
              assignedByBp.put(a.boardingPassId(), any.seatId());
              markTaken(freeRows, any);
              usedNow.add(any.seatId());
            }
          }
        } else {
          var rows = new ArrayList<>(freeRows.keySet());
          for (int j = 0; j < rows.size() && !adults.isEmpty(); j++) {
            int row = rows.get(j);
            var list = freeRows.getOrDefault(row, new ArrayList<>());
            var it = list.iterator();
            while (it.hasNext() && !adults.isEmpty()) {
              var s = it.next();
              var a = adults.removeFirst();
              assignedByBp.put(a.boardingPassId(), s.seatId());
              it.remove();
            }
          }
        }
      }
    }

    var out = new ArrayList<PassengerDto>(passengers.size());
    for (var p : passengers) {
      var sid = p.seatId() != null ? p.seatId() : assignedByBp.get(p.boardingPassId());
      out.add(new PassengerDto(
        p.passengerId(), p.dni(), p.name(), p.age(), p.country(),
        p.boardingPassId(), p.purchaseId(), p.seatTypeId(), sid
      ));
    }
    return out;
  }

  private static class Pair {
    final SeatSlot adultSeat;
    final SeatSlot minorSeat;
    Pair(SeatSlot a, SeatSlot m) { adultSeat = a; minorSeat = m; }
  }

  private static LinkedList<Pair> listPairs(List<SeatSlot> freeRow) {
    var out = new LinkedList<Pair>();
    if (freeRow == null || freeRow.isEmpty()) return out;
    SeatSlot A=null,B=null,C=null,D=null,E=null,F=null;
    for (var s : freeRow) {
      if (s.col().equals("A")) A=s;
      else if (s.col().equals("B")) B=s;
      else if (s.col().equals("C")) C=s;
      else if (s.col().equals("D")) D=s;
      else if (s.col().equals("E")) E=s;
      else if (s.col().equals("F")) F=s;
    }
    if (A!=null && B!=null) out.add(new Pair(B, A));
    if (B!=null && C!=null) out.add(new Pair(B, C));
    if (D!=null && E!=null) out.add(new Pair(E, D));
    if (E!=null && F!=null) out.add(new Pair(E, F));
    return out;
  }

  private static SeatSlot findSeat(TreeMap<Integer, List<SeatSlot>> freeRows, int row, String col) {
    var list = freeRows.get(row);
    if (list == null) return null;
    for (var s : list) if (s.col().equals(col)) return s;
    return null;
  }

  private static void markTaken(TreeMap<Integer, List<SeatSlot>> freeRows, SeatSlot seat) {
    var list = freeRows.get(seat.row());
    if (list == null) return;
    list.removeIf(s -> s.seatId().equals(seat.seatId()));
  }

  private static SeatSlot takeAny(TreeMap<Integer, List<SeatSlot>> freeRows) {
    for (var e : freeRows.entrySet()) {
      var list = e.getValue();
      if (list.isEmpty()) continue;
      return list.remove(0);
    }
    return null;
  }

  private static List<String> neighborColumns(String c) {
    var cols = "ABCDEF";
    int i = cols.indexOf(c.charAt(0));
    if (i < 0) return List.of();
    if (i <= 2) {
      if (i == 0) return List.of("B");
      if (i == 1) return List.of("A", "C");
      return List.of("B");
    } else {
      if (i == 3) return List.of("E");
      if (i == 4) return List.of("D", "F");
      return List.of("E");
    }
  }

  private static int colIndex(String c) {
    var cols = "ABCDEF";
    return cols.indexOf(c.charAt(0));
  }

  private static TreeMap<Integer, List<SeatSlot>> deepCopy(TreeMap<Integer, List<SeatSlot>> src) {
    var copy = new TreeMap<Integer, List<SeatSlot>>();
    for (var e : src.entrySet()) copy.put(e.getKey(), new ArrayList<>(e.getValue()));
    return copy;
  }
}
