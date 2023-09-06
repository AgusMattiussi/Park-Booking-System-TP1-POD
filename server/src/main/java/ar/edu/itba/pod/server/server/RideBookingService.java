package ar.edu.itba.pod.server.server;

import ar.edu.itba.pod.server.Models.Ride;
import ar.edu.itba.pod.server.persistance.RideRepository;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

import rideBooking.RideBookingServiceGrpc;
import rideBooking.RideBookingServiceOuterClass.*;

public class RideBookingService extends RideBookingServiceGrpc.RideBookingServiceImplBase {

    private static Logger logger = LoggerFactory.getLogger(Server.class);
    private final RideRepository rideRepository = RideRepository.getInstance();

    @Override
    public void getRides(Empty request, StreamObserver<GetRideResponse> responseObserver) {
        //TODO: Cambiar getRides() a Collection?
        Collection<Ride> rides = rideRepository.getRidesList();
        GetRideResponse.Builder responseBuilder = GetRideResponse.newBuilder();

        for (Ride ride : rides) {
            responseBuilder.addRides(ride.convertToGRPC());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }



    @Override
    public void getRideAvailability(GetRideAvailabilityRequest request, StreamObserver<GetRideAvailabilityResponse> responseObserver) {
        super.getRideAvailability(request, responseObserver);
    }

    @Override
    public void bookRide(BookRideRequest request, StreamObserver<BookRideResponse> responseObserver) {
        List<BookRide> ridesToBook = request.getRidesList();

        // TODO: Validaciones
        int booked = 0;

        StringBuilder responseBuilder = new StringBuilder();

        for (BookRide ride : ridesToBook) {
            String rideName = ride.getRideName().getValue();
            String visitorId = ride.getVisitorId().getValue();
            String day = ride.getDayOfYear().getValue();
            String slot = ride.getTimeSlot().getValue();

            LocalDate date = LocalDate.parse(day, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime time = LocalTime.parse(slot, DateTimeFormatter.ofPattern("HH:mm"));
            LocalDateTime dateTime = date.atTime(time);

            if(!rideRepository.bookRide(rideName, visitorId, dateTime))
                continue;

            booked++;
            //TODO: Status pending o reservado
            responseBuilder.append(rideName)
                    .append(" booked for ")
                    .append(visitorId)
                    .append(" at ")
                    .append(dateTime)
                    .append(" - CONFIRMED")
                    .append("\n");

        }

        BookRideResponse response = BookRideResponse.newBuilder()
                                    .setResponse(StringValue.newBuilder()
                                            .setValue(responseBuilder.toString())
                                            .build())
                                    .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void confirmBooking(BookRide request, StreamObserver<BookRideResponse> responseObserver) {
        super.confirmBooking(request, responseObserver);
    }

    @Override
    public void cancelBooking(BookRide request, StreamObserver<BookRideResponse> responseObserver) {
        super.cancelBooking(request, responseObserver);
    }
}
