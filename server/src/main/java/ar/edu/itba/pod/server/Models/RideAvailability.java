package ar.edu.itba.pod.server.Models;

import java.time.LocalTime;
import java.util.Objects;

public class RideAvailability implements Comparable<RideAvailability> {
    private LocalTime timeSlot;
    private int pendingBookingsCount;
    private int confirmedBookingsCount;
    private int rideCapacity;

    public RideAvailability(LocalTime timeSlot, int pendingBookingsCount, int confirmedBookingsCount, int rideCapacity) {
        this.timeSlot = timeSlot;
        this.pendingBookingsCount = pendingBookingsCount;
        this.confirmedBookingsCount = confirmedBookingsCount;
        this.rideCapacity = rideCapacity;
    }

    public LocalTime getTimeSlot() {
        return timeSlot;
    }

    public int getPendingBookingsCount() {
        return pendingBookingsCount;
    }

    public int getConfirmedBookingsCount() {
        return confirmedBookingsCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RideAvailability that = (RideAvailability) o;
        return pendingBookingsCount == that.pendingBookingsCount && confirmedBookingsCount == that.confirmedBookingsCount && Objects.equals(timeSlot, that.timeSlot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeSlot, pendingBookingsCount, confirmedBookingsCount);
    }

    @Override
    public int compareTo(RideAvailability o) {
        return this.timeSlot.compareTo(o.timeSlot);
    }
}
