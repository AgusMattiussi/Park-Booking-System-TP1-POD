package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.server.Models.ParkPass;
import ar.edu.itba.pod.server.Models.Ride;
import ar.edu.itba.pod.server.Models.RideTime;
import ar.edu.itba.pod.server.Models.requests.AddPassRequestModel;
import ar.edu.itba.pod.server.Models.requests.AddRideRequestModel;
import ar.edu.itba.pod.server.Models.requests.AddSlotCapacityRequestModel;
import ar.edu.itba.pod.server.ridePersistance.RideRepository;
import com.google.protobuf.BoolValue;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.AdminParkServiceGrpc;
import rideBooking.AdminParkServiceOuterClass.*;

import java.util.Optional;


public class AdminParkService extends AdminParkServiceGrpc.AdminParkServiceImplBase{
    private final static Logger logger = LoggerFactory.getLogger(AdminParkService.class);
    private static final RideRepository repository = RideRepository.getInstance();

    @Override
    public void addRide(AddRideRequest request, StreamObserver<BoolValue> responseObserver) {
        AddRideRequestModel requestModel = AddRideRequestModel.fromAddRideRequest(request);

        Optional<Ride> newRide = repository.addRide(requestModel.getRideName(),
                new RideTime(requestModel.getStartTime(), requestModel.getEndTime(), requestModel.getSlotMinutes()),
                requestModel.getSlotMinutes());

        newRide.ifPresentOrElse(
                ride -> {
                    responseObserver.onNext(BoolValue.of(true));
                    responseObserver.onCompleted();
                },
                () -> {
                    responseObserver.onNext(BoolValue.of(false));
                    responseObserver.onCompleted();
                    logger.error(String.format("Could not create Ride %s", requestModel.getRideName()));
                }
        );
    }

    @Override
    public void addPassToPark(AddPassRequest request, StreamObserver<BoolValue> responseObserver) {
        AddPassRequestModel requestModel = AddPassRequestModel.fromAddPassRequest(request);

        Optional<ParkPass> newPassToPark = repository.addParkPass(requestModel.getVisitorId(), requestModel.getPassType(),
                requestModel.getDay());

        newPassToPark.ifPresentOrElse(
                parkPass -> {
                    responseObserver.onNext(BoolValue.of(true));
                    responseObserver.onCompleted();
                },
                () -> {
                    responseObserver.onNext(BoolValue.of(false));
                    responseObserver.onCompleted();
                    logger.error(String.format("Could not create Pass for visitor %s for the day %d",requestModel.getVisitorId(), requestModel.getDay()));
                }
        );
    }

    @Override
    public void addSlotCapacity(AddSlotCapacityRequest request, StreamObserver<SlotCapacityResponse> responseObserver) {
        AddSlotCapacityRequestModel requestModel = AddSlotCapacityRequestModel.fromAddSlotCapacityRequest(request);

        SlotCapacityResponse reservations_amount = repository.addSlotsPerDay(requestModel.getRideName(),
                requestModel.getDay(), requestModel.getSlotCapacity());
        responseObserver.onNext(reservations_amount);
        responseObserver.onCompleted();
    }
}
