package ar.edu.itba.pod.server.server;

import ar.edu.itba.pod.server.Models.ParkLocalTime;
import ar.edu.itba.pod.server.Models.RideAvailability;
import ar.edu.itba.pod.server.Models.requests.BookRideRequestModel;
import ar.edu.itba.pod.server.Models.requests.GetRideAvailabilityRequestModel;
import ar.edu.itba.pod.server.ridePersistance.RideRepository;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import java.util.*;

import rideBooking.Models;
import rideBooking.RideBookingServiceGrpc;
import rideBooking.RideBookingServiceOuterClass;
import rideBooking.RideBookingServiceOuterClass.*;

//TODO: Hace falta try catch aca?
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

        GetRideAvailabilityRequestModel requestModel = GetRideAvailabilityRequestModel.fromGetRideAvailabilityRequest(request);

        // TODO: Y si resuelvo esto adentro de RideRepository?
        Map<String, Map<ParkLocalTime, RideAvailability>> ridesAvailability;
        if(requestModel.getRideName() != null){
            if(requestModel.getEndTimeSlot() != null)
                ridesAvailability = rideRepository.getRidesAvailability(requestModel.getRideName(), requestModel.getStartTimeSlot(), requestModel.getEndTimeSlot(), requestModel.getDay());
            else
                ridesAvailability = rideRepository.getRidesAvailability(requestModel.getRideName(), requestModel.getStartTimeSlot(), requestModel.getDay());
        } else {
            ridesAvailability = rideRepository.getRidesAvailability(requestModel.getStartTimeSlot(), requestModel.getEndTimeSlot(), requestModel.getDay());
        }

        responseObserver.onNext(buildGetRideAvailabilityResponse(ridesAvailability));
        responseObserver.onCompleted();
    }

    private GetRideAvailabilityResponse buildGetRideAvailabilityResponse(Map<String, Map<ParkLocalTime, RideAvailability>> ridesAvailability){
        GetRideAvailabilityResponse.Builder responseBuilder = GetRideAvailabilityResponse.newBuilder();

        ridesAvailability.forEach((ride, availability) -> {
            RideBookingServiceOuterClass.RideAvailability.Builder rideAvailabilityBuilder =
                    RideBookingServiceOuterClass.RideAvailability.newBuilder().setRideName(StringValue.of(ride));

            availability.forEach((time, rideAvailability) -> rideAvailabilityBuilder.addTimeSlotAvailability(rideAvailability.convertToGRPC()));
            responseBuilder.addRideAvailability(rideAvailabilityBuilder.build());
        });

        return responseBuilder.build();
    }

    @Override
    public void bookRide(BookRideRequest request, StreamObserver<BookRideResponse> responseObserver) {
        BookRideRequestModel requestModel = BookRideRequestModel.fromBookRideRequest(request);

        Models.ReservationState status = rideRepository.bookRide(requestModel.getRideName(), requestModel.getDay(),
                requestModel.getTimeSlot(), requestModel.getVisitorId());

        responseObserver.onNext(BookRideResponse.newBuilder().setStatus(status).build());
        responseObserver.onCompleted();
    }

    @Override
    public void confirmBooking(BookRideRequest request, StreamObserver<BookRideResponse> responseObserver) {
        BookRideRequestModel requestModel = BookRideRequestModel.fromBookRideRequest(request);

        //TODO: Cambiar a boolean?
        rideRepository.confirmBooking(requestModel.getRideName(), requestModel.getDay(), requestModel.getTimeSlot(),
                requestModel.getVisitorId());

        responseObserver.onNext(BookRideResponse.newBuilder().setStatus(Models.ReservationState.CONFIRMED).build());
        responseObserver.onCompleted();
    }

    @Override
    public void cancelBooking(BookRideRequest request, StreamObserver<BookRideResponse> responseObserver) {
        BookRideRequestModel requestModel = BookRideRequestModel.fromBookRideRequest(request);

        //TODO: Cambiar a boolean?
        rideRepository.cancelBooking(requestModel.getRideName(), requestModel.getDay(), requestModel.getTimeSlot(),
                requestModel.getVisitorId());

        responseObserver.onNext(BookRideResponse.newBuilder().setStatus(Models.ReservationState.CANCELLED).build());
        responseObserver.onCompleted();
    }


}
