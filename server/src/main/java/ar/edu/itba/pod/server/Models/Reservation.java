package ar.edu.itba.pod.server.Models;

import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import rideBooking.Models.ReservationState;

public class Reservation implements Comparable<Reservation> {
    private final UUID visitorId;
    private ReservationState state;
    private final int day;
    private final LocalTime time;

    public Reservation(UUID visitorId, ReservationState state, int day, LocalTime time) {
        this.visitorId = visitorId;
        this.state = state;
        this.day = day;
        this.time = time;
    }

    public UUID getVisitorId() {
        return visitorId;
    }

    public ReservationState getState() {
        return state;
    }

    public int getDay() {
        return day;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setState(ReservationState state) {
        this.state = state;
    }

    public void confirm(){
        setState(ReservationState.ACCEPTED);
    }

    public void cancel(){
        setState(ReservationState.CANCELLED);
    }

    public boolean isConfirmed(){
        return this.state == ReservationState.ACCEPTED;
    }

    @Override
    public int compareTo(Reservation other) {
        int dayDiff = this.day - other.day;
        if (dayDiff != 0) {
            return dayDiff;
        }

        return this.time.compareTo(other.time);
    }

    /* Note that 'status' is not part of the equals() method */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return day == that.day && Objects.equals(visitorId, that.visitorId) && Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visitorId, state, day, time);
    }
}
