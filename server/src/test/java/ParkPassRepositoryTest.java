import ar.edu.itba.pod.server.passPersistance.ParkPassRepository;
import org.junit.BeforeClass;
import org.junit.Test;
import rideBooking.Models;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


public class ParkPassRepositoryTest {

    private static ParkPassRepository repository;
    private static UUID id;

    @BeforeClass
    public static void setUp() {
        repository = ParkPassRepository.getInstance();
        id = UUID.fromString("ca286ef0-162a-42fd-b9ea-60166ff0a593");
    }

    @Test
    public void testAddParkPass(){
        repository.addParkPass(id, Models.PassTypeEnum.UNLIMITED, 100);
        assertTrue(repository.getParkPasses().containsKey(id));
    }

    @Test
    public void testCheckVisitorPass(){
        assertTrue(repository.checkVisitorPass(UUID.fromString("ca286ef0-162a-42fd-b9ea-60166ff0a593"), 10));
    }

    @Test
    public void testGetVisitorParkType(){
//        assertEquals(Models.PassTypeEnum.UNLIMITED, repository.getVisitorParkType(id, 100));
    }

    @Test
    public void testHasValidPass(){
        assertFalse(repository.hasValidPass(id, 100));
    }
}
