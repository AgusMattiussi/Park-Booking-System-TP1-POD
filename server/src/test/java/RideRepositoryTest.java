import ar.edu.itba.pod.server.Models.ParkLocalTime;
import ar.edu.itba.pod.server.Models.RideTime;
import ar.edu.itba.pod.server.ridePersistance.RideRepository;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class RideRepositoryTest {
    private static RideRepository repository;

    @BeforeClass
    public static void setUp() {
        repository = RideRepository.getInstance();
    }

    @Test
    public void testAddRide() {
        RideTime rideTime = new RideTime(new ParkLocalTime(LocalTime.of(9, 0)), new ParkLocalTime(LocalTime.of(18, 0)), 30);
        repository.addRide("ride1", rideTime, 30);
        assertTrue(repository.getRides().containsKey("ride1"));
    }

    @Test
    public void testAddSlotsPerDay() {

    }

    @Test
    public void testRidesExist() {
        repository.rideExists("ride1");
    }
}
