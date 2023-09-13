package ar.edu.itba.pod.server.Models;

import java.security.InvalidParameterException;
import java.util.Objects;
import java.util.UUID;

import io.grpc.stub.StreamObserver;
import rideBooking.Models.ReservationState;
import rideBooking.NotifyServiceOuterClass;

public class Reservation implements Comparable<Reservation> {
    private final String rideName;
    private final UUID visitorId;
    //TODO: Esto requiere lock si o si
    private ReservationState state;
    private final int day;
    private final ParkLocalTime time;
    private boolean shouldNotify;
    //TODO: Chequear thread-safety
    private StreamObserver<NotifyServiceOuterClass.Notification> notificationObserver;

    public Reservation(String rideName, UUID visitorId, ReservationState state, int day, ParkLocalTime time) {
        this.rideName = rideName;
        this.visitorId = visitorId;
        this.state = state;
        this.day = day;
        this.time = time;
        this.shouldNotify = false;
        notificationObserver = null;
    }

    public UUID getVisitorId() {
        return visitorId;
    }

    public ReservationState getState() {
        return state;
    }

    public int getDay() {
        return day;
    }

    public ParkLocalTime getTime() {
        return time;
    }

    public void setState(ReservationState state) {
        this.state = state;
    }


    public void registerForNotifications(StreamObserver<NotifyServiceOuterClass.Notification> notificationObserver) {
        if(shouldNotify)
            throw new InvalidParameterException("Reservation is already registered for notifications");
        shouldNotify = true;

        this.notificationObserver = notificationObserver;
    }

    /* Returns the observer to be closed by the caller method */
    public StreamObserver<NotifyServiceOuterClass.Notification> unregisterForNotifications() {
        if(!shouldNotify)
            throw new InvalidParameterException("Reservation is not registered for notifications");
        shouldNotify = false;

        StreamObserver<NotifyServiceOuterClass.Notification> aux = notificationObserver;
        this.notificationObserver = null;
        return aux;
    }

    public boolean isRegisteredForNotifications() {
        return shouldNotify;
    }

    public void notifyVisitor(String message) {
        if(!shouldNotify)
            throw new InvalidParameterException("Reservation is not registered for notifications");
        notificationObserver.onNext(NotifyServiceOuterClass.Notification.newBuilder().setMessage(message).build());
    }

    private void notifyState(ReservationState state){
        notifyVisitor(String.format("The reservation for %s at %s on the day %d is %s", this.rideName, this.time.toString(), this.day, state.toString()));
    }

    public void notifyRegistered() {
        notifyState(this.state);
    }

    public void notifyConfirmed(){
        notifyState(ReservationState.CONFIRMED);
    }

    public void notifyCancelled(){
        notifyState(ReservationState.CANCELLED);
    }

    //FIXME: Va a devolver la misma notificacion para cada Reservation del dia
    public void notifySlotsCapacityAdded(int capacity){
        notifyVisitor(String.format("%s announced slot capacity for the day %d: %d places.", rideName, day, capacity));
    }

    //TODO: Siempre queda pending despues de realocar?
    public void notifyRelocated(String previousTime){
        notifyVisitor(String.format("The reservation for %s at %s on the day %d was moved to 15:45 and is %s.",
                this.rideName, previousTime, this.day, ReservationState.PENDING));
    }

    public void notifyRelocated(ParkLocalTime previousTime){
        notifyRelocated(previousTime.toString());
    }

    public void setConfirmed(){
        setState(ReservationState.CONFIRMED);
    }

    public void setRelocated(){
        setState(ReservationState.RELOCATED);
    }

    public void setCanceled(){
        setState(ReservationState.CANCELLED);
    }

    public boolean isConfirmed(){
        return this.state == ReservationState.CONFIRMED;
    }

    @Override
    public int compareTo(Reservation other) {
        int comp = Integer.compare(this.day, other.day);
        if (comp == 0) {
            comp = this.time.compareTo(other.time);
            if(comp == 0){
                return this.visitorId.compareTo(other.visitorId);
            }
        }
        return comp;
    }

    /* Note that 'status' is not part of the equals() method */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return day == that.day && visitorId.equals(that.visitorId) && time.equals(that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visitorId, day, time);
    }
}
