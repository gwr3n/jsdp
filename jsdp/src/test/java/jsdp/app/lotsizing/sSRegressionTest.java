package jsdp.app.lotsizing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import jsdp.sdp.State;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.PoissonDist;

public class sSRegressionTest {

    private static final double FIXED_ORDERING_COST = 10.0;
    private static final double PROPORTIONAL_ORDERING_COST = 0.0;
    private static final double HOLDING_COST = 1.0;
    private static final double PENALTY_COST = 4.0;

    private Distribution[] distributions;
    private double minDemand;
    private double maxDemand;

    @Before
    public void setUp() {
        sS_State.setStateBoundaries(1, -20, 40);
        this.distributions = new Distribution[] {
                new PoissonDist(4),
                new PoissonDist(6),
                new PoissonDist(5)
        };
        this.minDemand = 0;
        this.maxDemand = sS_State.getMaxInventory() - sS_State.getMinInventory();
    }

    @Test
    public void testForwardAndBackwardRecursionAgreeOnExpectedCost() {
        double initialInventory = 0.0;

        sS_ForwardRecursion forwardRecursion = new sS_ForwardRecursion(
                this.distributions,
                this.minDemand,
                this.maxDemand,
                FIXED_ORDERING_COST,
                PROPORTIONAL_ORDERING_COST,
                HOLDING_COST,
                PENALTY_COST);

        sS_StateDescriptor initialStateDescriptor =
                new sS_StateDescriptor(0, sS_State.inventoryToState(initialInventory));
        State initialState = ((sS_StateSpace) forwardRecursion.getStateSpace(0)).getState(initialStateDescriptor);
        double forwardCost = forwardRecursion.runForwardRecursion(initialState);

        sS_BackwardRecursion backwardRecursion = new sS_BackwardRecursion(
                this.distributions,
                this.minDemand,
                this.maxDemand,
                FIXED_ORDERING_COST,
                PROPORTIONAL_ORDERING_COST,
                HOLDING_COST,
                PENALTY_COST,
                sS_StateSpaceSampleIterator.SamplingScheme.NONE,
                100);
        backwardRecursion.runBackwardRecursion();

        double backwardCost = backwardRecursion.getExpectedCost(initialInventory);

        assertEquals(backwardCost, forwardCost, 1e-6);
        assertNotNull(backwardRecursion.getOptimalAction(initialStateDescriptor));
        assertNotNull(forwardRecursion.getOptimalAction(initialStateDescriptor));
    }

    @Test
    public void testBackwardRecursionPolicyShapeIsValid() {
        sS_BackwardRecursion recursion = new sS_BackwardRecursion(
                this.distributions,
                this.minDemand,
                this.maxDemand,
                FIXED_ORDERING_COST,
                PROPORTIONAL_ORDERING_COST,
                HOLDING_COST,
                PENALTY_COST,
                sS_StateSpaceSampleIterator.SamplingScheme.NONE,
                100);
        recursion.runBackwardRecursion();

        double[][] policy = recursion.getOptimalPolicy(0.0);
        assertEquals(this.distributions.length, policy[0].length);
        assertEquals(this.distributions.length, policy[1].length);

        for (int i = 0; i < this.distributions.length; i++) {
            assertTrue("Expected s <= S at period index " + i, policy[0][i] <= policy[1][i]);
        }
    }
}
