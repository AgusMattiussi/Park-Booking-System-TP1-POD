package ar.edu.itba.pod.server.Models;


import java.sql.Time;
import java.time.LocalTime;
import java.util.*;

public class Ride {
    private static int rideCount = 0; //FIXME: no se si es la mejor implementación pero bué

    private final String name;
    private final int id;

    private final RideTime rideTime;
    private final int slotTime;

    // <Date, <Slot Time, Capacity>>, ordered by date
    private final Map<Integer, Map<Integer, List<Reservation>>> slotsPerDay;

    public Ride(String name, RideTime rideTime, Integer slotTime){
        this(getNextId(), name, rideTime, slotTime);
    }
    public Ride(int id, String name, RideTime rideTime, int slotTime) {
        this.name = name;
        this.id = id;
        this.rideTime = rideTime;
        this.slotTime = slotTime;
        this.slotsPerDay = new TreeMap<>();
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

    public Map<Integer, Map<Integer, List<Reservation>>> getSlotsPerDay() {
        return slotsPerDay;
    }

    public int getId() {
        return id;
    }

    private static synchronized int getNextId(){
        return rideCount++;
    }

}
