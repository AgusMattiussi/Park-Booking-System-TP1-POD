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


import java.sql.Time;
import java.time.LocalTime;
import java.util.*;

public class QueryServer extends QueryServiceGrpc.QueryServiceImplBase{

    private final static Logger logger = LoggerFactory.getLogger(AdminParkServer.class);
    private static final RideRepository repository = RideRepository.getInstance();

    @Override
    public void queryCapacitySuggestion(QueryServiceOuterClass.QueryDayRequest request, StreamObserver<QueryServiceOuterClass.CapacitySuggestionResponse> responseObserver) {
        if(request.getDayOfYear().getValue() < 1 || request.getDayOfYear().getValue() > 365) {
            throw new InvalidTimeException("Invalid day of year");
        }

        Map<String, Ride> rides = repository.getRides();
        List<CapacitySuggestion> responseList = new LinkedList<>();

        logger.error("Capacity Suggestion Query\n");
        logger.error("Slot | Capacity | Ride\n");

        rides.values().forEach(ride -> {
            if(ride.getSlotCapacity() == null) {
                String rideName = ride.getName();

                Map<Integer, Map<LocalTime, List<Reservation>>> reservations = ride.getReservationsPerDay();
                int pendingBookings = 0;

                for (Map.Entry<LocalTime, List<Reservation>> entry : reservations.get(request.getDayOfYear().getValue()).entrySet()) {
                    LocalTime slot = entry.getKey();
                    for (Reservation reservation : entry.getValue()) {
                        if (reservation.getState() == ReservationState.PENDING) {
                            pendingBookings++;
                        }
                    }
                    responseList.add(new CapacitySuggestion(rideName, pendingBookings, slot.toString()));
                }
            }
        });

        responseList.sort(Comparator.comparingInt(CapacitySuggestion::getSuggestedCapacity).reversed());

        CapacitySuggestionResponse response = CapacitySuggestionResponse.newBuilder()
                // TODO: iterable?
//                .addAllCapacitySuggestions(responseList)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void queryConfirmedBookings(QueryServiceOuterClass.QueryDayRequest request, StreamObserver<QueryServiceOuterClass.ConfirmedBookingsResponse> responseObserver) {
        if(request.getDayOfYear().getValue() < 1 || request.getDayOfYear().getValue() > 365) {
            throw new InvalidTimeException("Invalid day of year");
        }

//        ConcurrentMap<String, ConcurrentMap<String, ConcurrentSkipListSet<LocalDateTime>>> bookedRides = repository.getBookedRides();
        // TODO: revisar estructura de estado
        Map<String, Ride> rides = repository.getRides();
        List<ConfirmedBookings> responseList = new LinkedList<>();

        logger.error("Confirmed Bookings Query\n");
        logger.error("Slot | Visitor | Ride\n");

        rides.values().forEach(ride -> {
            String rideName = ride.getName();

            Map<Integer, Map<LocalTime, List<Reservation>>> reservations = ride.getReservationsPerDay();

            for (Map.Entry<LocalTime, List<Reservation>> entry : reservations.get(request.getDayOfYear().getValue()).entrySet()) {
                LocalTime slot = entry.getKey();
                for (Reservation reservation : entry.getValue()) {
                    responseList.add(new ConfirmedBookings(rideName, reservation.getVisitorId().toString(), slot.toString()));
                }
            }
        });

        responseList.sort(Comparator.comparing(ConfirmedBookings::getSlot));

        ConfirmedBookingsResponse response = ConfirmedBookingsResponse.newBuilder()
                // TODO: iterable?
//                .addAllCapacitySuggestions(responseList)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
