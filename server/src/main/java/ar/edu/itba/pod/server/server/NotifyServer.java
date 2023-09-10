package ar.edu.itba.pod.server.server;


import ar.edu.itba.pod.server.Models.requests.BookRideRequestModel;
import ar.edu.itba.pod.server.persistance.RideRepository;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.Models;
import rideBooking.NotifyServiceGrpc;
import rideBooking.NotifyServiceOuterClass.NotifyRequest;
import rideBooking.NotifyServiceOuterClass.Notification;
import rideBooking.RideBookingServiceOuterClass;

import java.util.UUID;

public class NotifyServer extends NotifyServiceGrpc.NotifyServiceImplBase{

    private final static Logger logger = LoggerFactory.getLogger(Server.class);
    private static final RideRepository repository = RideRepository.getInstance();

    @Override
    public void notifyVisitor(NotifyRequest request, StreamObserver<Notification> responseObserver) {
        boolean status = repository.addVisitor(UUID.fromString(request.getVisitorId()), request.getRideName(), request.getDayOfYear());
        final String msg;
        if(status) {
            msg = "Visitor has been registered for notifications from ride " + request.getRideName() + " on day " + request.getDayOfYear();
        }
        else {
            msg = "Visitor could not be registered for notifications.";
        }
        responseObserver.onNext(Notification.newBuilder().setMessage(msg).build());
        responseObserver.onCompleted();
    }

    @Override
    public void notifyRemoveVisitor(NotifyRequest request, StreamObserver<Empty> responseObserver) {
        boolean status = repository.removeVisitor(UUID.fromString(request.getVisitorId()), request.getRideName(), request.getDayOfYear());
        final String msg;
        if(status) {
            msg = "Visitor has been registered for notifications from ride " + request.getRideName() + " on day " + request.getDayOfYear();
        }
        else {
            msg = "Visitor could not be registered for notifications.";
        }
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
