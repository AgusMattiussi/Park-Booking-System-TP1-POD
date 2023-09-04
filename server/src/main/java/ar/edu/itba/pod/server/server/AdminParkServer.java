package ar.edu.itba.pod.server.server;

import ar.edu.itba.pod.server.Models.Ride;
import ar.edu.itba.pod.server.Models.RideTime;
import ar.edu.itba.pod.server.persistance.RideRepository;
import com.google.protobuf.BoolValue;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.AdminParkServiceGrpc;
import rideBooking.AdminParkServiceOuterClass.AddRideRequest;
import rideBooking.AdminParkServiceOuterClass.AddPassRequest;
import rideBooking.AdminParkServiceOuterClass.AddSlotCapacityRequest;


public class AdminParkServer extends AdminParkServiceGrpc.AdminParkServiceImplBase{
    private final static Logger logger = LoggerFactory.getLogger(AdminParkServer.class);
    private static final RideRepository repository = RideRepository.getInstance();

    @Override
    public void addRide(AddRideRequest request, StreamObserver<BoolValue> responseObserver) {
        Ride newRide = new Ride(request.getRideName(), new RideTime(request.getRideTime()), request.getSlotMinutes());
        repository.addRide(newRide);

    }

    @Override
    public void addPassToRide(AddPassRequest request, StreamObserver<BoolValue> responseObserver) {
        super.addPassToRide(request, responseObserver);
    }

    @Override
    public void addSlotCapacity(AddSlotCapacityRequest request, StreamObserver<BoolValue> responseObserver) {
        super.addSlotCapacity(request, responseObserver);
    }
}
