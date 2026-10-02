package net.runelite.client.plugins.microbot.util.walker.recovery;

import java.util.Objects;
import java.util.function.BiPredicate;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

public final class CantReachTargetRecovery
{
    private static final ThreadLocal<Integer> RECOVERY_DEPTH = ThreadLocal.withInitial(() -> 0);

    private CantReachTargetRecovery() {}

    public static boolean shouldStart(boolean detectionEnabled, boolean cantReachTarget)
    {
        return detectionEnabled && cantReachTarget && RECOVERY_DEPTH.get() == 0;
    }

    public static boolean retryExhausted(int retries, int retryLimit)
    {
        return retries >= retryLimit;
    }

    public static boolean walkTo(WorldPoint target, int distance)
    {
        return walkTo(target, distance, Rs2Walker::walkTo);
    }

    static boolean walkTo(WorldPoint target, int distance, BiPredicate<WorldPoint, Integer> walker)
    {
        Objects.requireNonNull(walker, "recovery walker");
        int previousDepth = RECOVERY_DEPTH.get();
        RECOVERY_DEPTH.set(previousDepth + 1);
        try
        {
            return walker.test(target, distance);
        }
        finally
        {
            if (previousDepth == 0) RECOVERY_DEPTH.remove();
            else RECOVERY_DEPTH.set(previousDepth);
        }
    }
}
