package ar.edu.itba.pod.server.persistance;

import ar.edu.itba.pod.server.Models.*;
import ar.edu.itba.pod.server.exceptions.*;
import io.grpc.stub.StreamObserver;
import rideBooking.AdminParkServiceOuterClass;
import rideBooking.Models;
import rideBooking.Models.ReservationState;
import rideBooking.NotifyServiceOuterClass;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class RideRepository {

    private static final ParkLocalTime HALF_DAY_TIME = ParkLocalTime.fromString("14:00");

    private static RideRepository instance;
    private final ConcurrentMap<String, Ride> rides;

    private final ConcurrentMap<UUID, ConcurrentMap<Integer,ParkPass>> parkPasses;
    // TODO: Considerar cual es el caso de uso mas comun para definir el mapeo
    /* Maps ride name -> day -> time -> reservations */
    private final ConcurrentMap<String, ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentSkipListSet<Reservation>>>> bookedRides;


    private int acceptedAmount = 0;
    private int relocatedAmount = 0;
    private int cancelledAmount = 0;

    private RideRepository() {
        this.rides = new ConcurrentHashMap<>();
        this.parkPasses = new ConcurrentHashMap<>();
        this.bookedRides = new ConcurrentHashMap<>();

        //TODO: Borrar
//        rides.put("Space Mountain", new Ride("Space Mountain", new RideTime(ParkLocalTime.fromString("10:00"), ParkLocalTime.fromString("18:00")), 15));
//        rides.put("Splash Mountain", new Ride("Splash Mountain", new RideTime(ParkLocalTime.fromString("10:00"), ParkLocalTime.fromString("16:00")), 15));
//        rides.put("It's a Small World", new Ride("It's a Small World", new RideTime(ParkLocalTime.fromString("09:00"), ParkLocalTime.fromString("19:00")), 20));
//        addParkPass(UUID.fromString("7727e3b9-a2d8-46fe-b581-0c63e8739694"), Models.PassTypeEnum.UNLIMITED, 22);
    }

    public static RideRepository getInstance() {
        if (instance == null) {
            instance = new RideRepository();
        }
        return instance;
    }

    private void checkValidSlot(RideTime rideTime, int slotTime){
        long open_minutes = rideTime.getOpen().until(rideTime.getClose(), ChronoUnit.MINUTES);
        if(open_minutes < slotTime){
            throw new SlotCapacityException("Slot of " + slotTime + " minutes is not possible, between " + rideTime.getOpen() + " and " + rideTime.getClose());
        }
    }

    private void checkRideName(String rideName){
        if (rideExists(rideName)){
            throw new AlreadyExistsException("Already exist a ride called " + rideName);
        }
    }

    private void checkPassExistence(UUID visitorId, int day){
        if(this.parkPasses.get(visitorId).containsKey(day)){
//              si ya existe un pase para el visitante para el día indicado
            throw new AlreadyExistsException("There already exist a parkPass for the visitor for the day " + day);
        }
    }


    //    Si el pase es halfDay y quiero reservas desp de las 14hs
    private boolean checkHalfDayPass(ParkLocalTime reservationTime){
        return !reservationTime.isAfter(HALF_DAY_TIME);
    }

    private void validateDay(int day){
        if (day > 365 || day < 1){
            throw new InvalidTimeException("The day must be between 1 and 365");
        }
    }

    private void invalidTime(ParkLocalTime before, ParkLocalTime after){
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
        Ride ride = new Ride(name, rideTime);
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
            checkPassExistence(visitorId, day);
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
    }

//    True si puedo seguir reservando, false si no puedo
//    Chequeo si es half day que la reserva sea antes de las 14hs
//    y si es three que no tenga 3 o mas ya hechas
    private boolean checkVisitorPass(Models.PassTypeEnum passType, UUID visitorId,  Set<Reservation> reservationSet,
                                     ParkLocalTime reservationTime){
        int passes = 0;
        if(passType == Models.PassTypeEnum.THREE){
            for (Reservation r: reservationSet) {
                if (r.getVisitorId() == visitorId){
                    passes+=1;
                }
            }
        }else if(passType == Models.PassTypeEnum.HALFDAY){
            return checkHalfDayPass(reservationTime);
        }
        return passType == Models.PassTypeEnum.UNLIMITED || passes < 3;
    }

    //TODO: Chequar funcionamiento
    public AdminParkServiceOuterClass.SlotCapacityResponse addSlotsPerDay(String rideName, int day, int capacity){
        addSlotsExceptions(rideName, day, capacity);
        Ride ride = this.rides.get(rideName);

        // si ya tiene una capacidad asignada falla, sino la agrega
        ride.setSlotCapacityForDay(day, capacity);


        ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentSkipListSet<Reservation>>> reservationsPerDay = bookedRides.get(rideName);
        ConcurrentMap<String, ConcurrentSkipListSet<Reservation>> realocateReservations = new ConcurrentHashMap<>();

        if(reservationsPerDay!=null && reservationsPerDay.containsKey(day)){
            // Iterate over each member of the map
            Set<Map.Entry<String, ConcurrentSkipListSet<Reservation>>> entrySet = reservationsPerDay.get(day).entrySet();
            for (Map.Entry<String, ConcurrentSkipListSet<Reservation>> reservations: entrySet) {
                String reservationTimeString = reservations.getKey();

                ParkLocalTime reservationTime = ParkLocalTime.fromString(reservationTimeString);
                ConcurrentSkipListSet<Reservation> reservationSet = reservations.getValue();
                int count = 0;

//                Si tengo para realocar lo hago
                if(realocateReservations.containsKey(reservationTimeString)){
                    reservationSet.addAll(realocateReservations.get(reservationTimeString));
                    realocateReservations.remove(reservationTimeString);
                    reservations.setValue(reservationSet);
                }

                for(Reservation r : reservationSet){

                    if(r.isRegisteredForNotifications())
                        r.notifySlotsCapacityAdded(capacity);

                    UUID visitorId = r.getVisitorId();
                    Models.PassTypeEnum passType = this.parkPasses.get(visitorId).get(day).getType();

                    if (checkVisitorPass(passType, visitorId, reservationSet, reservationTime)){
                        if(count<capacity){ // Si tengo lugar para guardar, guardo
                            if(r.getState() == ReservationState.RELOCATED){
                                relocatedAmount +=1;
                            }else{
                                r.setConfirmed();

                                if(r.isRegisteredForNotifications())
                                    r.notifyConfirmed();

                                acceptedAmount +=1;
                            }
                            count++; // Se agrego uno mas
                        }else{ // Si no me entran, trato de reubicar
                            for (Map.Entry<String, ConcurrentSkipListSet<Reservation>>  afterTimeR: entrySet) {
                                String afterTimeString = afterTimeR.getKey();
                                ParkLocalTime afterTime = ParkLocalTime.fromString(afterTimeString);
                                Set<Reservation> afterReservations = afterTimeR.getValue();
                                if(!checkVisitorPass(passType, visitorId, afterReservations, afterTime)) {
                                    // Chequeo el pase, y sino puedo salgo
                                    break;
                                }
                                // Solo intento slots posteriores al de la reserva original.
                                if(afterTime.isAfter(reservationTime)){
                                    int realocated = 0;
                                    if(realocateReservations.containsKey(afterTimeString)){
                                        realocated=realocateReservations.get(afterTimeString).size();
                                    }
                                    // Si tengo capacidad la reubico (las reservas + las que quiero realocar ahi)
                                    if(afterReservations.size() + realocated < capacity){
                                        r.setRelocated();
                                        if(!realocateReservations.containsKey(afterTimeString)){
                                            realocateReservations.put(afterTimeString, new ConcurrentSkipListSet<>());
                                        }
                                        realocateReservations.get(afterTimeString).add(r);
//                                      Cancel para la lista en la que estaba originalmente
                                        r.setCanceled();

                                        if(r.isRegisteredForNotifications())
                                            r.notifyRelocated(reservationTime);

                                        break;
                                    }
                                }
                            }
                        }
                        // Si sigue pendiente es porque no la pude reubicar => cancelo
                        if(r.getState() == ReservationState.PENDING){
                            r.setCanceled();

                            if(r.isRegisteredForNotifications())
                                r.notifyCancelled();
                            cancelledAmount +=1;
                        }
                    }
                }
//                Elimino todas las reservas canceladas o q se realocaron en otra
                reservationSet.removeIf(reserv -> (reserv.getState().equals(ReservationState.CANCELLED)));
            }
        }


        AdminParkServiceOuterClass.SlotCapacityResponse response = AdminParkServiceOuterClass.SlotCapacityResponse.newBuilder()
                .setAcceptedAmount(acceptedAmount).setCancelledAmount(cancelledAmount).setRelocatedAmount(relocatedAmount)
                .build();

        cleanAmounts();
        return response;
    }


    private void cleanAmounts(){
        this.acceptedAmount = 0;
        this.relocatedAmount = 0;
        this.cancelledAmount = 0;
    }
    private Ride getRide(String name) {
        if(!rideExists(name))
            throw new RideNotFoundException(String.format("Ride '%s' does not exist", name));

        return rides.get(name);
    }

    /*private void validateRideTimeSlot(Ride ride, ParkLocalTime timeSlot){
        if(!ride.isTimeSlotValid(timeSlot))
            throw new InvalidTimeException(String.format("Time slot '%s' is invalid for ride '%s'", timeSlot, ride.getName()));
    }*/

    private void validateRideTimeAndAccess(Ride ride, int day, ParkLocalTime timeSlot, UUID visitorId){
        if(!ride.isTimeSlotValid(timeSlot))
            throw new InvalidTimeException(String.format("Time slot '%s' is invalid for ride '%s'", timeSlot, ride.getName()));

        if(!hasValidPass(visitorId, day))
            throw new PassNotFoundException(String.format("No valid pass for day %s", day));
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

    public NavigableSet<Reservation> getReservationsByTimeSlot(String rideName, int day, ParkLocalTime timeSlot){
        ConcurrentMap<String, ConcurrentSkipListSet<Reservation>> reservationsByTime = getReservationsByDay(rideName, day);
        if(reservationsByTime != null)
            return reservationsByTime.get(timeSlot.toString());
        return null;
    }
    public ConcurrentMap<String, ConcurrentSkipListSet<Reservation>> getReservationsByDay(String rideName, int day){
        ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentSkipListSet<Reservation>>> rideReservations = bookedRides.get(rideName);
        if(rideReservations == null)
            return null;

        return rideReservations.get(day);
    }

    private ConcurrentSkipListSet<Reservation> initializeOrGetReservationsForSlot(String rideName, int day, String timeSlot){
        ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentSkipListSet<Reservation>>> reservationsByDay = bookedRides.computeIfAbsent(rideName, k -> new ConcurrentHashMap<>());
        ConcurrentMap<String, ConcurrentSkipListSet<Reservation>> reservationsByTime = reservationsByDay.computeIfAbsent(day, k -> new ConcurrentHashMap<>());
        return reservationsByTime.computeIfAbsent(timeSlot, k -> new ConcurrentSkipListSet<>());
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
    // TODO: Agregar locks para el caso donde dos hilos piensan que queda 1 slot y ambos lo reservan
    public ReservationState bookRide(String rideName, int day, ParkLocalTime timeSlot, UUID visitorId) {
        Ride ride = getRide(rideName);
        if(ride == null)
            throw new RideNotFoundException(String.format("Ride '%s' does not exist", rideName));

        validateRideTimeAndAccess(ride, day, timeSlot, visitorId);

        boolean isCapacitySet = ride.isSlotCapacitySet(day);

        if(isCapacitySet)
            if(ride.getSlotsLeft(day, timeSlot).get() == 0)
                throw new ReservationLimitException(String.format("No more reservations available for ride '%s' on day %s at %s", rideName, day, timeSlot));

        ConcurrentSkipListSet<Reservation> reservations = initializeOrGetReservationsForSlot(rideName, day, timeSlot.toString());

        ReservationState state = isCapacitySet ? ReservationState.CONFIRMED : ReservationState.PENDING;
        Reservation reservation = new Reservation(rideName, visitorId, state, day, timeSlot);

        if(reservations.contains(reservation))
            throw new AlreadyExistsException(String.format("Visitor '%s' already booked a ticket for '%s' at time slot '%s'", visitorId, rideName, timeSlot));

        if(isCapacitySet)
            ride.decrementCapacity(day, timeSlot);

        reservations.add(reservation);
        return state;
    }

    private Optional<Reservation> getReservation(String rideName, int day, ParkLocalTime timeSlot, UUID visitorId){
        NavigableSet<Reservation> reservations = getReservationsByTimeSlot(rideName, day, timeSlot);
        if(reservations != null) {
            /* It is more efficient to create a new reservation and compare it to the ones in the set than filtering the set */
            Reservation reservation = new Reservation(rideName, visitorId, ReservationState.UNKNOWN_STATE, day, timeSlot);
            if (reservations.contains(reservation))
                return Optional.of(reservations.floor(reservation));  // Will not return null because the reservation is in the set
        }
        return Optional.empty();
    }

    // FIXME: Muy costoso. Tiene sentido agregar otro nivel de indireccion para que sea mas eficiente?
    private List<Reservation> getUserReservationsByDay(String rideName, int day, UUID visitorId){
        ConcurrentMap<String, ConcurrentSkipListSet<Reservation>> reservations = getReservationsByDay(rideName, day);
        if(reservations != null) {
            return reservations.values().stream()
                    .flatMap(Collection::stream)
                    .filter(reservation -> reservation.getVisitorId().equals(visitorId))
                    .collect(Collectors.toList());
        }
        return null;
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
    public void confirmBooking(String rideName, int day, ParkLocalTime timeSlot, UUID visitorId){
        Ride ride = getRide(rideName);
        validateRideTimeAndAccess(ride, day, timeSlot, visitorId);

        if(!ride.isSlotCapacitySet(day))
            throw new SlotCapacityException(String.format("Slot capacity not set for ride '%s'", rideName));

        Reservation reservation = getReservation(rideName, day, timeSlot, visitorId).orElseThrow(() ->
                new ReservationNotFoundException(String.format(
                        "Reservation not found for visitor '%s' at ride '%s' at time slot '%s'", visitorId, rideName, timeSlot)));

        if(reservation.isConfirmed())
            throw new AlreadyConfirmedException(String.format(
                    "Reservation for visitor '%s' at ride '%s' at time slot '%s' is already confirmed", visitorId, rideName, timeSlot));

        reservation.setConfirmed();
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
    //TODO: Deberia llamar a ride.incrementCapacity()?
    public void cancelBooking(String rideName, int day, ParkLocalTime timeSlot, UUID visitorId) {
        Ride ride = getRide(rideName);
        validateRideTimeAndAccess(ride, day, timeSlot, visitorId);

        Reservation toRemove = new Reservation(rideName, visitorId, ReservationState.UNKNOWN_STATE, day, timeSlot);

        Set<Reservation> reservations = getReservationsByTimeSlot(rideName, day, timeSlot);
        if(reservations == null || !reservations.remove(toRemove))
            throw new ReservationNotFoundException(String.format(
                    "Reservation not found for visitor '%s' at ride '%s' at time slot '%s'", visitorId, rideName, timeSlot));
    }

    //FIXME: Chequear si anda
    private boolean hasValidPass(UUID visitorId, int day) {
        return this.parkPasses.containsKey(visitorId) && this.parkPasses.get(visitorId).containsKey(day);
    }

    private List<Reservation> getUserReservationsByDayAndValidateParameters(UUID visitorId, String rideName, int day){
        if (!rideExists(rideName))
            throw new RideNotFoundException("This ride does not exist");

        validateDay(day);

        if(!hasValidPass(visitorId, day))
            throw new PassNotFoundException("No valid pass for day " + day);


        List<Reservation> reservations = getUserReservationsByDay(rideName, day, visitorId);
        if(reservations == null || reservations.isEmpty())
            throw new ReservationNotFoundException(String.format("No reservations for visitor %s for ride %s on day %d", visitorId, rideName, day));

        return reservations;
    }

    public void registerForNotifications(UUID visitorId, String rideName, int day, StreamObserver<NotifyServiceOuterClass.Notification> notificationObserver) {
        List<Reservation> reservations = getUserReservationsByDayAndValidateParameters(visitorId, rideName, day);

        /* Register all reservations for notifications */
        reservations.forEach(reservation -> {
            reservation.registerForNotifications(notificationObserver);
            reservation.notifyRegistered();
        });
    }


    public StreamObserver<NotifyServiceOuterClass.Notification> unregisterForNotifications(UUID visitorId, String rideName, int day) {
        List<Reservation> reservations = getUserReservationsByDayAndValidateParameters(visitorId, rideName, day);

        /* All these reservations share the same notificationObserver */
        StreamObserver<NotifyServiceOuterClass.Notification> notificationObserver = null;
        for (Reservation reservation : reservations) {
            notificationObserver = reservation.unregisterForNotifications();
        }
        return notificationObserver;
    }

    /* Returns the availability for a ride in a given day and time slot */
    private RideAvailability getRideAvailabilityForTimeSlot(String rideName, ParkLocalTime timeSlot, int day) {
        Ride ride = getRide(rideName);
        // TODO: Extraer validacion al service?
        validateDay(day);
        //validateRideTimeSlot(ride, day, timeSlot);

        return new RideAvailability(timeSlot,
                getPendingCountForTimeSlot(rideName, day, timeSlot),
                getConfirmedCountForTimeSlot(rideName, day, timeSlot),
                ride.getSlotCapacityForDay(day));
    }

    private Map<ParkLocalTime, RideAvailability> getRideAvailability(String rideName, ParkLocalTime startTimeSlot, ParkLocalTime endTimeSlot, int day){
        Ride ride = getRide(rideName);

        if (startTimeSlot.isAfter(endTimeSlot))
            throw new IllegalArgumentException("Start time slot must be before end time slot");

        Map<ParkLocalTime, RideAvailability> timeSlotAvailability = new HashMap<>();
        List<ParkLocalTime> timeSlots = ride.getTimeSlotsBetween(startTimeSlot, endTimeSlot);

        for (ParkLocalTime currentTimeSlot : timeSlots)
            timeSlotAvailability.put(currentTimeSlot, getRideAvailabilityForTimeSlot(rideName, currentTimeSlot, day));

        return timeSlotAvailability;
    }

    public Map<String, Map<ParkLocalTime, RideAvailability>> getRidesAvailability(String rideName, ParkLocalTime timeSlot, int day){
        Map<String, Map<ParkLocalTime, RideAvailability>> rideAvailability = new HashMap<>();
        rideAvailability.put(rideName, getRideAvailability(rideName, timeSlot, timeSlot, day));
        return rideAvailability;
    }

    public Map<String, Map<ParkLocalTime, RideAvailability>> getRidesAvailability(String rideName, ParkLocalTime startTimeSlot, ParkLocalTime endTimeSlot, int day) {
        Map<String, Map<ParkLocalTime, RideAvailability>> rideAvailability = new HashMap<>();
        rideAvailability.put(rideName, getRideAvailability(rideName, startTimeSlot, endTimeSlot, day));
        return rideAvailability;
    }

    public Map<String, Map<ParkLocalTime, RideAvailability>> getRidesAvailability(ParkLocalTime startTimeSlot, ParkLocalTime endTimeSlot, int day) {
        Map<String, Map<ParkLocalTime, RideAvailability>> rideAvailability = new HashMap<>();

        for (String rideName : rides.keySet())
            rideAvailability.put(rideName, getRideAvailability(rideName, startTimeSlot, endTimeSlot, day));

        return rideAvailability;
    }

    // TODO: Implementar aca
    private int countStateForTimeSlot(String rideName, int day, ParkLocalTime timeSlot, ReservationState state) {
        NavigableSet<Reservation> reservations = getReservationsByTimeSlot(rideName, day, timeSlot);
        if(reservations == null)
            return 0; // TODO: deberia tirar excepcion?

        return (int) reservations.stream().filter(reservation -> reservation.getState() == state).count();
    }

    public int getConfirmedCountForTimeSlot(String rideName, int day, ParkLocalTime timeSlot) {
        return countStateForTimeSlot(rideName, day, timeSlot, ReservationState.CONFIRMED);
    }

    public int getPendingCountForTimeSlot(String rideName, int day, ParkLocalTime timeSlot) {
        return countStateForTimeSlot(rideName, day, timeSlot, ReservationState.PENDING);
    }
}
