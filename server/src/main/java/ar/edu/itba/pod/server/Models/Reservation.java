package ar.edu.itba.pod.server.Models;

import java.time.LocalTime;
import java.util.UUID;
import rideBooking.Models.ReservationState;

public class Reservation {
    UUID visitorId;
    ReservationState state;
    int day;
    LocalTime time;

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
}
