package ar.edu.itba.pod.grpc.server;

import ar.edu.itba.pod.grpc.rideBooking.*;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

public class RideBookingServer extends RideBookingServiceGrpc.RideBookingServiceImplBase {

    @Override
    public void getRides(Empty request, StreamObserver<GetRideResponse> responseObserver) {
        super.getRides(request, responseObserver);
    }

    @Override
    public void getRideAvailability(GetRideAvailabilityRequest request, StreamObserver<GetRideAvailabilityResponse> responseObserver) {
        super.getRideAvailability(request, responseObserver);
    }

    @Override
    public void bookRide(BookRideRequest request, StreamObserver<BookRideResponse> responseObserver) {
        super.bookRide(request, responseObserver);
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
