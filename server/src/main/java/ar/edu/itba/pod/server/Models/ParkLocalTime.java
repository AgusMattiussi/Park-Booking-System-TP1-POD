package ar.edu.itba.pod.server.Models;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/*
 *  This class is used to represent the time of a ride, which is a LocalTime with only the hour
 *  and minute fields set.
 *
 *  Ideally, this class would extend from LocalTime (java.time), but since LocalTime is declared
 *  as a final class, this is not possible. Therefore, this class is a wrapper for LocalTime.
 */
public class ParkLocalTime {

    private final LocalTime time;

    /* Returns a ParkLocalTime parsed from a HH:mm formatted String */
    public static ParkLocalTime fromString(String time) {
        DateTimeFormatter formatter;

        try {
            formatter = DateTimeFormatter.ofPattern("HH:mm");
        } catch (Exception e) {
            throw new IllegalArgumentException("Time must be in HH:mm format");
        }

        return new ParkLocalTime(LocalTime.parse(time, formatter));
    }

    public ParkLocalTime(LocalTime time) {
        if(time.getSecond() != 0 || time.getNano() != 0)
            throw new IllegalArgumentException("Time must be in HH:mm format");

        this.time = time;
    }

    public LocalTime getTime() {
        return time;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }

}
