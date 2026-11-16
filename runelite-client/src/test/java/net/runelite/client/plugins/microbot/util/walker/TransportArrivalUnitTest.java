package net.runelite.client.plugins.microbot.util.walker;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransportArrivalUnitTest {
    @Test
    public void stairsRequireTheDestinationPlane() {
        WorldPoint origin = new WorldPoint(3200, 3200, 0);
        WorldPoint destination = new WorldPoint(3200, 3200, 1);
        assertFalse(Rs2WalkerTransports.hasObservedObjectLanding(origin, new WorldPoint(3201, 3200, 0), origin, destination, 2));
        assertTrue(Rs2WalkerTransports.hasObservedObjectLanding(origin, destination, origin, destination, 2));
    }

    @Test
    public void portalApproachDoesNotCountAsLanding() {
        WorldPoint origin = new WorldPoint(3200, 3200, 0);
        WorldPoint start = new WorldPoint(3197, 3200, 0);
        WorldPoint destination = new WorldPoint(3204, 3200, 0);
        assertFalse(Rs2WalkerTransports.hasObservedObjectLanding(start, new WorldPoint(3201, 3200, 0), origin, destination, 3));
        assertFalse(Rs2WalkerTransports.hasObservedObjectLanding(destination, destination, origin, destination, 3));
        assertTrue(Rs2WalkerTransports.hasObservedObjectLanding(start, destination, origin, destination, 3));
    }
}
