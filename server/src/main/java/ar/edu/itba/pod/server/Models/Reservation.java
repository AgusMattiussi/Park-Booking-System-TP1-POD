package ar.edu.itba.pod.server.Models;

import java.security.InvalidParameterException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.grpc.stub.StreamObserver;
import rideBooking.Models.ReservationState;
import rideBooking.NotifyServiceOuterClass;

public class Reservation implements Comparable<Reservation> {
    private final String rideName;

    private final UUID visitorId;
    private ReservationState state;
    private final int day;
    private final ParkLocalTime time;
    private boolean shouldNotify;
    private StreamObserver<NotifyServiceOuterClass.Notification> notificationObserver;
    private final Lock shouldNotifyLock;
    
    public Reservation(String rideName, UUID visitorId, ReservationState state, int day, ParkLocalTime time) {
        this.rideName = rideName;
        this.visitorId = visitorId;
        this.state = state;
        this.day = day;
        this.time = time;
        this.shouldNotify = false;
        this.notificationObserver = null;
        this.shouldNotifyLock = new ReentrantLock();
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
        shouldNotifyLock.lock();
        try {
            if(shouldNotify)
                throw new InvalidParameterException("Reservation is already registered for notifications");
            shouldNotify = true;

            this.notificationObserver = notificationObserver;
        } finally {
            shouldNotifyLock.unlock();
        }
    }

    /* Returns the observer to be closed by the caller method */
    public StreamObserver<NotifyServiceOuterClass.Notification> unregisterForNotifications() {
        StreamObserver<NotifyServiceOuterClass.Notification> toReturn;

        shouldNotifyLock.lock();
        try {
            if(!shouldNotify)
                throw new InvalidParameterException("Reservation is not registered for notifications");
            shouldNotify = false;

            toReturn = notificationObserver;
            this.notificationObserver = null;
        } finally {
            shouldNotifyLock.unlock();
        }

        return toReturn;
    }

    public boolean isRegisteredForNotifications() {
        return shouldNotify;
    }

    public void notifyVisitor(String message) {
        shouldNotifyLock.lock();
        try {
            if(!shouldNotify)
                throw new InvalidParameterException("Reservation is not registered for notifications");
            notificationObserver.onNext(NotifyServiceOuterClass.Notification.newBuilder().setMessage(message).build());
        } finally {
            shouldNotifyLock.unlock();
        }
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


    public void notifySlotsCapacityAdded(int capacity){
        notifyVisitor(String.format("%s announced slot capacity for the day %d: %d places.", rideName, day, capacity));
    }

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

    public boolean isCancelled(){
        return this.state == ReservationState.CANCELLED;
    }

    public boolean isRelocated(){
        return this.state == ReservationState.RELOCATED;
    }

    public boolean isPending(){
        return this.state == ReservationState.PENDING;
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
        return rideName.equals(that.rideName) && day == that.day && visitorId.equals(that.visitorId) && time.equals(that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visitorId, day, time);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Reservation{")
                .append("rideName='").append(rideName).append('\'')
                .append(", visitorId=").append(visitorId)
                .append(", state=").append(state)
                .append(", day=").append(day)
                .append(", time=").append(time)
                .append(", shouldNotify=").append(shouldNotify)
                .append(", notificationObserver=").append(notificationObserver)
                .append(", shouldNotifyLock=").append(shouldNotifyLock)
                .append('}')
                .toString();
    }

}
