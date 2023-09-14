package ar.edu.itba.pod.server.passPersistance;

import ar.edu.itba.pod.server.Models.ParkLocalTime;
import ar.edu.itba.pod.server.Models.ParkPass;
import ar.edu.itba.pod.server.Models.Reservation;
import ar.edu.itba.pod.server.Models.Ride;
import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.ridePersistance.RideRepository;
import rideBooking.Models;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ParkPassRepository {
    private static final ParkLocalTime HALF_DAY_TIME = ParkLocalTime.fromString("14:00");
    private static ParkPassRepository instance;
    private static RideRepository rideRepository = RideRepository.getInstance();

    private final ConcurrentMap<UUID, ConcurrentMap<Integer, ParkPass>> parkPasses;

    private ParkPassRepository() {
        this.parkPasses = new ConcurrentHashMap<>();
    }

    public static ParkPassRepository getInstance() {
        if (instance == null) {
            instance = new ParkPassRepository();
        }
        return instance;
    }

    private void checkPassExistence(UUID visitorId, int day){
        if(this.parkPasses.get(visitorId).containsKey(day)){
//              si ya existe un pase para el visitante para el día indicado
            throw new AlreadyExistsException("There already exist a parkPass for the visitor for the day " + day);
        }
    }


    //    Si el pase es halfDay y quiero reservas desp de las 14hs
    public boolean checkHalfDayPass(ParkLocalTime reservationTime){
        return reservationTime.isBefore(HALF_DAY_TIME);
    }

    public Optional<ParkPass> addParkPass(UUID visitorId, Models.PassTypeEnum type, int day){
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

//    True si puedo seguir reservando, false si no puedo
//    Chequeo si es half day que la reserva sea antes de las 14hs
//    y si es three que no tenga 3 o mas ya hechas
    public boolean checkVisitorPass(UUID visitorId, int day){
        List<Reservation> reservationSet = new ArrayList<>();
        System.out.println("ARRANCO A CHEQUEAR");
        for (String rideName: rideRepository.getRides().keySet()) {
            System.out.println("Estoy aca");
            List<Reservation> reservations = rideRepository.getUserReservationsByDay(rideName, day, visitorId);
            if(reservations != null){
                System.out.println("Agrego estos: ");
                System.out.println(reservations);
                reservationSet.addAll(reservations);}

        }

        System.out.println("\n\n\n" + "Set = " + reservationSet + "\n\n\n");

        Models.PassTypeEnum passType = this.parkPasses.get(visitorId).get(day).getType();
        if(passType == Models.PassTypeEnum.THREE){
            System.out.println("Found " + reservationSet.size() + " rides for visitor");
        }
        return passType == Models.PassTypeEnum.UNLIMITED || reservationSet.size() < 3;
    }

    public Models.PassTypeEnum getVisitorParkType(UUID visitorId, int day){
        return this.parkPasses.get(visitorId).get(day).getType();
    }

    public boolean hasValidPass(UUID visitorId, int day) {
        return this.parkPasses.containsKey(visitorId) && this.parkPasses.get(visitorId).containsKey(day);
    }

    public ConcurrentMap<UUID, ConcurrentMap<Integer, ParkPass>> getParkPasses() {
        return parkPasses;
    }
}
