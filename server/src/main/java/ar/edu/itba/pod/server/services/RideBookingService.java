package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.server.Models.ParkLocalTime;
import ar.edu.itba.pod.server.Models.RideAvailability;
import ar.edu.itba.pod.server.Models.requests.BookRideRequestModel;
import ar.edu.itba.pod.server.Models.requests.GetRideAvailabilityRequestModel;
import ar.edu.itba.pod.server.ridePersistence.RideRepository;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import java.util.*;

import rideBooking.Models;
import rideBooking.RideBookingServiceGrpc;
import rideBooking.RideBookingServiceOuterClass;
import rideBooking.RideBookingServiceOuterClass.*;

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
        System.out.println("ACA 1");
        GetRideAvailabilityRequestModel requestModel = GetRideAvailabilityRequestModel.fromGetRideAvailabilityRequest(request);
        System.out.println("ACA 2");
        // TODO: Y si resuelvo esto adentro de RideRepository?
        Map<String, Map<ParkLocalTime, RideAvailability>> ridesAvailability;
        if(requestModel.getRideName() != null){
            System.out.println("ACA 3");
            if(requestModel.getEndTimeSlot() != null)
                ridesAvailability = rideRepository.getRidesAvailability(requestModel.getRideName(), requestModel.getStartTimeSlot(), requestModel.getEndTimeSlot(), requestModel.getDay());
            else
                ridesAvailability = rideRepository.getRidesAvailability(requestModel.getRideName(), requestModel.getStartTimeSlot(), requestModel.getDay());
        } else {
            System.out.println("ACA 4");
            ridesAvailability = rideRepository.getRidesAvailability(requestModel.getStartTimeSlot(), requestModel.getEndTimeSlot(), requestModel.getDay());
        }
        System.out.println("ACA 5");
        responseObserver.onNext(buildGetRideAvailabilityResponse(ridesAvailability));
        System.out.println("ACA 6");
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

        rideRepository.confirmBooking(requestModel.getRideName(), requestModel.getDay(), requestModel.getTimeSlot(),
                requestModel.getVisitorId());

        responseObserver.onNext(BookRideResponse.newBuilder().setStatus(Models.ReservationState.CONFIRMED).build());
        responseObserver.onCompleted();
    }

    @Override
    public void cancelBooking(BookRideRequest request, StreamObserver<BookRideResponse> responseObserver) {
        BookRideRequestModel requestModel = BookRideRequestModel.fromBookRideRequest(request);

        rideRepository.cancelBooking(requestModel.getRideName(), requestModel.getDay(), requestModel.getTimeSlot(),
                requestModel.getVisitorId());

        responseObserver.onNext(BookRideResponse.newBuilder().setStatus(Models.ReservationState.CANCELLED).build());
        responseObserver.onCompleted();
    }


}
