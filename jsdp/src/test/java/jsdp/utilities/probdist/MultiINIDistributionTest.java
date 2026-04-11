package jsdp.utilities.probdist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import jsdp.sdp.impl.multivariate.StateImpl;
import umontreal.ssj.probdist.BinomialDist;
import umontreal.ssj.probdist.Distribution;

public class MultiINIDistributionTest {

    @Before
    public void setUp() {
        StateImpl.setStateBoundaries(new double[]{1.0}, new double[]{0.0}, new double[]{2.0});
    }

    private MultiINIDistribution makeDist() {
        Distribution[] dists = {new BinomialDist(2, 0.5)};
        MultiINIDistribution multi = new MultiINIDistribution(dists, new double[]{0.0}, new double[]{2.0});
        multi.discretizeDistributions();
        return multi;
    }

    @Test
    public void testProbAfterDiscretize() {
        MultiINIDistribution multi = makeDist();
        int[] x = {1};
        double p = multi.prob(x);
        assertTrue("Probability of [1] under Binomial(2,0.5) should be > 0", p > 0);
    }

    @Test
    public void testGetMeanAfterDiscretize() {
        MultiINIDistribution multi = makeDist();
        double[] mean = multi.getMean();
        assertNotNull(mean);
        assertEquals(1, mean.length);
        assertEquals(1.0, mean[0], 0.1); // Binomial(2, 0.5) mean = 1.0
    }

    @Test
    public void testGetCovarianceAfterDiscretize() {
        MultiINIDistribution multi = makeDist();
        double[][] cov = multi.getCovariance();
        assertNotNull(cov);
        assertEquals(1, cov.length);
        assertTrue("Variance should be > 0", cov[0][0] > 0);
    }

    @Test
    public void testGetCorrelationAfterDiscretize() {
        MultiINIDistribution multi = makeDist();
        double[][] corr = multi.getCorrelation();
        assertNotNull(corr);
        assertEquals(1, corr.length);
        assertEquals("Diagonal correlation should be 1.0", 1.0, corr[0][0], 1e-9);
    }
}
