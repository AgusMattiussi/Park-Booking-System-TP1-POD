package ar.edu.itba.pod.server.Models;


import ar.edu.itba.pod.server.exceptions.SlotCapacityException;
import com.google.protobuf.StringValue;
import rideBooking.RideBookingServiceOuterClass;
import rideBooking.Models.ReservationState;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//TODO: Cambiar las colecciones a su version concurrente
//TODO: Cambiar a Atomics
public class Ride implements GRPCModel<rideBooking.RideBookingServiceOuterClass.Ride>{

    private final String name;
    private final RideTime rideTime;
    private final Map<Integer, Map<String, AtomicInteger>> slotsLeftByDayAndTimeSlot;
    private final Map<Integer, Integer> slotCapacityByDay;
    //TODO: Volarlo
    private final Map<Integer, Map<ParkLocalTime, Set<Reservation>>> reservationsPerDay;
    //TODO: Ver si es necesario
    private final Set<Reservation> cancelledReservations;

    public Ride(String name, RideTime rideTime, int slotTime) {
        this.name = name;
        this.rideTime = rideTime;
        this.slotsLeftByDayAndTimeSlot = new ConcurrentHashMap<>();
        this.slotCapacityByDay = new ConcurrentHashMap<>();
        this.reservationsPerDay = new ConcurrentHashMap<>();
        //TODO: Concurrent
        this.cancelledReservations = new ConcurrentSkipListSet<>();
    }

    public String getName() {
        return name;
    }

    public RideTime getRideTime() {
        return rideTime;
    }

    public Duration getTimeSlotDuration() {
        return rideTime.getTimeSlotDuration();
    }

    public Map<Integer, Integer> getSlotCapacityByDay() {
        return slotCapacityByDay;
    }

    // -1 Means no capacity set
    public int getSlotCapacityForDay(int day) {
        return slotCapacityByDay.getOrDefault(day, -1);
    }

    public AtomicInteger getSlotsLeft(int day, String timeSlot) {
        return slotsLeftByDayAndTimeSlot.getOrDefault(day, new HashMap<>()).getOrDefault(timeSlot, null);
    }

    public AtomicInteger getSlotsLeft(int day, ParkLocalTime timeSlot) {
        return getSlotsLeft(day, timeSlot.toString());
    }

    public void decrementCapacity(int day, ParkLocalTime timeSlot) {
        AtomicInteger slotsLeft = getSlotsLeft(day, timeSlot);

        if(slotsLeft == null || !slotCapacityByDay.containsKey(day))
            throw new IllegalArgumentException(String.format("%s - Day %d has no capacity yet", name, day));

        if(slotsLeft.get() == 0)
            throw new IllegalArgumentException(String.format("%s - Day %d has no slots left", name, day));

        slotsLeft.decrementAndGet();
    }

    public void incrementCapacity(int day, ParkLocalTime timeSlot){
        AtomicInteger slotsLeft = getSlotsLeft(day, timeSlot);

        if(slotsLeft == null || !slotCapacityByDay.containsKey(day))
            throw new IllegalArgumentException(String.format("%s - Day %d has no capacity yet", name, day));

        if(slotsLeft.get() == slotCapacityByDay.get(day))
            throw new IllegalArgumentException(String.format("%s - Day %d has reached its maximum capacity. Cannot add a slot", name, day));

        slotsLeft.incrementAndGet();
    }


    public Map<Integer, Map<ParkLocalTime, Set<Reservation>>> getReservationsPerDay() {
        return reservationsPerDay;
    }

    private Optional<Set<Reservation>> getReservationsForTimeSlot(int day, ParkLocalTime timeSlot) {
        Optional<Set<Reservation>> optional = Optional.empty();
        if (reservationsPerDay.containsKey(day) && reservationsPerDay.get(day).containsKey(timeSlot))
            optional = Optional.of(reservationsPerDay.get(day).get(timeSlot));
        return optional;
    }

    private int countStateForTimeSlot(int day, ParkLocalTime timeSlot, ReservationState state) {
        Optional<Set<Reservation>> reservations = getReservationsForTimeSlot(day, timeSlot);

        return reservations.map(reservationList -> (int) reservationList.stream().filter(
                reservation -> reservation.getState() == state).count())
                .orElse(0);
    }

    public int getConfirmedCountForTimeSlot(int day, ParkLocalTime timeSlot) {
        return countStateForTimeSlot(day, timeSlot, ReservationState.CONFIRMED);
    }

    public int getPendingCountForTimeSlot(int day, ParkLocalTime timeSlot) {
        return countStateForTimeSlot(day, timeSlot, ReservationState.PENDING);
    }

    public boolean isSlotCapacitySet(Integer day) {
        return getSlotCapacityForDay(day) != -1;
    }


    public void setSlotCapacityForDay(Integer day, Integer slotCapacity) {
        synchronized (slotCapacityByDay) {
            if (isSlotCapacitySet(day))
                throw new SlotCapacityException(String.format("Capacity is already set for day %d in ride %s", day, this.name));

            slotCapacityByDay.put(day, slotCapacity);

            if(!slotsLeftByDayAndTimeSlot.containsKey(day))
                slotsLeftByDayAndTimeSlot.put(day, new HashMap<>());

            Map<String, AtomicInteger> daySlots = slotsLeftByDayAndTimeSlot.get(day);
            List<String> times = rideTime.getTimeSlotsAsStrings();

            for(String time : times){
                daySlots.put(time, new AtomicInteger(slotCapacity));
            }
        }
    }

    public void addCancelledReservations(Reservation cancelledReservation) {
        cancelledReservations.add(cancelledReservation);
    }

    public boolean isTimeSlotValid(ParkLocalTime time) {
        return rideTime.isTimeSlotValid(time);
    }

    public boolean isTimeSlotValid(String time) {
        return rideTime.isTimeSlotValid(time);
    }


    //TODO: Ver si esta es la unica implementacion posible (o si algun metodo necesita otra)
    @Override
    public RideBookingServiceOuterClass.Ride convertToGRPC() {
        return RideBookingServiceOuterClass.Ride.newBuilder()
                .setName(StringValue.of(this.name))
                .setOpeningTime(StringValue.of(rideTime.getOpen().toString()))
                .setClosingTime(StringValue.of(rideTime.getClose().toString()))
                .build();
    }

}
