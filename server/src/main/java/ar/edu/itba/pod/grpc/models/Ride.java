package ar.edu.itba.pod.grpc.models;

import java.sql.Time;
import java.util.*;

public class Ride {
    private final String name;
    private final Time openingTime;
    private final Time closingTime;
    private final int slotTime;

    // <Date, <Slot Time, Capacity>>, ordered by date
    private final Map<Date, Map<Time,Integer>> slotsPerDay;

    public Ride(String name, Time openingTime, Time closingTime, int slotTime) {
        this.name = name;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.slotTime = slotTime;
        this.slotsPerDay = new TreeMap<Date, Map<Time, Integer>>();
    }

    public String getName() {
        return name;
    }

    public Time getOpeningTime() {
        return openingTime;
    }

    public Time getClosingTime() {
        return closingTime;
    }

    public int getSlotTime() {
        return slotTime;
    }

    public Map<Date, Map<Time, Integer>> getSlotsPerDay() {
        return slotsPerDay;
    }
}
