package ar.edu.itba.pod.server.server;

import ar.edu.itba.pod.server.Models.*;
import ar.edu.itba.pod.server.exceptions.InvalidTimeException;
import ar.edu.itba.pod.server.persistance.RideRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.QueryServiceGrpc;
import rideBooking.QueryServiceOuterClass;
import rideBooking.QueryServiceOuterClass.CapacitySuggestionResponse;
import rideBooking.QueryServiceOuterClass.ConfirmedBookingsResponse;
import rideBooking.Models.ReservationState;
import java.util.*;

public class QueryService extends QueryServiceGrpc.QueryServiceImplBase{

    private final static Logger logger = LoggerFactory.getLogger(AdminParkServer.class);
    private static final RideRepository repository = RideRepository.getInstance();

    @Override
    public void queryCapacitySuggestion(QueryServiceOuterClass.QueryDayRequest request, StreamObserver<QueryServiceOuterClass.CapacitySuggestionResponse> responseObserver) {
        int day = Integer.parseInt(request.getDayOfYear().getValue());

        if(day < 1 || day > 365) {
            throw new InvalidTimeException("Day must be between 1 and 365");
        }

        List<CapacitySuggestion> responseList = getCapacitySuggestionList(day);

        CapacitySuggestionResponse.Builder responseBuilder = CapacitySuggestionResponse.newBuilder();

        responseList.forEach(capacitySuggestion -> {
            responseBuilder.addCapacitySuggestions(capacitySuggestion.convertToGRPC());
        });

        CapacitySuggestionResponse response = responseBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private List<CapacitySuggestion> getCapacitySuggestionList(int day){
        Map<String, Ride> rides = repository.getRides();
        List<CapacitySuggestion> responseList = new LinkedList<>();

        rides.values().forEach(ride -> {
            if(!ride.isSlotCapacitySet(day)) { // Si la atracción ya cuenta con una capacidad cargada entonces no debe listarse en la consulta.
                String rideName = ride.getName();

                Map<Integer, Map<ParkLocalTime, Set<Reservation>>> reservations = ride.getReservationsPerDay();
                int pendingBookings = 0;

                for (Map.Entry<ParkLocalTime, Set<Reservation>> entry : reservations.get(day).entrySet()) {
                    ParkLocalTime slot = entry.getKey();
                    for (Reservation reservation : entry.getValue()) {
                        if (reservation.getState() == ReservationState.PENDING) {
                            pendingBookings++;
                        }
                    }
                    responseList.add(new CapacitySuggestion(rideName, pendingBookings, slot.toString()));
                }
            }
        });
        responseList.sort(Comparator.comparingInt(CapacitySuggestion::getSuggestedCapacity).reversed()
                .thenComparing(CapacitySuggestion::getRideName));

        return responseList;
    }

    @Override
    public void queryConfirmedBookings(QueryServiceOuterClass.QueryDayRequest request, StreamObserver<QueryServiceOuterClass.ConfirmedBookingsResponse> responseObserver) {
        int day = Integer.parseInt(request.getDayOfYear().getValue());

        if(day < 1 || day > 365) {
            throw new IllegalArgumentException("Day must be between 1 and 365");
        }

        List<ConfirmedBookings> responseList = getConfirmedBookingsList(day);

        ConfirmedBookingsResponse.Builder responseBuilder = ConfirmedBookingsResponse.newBuilder();

        responseList.forEach(confirmedBookings -> {
            responseBuilder.addConfirmedBookings(confirmedBookings.convertToGRPC());
        });

        ConfirmedBookingsResponse response = responseBuilder.build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private List<ConfirmedBookings> getConfirmedBookingsList(int day){
        Map<String, Ride> rides = repository.getRides();
        List<ConfirmedBookings> confirmedBookings = new LinkedList<>();

        rides.values().forEach(ride -> {
            String rideName = ride.getName();

            Map<Integer, Map<ParkLocalTime, Set<Reservation>>> reservations = ride.getReservationsPerDay();

            for (Map.Entry<ParkLocalTime, Set<Reservation>> entry : reservations.get(day).entrySet()) {
                ParkLocalTime slot = entry.getKey();
                for (Reservation reservation : entry.getValue()) {
                    if(reservation.getState() == ReservationState.CONFIRMED){
                        confirmedBookings.add(new ConfirmedBookings(rideName, reservation.getVisitorId().toString(), slot.toString()));
                    }
                }
            }
        });

        //TODO: Revisar orden
        confirmedBookings.sort(Comparator.comparing(ConfirmedBookings::getSlot).thenComparing(ConfirmedBookings::getRideName));

        return confirmedBookings;
    }
}
