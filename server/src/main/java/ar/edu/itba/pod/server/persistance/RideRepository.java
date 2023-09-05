package ar.edu.itba.pod.server.persistance;

import ar.edu.itba.pod.server.Models.ParkPass;
import ar.edu.itba.pod.server.Models.Ride;
import ar.edu.itba.pod.server.Models.RideTime;
import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.InvalidPassTypeException;
import ar.edu.itba.pod.server.exceptions.InvalidTimeException;
import ar.edu.itba.pod.server.exceptions.SlotCapacityException;
import com.google.protobuf.Empty;
import rideBooking.Models;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class RideRepository {

    private static RideRepository instance;
    private final ConcurrentMap<String, Ride> rides;
    private final ConcurrentMap<UUID, ConcurrentMap<Integer,ParkPass>> parkPasses;
    // TODO: Considerar cual es el caso de uso mas comun para definir el mapeo
    /* Maps ride names to a set of <User ID, List of time slots reserved> */
    private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentSkipListSet<LocalDateTime>>> bookedRides;

    private RideRepository() {
        this.rides = new ConcurrentHashMap<>();
        this.parkPasses = new ConcurrentHashMap<>();
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
//        Falla:
//        si existe una atracción con ese nombre
        if (this.rides.containsKey(name)){
            throw new AlreadyExistsException("Already exist a ride called " + name);
        }
//        si los valores de los horarios son inválidos
        if(rideTime.getClose().isBefore(rideTime.getOpen())){
            throw new InvalidTimeException("Closed ride time must be after open ride time");
        }
//        si con los valores provistos no existe un slot posible.
        long minutes = rideTime.getClose().until(ride.getRideTime().getOpen(), ChronoUnit.MINUTES) / slotTime;
        if(minutes == Math.floor(minutes)){
            throw new SlotCapacityException("Slot is not possible, between " + rideTime.getOpen() + " and " + rideTime.getClose());
        }
        this.rides.put(ride.getName(), ride);
        return Optional.of(ride);
    }

    public Optional<ParkPass> addParkPass(UUID visitorId, Models.PassTypeEnum type, int day) {
//        Falla:
//        si el tipo de pase es inválido
        if (type.equals(Models.PassTypeEnum.UNKNOWN)){
            throw new InvalidPassTypeException("There are 3 valid pass types [UNLIMITED, THREE, HALF_DAY]");
        }
//        si el día del año es inválido.
        if (day > 365 || day < 1){
            throw new InvalidTimeException("The day must be between 1 and 365");
        }
        if(this.parkPasses.containsKey(visitorId)){
            if(this.parkPasses.get(visitorId).containsKey(day)){
//              si ya existe un pase para el visitante para el día indicado
                throw new AlreadyExistsException("There already exist a parkPass for the visitor for the day " + day);
            }
        }else{
            this.parkPasses.put(visitorId, new ConcurrentHashMap<>());
        }
        ParkPass parkPass = new ParkPass(visitorId, type, day);
        this.parkPasses.get(visitorId).put(day, parkPass);
        return Optional.of(parkPass);
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
