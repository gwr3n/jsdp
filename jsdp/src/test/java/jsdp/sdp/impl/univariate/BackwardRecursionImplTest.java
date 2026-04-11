package jsdp.sdp.impl.univariate;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.ImmediateValueFunction;
import jsdp.sdp.RandomOutcomeFunction;
import jsdp.sdp.State;
import jsdp.sdp.Recursion.OptimisationDirection;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.PoissonDist;

public class BackwardRecursionImplTest {

    private Distribution[] demand;
    private double[] supportLB;
    private double[] supportUB;
    private ImmediateValueFunction<State, Action, Double> immediateValueFunction;
    private RandomOutcomeFunction<State, Action, Double> randomOutcomeFunction;
    private Function<State, ArrayList<Action>> buildActionList;
    private Function<State, Action> idempotentAction;

    @Before
    public void setUp() {
        StateImpl.setStateBoundaries(1.0, -1.0, 3.0);

        demand = new Distribution[] {
                new PoissonDist(1.0),
                new PoissonDist(1.0)
        };
        supportLB = new double[] { 0.0, 0.0 };
        supportUB = new double[] { 2.0, 2.0 };

        immediateValueFunction = (initialState, action, finalState) -> {
            ActionImpl a = (ActionImpl) action;
            StateImpl fs = (StateImpl) finalState;
            return a.getAction() + Math.abs(fs.getInitialState());
        };

        randomOutcomeFunction = (initialState, action, finalState) -> {
            StateImpl is = (StateImpl) initialState;
            ActionImpl a = (ActionImpl) action;
            StateImpl fs = (StateImpl) finalState;
            return is.getInitialState() + a.getAction() - fs.getInitialState();
        };

        buildActionList = state -> {
            ArrayList<Action> actions = new ArrayList<>();
            actions.add(new ActionImpl(state, 0));
            actions.add(new ActionImpl(state, 1));
            return actions;
        };

        idempotentAction = state -> new ActionImpl(state, 0);
    }

    @Test
    public void testRunBackwardRecursionAndReadPolicy() {
        BackwardRecursionImpl recursion = new BackwardRecursionImpl(
                OptimisationDirection.MIN,
                demand,
                supportLB,
                supportUB,
                immediateValueFunction,
                randomOutcomeFunction,
                buildActionList,
                idempotentAction,
                1.0,
                SamplingScheme.NONE,
                10,
                1.0,
                HashType.HASHTABLE);

        recursion.runBackwardRecursion();

        StateDescriptorImpl initial = new StateDescriptorImpl(0, 0.0);
        double expectedCost = recursion.getExpectedCost(initial);
        ActionImpl optimalAction = recursion.getOptimalAction(initial);

        assertTrue(Double.isFinite(expectedCost));
        assertNotNull(optimalAction);
    }

    @Test
    public void testStateActionDependentTransitionProbabilityPath() {
        Distribution[][][] distributions = new Distribution[1][2][1];
        double[][][] lb = new double[1][2][1];
        double[][][] ub = new double[1][2][1];
        for (int a = 0; a < 2; a++) {
            distributions[0][a][0] = new PoissonDist(0.0);
            lb[0][a][0] = 0.0;
            ub[0][a][0] = 0.0;
        }

        StateSpaceImpl[] stateSpace = new StateSpaceImpl[] {
                new StateSpaceImpl(0, buildActionList, idempotentAction, HashType.HASHTABLE),
                new StateSpaceImpl(1, buildActionList, idempotentAction, HashType.HASHTABLE)
        };

        TransitionProbabilityImpl transition = new TransitionProbabilityImpl(
                distributions,
                lb,
                ub,
                (initialState, action, finalState) -> 0.0,
                stateSpace,
                1.0);

        State initial = stateSpace[0].getState(new StateDescriptorImpl(0, 0));
        Action action = new ActionImpl(initial, 1);
        State finalState = stateSpace[1].getState(new StateDescriptorImpl(1, 0));

        double probability = transition.getTransitionProbability(initial, action, finalState);
        assertTrue(probability >= 0.0);
        assertTrue(transition.getFinalStates(initial, action).size() >= 1);
    }

    @Test(expected = NullPointerException.class)
    public void testUnimplementedStateDependentConstructorThrows() {
        Distribution[][] demand2D = new Distribution[][] { { new PoissonDist(1.0) } };
        double[][] lb2D = new double[][] { { 0.0 } };
        double[][] ub2D = new double[][] { { 1.0 } };

        new BackwardRecursionImpl(
                OptimisationDirection.MIN,
                demand2D,
                lb2D,
                ub2D,
                immediateValueFunction,
                randomOutcomeFunction,
                buildActionList,
                idempotentAction,
                1.0,
                SamplingScheme.NONE,
                5,
                1.0,
                HashType.HASHTABLE);
    }

    @Test
    public void testSizedConstructorCovershExpectedCostDoubleOverload() {
        BackwardRecursionImpl recursion = new BackwardRecursionImpl(
                OptimisationDirection.MIN,
                demand,
                supportLB,
                supportUB,
                immediateValueFunction,
                randomOutcomeFunction,
                buildActionList,
                idempotentAction,
                1.0,
                SamplingScheme.NONE,
                10,
                1.0,
                64, 0.8f,
                HashType.HASHTABLE);

        recursion.runBackwardRecursion();

        // Exercises the getExpectedCost(double) overload (not the StateDescriptorImpl one)
        double cost = recursion.getExpectedCost(0.0);
        assertTrue(Double.isFinite(cost));
        assertNotNull(recursion.getOptimalAction(new StateDescriptorImpl(0, 0.0)));
    }

    @Test
    public void testStateActionDependentConstructorRuns() {
        // Override boundaries: min=0, max=2 so intState in {0,1,2} (no negative array indices)
        StateImpl.setStateBoundaries(1.0, 0.0, 2.0);

        int horizons = 2;
        int numActions = 2; // intAction in {0, 1}
        int numStates = 3;  // intState in {0, 1, 2}

        Distribution[][][] d3 = new Distribution[horizons][numActions][numStates];
        double[][][] lb3 = new double[horizons][numActions][numStates];
        double[][][] ub3 = new double[horizons][numActions][numStates];
        for (int t = 0; t < horizons; t++) {
            for (int a = 0; a < numActions; a++) {
                for (int s = 0; s < numStates; s++) {
                    d3[t][a][s] = new PoissonDist(0.0); // demand always 0
                    lb3[t][a][s] = 0.0;
                    ub3[t][a][s] = 0.0;
                }
            }
        }

        BackwardRecursionImpl recursion = new BackwardRecursionImpl(
                OptimisationDirection.MIN,
                d3, lb3, ub3,
                immediateValueFunction,
                randomOutcomeFunction,
                buildActionList,
                idempotentAction,
                1.0,
                SamplingScheme.NONE,
                10,
                1.0,
                HashType.HASHTABLE);

        recursion.runBackwardRecursion();
        double cost = recursion.getExpectedCost(new StateDescriptorImpl(0, 0.0));
        assertTrue(Double.isFinite(cost));
    }
}
