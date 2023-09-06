package ar.edu.itba.pod.server.persistance;

import ar.edu.itba.pod.server.Models.*;
import ar.edu.itba.pod.server.exceptions.*;
import rideBooking.AdminParkServiceOuterClass;
import rideBooking.Models;
import rideBooking.Models.ReservationState;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    /* Maps visitor ID to ride notifications */
    private final ConcurrentMap<UUID, ConcurrentMap<Integer, ConcurrentSkipListSet<String>>> notifications;

    private RideRepository() {
        this.rides = new ConcurrentHashMap<>();
        this.parkPasses = new ConcurrentHashMap<>();
        this.bookedRides = new ConcurrentHashMap<>();
        this.notifications = new ConcurrentHashMap<>();
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

    private void invalidDate(int day){
        if (day > 365 || day < 1){
            throw new InvalidTimeException("The day must be between 1 and 365");
        }
    }
    public Optional<ParkPass> addParkPass(UUID visitorId, Models.PassTypeEnum type, int day) {
//        Falla:
//        si el tipo de pase es inválido
        if (type.equals(Models.PassTypeEnum.UNKNOWN)){
            throw new InvalidPassTypeException("There are 3 valid pass types [UNLIMITED, THREE, HALF_DAY]");
        }
//        si el día del año es inválido.
        invalidDate(day);
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

    public AdminParkServiceOuterClass.SlotCapacityResponse addSlotsPerDay(String rideName, int day, int capacity){
//        Falla:
//        si la atracción no existe
        if(!this.rides.containsKey(rideName)){
            throw new NotFoundRideException("There is no ride called " + rideName);
        }
//        si el día es inválido
        invalidDate(day);
//        si la capacidad es negativa
        if(capacity < 0){
            throw new SlotCapacityException("Slot capacity must be positive");
        }
        Ride ride = this.rides.get(rideName);
        if (ride.getSlotCapacity() != null){
//            si ya se cargó una capacidad para esa atracción y día
            throw new SlotCapacityException("There already is a loaded capacity for ride " + rideName + " for day " + day);
        }
        ride.setSlotCapacity(capacity);

        Map<Integer, Map<LocalTime, List<Reservation>>> reservationsPerDay = ride.getReservationsPerDay();
        List<Reservation> cancelledReservations = new ArrayList<>();
        int accepted = 0;
        int relocated = 0;
        int cancelled = 0;
        for (Map.Entry<LocalTime, List<Reservation>> reservations: reservationsPerDay.get(day).entrySet()) {
            LocalTime reservationTime = reservations.getKey();
            List<Reservation> reservationList = reservations.getValue();
            int i = 0;
            while (i<reservationList.size()){
                Reservation r = reservationList.get(i);
//              Primero acepto la capacidad que me permite cada slot
                if(i<capacity){
//                  Si la estoy reubicando mantiene el estado de pendiente y sera confirmada luego
                    if(r.getState() == ReservationState.RELOCATED){
                        r.setState(ReservationState.PENDING_WAITING_ACCEPT);
                        relocated+=1;
                    }else{
                        r.setState(ReservationState.ACCEPTED);
                        accepted+=1;
                    }
                }else{
//                  Luego reubico las que no entran y cancelo las que no puedo ubicar
                    for (Map.Entry<LocalTime, List<Reservation>> afterTime: reservationsPerDay.get(day).entrySet()) {
//                      Se deberán intentar todos los slots posteriores al de la reserva original.
                        if(afterTime.getKey().isAfter(reservationTime)){
                            List<Reservation> afterReservations = afterTime.getValue();
//                          La puedo reubicar en caso de que no haya reservas aceptadas o pendientes que superen la capacidad
                            if(afterReservations.size() < capacity - 1){
                                r.setState(ReservationState.RELOCATED);
                                afterReservations.add(r);
                                reservationList.remove(r);
                                break;
                            }
                        }
                    }
//                    Si sigue pendiente es porque no la pude reubicar => cancelo
                    if(r.getState() == ReservationState.PENDING){
                        r.setState(ReservationState.CANCELLED);
                        cancelledReservations.add(r);
                        reservationList.remove(r);
                        cancelled+=1;
                    }
                }
                i++;
            }
        }
        ride.setCancelledReservations(cancelledReservations);

        AdminParkServiceOuterClass.SlotCapacityResponse response = AdminParkServiceOuterClass.SlotCapacityResponse.newBuilder()
                .setAcceptedAmount(accepted).setCancelledAmount(cancelled).setRelocatedAmount(relocated)
                .build();
        return response;
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

    public boolean addVisitor(UUID visitorId, String rideName, int day) {
        /*
        FAIL CONDITIONS:
        - No ride under that name
        - Invalid date
        - Invalid pass
        - Already registered for notification
         */
        if (!this.rides.containsKey(rideName))
            throw new NotFoundRideException("This ride does not exist");

        invalidDate(day);

        if(!this.parkPasses.containsKey(visitorId) || !this.parkPasses.get(visitorId).containsKey(day)){
            throw new NotFoundPassException("No valid pass for day " + day);
        }

        notifications.putIfAbsent(visitorId, new ConcurrentHashMap<>());
        notifications.get(visitorId).putIfAbsent(day, new ConcurrentSkipListSet<>());
        return notifications.get(visitorId).get(day).add(rideName);
    }

    public boolean removeVisitor(UUID visitorId, String rideName, int day) {
        if(!this.notifications.containsKey(visitorId)) {
            throw new NotFoundVisitorException("No user found for the visitorId " + visitorId);
        }

        if(this.notifications.get(visitorId).containsKey(day)) {
            return this.notifications.get(visitorId).get(day).remove(rideName);
        }
        else {
            return false;
        }
    }


}
