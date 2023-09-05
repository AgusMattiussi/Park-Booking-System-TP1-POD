package ar.edu.itba.pod.server.Models;

import java.time.LocalTime;
import java.util.UUID;

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
}
