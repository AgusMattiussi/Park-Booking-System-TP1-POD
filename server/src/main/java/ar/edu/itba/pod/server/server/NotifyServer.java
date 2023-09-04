package ar.edu.itba.pod.server.server;


import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import rideBooking.NotifyServiceGrpc;
import rideBooking.NotifyServiceOuterClass.NotifyRequest;
import rideBooking.NotifyServiceOuterClass.Notification;

public class NotifyServer extends NotifyServiceGrpc.NotifyServiceImplBase{
    @Override
    public void notifyVisitor(NotifyRequest request, StreamObserver<Notification> responseObserver) {
        super.notifyVisitor(request, responseObserver);
    }

    @Override
    public void notifyRemoveVisitor(NotifyRequest request, StreamObserver<Empty> responseObserver) {
        super.notifyRemoveVisitor(request, responseObserver);
    }
}
