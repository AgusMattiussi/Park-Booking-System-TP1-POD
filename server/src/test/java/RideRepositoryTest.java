import ar.edu.itba.pod.server.Models.ParkLocalTime;
import ar.edu.itba.pod.server.Models.ParkPass;
import ar.edu.itba.pod.server.Models.Ride;
import ar.edu.itba.pod.server.Models.RideTime;
import ar.edu.itba.pod.server.passPersistance.ParkPassRepository;
import ar.edu.itba.pod.server.ridePersistance.RideRepository;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import rideBooking.Models;

import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    public void testAddParkPass() {
//        UUID id = UUID.fromString("ca286ef0-162a-42fd-b9ea-60166ff0a593");
//        repository.addParkPass(id, Models.PassTypeEnum.HALFDAY, 100);
//        ParkPassRepository parkPassRepository = ParkPassRepository.getInstance();
//        assertTrue(parkPassRepository.containsKey(id));
    }

    @Test
    public void testAddSlotsPerDay() {

    }

    @Test
    public void testRidesExist() {
        repository.rideExists("ride1");
    }
}
