package jsdp.utilities.probdist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import umontreal.ssj.probdist.DiscreteDistribution;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.probdist.PoissonDist;

public class DiscreteDistributionFactoryTest {

    /** Minimal Distribution implementation that is none of the three recognised subtypes. */
    private static class UnknownDistribution implements Distribution {
        @Override public double cdf(double x) { return 0; }
        @Override public double barF(double x) { return 1; }
        @Override public double inverseF(double u) { return 0; }
        @Override public double getMean() { return 0; }
        @Override public double getVariance() { return 0; }
        @Override public double getStandardDeviation() { return 0; }
        @Override public double[] getParams() { return new double[0]; }
        @Override public String toString() { return "unknown"; }
    }

    @Test
    public void testConstructorIsInstantiable() {
        assertNotNull(new DiscreteDistributionFactory());
    }

    @Test
    public void testGetTruncatedDiscreteDistributionFromDiscrete() {
        // Test truncating a Poisson distribution (DiscreteDistributionInt)
        PoissonDist poisson = new PoissonDist(5.0);
        DiscreteDistribution truncated = DiscreteDistributionFactory.getTruncatedDiscreteDistribution(poisson, 2, 8,
                1.0);

        assertNotNull(truncated);
        assertEquals(7, truncated.getN()); // Support [2, 8] inclusive = 7 values

        // Sum of probabilities should be close to 1.0 due to normalization
        double sum = 0;
        for (int i = 0; i < truncated.getN(); i++) {
            sum += truncated.prob(i);
        }
        assertEquals(1.0, sum, 1e-4);

        assertEquals(2.0, truncated.getValue(0), 1e-6);
        assertEquals(8.0, truncated.getValue(6), 1e-6);
    }

    @Test
    public void testGetTruncatedDiscreteDistributionFromContinuous() {
        // Test discretizing a Normal distribution (ContinuousDistribution)
        NormalDist normal = new NormalDist(10.0, 2.0);
        DiscreteDistribution discretized = DiscreteDistributionFactory.getTruncatedDiscreteDistribution(normal, 5, 15,
                1.0);

        assertNotNull(discretized);
        assertEquals(11, discretized.getN()); // Support [5, 15] inclusive = 11 values

        // Sum of probabilities should be close to 1.0 due to normalization
        double sum = 0;
        for (int i = 0; i < discretized.getN(); i++) {
            sum += discretized.prob(i);
        }
        assertEquals(1.0, sum, 1e-4);

        assertEquals(5.0, discretized.getValue(0), 1e-6);
        assertEquals(15.0, discretized.getValue(10), 1e-6);
    }

    @Test(expected = NullPointerException.class)
    public void testDiscreteDistributionIntWithWrongStepSizeThrows() {
        PoissonDist poisson = new PoissonDist(3.0);
        DiscreteDistributionFactory.getTruncatedDiscreteDistribution(poisson, 0, 6, 2.0);
    }

    @Test
    public void testDiscreteDistributionPassthrough() {
        // A DiscreteDistribution (over doubles) should be returned as-is with stepSize=1
        double[] vals = {1.0, 2.0, 3.0};
        double[] probs = {1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0};
        DiscreteDistribution dd = new DiscreteDistribution(vals, probs, 3);
        DiscreteDistribution result = DiscreteDistributionFactory.getTruncatedDiscreteDistribution(dd, 1.0, 3.0, 1.0);
        assertNotNull(result);
        assertEquals(3, result.getN());
    }

    @Test(expected = NullPointerException.class)
    public void testDiscreteDistributionWithWrongStepSizeThrows() {
        double[] vals = {1.0, 2.0, 3.0};
        double[] probs = {1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0};
        DiscreteDistribution dd = new DiscreteDistribution(vals, probs, 3);
        DiscreteDistributionFactory.getTruncatedDiscreteDistribution(dd, 1.0, 3.0, 2.0);
    }

    @Test(expected = NullPointerException.class)
    public void testUnknownDistributionThrows() {
        DiscreteDistributionFactory.getTruncatedDiscreteDistribution(new UnknownDistribution(), 0, 5, 1.0);
    }
}
