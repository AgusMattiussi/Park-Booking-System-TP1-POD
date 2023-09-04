package ar.edu.itba.pod.server.Models;


import java.sql.Time;
import java.util.*;

public class Ride {
    private final String name;

    private final RideTime rideTime;
    private final int slotTime;

    // <Date, <Slot Time, Capacity>>, ordered by date
    private final Map<Date, Map<Time,Integer>> slotsPerDay;

    public Ride(String name, RideTime rideTime, int slotTime) {
        this.name = name;
        this.rideTime = rideTime;
        this.slotTime = slotTime;
        this.slotsPerDay = new TreeMap<Date, Map<Time, Integer>>();
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

    public Map<Date, Map<Time, Integer>> getSlotsPerDay() {
        return slotsPerDay;
    }
}
