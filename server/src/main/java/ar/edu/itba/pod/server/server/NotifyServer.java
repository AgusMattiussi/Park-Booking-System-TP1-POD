package ar.edu.itba.pod.server.server;


import ar.edu.itba.pod.server.persistance.RideRepository;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.Models;
import rideBooking.NotifyServiceGrpc;
import rideBooking.NotifyServiceOuterClass;
import rideBooking.NotifyServiceOuterClass.NotifyRequest;
import rideBooking.NotifyServiceOuterClass.Notification;

import java.util.UUID;

public class NotifyServer extends NotifyServiceGrpc.NotifyServiceImplBase{

    private final static Logger logger = LoggerFactory.getLogger(Server.class);
    private static final RideRepository repository = RideRepository.getInstance();

    @Override
    public void notifyVisitor(NotifyRequest request, StreamObserver<NotifyServiceOuterClass.Notification> responseObserver) {



    }

    @Override
    public void notifyRemoveVisitor(NotifyRequest request, StreamObserver<NotifyServiceOuterClass.Notification> responseObserver) {

    }
}
