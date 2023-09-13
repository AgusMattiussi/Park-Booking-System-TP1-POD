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
    public Ride(String name, RideTime rideTime) {
        this.name = name;
        this.rideTime = rideTime;
        this.slotsLeftByDayAndTimeSlot = new ConcurrentHashMap<>();
        this.slotCapacityByDay = new ConcurrentHashMap<>();
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

    public boolean isTimeSlotValid(ParkLocalTime time) {
        return rideTime.isTimeSlotValid(time);
    }

    public boolean isTimeSlotValid(String time) {
        return rideTime.isTimeSlotValid(time);
    }

    public List<ParkLocalTime> getTimeSlotsBetween(ParkLocalTime startTimeSlot, ParkLocalTime endTimeSlot){
        return rideTime.getTimeSlotsBetween(startTimeSlot, endTimeSlot);
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
