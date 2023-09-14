import ar.edu.itba.pod.server.Models.ParkLocalTime;
import ar.edu.itba.pod.server.Models.Ride;
import ar.edu.itba.pod.server.Models.RideTime;
import ar.edu.itba.pod.server.exceptions.SlotCapacityException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class RideTest {
    private static Ride ride;
    
    @BeforeClass
    public static void setUp() {
        ride = new Ride("ride1", new RideTime(new ParkLocalTime(LocalTime.of(9, 0)), new ParkLocalTime(LocalTime.of(18, 0)), 30));
        ride.setSlotCapacityForDay(1, 50);
    }

    @Test
    public void testGetName() {
        assertEquals("ride1", ride.getName());
    }

    @Test
    public void testGetRideTime() {
        assertEquals(new RideTime(new ParkLocalTime(LocalTime.of(9, 0)), new ParkLocalTime(LocalTime.of(18, 0)), 30), ride.getRideTime());
    }

    @Test
    public void testGetTimeSlotDuration() {
        assertEquals(30, ride.getTimeSlotDuration().toMinutes());
    }

    @Test
    public void testGetSlotCapacityByDay() {
        assertEquals(1, ride.getSlotCapacityByDay().size());
    }

    @Test
    public void testGetSlotCapacityForDay() {
        assertEquals(50, ride.getSlotCapacityForDay(1));
    }

    @Test
    public void testGetSlotsLeft() {
        assertEquals(50, ride.getSlotsLeft(1, new ParkLocalTime(LocalTime.of(10, 0))).get());
    }


    @Test
    public void testDecrementCapacity() {
        ride.decrementCapacity(1, new ParkLocalTime(LocalTime.of(10, 0)));
        assertEquals(49, ride.getSlotsLeft(1, new ParkLocalTime(LocalTime.of(10, 0))).get());
    }

    @Test
    public void testIncrementCapacity(){
        ride.incrementCapacity(1, new ParkLocalTime(LocalTime.of(10, 0)));
        assertEquals(50, ride.getSlotsLeft(1, new ParkLocalTime(LocalTime.of(10, 0))).get());
    }

    @Test
    public void testIsSlotCapacitySet() {
        assertTrue(ride.isSlotCapacitySet(1));
    }

    @Test(expected = SlotCapacityException.class)
    public void testSetSlotCapacityForDay() {
        ride.setSlotCapacityForDay(1, 50);
    }
    
    @Test
    public void testisTimeSlotValid() {
        assertTrue(ride.isTimeSlotValid(new ParkLocalTime(LocalTime.of(10, 0))));
    }
}
