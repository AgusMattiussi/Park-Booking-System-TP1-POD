package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.server.Models.*;
import ar.edu.itba.pod.server.Models.requests.QueryDayRequestModel;
import ar.edu.itba.pod.server.ridePersistence.RideRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.QueryServiceGrpc;
import rideBooking.QueryServiceOuterClass;
import rideBooking.QueryServiceOuterClass.CapacitySuggestionResponse;
import rideBooking.QueryServiceOuterClass.ConfirmedBookingsResponse;
import rideBooking.Models.ReservationState;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class QueryService extends QueryServiceGrpc.QueryServiceImplBase{

    private final static Logger logger = LoggerFactory.getLogger(AdminParkService.class);
    private static final RideRepository repository = RideRepository.getInstance();

    @Override
    public void queryCapacitySuggestion(QueryServiceOuterClass.QueryDayRequest request, StreamObserver<QueryServiceOuterClass.CapacitySuggestionResponse> responseObserver) {
        QueryDayRequestModel requestModel = QueryDayRequestModel.fromQueryDayRequest(request);

        List<CapacitySuggestion> responseList = getCapacitySuggestionList(requestModel.getDay());

        CapacitySuggestionResponse.Builder responseBuilder = CapacitySuggestionResponse.newBuilder();

        responseList.forEach(capacitySuggestion ->
                responseBuilder.addCapacitySuggestions(capacitySuggestion.convertToGRPC()));

        CapacitySuggestionResponse response = responseBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private List<CapacitySuggestion> getCapacitySuggestionList(int day){
        Map<String, Ride> rides = repository.getRides();
        List<CapacitySuggestion> responseList = new LinkedList<>();

        rides.values().forEach(ride -> {
            if(!ride.isSlotCapacitySet(day)) {
                String rideName = ride.getName();

                ConcurrentMap<String, ConcurrentSkipListSet<Reservation>> reservationsForQueryDay = repository.getReservationsByDay(rideName, day);
                int pendingBookings;

                if(reservationsForQueryDay != null) {
                    for (Map.Entry<String, ConcurrentSkipListSet<Reservation>> entry : reservationsForQueryDay.entrySet()) {
                        ParkLocalTime slot = ParkLocalTime.fromString(entry.getKey());
                        pendingBookings = 0;
                        for (Reservation reservation : entry.getValue()) {
                            if (reservation.getState() == ReservationState.PENDING) {
                                pendingBookings++;
                            }
                        }
                        responseList.add(new CapacitySuggestion(rideName, pendingBookings, slot.toString()));
                    }
                }
            }
        });
        responseList.sort(Comparator.comparingInt(CapacitySuggestion::getSuggestedCapacity).reversed());

        return responseList;
    }

    @Override
    public void queryConfirmedBookings(QueryServiceOuterClass.QueryDayRequest request, StreamObserver<QueryServiceOuterClass.ConfirmedBookingsResponse> responseObserver) {
        QueryDayRequestModel requestModel = QueryDayRequestModel.fromQueryDayRequest(request);

        List<ConfirmedBookings> responseList = getConfirmedBookingsList(requestModel.getDay());

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

            ConcurrentMap<String, ConcurrentSkipListSet<Reservation>> reservationsForQueryDay = repository.getReservationsByDay(rideName, day);

            if(reservationsForQueryDay != null) {
                for (Map.Entry<String, ConcurrentSkipListSet<Reservation>> entry : reservationsForQueryDay.entrySet()) {
                    ParkLocalTime slot = ParkLocalTime.fromString(entry.getKey());
                    for (Reservation reservation : entry.getValue()) {
                        if (reservation.getState() == ReservationState.CONFIRMED) {
                            confirmedBookings.add(new ConfirmedBookings(rideName, reservation.getVisitorId().toString(), slot.toString()));
                        }
                    }
                }
            }
        });

        confirmedBookings.sort(Comparator.comparing(ConfirmedBookings::getSlot).reversed());

        return confirmedBookings;
    }
}
