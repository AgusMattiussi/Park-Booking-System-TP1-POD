package ar.edu.itba.pod.server.server;


import ar.edu.itba.pod.server.Models.requests.NotifyRequestModel;
import ar.edu.itba.pod.server.ridePersistance.RideRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rideBooking.Models;
import rideBooking.NotifyServiceGrpc;
import rideBooking.NotifyServiceOuterClass;
import rideBooking.NotifyServiceOuterClass.NotifyRequest;

public class NotifyService extends NotifyServiceGrpc.NotifyServiceImplBase{

    private final static Logger logger = LoggerFactory.getLogger(Server.class);
    private static final RideRepository repository = RideRepository.getInstance();

    @Override
    public void notifyVisitor(NotifyRequest request, StreamObserver<NotifyServiceOuterClass.Notification> responseObserver) {
        NotifyRequestModel requestModel = NotifyRequestModel.fromNotifyRequest(request);

        repository.registerForNotifications(requestModel.getVisitorId(),
                requestModel.getRideName(), requestModel.getDay(), responseObserver);
    }

    @Override
    public void notifyRemoveVisitor(NotifyRequest request, StreamObserver<NotifyServiceOuterClass.NotificationResponse> responseObserver) {
        NotifyRequestModel requestModel = NotifyRequestModel.fromNotifyRequest(request);

        StreamObserver<NotifyServiceOuterClass.Notification> notificationObserver =
                repository.unregisterForNotifications(requestModel.getVisitorId(),
                        requestModel.getRideName(), requestModel.getDay());

        notificationObserver.onCompleted();

        responseObserver.onNext(NotifyServiceOuterClass.NotificationResponse
                        .newBuilder()
                        .setStatus(Models.SimpleStatusResponse.OK)
                        .build());

        responseObserver.onCompleted();
    }
}
