package jsdp.sdp.impl.multivariate;

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
import jsdp.utilities.probdist.MultiINIDistribution;
import umontreal.ssj.probdist.BinomialDist;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdistmulti.DiscreteDistributionIntMulti;

public class BackwardRecursionImplTest {

    private DiscreteDistributionIntMulti[] demand;
    private ImmediateValueFunction<State, Action, Double> immediateValueFunction;
    private RandomOutcomeFunction<State, Action, double[]> randomOutcomeFunction;
    private Function<State, ArrayList<Action>> buildActionList;
    private Function<State, Action> idempotentAction;

    @Before
    public void setUp() {
        StateImpl.setStateBoundaries(new double[] { 1.0 }, new double[] { -1.0 }, new double[] { 2.0 });

        demand = new DiscreteDistributionIntMulti[] {
                new MultiINIDistribution(new Distribution[] { new BinomialDist(1, 1.0) }, new double[] { 0.0 },
                        new double[] { 1.0 }),
                new MultiINIDistribution(new Distribution[] { new BinomialDist(1, 1.0) }, new double[] { 0.0 },
                        new double[] { 1.0 })
        };

        immediateValueFunction = (initialState, action, finalState) -> {
            ActionImpl a = (ActionImpl) action;
            StateImpl fs = (StateImpl) finalState;
            return a.getAction()[0] + Math.abs(fs.getInitialState()[0]);
        };

        randomOutcomeFunction = (initialState, action, finalState) -> {
            StateImpl is = (StateImpl) initialState;
            ActionImpl a = (ActionImpl) action;
            StateImpl fs = (StateImpl) finalState;
            return new double[] { is.getInitialState()[0] + a.getAction()[0] - fs.getInitialState()[0] };
        };

        buildActionList = state -> {
            ArrayList<Action> actions = new ArrayList<>();
            actions.add(new ActionImpl(state, new int[] { 0 }));
            actions.add(new ActionImpl(state, new int[] { 1 }));
            return actions;
        };

        idempotentAction = state -> new ActionImpl(state, new int[] { 0 });
    }

    @Test
    public void testRunBackwardRecursionAndRetrievePolicy() {
        BackwardRecursionImpl recursion = new BackwardRecursionImpl(
                OptimisationDirection.MIN,
                demand,
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

        StateDescriptorImpl initial = new StateDescriptorImpl(0, new double[] { 0.0 });
        double expectedCost = recursion.getExpectedCost(initial);
        ActionImpl action = recursion.getOptimalAction(initial);

        assertTrue(Double.isFinite(expectedCost));
        assertNotNull(action);
        assertTrue(action.getAction()[0] >= 0.0);
    }

    @Test
    public void testTransitionProbabilityAndGetExpectedCostOverload() {
        BackwardRecursionImpl recursion = new BackwardRecursionImpl(
                OptimisationDirection.MIN,
                demand,
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
        double expectedCost = recursion.getExpectedCost(new double[] { 0.0 });
        assertTrue(Double.isFinite(expectedCost));

        StateSpaceImpl[] stateSpace = (StateSpaceImpl[]) recursion.getStateSpace();
        State initialState = stateSpace[0].getState(new StateDescriptorImpl(0, new int[] { 0 }));
        Action action = new ActionImpl(initialState, new int[] { 0 });
        State finalState = stateSpace[1].getState(new StateDescriptorImpl(1, new int[] { -1 }));

        double probability = recursion.getTransitionProbability().getTransitionProbability(initialState, action, finalState);
        assertTrue(probability >= 0.0);
        assertTrue(recursion.getTransitionProbability().getFinalStates(initialState, action).size() >= 1);
    }

    @Test
    public void testSizedConstructorRuns() {
        BackwardRecursionImpl recursion = new BackwardRecursionImpl(
                OptimisationDirection.MIN,
                demand,
                immediateValueFunction,
                randomOutcomeFunction,
                buildActionList,
                idempotentAction,
                1.0,
                SamplingScheme.NONE,
                10,
                1.0,
                32,
                0.8f,
                HashType.HASHTABLE);

        recursion.runBackwardRecursion();

        StateDescriptorImpl initial = new StateDescriptorImpl(0, new double[] { 0.0 });
        assertTrue(Double.isFinite(recursion.getExpectedCost(initial)));
        assertNotNull(recursion.getOptimalAction(initial));
    }
}
