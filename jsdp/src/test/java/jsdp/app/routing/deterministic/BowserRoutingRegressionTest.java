package jsdp.app.routing.deterministic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BowserRoutingRegressionTest {

    @Test(timeout = 30000)
    public void testTinyInstanceRunsAndProducesStats() {
        BowserRouting bowserRouting = new BowserRouting(BowserRouting.InstanceType.TINY);

        bowserRouting.runInstance();

        String headers = BowserRouting.getHeadersString();
        String stats = bowserRouting.toString();

        assertNotNull(headers);
        assertTrue(headers.contains("ETC"));
        assertNotNull(stats);
        assertFalse(stats.trim().isEmpty());

        String[] parts = stats.split(",");
        assertTrue("Expected 6 comma-separated statistics", parts.length == 6);

        double expectedTotalCost = Double.parseDouble(parts[0].trim());
        long generatedStates = Long.parseLong(parts[4].trim());
        long reusedStates = Long.parseLong(parts[5].trim());

        assertTrue("Expected total cost should be non-negative", expectedTotalCost >= 0.0);
        assertTrue("Generated states should be positive", generatedStates > 0L);
        assertTrue("Reused states should be non-negative", reusedStates >= 0L);
    }
}
