package ar.edu.itba.pod.server.ridePersistence;

import ar.edu.itba.pod.server.Models.*;
import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.passPersistence.ParkPassRepository;
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

    private static RideRepository instance;
    private static ParkPassRepository parkPassInstance = ParkPassRepository.getInstance();
    private final ConcurrentMap<String, Ride> rides;


    // TODO: Considerar cual es el caso de uso mas comun para definir el mapeo
    /* Maps ride name -> day -> time -> reservations */
//    private final ConcurrentMap<String, ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentSkipListSet<Reservation>>>> bookedRides;


    private RideRepository() {
        this.rides = new ConcurrentHashMap<>();
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
//        Falla:
//        si existe una atracción con ese nombre
        checkRideName(name);
//        si los valores de los horarios son inválidos
        invalidTime(rideTime.getOpen(), rideTime.getClose());
//        si con los valores provistos no existe un slot posible.
        checkValidSlot(rideTime, slotTime);
        Ride ride = new Ride(name, rideTime);
        this.rides.put(ride.getName(), ride);
        return Optional.of(ride);
    }

    public Optional<ParkPass> addParkPass(UUID visitorId, Models.PassTypeEnum type, int day) {
//        Falla:
//        si el tipo de pase es inválido
        invalidPass(type);
//        si el día del año es inválido.
        validateDay(day);
        return parkPassInstance.addParkPass(visitorId, type, day);
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


    //TODO: Chequar funcionamiento
    public AdminParkServiceOuterClass.SlotCapacityResponse addSlotsPerDay(String rideName, int day, int capacity){
        addSlotsExceptions(rideName, day, capacity);
        Ride ride = this.rides.get(rideName);
        return ride.addSlotCapacityPerDay(parkPassInstance, day, capacity);
    }

    private Ride getRide(String name) {
        if(!rideExists(name))
            throw new RideNotFoundException(String.format("Ride '%s' does not exist", name));

        return rides.get(name);
    }

    private Set<Reservation> getVisitorReservations(UUID visitorId){
        Set<Reservation> visitorReservations = new HashSet<>();
        for (Map.Entry<String, Ride> ride: rides.entrySet()) {
            ride.getValue().getBookedSlots();
        }
        return  visitorReservations;
    }

    private void validateRideTimeAndAccess(Ride ride, int day, ParkLocalTime timeSlot, UUID visitorId, Models.PassTypeEnum passType){
        if(!ride.isTimeSlotValid(timeSlot))
            throw new InvalidTimeException(String.format("Time slot '%s' is invalid for ride '%s'", timeSlot, ride.getName()));

        if(!parkPassInstance.hasValidPass(visitorId, day))
            throw new PassNotFoundException(String.format("No valid pass for day %s", day));


        if(passType.equals(Models.PassTypeEnum.HALFDAY) && !parkPassInstance.checkHalfDayPass(timeSlot)) {
            throw new InvalidTimeException(String.format("No valid time for day %s, according to HALFDAY pass", day));
        }else if(!parkPassInstance.checkVisitorPass(visitorId, day)) {
            throw new InvalidPassTypeException(String.format("No reservation for day %s, according to THREE pass, yo already have 3 reservations", day));
        }
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


    public NavigableSet<Reservation> getReservationsByTimeSlot(String rideName, int day, ParkLocalTime timeSlot){
        ConcurrentMap<String, ConcurrentSkipListSet<Reservation>> reservationsByTime = getReservationsByDay(rideName, day);
        if(reservationsByTime != null)
            return reservationsByTime.get(timeSlot.toString());
        return null;
    }

    public ConcurrentMap<String, ConcurrentSkipListSet<Reservation>> getReservationsByDay(String rideName, int day){
        Ride ride = rides.get(rideName);
        if(ride == null)
            throw new RideNotFoundException(String.format("Ride '%s' does not exist", rideName));

        ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentSkipListSet<Reservation>>> rideReservations = ride.getBookedSlots();
        if(rideReservations == null)
            return null;

        return rideReservations.get(day);
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

        Models.PassTypeEnum passType = parkPassInstance.getVisitorParkType(visitorId, day);
        validateRideTimeAndAccess(ride, day, timeSlot, visitorId, passType);

        return ride.bookRide(day, timeSlot, visitorId);
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
    public List<Reservation> getUserReservationsByDay(String rideName, int day, UUID visitorId){
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
        Models.PassTypeEnum passType = parkPassInstance.getVisitorParkType(visitorId, day);
        validateRideTimeAndAccess(ride, day, timeSlot, visitorId, passType);

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
        Models.PassTypeEnum passType = parkPassInstance.getVisitorParkType(visitorId, day);
        validateRideTimeAndAccess(ride, day, timeSlot, visitorId, passType);

        Reservation toRemove = new Reservation(rideName, visitorId, ReservationState.UNKNOWN_STATE, day, timeSlot);

        Set<Reservation> reservations = getReservationsByTimeSlot(rideName, day, timeSlot);
        if(reservations == null || !reservations.remove(toRemove))
            throw new ReservationNotFoundException(String.format(
                    "Reservation not found for visitor '%s' at ride '%s' at time slot '%s'", visitorId, rideName, timeSlot));
    }



    private List<Reservation> getUserReservationsByDayAndValidateParameters(UUID visitorId, String rideName, int day){
        if (!rideExists(rideName))
            throw new RideNotFoundException("This ride does not exist");

        validateDay(day);


        if(!parkPassInstance.checkVisitorPass(visitorId, day))
            throw new InvalidPassTypeException(String.format("No reservation for day %s, according to THREE pass, yo already have 3 reservations", day));


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
        validateDay(day);

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
        List<ParkLocalTime> timeSlots;
        if(!endTimeSlot.equals(startTimeSlot))
            timeSlots = ride.getTimeSlotsBetween(startTimeSlot, endTimeSlot);
        else {
            timeSlots = new ArrayList<>(Collections.singletonList(startTimeSlot));
        }

        for (ParkLocalTime currentTimeSlot : timeSlots) {
            timeSlotAvailability.put(currentTimeSlot, getRideAvailabilityForTimeSlot(rideName, currentTimeSlot, day));
        }
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

    public ParkPassRepository getParkPasses() {
        return parkPassInstance;
    }
}
