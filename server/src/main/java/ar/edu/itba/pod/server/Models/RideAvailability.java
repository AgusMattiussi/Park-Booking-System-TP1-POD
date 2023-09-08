package ar.edu.itba.pod.server.Models;

import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import rideBooking.RideBookingServiceOuterClass;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class RideAvailability implements Comparable<RideAvailability>, GRPCModel<RideBookingServiceOuterClass.TimeSlotAvailability> {
    private final ParkLocalTime timeSlot;
    private final int pendingBookingsCount;
    private final int confirmedBookingsCount;
    private final int rideCapacity;

    public RideAvailability(ParkLocalTime timeSlot, int pendingBookingsCount, int confirmedBookingsCount, int rideCapacity) {
        this.timeSlot = timeSlot;
        this.pendingBookingsCount = pendingBookingsCount;
        this.confirmedBookingsCount = confirmedBookingsCount;
        this.rideCapacity = rideCapacity;
    }

    public ParkLocalTime getTimeSlot() {
        return timeSlot;
    }

    public int getPendingBookingsCount() {
        return pendingBookingsCount;
    }

    public int getConfirmedBookingsCount() {
        return confirmedBookingsCount;
    }

    public int getRideCapacity() {
        return rideCapacity;
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


    @Override
    public RideBookingServiceOuterClass.TimeSlotAvailability convertToGRPC() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        return RideBookingServiceOuterClass.TimeSlotAvailability.newBuilder()
                       .setTimeSlot(StringValue.of(timeSlot.toString()))
                       .setConfirmedBookings(Int32Value.of(confirmedBookingsCount))
                       .setPendingBookings(Int32Value.of(pendingBookingsCount))
                       .setRideCapacity(Int32Value.of(rideCapacity))
                       .build();
    }
}
