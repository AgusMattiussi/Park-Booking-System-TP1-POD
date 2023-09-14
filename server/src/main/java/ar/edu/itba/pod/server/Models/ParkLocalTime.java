package ar.edu.itba.pod.server.Models;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Objects;

/*
 *  This class is used to represent the time of a ride, which is a LocalTime with only the hour
 *  and minute fields set.
 *
 *  Ideally, this class would extend from LocalTime (java.time), but since LocalTime is declared
 *  as a final class, this is not possible. Therefore, this class is a wrapper for LocalTime.
 */
public class ParkLocalTime implements Comparable<ParkLocalTime> {

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

    public LocalTime asLocalTime() {
        return time;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkLocalTime that = (ParkLocalTime) o;
        return time.equals(that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time);
    }

    @Override
    public int compareTo(ParkLocalTime o) {
        return time.compareTo(o.asLocalTime());
    }

    public long until(ParkLocalTime endExclusive, TemporalUnit unit) {
        return time.until(endExclusive.asLocalTime(), unit);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        return time.until(endExclusive, unit);
    }

    public boolean isBefore(ParkLocalTime other) {
        return time.isBefore(other.asLocalTime());
    }

    public boolean isAfter(ParkLocalTime other) {
        return time.isAfter(other.asLocalTime());
    }

    public ParkLocalTime plusMinutes(long minutesToAdd) {
        return new ParkLocalTime(time.plusMinutes(minutesToAdd));
    }


    public long until(ParkLocalTime close) {
        return time.until(close.asLocalTime(), ChronoUnit.MINUTES);
    }
}
