package ar.edu.itba.pod.server.server;

import ar.edu.itba.pod.server.Models.Ride;
import ar.edu.itba.pod.server.Models.RideAvailability;
import ar.edu.itba.pod.server.persistance.RideRepository;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import rideBooking.Models;
import rideBooking.RideBookingServiceGrpc;
import rideBooking.RideBookingServiceOuterClass;
import rideBooking.RideBookingServiceOuterClass.*;

//TODO: Para no repetir codigo, podria cambiar los parametros de RideRepository para recibir Reservation y crearla afuera
public class RideBookingService extends RideBookingServiceGrpc.RideBookingServiceImplBase {

    private final RideRepository rideRepository = RideRepository.getInstance();

    @Override
    public void getRides(Empty request, StreamObserver<GetRideResponse> responseObserver) {
        //TODO: Cambiar getRides() a Collection?
        GetRideResponse.Builder responseBuilder = GetRideResponse.newBuilder();

        rideRepository.getRidesList().forEach(ride -> responseBuilder.addRides(ride.convertToGRPC()));
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getRideAvailability(GetRideAvailabilityRequest request, StreamObserver<GetRideAvailabilityResponse> responseObserver) {
        LocalTime startTimeSlot;
        LocalTime endTimeSlot = null;
        String rideName = null;
        int dayOfTheYear;


        try {
            startTimeSlot = parseTime(request.getStartTimeSlot().getValue());
        } catch (DateTimeParseException e) {
            //TODO: Pensar que hacer en este caso. Quien maneja los throws?
            return;

        }

        if(request.hasEndTimeSlot()) {
            try {
                endTimeSlot = parseTime(request.getEndTimeSlot().getValue());
            } catch (DateTimeParseException e) {
                //TODO: Pensar que hacer en este caso. Quien maneja los throws?
                return;
            }
        }

        if(request.hasRideName())
            rideName = request.getRideName().getValue();

        dayOfTheYear = Integer.parseInt(request.getDayOfYear().getValue());

        // TODO: Este condicional puede no ser suficiente si estan mal los parametros
        Map<String, Map<LocalTime, RideAvailability>> ridesAvailability = new HashMap<>();
        if(rideName != null){
            if(endTimeSlot != null)
                ridesAvailability = rideRepository.getRidesAvailability(rideName, startTimeSlot, endTimeSlot, dayOfTheYear);
            else
                ridesAvailability = rideRepository.getRidesAvailability(rideName, startTimeSlot, dayOfTheYear);
        } else {
            ridesAvailability = rideRepository.getRidesAvailability(startTimeSlot, endTimeSlot, dayOfTheYear);
        }

        GetRideAvailabilityResponse.Builder responseBuilder = GetRideAvailabilityResponse.newBuilder();

        ridesAvailability.forEach((ride, availability) -> {
            RideBookingServiceOuterClass.RideAvailability.Builder rideAvailabilityBuilder =
                    RideBookingServiceOuterClass.RideAvailability.newBuilder().setRideName(StringValue.of(ride));

            availability.forEach((time, rideAvailability) -> rideAvailabilityBuilder.addTimeSlotAvailability(rideAvailability.convertToGRPC()));

            responseBuilder.addRideAvailability(rideAvailabilityBuilder.build());
        });

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
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
        boolean result = rideRepository.bookRide(rideName, dayOfTheYear, timeSlot, visitorId);
        Models.SimpleStatusResponse status = result ? Models.SimpleStatusResponse.OK : Models.SimpleStatusResponse.ERROR;

        responseObserver.onNext(BookRideResponse.newBuilder().setStatus(status).build());
        responseObserver.onCompleted();
    }

    @Override
    public void confirmBooking(BookRideRequest request, StreamObserver<BookRideResponse> responseObserver) {
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
        //TODO: Cambiar a boolean?
        rideRepository.confirmBooking(rideName, dayOfTheYear, timeSlot, visitorId);


        responseObserver.onNext(BookRideResponse.newBuilder().setStatus(Models.SimpleStatusResponse.OK).build());
        responseObserver.onCompleted();
    }

    @Override
    public void cancelBooking(BookRideRequest request, StreamObserver<BookRideResponse> responseObserver) {
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
        //TODO: Cambiar a boolean?
        rideRepository.cancelBooking(rideName, dayOfTheYear, timeSlot, visitorId);
        responseObserver.onNext(BookRideResponse.newBuilder().setStatus(Models.SimpleStatusResponse.OK).build());
        responseObserver.onCompleted();
    }

    private LocalTime parseTime(String time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return LocalTime.parse(time, formatter);
    }
}
