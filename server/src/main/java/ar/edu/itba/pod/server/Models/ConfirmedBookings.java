package ar.edu.itba.pod.server.Models;

public class ConfirmedBookings {
    private final String rideName;
    private final String visitorID;
    private final String slot;

    public ConfirmedBookings(String rideName, String visitorID, String slot) {
        this.rideName = rideName;
        this.visitorID = visitorID;
        this.slot = slot;
    }

    public String getRideName() {
        return rideName;
    }

    public String getVisitorID() {
        return visitorID;
    }

    public String getSlot() {
        return slot;
    }
}
