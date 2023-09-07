package ar.edu.itba.pod.server.Models;


import ar.edu.itba.pod.server.exceptions.SlotCapacityException;
import com.google.protobuf.StringValue;
import rideBooking.RideBookingServiceOuterClass;

import java.time.LocalTime;
import java.util.*;

//TODO: Cambiar las colecciones a su version concurrente
public class Ride implements GRPCModel<rideBooking.RideBookingServiceOuterClass.Ride>{
    private static int rideCount = 0; //FIXME: no se si es la mejor implementación pero bué

    private final String name;
    private final int id;

    private final RideTime rideTime;
    private final int slotTime;
    private Map<Integer, Integer> slotCapacityPerDay;

    // <Date, <Slot Time, Capacity>>, ordered by date
    private final Map<Integer,Map<LocalTime, Integer>> slotsPerDay;

    private final Map<Integer, Map<LocalTime, List<Reservation>>> reservationsPerDay;

    private List<Reservation> cancelledReservations;

    public Ride(String name, RideTime rideTime, Integer slotTime){
        this(getNextId(), name, rideTime, slotTime);
    }
    public Ride(int id, String name, RideTime rideTime, int slotTime) {
        this.name = name;
        this.id = id;
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

    public Map<Integer, Map<LocalTime, Integer>> getSlotsPerDay() {
        return slotsPerDay;
    }

    public int getId() {
        return id;
    }

    public Integer getSlotCapacityPerDay(Integer day) {
        return slotCapacityPerDay.get(day);
    }

    public Map<Integer, Map<LocalTime, List<Reservation>>> getReservationsPerDay() {
        return reservationsPerDay;
    }

    private static synchronized int getNextId(){
        return rideCount++;
    }

    public void setSlotCapacityPerDay(Integer day, Integer slotCapacity) {
        if (isSlotCapacitySet(day)){
            throw new SlotCapacityException("Capacity is already setted for day " + day + " in ride " + this.name);
        }else {
            slotCapacityPerDay.put(day, slotCapacity);
        }
    }

    public void setCancelledReservations(List<Reservation> cancelledReservations) {
        if(this.cancelledReservations.isEmpty()){
            this.cancelledReservations = cancelledReservations;
        }else{
            this.cancelledReservations.addAll(cancelledReservations);
        }
    }

    public boolean isSlotValid(int day, LocalTime time) {
        return slotsPerDay.containsKey(day) && slotsPerDay.get(day).containsKey(time);
    }

    public boolean isSlotCapacitySet(Integer day) {
        return slotCapacityPerDay.containsKey(day);
    }

    //TODO: Ver si esta es la unica implementacion posible (o si algun metodo necesita otra)
    @Override
    public RideBookingServiceOuterClass.Ride convertToGRPC() {
        return RideBookingServiceOuterClass.Ride.newBuilder()
                .setName(StringValue.of(this.name))
                .setOpeningTime(StringValue.of(this.rideTime.getOpenTimeFormattedString()))
                .setClosingTime(StringValue.of(this.rideTime.getCloseTimeFormattedString()))
                .build();
    }


}
