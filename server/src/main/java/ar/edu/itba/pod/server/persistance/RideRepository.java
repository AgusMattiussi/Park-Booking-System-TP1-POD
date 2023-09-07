package ar.edu.itba.pod.server.persistance;

import ar.edu.itba.pod.server.Models.*;
import ar.edu.itba.pod.server.exceptions.*;
import rideBooking.AdminParkServiceOuterClass;
import rideBooking.Models;
import rideBooking.Models.ReservationState;

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
    /* Maps ride names to a map of <User ID, List of reservations> */
    private final ConcurrentMap<String, ConcurrentMap<UUID, ConcurrentMap<Integer, ConcurrentSkipListSet<Reservation>>>> bookedRides;
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

    private void addSlotsExceptions(String rideName, int day, int capacity){
//        Falla:
//        si la atracción no existe
        if(!this.rides.containsKey(rideName)){
            throw new RideNotFoundException("There is no ride called " + rideName);
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
    }

//    True si no puedo seguir reservando, false si puedo
    private boolean checkIfVisitorPassIsThree(UUID visitorId, int day, List<Reservation> reservationList){
        int passes = 0;
        if(this.parkPasses.get(visitorId).get(day).getType() == Models.PassTypeEnum.THREE){
            for (Reservation r: reservationList) {
                if (r.getVisitorId() == visitorId){
                    passes+=1;
                }
            }
        }else{
//          Si no es THREE puedo seguir
            return false;
        }
        return passes < 3;
    }
    public AdminParkServiceOuterClass.SlotCapacityResponse addSlotsPerDay(String rideName, int day, int capacity){
        addSlotsExceptions(rideName, day, capacity);
        Ride ride = this.rides.get(rideName);
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
//              Si el pase es THREE y puedo seguir guardando, o no es three
                if (!checkIfVisitorPassIsThree(r.getVisitorId(), day, reservationList)){
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
//                      Si el pase del visitante es de tipo HalfDay solo puedo reubicar antes de las 14hs, sino salgo y cancelo afuera
                            if(this.parkPasses.get(r.getVisitorId()).get(day).getType() == Models.PassTypeEnum.HALF_DAY && afterTime.getKey().isAfter(LocalTime.parse("14:00"))) {
                                break;
                            }
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
    private Ride getRide(String name) {
        if(!rideExists(name))
            throw new RideNotFoundException(String.format("Ride '%s' does not exist", name));

        return rides.get(name);
    }

    private void validateRideTimeAndAccess(Ride ride, int dayOfTheYear, LocalTime timeSlot, UUID visitorId){
        if(!ride.isSlotValid(dayOfTheYear, timeSlot))
            throw new InvalidTimeException(String.format("Time slot '%s' is invalid for ride '%s'", timeSlot, ride.getName()));

        if(!hasValidPass(visitorId, dayOfTheYear))
            throw new PassNotFoundException(String.format("No valid pass for day %s", dayOfTheYear));
    }

    public Map<String, Ride> getRides() {
        return rides;
    }

    public List<Ride> getRidesList() {
        return new ArrayList<>(this.rides.values());
    }

    public boolean rideExists(String name) {
        return this.rides.containsKey(name);
    }

    public boolean removeRide(String name) {
        return this.rides.remove(name) != null;
    }

    //TODO: Que pasa si me consultan por reservas y empiezan a crear nuevas?
    private ConcurrentSkipListSet<Reservation> getUserReservationsByDay(String rideName, int dayOfTheYear, UUID visitorId){
        bookedRides.putIfAbsent(rideName, new ConcurrentHashMap<>());

        Map<UUID, ConcurrentMap<Integer, ConcurrentSkipListSet<Reservation>>> rideBookedSlotsByUser = bookedRides.get(rideName);
        rideBookedSlotsByUser.putIfAbsent(visitorId, new ConcurrentHashMap<>());

        ConcurrentMap<Integer, ConcurrentSkipListSet<Reservation>> userBookedSlotsByDay = rideBookedSlotsByUser.get(visitorId);
        userBookedSlotsByDay.putIfAbsent(dayOfTheYear, new ConcurrentSkipListSet<>());
        return userBookedSlotsByDay.get(dayOfTheYear);
    }

    /*
     *  Books a ride for a visitor
     *
     *  FAIL CONDITIONS:
     *  - Reservation already exists
     *  - Invalid pass
     *  - No ride under that name
     *  - Invalid date
     *  - Invalid time slot
     *
     */
    // TODO: Mover excepciones al Service?
    public boolean bookRide(String rideName, int dayOfTheYear, LocalTime timeSlot, UUID visitorId) {
        Ride ride = getRide(rideName);
        validateRideTimeAndAccess(ride, dayOfTheYear, timeSlot, visitorId);
        
        ConcurrentSkipListSet<Reservation> reservations = getUserReservationsByDay(rideName, dayOfTheYear, visitorId);

        ReservationState state = ride.isSlotCapacitySet() ? ReservationState.PENDING : ReservationState.ACCEPTED;
        Reservation reservation = new Reservation(visitorId, state, dayOfTheYear, timeSlot);

        if(reservations.contains(reservation))
            throw new AlreadyExistsException(String.format("Visitor '%s' already booked a ticket for '%s' at time slot '%s'", visitorId, rideName, timeSlot));

        //TODO: Podria hacer 'putIfAbsent' y chequear por false afuera
        return reservations.add(reservation);
    }

    private Optional<Reservation> getReservation(String rideName, int dayOfTheYear, LocalTime timeSlot, UUID visitorId){
        ConcurrentSkipListSet<Reservation> reservations = getUserReservationsByDay(rideName, dayOfTheYear, visitorId);
        if(reservations.isEmpty())
            return Optional.empty();

        Reservation toFind = new Reservation(visitorId, ReservationState.UNKNOWN_0, dayOfTheYear, timeSlot);

        for (Reservation r : reservations){
            if(r.equals(toFind))
                return Optional.of(r);
        }

        return Optional.empty();
    }

    /*
     *  Confirms a previously booked ride
     *
     *  FAIL CONDITIONS:
     *  - Ride slots not loaded yet
     *  - Reservation does not exist
     *  - Reservation already confirmed
     *  - Invalid pass
     *  - No ride under that name
     *  - Invalid date
     *  - Invalid time slot
     *
     */
    public void confirmBooking(String rideName, int dayOfTheYear, LocalTime timeSlot, UUID visitorId){
        Ride ride = getRide(rideName);
        validateRideTimeAndAccess(ride, dayOfTheYear, timeSlot, visitorId);

        if(!ride.isSlotCapacitySet())
            throw new SlotCapacityException(String.format("Slot capacity not set for ride '%s'", rideName));

        Reservation reservation = getReservation(rideName, dayOfTheYear, timeSlot, visitorId).orElseThrow(() ->
                new ReservationNotFoundException(String.format(
                        "Reservation not found for visitor '%s' at ride '%s' at time slot '%s'", visitorId, rideName, timeSlot)));

        if(reservation.isConfirmed())
            throw new AlreadyConfirmedException(String.format(
                    "Reservation for visitor '%s' at ride '%s' at time slot '%s' is already confirmed", visitorId, rideName, timeSlot));

        reservation.confirm();
    }

    /*
     *  Cancels a previously booked ride
     *
     *  FAIL CONDITIONS:
     *  - Reservation does not exist
     *  - Invalid pass
     *  - No ride under that name
     *  - Invalid date
     *  - Invalid time slot
     *
     */
    public void cancelBooking(String rideName, int dayOfTheYear, LocalTime timeSlot, UUID visitorId) {
        Reservation reservation = getReservation(rideName, dayOfTheYear, timeSlot, visitorId).orElseThrow(() ->
                new ReservationNotFoundException(String.format(
                        "Reservation not found for visitor '%s' at ride '%s' at time slot '%s'", visitorId, rideName, timeSlot)));

        reservation.cancel();
    }

    public boolean addVisitor(UUID visitorId, String rideName, int day) {
        /*
        FAIL CONDITIONS:
        - No ride under that name
        - Invalid date
        - Invalid pass
        - Already registered for notification
         */
        if (!rideExists(rideName))
            throw new RideNotFoundException("This ride does not exist");

        invalidDate(day);

        if(!hasValidPass(visitorId, day))
            throw new PassNotFoundException("No valid pass for day " + day);


        notifications.putIfAbsent(visitorId, new ConcurrentHashMap<>());
        notifications.get(visitorId).putIfAbsent(day, new ConcurrentSkipListSet<>());
        return notifications.get(visitorId).get(day).add(rideName);
    }

    private boolean hasValidPass(UUID visitorId, int day) {
        return this.parkPasses.containsKey(visitorId) && this.parkPasses.get(visitorId).containsKey(day);
    }

    public boolean removeVisitor(UUID visitorId, String rideName, int day) {
        if(!this.notifications.containsKey(visitorId)) {
            throw new VisitorNotFoundException("No user found for the visitorId " + visitorId);
        }

        if(this.notifications.get(visitorId).containsKey(day)) {
            return this.notifications.get(visitorId).get(day).remove(rideName);
        }
        else {
            return false;
        }
    }



}
