package ar.edu.itba.pod.server.Models;

import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import rideBooking.Models.ReservationState;

public class Reservation implements Comparable<Reservation> {
    private final UUID visitorId;
    private ReservationState state;
    private final int day;
    private final ParkLocalTime time;

    public Reservation(UUID visitorId, ReservationState state, int day, ParkLocalTime time) {
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

    public ParkLocalTime getTime() {
        return time;
    }

    public void setState(ReservationState state) {
        this.state = state;
    }

    public void confirm(){
        setState(ReservationState.CONFIRMED);
    }

    public void relocate(){
        setState(ReservationState.RELOCATED);
    }

    public void cancel(){
        setState(ReservationState.CANCELLED);
    }

    public boolean isConfirmed(){
        return this.state == ReservationState.CONFIRMED;
    }

    @Override
    public int compareTo(Reservation other) {
        int comp = Integer.compare(this.day, other.day);
        if (comp == 0) {
            return this.time.compareTo(other.time);
        }
        return comp;
    }

    /* Note that 'status' is not part of the equals() method */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return day == that.day && visitorId.equals(that.visitorId) && time.equals(that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visitorId, day, time);
    }
}
