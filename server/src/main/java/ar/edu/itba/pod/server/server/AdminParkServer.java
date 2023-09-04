package ar.edu.itba.pod.server.server;

import ar.edu.itba.pod.server.Models.ParkPass;
import ar.edu.itba.pod.server.Models.Ride;
import ar.edu.itba.pod.server.Models.RideTime;
import ar.edu.itba.pod.server.persistance.RideRepository;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.AdminParkServiceGrpc;
import rideBooking.AdminParkServiceOuterClass.AddRideRequest;
import rideBooking.AdminParkServiceOuterClass.AddPassRequest;
import rideBooking.AdminParkServiceOuterClass.AddSlotCapacityRequest;

import java.util.Optional;
import java.util.UUID;


public class AdminParkServer extends AdminParkServiceGrpc.AdminParkServiceImplBase{
    private final static Logger logger = LoggerFactory.getLogger(AdminParkServer.class);
    private static final RideRepository repository = RideRepository.getInstance();

    @Override
    public void addRide(AddRideRequest request, StreamObserver<Int32Value> responseObserver) {
        Optional<Ride> newRide = repository.addRide(request.getRideName(), new RideTime(request.getRideTime()), request.getSlotMinutes());
        newRide.ifPresentOrElse(
                ride -> {
                    responseObserver.onNext(Int32Value.of(ride.getId()));
                    responseObserver.onCompleted();
                },
                () -> {
                    final String msg = "Could not create Ride " + request.getRideName();
                    logger.error(msg);
                    responseObserver.onError(Status.INTERNAL.withDescription(msg).asRuntimeException());
                }
        );
    }

    @Override
    public void addPassToPark(AddPassRequest request, StreamObserver<BoolValue> responseObserver) {
        Optional<ParkPass> newPassToPark = repository.addParkPass(UUID.fromString(request.getVisitorId()), request.getPassType(), request.getValidDay());
        newPassToPark.ifPresentOrElse(
                parkPass -> {
                    responseObserver.onNext(BoolValue.of(true));
                    responseObserver.onCompleted();
                },
                () -> {
                    final String msg = "Could not create Pass for visitor " + request.getVisitorId() + " for the day " + request.getValidDay();
                    logger.error(msg);
                    responseObserver.onError(Status.INTERNAL.withDescription(msg).asRuntimeException());
                }
        );
    }

    @Override
    public void addSlotCapacity(AddSlotCapacityRequest request, StreamObserver<Int32Value> responseObserver) {
        super.addSlotCapacity(request, responseObserver);
    }
}
