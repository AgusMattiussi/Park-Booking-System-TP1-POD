package ar.edu.itba.pod.server.persistance;

import ar.edu.itba.pod.server.Models.Ride;
import ar.edu.itba.pod.server.Models.RideTime;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class RideRepository {

    private static RideRepository instance;
    private final ConcurrentMap<String, Ride> rides;
    // TODO: Considerar cual es el caso de uso mas comun para definir el mapeo
    /* Maps ride names to a set of <User ID, List of time slots reserved> */
    private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentSkipListSet<LocalDateTime>>> bookedRides;

    private RideRepository() {
        this.rides = new ConcurrentHashMap<>();
        this.bookedRides = new ConcurrentHashMap<>();
    }

    public static RideRepository getInstance() {
        if (instance == null) {
            instance = new RideRepository();
        }
        return instance;
    }

    public Optional<Ride> addRide(String name, RideTime rideTime, int slotTime) {
        Ride ride = new Ride(name, rideTime, slotTime);
        this.rides.put(ride.getName(), ride);
        return Optional.of(ride);
    }

    public Ride getRide(String name) {
        return this.rides.get(name);
    }

    public Map<String, Ride> getRides() {
        return this.rides;
    }

    public boolean containsRide(String name) {
        return this.rides.containsKey(name);
    }

    public boolean removeRide(String name) {
        return this.rides.remove(name) != null;
    }

    public boolean bookRide(String rideName, String visitorId, LocalDateTime time) {
        if (!this.bookedRides.containsKey(rideName))
            this.bookedRides.put(rideName, new ConcurrentHashMap<>());

        ConcurrentMap<String, ConcurrentSkipListSet<LocalDateTime>> rideBookings = this.bookedRides.get(rideName);
        if (!rideBookings.containsKey(visitorId))
            rideBookings.put(visitorId, new ConcurrentSkipListSet<>());

        ConcurrentSkipListSet<LocalDateTime> visitorBookings = rideBookings.get(visitorId);
        return visitorBookings.add(time);
    }

}
