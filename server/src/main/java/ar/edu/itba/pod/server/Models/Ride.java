package ar.edu.itba.pod.server.Models;


import ar.edu.itba.pod.server.exceptions.SlotCapacityException;
import com.google.protobuf.StringValue;
import rideBooking.RideBookingServiceOuterClass;
import rideBooking.Models.ReservationState;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//TODO: Cambiar las colecciones a su version concurrente
public class Ride implements GRPCModel<rideBooking.RideBookingServiceOuterClass.Ride>{

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final String name;

    private final RideTime rideTime;
    private final int slotTime;
    private Map<Integer, Integer> slotCapacityPerDay;

    // <Date, <Slot Time, Capacity>>, ordered by date
    private final Map<Integer,Map<ParkLocalTime, Integer>> slotsPerDay;

    private final Map<Integer, Map<ParkLocalTime, List<Reservation>>> reservationsPerDay;

    private List<Reservation> cancelledReservations;

    public Ride(String name, RideTime rideTime, int slotTime) {
        this.name = name;
        this.rideTime = rideTime;
        this.slotTime = slotTime;
        this.slotsPerDay = new TreeMap<>();
        this.slotCapacityPerDay = new TreeMap<>();
        this.reservationsPerDay = new TreeMap<>();
        this.cancelledReservations = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public RideTime getRideTime() {
        return rideTime;
    }

    public int getSlotTime() {
        return slotTime;
    }

    public Map<Integer, Map<ParkLocalTime, Integer>> getSlotsPerDay() {
        return slotsPerDay;
    }


    public Integer getSlotCapacityPerDay(Integer day) {
        lockRead();
        Integer slotCapacity = slotCapacityPerDay.get(day);
        unlockRead();
        return slotCapacity;
    }

    public Map<Integer, Map<ParkLocalTime, List<Reservation>>> getReservationsPerDay() {
        lockRead();
        Map<Integer, Map<ParkLocalTime, List<Reservation>>> reservations = reservationsPerDay;
        unlockRead();
        return reservations;
    }

    private Optional<List<Reservation>> getReservationsForTimeSlot(int day, ParkLocalTime timeSlot) {
        Optional<List<Reservation>> optional = Optional.empty();
        lockRead();
        if (reservationsPerDay.containsKey(day) && reservationsPerDay.get(day).containsKey(timeSlot))
            optional = Optional.of(reservationsPerDay.get(day).get(timeSlot));
        unlockRead();
        return optional;
    }

    public int getCapacityForTimeSlot(int day, ParkLocalTime timeSlot) {
        int capacity = 0;
        lockRead();
        if (slotsPerDay.containsKey(day) && slotsPerDay.get(day).containsKey(timeSlot))
            capacity = slotsPerDay.get(day).get(timeSlot);
        unlockRead();
        return capacity;
    }

    private int countStateForTimeSlot(int day, ParkLocalTime timeSlot, ReservationState state) {
        Optional<List<Reservation>> reservations = getReservationsForTimeSlot(day, timeSlot);
        int statesForTimeSlot = 0;
        lockRead();
        statesForTimeSlot = reservations.map(reservationList -> (int) reservationList.stream().filter(
                        reservation -> reservation.getState() == state).count())
                .orElse(0);
        unlockRead();
        return statesForTimeSlot;
    }
    public int getConfirmedCountForTimeSlot(int day, ParkLocalTime timeSlot) {
        return countStateForTimeSlot(day, timeSlot, ReservationState.CONFIRMED);
    }

    public int getPendingCountForTimeSlot(int day, ParkLocalTime timeSlot) {
        return countStateForTimeSlot(day, timeSlot, ReservationState.PENDING);
    }


    public void setSlotCapacityPerDay(Integer day, Integer slotCapacity) {
        if (isSlotCapacitySet(day)){
            throw new SlotCapacityException("Capacity is already setted for day " + day + " in ride " + this.name);
        }else {
            lockWrite();
            slotCapacityPerDay.put(day, slotCapacity);
            unlockWrite();
        }
    }

    public void addCancelledReservations(Reservation cancelledReservation) {
        lockWrite();
        this.cancelledReservations.add(cancelledReservation);
        unlockWrite();
    }


    public boolean isSlotValid(int day, ParkLocalTime time) {
        //return true;
        //TODO: Descomentar
        lockRead();
        boolean isSet = slotsPerDay.containsKey(day) && slotsPerDay.get(day).containsKey(time);
        unlockRead();
        return isSet;
    }

    public boolean isSlotCapacitySet(Integer day) {
        lockRead();
        boolean isSet = slotCapacityPerDay.containsKey(day);
        unlockRead();
        return isSet;
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


    public void lockRead(){ lock.readLock().lock();}

    public void lockWrite(){ lock.writeLock().lock();}

    public void unlockRead(){ lock.readLock().unlock();}

    public void unlockWrite(){ lock.writeLock().unlock();}

}
