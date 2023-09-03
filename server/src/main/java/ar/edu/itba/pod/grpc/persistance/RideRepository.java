package ar.edu.itba.pod.grpc.persistance;

import ar.edu.itba.pod.grpc.models.Ride;

import java.util.HashMap;
import java.util.Map;

public class RideRepository {

    private static RideRepository instance;
    private final Map<String, Ride> rides;

    private RideRepository() {
        this.rides = new HashMap<>();
    }

    public static RideRepository getInstance() {
        if (instance == null) {
            instance = new RideRepository();
        }
        return instance;
    }

    public void addRide(Ride ride) {
        this.rides.put(ride.getName(), ride);
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

}
