package ar.edu.itba.pod.server.server;


import ar.edu.itba.pod.server.persistance.RideRepository;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.NotifyServiceGrpc;
import rideBooking.NotifyServiceOuterClass.NotifyRequest;
import rideBooking.NotifyServiceOuterClass.Notification;

public class NotifyServer extends NotifyServiceGrpc.NotifyServiceImplBase{

    private final static Logger logger = LoggerFactory.getLogger(Server.class);
    private static final RideRepository repository = RideRepository.getInstance();

    @Override
    public void notifyVisitor(NotifyRequest request, StreamObserver<Notification> responseObserver) {
        super.notifyVisitor(request, responseObserver);
    }

    @Override
    public void notifyRemoveVisitor(NotifyRequest request, StreamObserver<Empty> responseObserver) {
        super.notifyRemoveVisitor(request, responseObserver);
    }
}
