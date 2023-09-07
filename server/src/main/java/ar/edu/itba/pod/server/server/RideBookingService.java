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
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import rideBooking.RideBookingServiceGrpc;
import rideBooking.RideBookingServiceOuterClass.*;

public class RideBookingService extends RideBookingServiceGrpc.RideBookingServiceImplBase {

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
        //TODO: Validar parametros de entrada?

        LocalTime timeSlot;
        try {
            timeSlot = parseTime(request.getTimeSlot().getValue());
        } catch (DateTimeParseException e) {
            //TODO: Pensar que hacer en este caso. Quien maneja los throws?
            return;
        }

        String rideName = request.getRideName().getValue();
        int dayOfTheYear = Integer.parseInt(request.getDayOfYear().getValue());
        UUID visitorId = UUID.fromString(request.getVisitorId().getValue());

        //TODO: ver que onda throws
        rideRepository.bookRide(rideName, dayOfTheYear, timeSlot, visitorId);
    }

    @Override
    public void confirmBooking(BookRideRequest request, StreamObserver<BookRideResponse> responseObserver) {
        super.confirmBooking(request, responseObserver);
    }

    @Override
    public void cancelBooking(BookRideRequest request, StreamObserver<BookRideResponse> responseObserver) {
        super.cancelBooking(request, responseObserver);
    }

    private LocalTime parseTime(String time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return LocalTime.parse(time, formatter);
    }
}
