package jsdp.utilities.probdist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SafeMultinomialDistTest {

    @Test
    public void testProbWithAllPositiveComponents() {
        SafeMultinomialDist dist = new SafeMultinomialDist(10, new double[]{0.5, 0.5});
        int[] x = {5, 5};
        double p = dist.prob(x);
        assertTrue("Probability should be > 0 for valid inputs", p > 0);
    }

    @Test
    public void testProbWithNegativeComponent() {
        SafeMultinomialDist dist = new SafeMultinomialDist(10, new double[]{0.5, 0.5});
        int[] x = {-1, 11};
        double p = dist.prob(x);
        assertEquals("Probability must be 0 when any component is negative", 0.0, p, 1e-12);
    }

    @Test
    public void testCdfWithAllPositiveComponents() {
        SafeMultinomialDist dist = new SafeMultinomialDist(10, new double[]{0.5, 0.5});
        int[] x = {5, 5};
        double c = dist.cdf(x);
        assertTrue("CDF should be > 0 for valid inputs", c > 0);
    }

    @Test
    public void testCdfWithNegativeComponent() {
        SafeMultinomialDist dist = new SafeMultinomialDist(10, new double[]{0.5, 0.5});
        int[] x = {-3, 13};
        double c = dist.cdf(x);
        assertEquals("CDF must be 0 when any component is negative", 0.0, c, 1e-12);
    }
}
