package ar.edu.itba.pod.server.passPersistance;

import ar.edu.itba.pod.server.Models.ParkLocalTime;
import ar.edu.itba.pod.server.Models.ParkPass;
import ar.edu.itba.pod.server.Models.Reservation;
import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import rideBooking.Models;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ParkPassRepository {
    private static final ParkLocalTime HALF_DAY_TIME = ParkLocalTime.fromString("14:00");
    private static ParkPassRepository instance;

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
    private boolean checkHalfDayPass(ParkLocalTime reservationTime){
        return !reservationTime.isAfter(HALF_DAY_TIME);
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
    public boolean checkVisitorPass(Models.PassTypeEnum passType, UUID visitorId,  Set<Reservation> reservationSet,
                                     ParkLocalTime reservationTime){
        int passes = 0;
        if(passType == Models.PassTypeEnum.THREE){
            passes = (int) reservationSet.stream().filter(r -> r.getVisitorId() == visitorId).count();
        }else if(passType == Models.PassTypeEnum.HALFDAY){
            return checkHalfDayPass(reservationTime);
        }
        return passType == Models.PassTypeEnum.UNLIMITED || passes < 3;
    }

    public Models.PassTypeEnum  getVisitorParkType(UUID visitorId, int day){
        return this.parkPasses.get(visitorId).get(day).getType();
    }

    //FIXME: Chequear si anda
    public boolean hasValidPass(UUID visitorId, int day) {
        return this.parkPasses.containsKey(visitorId) && this.parkPasses.get(visitorId).containsKey(day);
    }
}
