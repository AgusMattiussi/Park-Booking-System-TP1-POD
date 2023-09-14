package ar.edu.itba.pod.server.Models;


import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.ReservationLimitException;
import ar.edu.itba.pod.server.exceptions.SlotCapacityException;
import ar.edu.itba.pod.server.passPersistence.ParkPassRepository;
import com.google.protobuf.StringValue;
import rideBooking.AdminParkServiceOuterClass;
import rideBooking.Models;
import rideBooking.RideBookingServiceOuterClass;
import rideBooking.Models.ReservationState;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//TODO: Cambiar las colecciones a su version concurrente
//TODO: Cambiar a Atomics
public class Ride implements GRPCModel<rideBooking.RideBookingServiceOuterClass.Ride>{

    private final String name;
    private final RideTime rideTime;
    private final Map<Integer, Map<String, AtomicInteger>> slotsLeftByDayAndTimeSlot;
    private final Map<Integer, Integer> slotCapacityByDay;
    private final ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentSkipListSet<Reservation>>> bookedSlots;
    private final Lock slotCapacityLock;

    public Ride(String name, RideTime rideTime) {
        this.name = name;
        this.rideTime = rideTime;
        this.slotsLeftByDayAndTimeSlot = new ConcurrentHashMap<>();
        this.slotCapacityByDay = new ConcurrentHashMap<>();
        this.bookedSlots = new ConcurrentHashMap<>();
        this.slotCapacityLock = new ReentrantLock();
    }

    public String getName() {
        return name;
    }

    public RideTime getRideTime() {
        return rideTime;
    }

    public Duration getTimeSlotDuration() {
        return rideTime.getTimeSlotDuration();
    }

    public Map<Integer, Integer> getSlotCapacityByDay() {
        return slotCapacityByDay;
    }

    // -1 Means no capacity set
    public int getSlotCapacityForDay(int day) {
        return slotCapacityByDay.getOrDefault(day, -1);
    }


    public AtomicInteger getSlotsLeft(int day, String timeSlot) {
        return slotsLeftByDayAndTimeSlot.getOrDefault(day, new HashMap<>()).getOrDefault(timeSlot, null);
    }

    public AtomicInteger getSlotsLeft(int day, ParkLocalTime timeSlot) {
        return getSlotsLeft(day, timeSlot.toString());
    }

    public void decrementCapacity(int day, ParkLocalTime timeSlot, int count) {
        for(int i = 0; i < count; i++)
            decrementCapacity(day, timeSlot);
    }

    public void decrementCapacity(int day, ParkLocalTime timeSlot) {
        slotCapacityLock.lock();
        try {
            AtomicInteger slotsLeft = getSlotsLeft(day, timeSlot);

            if(slotsLeft == null || !slotCapacityByDay.containsKey(day))
                throw new IllegalArgumentException(String.format("%s - Day %d has no capacity yet", name, day));

            if(slotsLeft.get() <= 0)
                throw new IllegalArgumentException(String.format("%s - Day %d has no slots left", name, day));

            slotsLeft.decrementAndGet();
        } finally {
            slotCapacityLock.unlock();
        }
    }

    public void incrementCapacity(int day, ParkLocalTime timeSlot){
        slotCapacityLock.lock();
        try {
            AtomicInteger slotsLeft = getSlotsLeft(day, timeSlot);

            if(slotsLeft == null || !slotCapacityByDay.containsKey(day))
                throw new IllegalArgumentException(String.format("%s - Day %d has no capacity yet", name, day));

            if(slotsLeft.get() >= slotCapacityByDay.get(day))
                throw new IllegalArgumentException(String.format("%s - Day %d has reached its maximum capacity. Cannot add a slot", name, day));

            slotsLeft.incrementAndGet();
        } finally {
            slotCapacityLock.unlock();
        }
    }


    public boolean isSlotCapacitySet(Integer day) {
        return getSlotCapacityForDay(day) != -1;
    }

    public ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentSkipListSet<Reservation>>> getBookedSlots() {
        return bookedSlots;
    }

    public void setSlotCapacityForDay(Integer day, Integer slotCapacity) {
        synchronized (slotCapacityByDay) {
            if (isSlotCapacitySet(day))
                throw new SlotCapacityException(String.format("Capacity is already set for day %d in ride %s", day, this.name));

            slotCapacityByDay.put(day, slotCapacity);

            slotsLeftByDayAndTimeSlot.putIfAbsent(day, new HashMap<>());
            bookedSlots.putIfAbsent(day, new ConcurrentHashMap<>());

            Map<String, AtomicInteger> daySlots = slotsLeftByDayAndTimeSlot.get(day);
            ConcurrentMap<String, ConcurrentSkipListSet<Reservation>> bookedDaySlots = bookedSlots.get(day);
            List<String> times = rideTime.getTimeSlotsAsStrings();


            for(String time : times){
                daySlots.put(time, new AtomicInteger(slotCapacity));
                bookedDaySlots.putIfAbsent(time, new ConcurrentSkipListSet<>());
            }
        }
    }

    public boolean isTimeSlotValid(ParkLocalTime time) {
        return rideTime.isTimeSlotValid(time);
    }

    public boolean isTimeSlotValid(String time) {
        return rideTime.isTimeSlotValid(time);
    }

    public List<ParkLocalTime> getTimeSlotsBetween(ParkLocalTime startTimeSlot, ParkLocalTime endTimeSlot){
        return rideTime.getTimeSlotsBetween(startTimeSlot, endTimeSlot);
    }

