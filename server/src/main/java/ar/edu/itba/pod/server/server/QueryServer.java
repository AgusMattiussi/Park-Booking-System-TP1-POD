package ar.edu.itba.pod.server.server;

import ar.edu.itba.pod.server.Models.CapacitySuggestion;
import ar.edu.itba.pod.server.Models.ConfirmedBookings;
import ar.edu.itba.pod.server.Models.Ride;
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
            String rideName = ride.getName();
            Map<Date, Map<Time,Integer>> slotsPerDay = ride.getSlotsPerDay();

            // TODO: capacidad cargada => no se lista en la consulta
            for (Map.Entry<Time, Integer> entry : slotsPerDay.get(request.getDayOfYear()).entrySet()) {
                Time slot = entry.getKey();
                Integer capacity = entry.getValue();
                logger.error(slot + " | " + capacity + " | " + rideName + "\n");
                responseList.add(new CapacitySuggestion(rideName, capacity, slot.toString()));
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
            Map<Date, Map<Time,Integer>> slotsPerDay = ride.getSlotsPerDay();

            for (Map.Entry<Time, Integer> entry : slotsPerDay.get(request.getDayOfYear()).entrySet()) {
                Time slot = entry.getKey();
                logger.error(slot + " | " + "Visitor UUID" + " | " + rideName + "\n");
                responseList.add(new ConfirmedBookings(rideName, "visitorID", slot.toString()));
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
