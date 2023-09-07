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

    private void checkValidSlot(RideTime rideTime, int slotTime){
        long minutes = rideTime.getClose().until(rideTime.getOpen(), ChronoUnit.MINUTES) / slotTime;
        if(minutes == Math.floor(minutes)){
            throw new SlotCapacityException("Slot is not possible, between " + rideTime.getOpen() + " and " + rideTime.getClose());
        }
    }

    private void checkRideName(String rideName){
        if (rideExists(rideName)){
            throw new AlreadyExistsException("Already exist a ride called " + rideName);
        }
    }

    private void checkPassExistance(UUID visitorId, int day){
        if(this.parkPasses.get(visitorId).containsKey(day)){
//              si ya existe un pase para el visitante para el día indicado
            throw new AlreadyExistsException("There already exist a parkPass for the visitor for the day " + day);
        }
    }


    //    Si el pase es halfDay y quiero reservas desp de las 14hs
    private boolean checkHalfDayPass(LocalTime reservationTime){
        return !reservationTime.isAfter(LocalTime.parse("14:00"));
    }

    private void validateDay(int day){
        if (day > 365 || day < 1){
            throw new InvalidTimeException("The day must be between 1 and 365");
        }
    }

    private void invalidTime(LocalTime before, LocalTime after){
        if(after.isBefore(before)){
            throw new InvalidTimeException(after + " ride time must be after "+ before + " ride time");
        }
    }


    private void invalidPass(Models.PassTypeEnum passTypeEnum){
        if (passTypeEnum.equals(Models.PassTypeEnum.UNKNOWN)){
            throw new InvalidPassTypeException("There are 3 valid pass types [UNLIMITED, THREE, HALF_DAY]");
        }
    }

    private void invalidRideName(String rideName){
        if(!rideExists(rideName)){
            throw new RideNotFoundException("There is no ride called " + rideName);
        }
    }

    private void invalidCapacity(int capacity) {
        if(capacity < 0){
            throw new SlotCapacityException("Slot capacity must be positive");
        }
    }

    public Optional<Ride> addRide(String name, RideTime rideTime, int slotTime) {
        Ride ride = new Ride(name, rideTime, slotTime);
//        Falla:
//        si existe una atracción con ese nombre
        checkRideName(name);
//        si los valores de los horarios son inválidos
        invalidTime(rideTime.getOpen(), rideTime.getClose());
//        si con los valores provistos no existe un slot posible.
        checkValidSlot(rideTime, slotTime);
        this.rides.put(ride.getName(), ride);
        return Optional.of(ride);
    }

    public Optional<ParkPass> addParkPass(UUID visitorId, Models.PassTypeEnum type, int day) {
//        Falla:
//        si el tipo de pase es inválido
        invalidPass(type);
//        si el día del año es inválido.
        validateDay(day);
        if(this.parkPasses.containsKey(visitorId)){
//          si ya tiene un pase para ese dia
            checkPassExistance(visitorId, day);
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
        invalidRideName(rideName);
//        si el día es inválido
        validateDay(day);
//        si la capacidad es negativa
        invalidCapacity(capacity);

        Ride ride = this.rides.get(rideName);
//        si ya tiene una capacidad asignada falla, sino la agrega
        ride.setSlotCapacityPerDay(day, capacity);
    }

//    True si puedo seguir reservando, false si no puedo
//    Chequeo si es half day que la reserva sea antes de las 14hs
//    y si es three que no tenga 3 o mas ya hechas
    private boolean checkVisitorPass(Models.PassTypeEnum passType, UUID visitorId,  List<Reservation> reservationList,
                                     LocalTime reservationTime){
        int passes = 0;
        if(passType == Models.PassTypeEnum.THREE){
            for (Reservation r: reservationList) {
                if (r.getVisitorId() == visitorId){
                    passes+=1;
                }
            }
        }else if(passType == Models.PassTypeEnum.HALF_DAY){
            return checkHalfDayPass(reservationTime);
        }
        return passType == Models.PassTypeEnum.UNLIMITED || passes < 3;
    }
    public AdminParkServiceOuterClass.SlotCapacityResponse addSlotsPerDay(String rideName, int day, int capacity){
        addSlotsExceptions(rideName, day, capacity);
        Ride ride = this.rides.get(rideName);

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
                UUID visitorId = r.getVisitorId();
                Models.PassTypeEnum passType = this.parkPasses.get(visitorId).get(day).getType();

                if (checkVisitorPass(passType, visitorId, reservationList, reservationTime)){
//              Primero acepto la capacidad que me permite cada slot
                    if(i<capacity){
//                  Si la estoy reubicando mantiene el estado de pendiente y sera confirmada luego
                        if(r.getState() == ReservationState.RELOCATED){
                            relocated+=1;
                        }else{
                            r.confirm();
                            accepted+=1;
                        }
                    }else{
//                  Luego reubico las que no entran y cancelo las que no puedo ubicar
                        for (Map.Entry<LocalTime, List<Reservation>> afterTimeR: reservationsPerDay.get(day).entrySet()) {
//                      Si el pase del visitante es de tipo HalfDay y son desp de las 14 no lo puedo reubicar
                            LocalTime afterTime = afterTimeR.getKey();
                            if(passType == Models.PassTypeEnum.HALF_DAY && checkHalfDayPass(afterTime)) {
                                break;
                            }
//                      Se deberán intentar todos los slots posteriores al de la reserva original.
                            if(afterTime.isAfter(reservationTime)){
                                List<Reservation> afterReservations = afterTimeR.getValue();
//                          La puedo reubicar en caso de que no haya reservas aceptadas o pendientes que superen la capacidad
                                if(afterReservations.size() < capacity - 1){
                                    r.relocate();
                                    afterReservations.add(r);
                                    reservationList.remove(r);
                                    break;
                                }
                            }
                        }
                    }
//                    Si sigue pendiente es porque no la pude reubicar => cancelo
                    if(r.getState() == ReservationState.PENDING){
                        r.cancel();
                        cancelledReservations.add(r);
                        reservationList.remove(r);
                        cancelled+=1;
                    }
                }
                i++;
            }
        }
        ride.setCancelledReservations(cancelledReservations);

        return AdminParkServiceOuterClass.SlotCapacityResponse.newBuilder()
                .setAcceptedAmount(accepted).setCancelledAmount(cancelled).setRelocatedAmount(relocated)
                .build();
    }
    private Ride getRide(String name) {
        if(!rideExists(name))
            throw new RideNotFoundException(String.format("Ride '%s' does not exist", name));

        return rides.get(name);
    }

    private void validateRideTimeSlot(Ride ride, int dayOfTheYear, LocalTime timeSlot){
        if(!ride.isSlotValid(dayOfTheYear, timeSlot))
            throw new InvalidTimeException(String.format("Time slot '%s' is invalid for ride '%s'", timeSlot, ride.getName()));
    }

    private void validateRideTimeAndAccess(Ride ride, int dayOfTheYear, LocalTime timeSlot, UUID visitorId){
        validateRideTimeSlot(ride, dayOfTheYear, timeSlot);

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

        ReservationState state = ride.isSlotCapacitySet(dayOfTheYear) ? ReservationState.PENDING : ReservationState.ACCEPTED;
        Reservation reservation = new Reservation(visitorId, state, dayOfTheYear, timeSlot);

        if(reservations.contains(reservation))
            throw new AlreadyExistsException(String.format("Visitor '%s' already booked a ticket for '%s' at time slot '%s'", visitorId, rideName, timeSlot));

        //TODO: Podria hacer 'putIfAbsent' y chequear por false afuera
        return reservations.add(reservation);
    }

    private Optional<Reservation> getReservation(String rideName, int dayOfTheYear, LocalTime timeSlot, UUID visitorId){
        ConcurrentSkipListSet<Reservation> reservations = getUserReservationsByDay(rideName, dayOfTheYear, visitorId);
        if(!reservations.isEmpty()) {
            Reservation toFind = new Reservation(visitorId, ReservationState.UNKNOWN_0, dayOfTheYear, timeSlot);

            for (Reservation r : reservations) {
                if (r.equals(toFind))
                    return Optional.of(r);
            }
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

        if(!ride.isSlotCapacitySet(dayOfTheYear))
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
        Ride ride = getRide(rideName);
        validateRideTimeAndAccess(ride, dayOfTheYear, timeSlot, visitorId);

        ConcurrentSkipListSet<Reservation> reservations = getUserReservationsByDay(rideName, dayOfTheYear, visitorId);
        Reservation toRemove = new Reservation(visitorId, ReservationState.UNKNOWN_0, dayOfTheYear, timeSlot);

        if(!reservations.remove(toRemove))
            throw new ReservationNotFoundException(String.format(
                    "Reservation not found for visitor '%s' at ride '%s' at time slot '%s'", visitorId, rideName, timeSlot));
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

        validateDay(day);

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

    /* Returns the availability for a ride in a given day and time slot */
    public Map<String, RideAvailability> getRideAvailability(String rideName, LocalTime timeSlot, int day) {
        Ride ride = getRide(rideName);
        // TODO: Extraer validacion al service?
        validateDay(day);
        validateRideTimeSlot(ride, day, timeSlot);

        RideAvailability availability = new RideAvailability(timeSlot,
                ride.getPendingCountForTimeSlot(day, timeSlot),
                ride.getConfirmedCountForTimeSlot(day, timeSlot),
                ride.getCapacityForTimeSlot(day, timeSlot));

        Map<String, RideAvailability> rideAvailability = new HashMap<>();
        rideAvailability.put(rideName, availability);

        return rideAvailability;
    }



}