    public AdminParkServiceOuterClass.SlotCapacityResponse addSlotCapacityPerDay(ParkPassRepository parkPassInstance, int day, int capacity){
        // si ya tiene una capacidad asignada falla, sino la agrega
        setSlotCapacityForDay(day, capacity);

        int acceptedAmount = 0;
        int relocatedAmount = 0;
        int cancelledAmount = 0;

        ConcurrentMap<String, ConcurrentSkipListSet<Reservation>> realocateReservations = new ConcurrentHashMap<>();

        if(bookedSlots.containsKey(day)){
            List<String> timeSlots = rideTime.getTimeSlotsAsStrings();
            for (int i =0 ; i< timeSlots.size(); i++) {
                String timeSlot = timeSlots.get(i);
                ParkLocalTime parkLocalTime =  ParkLocalTime.fromString(timeSlot);
                ConcurrentSkipListSet<Reservation> reservations = bookedSlots.get(day).get(timeSlot);

                // Si tengo para realocar lo hago
                if(realocateReservations.containsKey(timeSlot)){
                    reservations.addAll(realocateReservations.get(timeSlot));
                    realocateReservations.remove(timeSlot);
                }

                for (Reservation r: reservations) {

                    if(r.isRegisteredForNotifications())
                        r.notifySlotsCapacityAdded(capacity);

                    UUID visitorId = r.getVisitorId();
                    Models.PassTypeEnum passType = parkPassInstance.getVisitorParkType(visitorId, day);

                    if (checkVisitorPass(parkPassInstance, passType, visitorId, parkLocalTime, day)) {
                        if (getSlotsLeft(day, parkLocalTime).get() > 0) { // Si tengo lugar para guardar, guardo
                            if (r.getState() == ReservationState.RELOCATED) {
                                relocatedAmount += 1;
                            } else {
                                r.setConfirmed();
                                if (r.isRegisteredForNotifications())
                                    r.notifyConfirmed();
                                acceptedAmount += 1;
                            }
                            decrementCapacity(day, parkLocalTime);
                        }else{
                            for(String after :timeSlots.subList(i + 1, timeSlots.size())){
                                ParkLocalTime afterTime =  ParkLocalTime.fromString(after);
                                ConcurrentSkipListSet<Reservation> afterReservations = bookedSlots.get(day).get(after);
                                if(passType.equals(Models.PassTypeEnum.HALFDAY) && !parkPassInstance.checkHalfDayPass(afterTime)) {
                                    // Chequeo el pase, y sino puedo salgo
                                    break;
                                }
                                // Cuantas ya tengo reubicadas aca
                                int realocated = 0;
                                if(realocateReservations.containsKey(after)){
                                    realocated=realocateReservations.get(after).size();
                                }
                                // Si tengo capacidad la reubico (las reservas + las que quiero realocar ahi)
                                if(afterReservations.size() + realocated < capacity){
                                    r.setRelocated();
                                    realocateReservations.putIfAbsent(after, new ConcurrentSkipListSet<>());
                                    realocateReservations.get(after).add(r);

                                    if(r.isRegisteredForNotifications())
                                        r.notifyRelocated(parkLocalTime);
                                    break;
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

                }
                // Elimino todas las reservas canceladas o q se realocaron en otra
                reservations.removeIf(reserv -> reserv.isCancelled() || reserv.isRelocated());
            }
        }

        return AdminParkServiceOuterClass.SlotCapacityResponse.newBuilder()
                .setAcceptedAmount(acceptedAmount).setCancelledAmount(cancelledAmount).setRelocatedAmount(relocatedAmount)
                .build();
    }

    private boolean checkVisitorPass(ParkPassRepository parkPassInstance, Models.PassTypeEnum passType, UUID visitorId, ParkLocalTime parkLocalTime, int day) {
        return passType.equals(Models.PassTypeEnum.HALFDAY) ? parkPassInstance.checkHalfDayPass(parkLocalTime) : parkPassInstance.checkVisitorPass(visitorId, day);
    }

    public ReservationState bookRide(int day, ParkLocalTime timeSlot, UUID visitorId) {
        boolean isCapacitySet = this.isSlotCapacitySet(day);
        if(isCapacitySet)
            if(this.getSlotsLeft(day, timeSlot).get() == 0)
                throw new ReservationLimitException(String.format("No more reservations available for ride '%s' on day %s at %s", this.name, day, timeSlot));


        bookedSlots.putIfAbsent(day, new ConcurrentHashMap<>());

        ConcurrentMap<String,ConcurrentSkipListSet<Reservation>> reservations = bookedSlots.get(day);

        ReservationState state = isCapacitySet ? ReservationState.CONFIRMED : ReservationState.PENDING;
        Reservation reservation = new Reservation(this.name, visitorId, state, day, timeSlot);
        String timeSlotString = timeSlot.toString();

        reservations.putIfAbsent(timeSlotString, new ConcurrentSkipListSet<>());
        Set<Reservation> timeSlotReservations = reservations.get(timeSlotString);

        if(timeSlotReservations.contains(reservation))
            throw new AlreadyExistsException(String.format("Visitor '%s' already booked a ticket for '%s' at time slot '%s'", visitorId, this.name, timeSlot));

        if(isCapacitySet)
            this.decrementCapacity(day, timeSlot);

        timeSlotReservations.add(reservation);
        return state;
    }

    @Override
    public RideBookingServiceOuterClass.Ride convertToGRPC() {
        return RideBookingServiceOuterClass.Ride.newBuilder()
                .setName(StringValue.of(this.name))
                .setOpeningTime(StringValue.of(rideTime.getOpen().toString()))
                .setClosingTime(StringValue.of(rideTime.getClose().toString()))
                .build();
    }

}
