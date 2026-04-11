package jsdp.sdp.impl.multivariate;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.RandomOutcomeFunction;
import jsdp.sdp.State;
import jsdp.utilities.probdist.MultiINIDistribution;
import umontreal.ssj.probdist.BinomialDist;
import umontreal.ssj.probdist.Distribution;

public class TransitionProbabilityImplTest {

    private Function<State, ArrayList<Action>> buildActionList;
    private Function<State, Action> idempotentAction;
    private RandomOutcomeFunction<State, Action, double[]> randomOutcomeFunction;
    private StateSpaceImpl[] stateSpace;

    @Before
    public void setUp() {
        StateImpl.setStateBoundaries(new double[] { 1.0 }, new double[] { 0.0 }, new double[] { 1.0 });

        buildActionList = state -> {
            ArrayList<Action> actions = new ArrayList<>();
            actions.add(new ActionImpl(state, new int[] { 0 }));
            actions.add(new ActionImpl(state, new int[] { 1 }));
            return actions;
        };
        idempotentAction = state -> new ActionImpl(state, new int[] { 0 });
        randomOutcomeFunction = (initialState, action, finalState) -> {
            StateImpl initial = (StateImpl) initialState;
            ActionImpl chosenAction = (ActionImpl) action;
            StateImpl next = (StateImpl) finalState;
            return new double[] { initial.getInitialState()[0] + chosenAction.getAction()[0] - next.getInitialState()[0] };
        };

        stateSpace = new StateSpaceImpl[] {
                new StateSpaceImpl(0, buildActionList, idempotentAction, HashType.HASHTABLE),
                new StateSpaceImpl(1, buildActionList, idempotentAction, HashType.HASHTABLE)
        };
    }

    @Test
    public void testMultiIniConstructorSupportsProbabilityQueries() {
        MultiINIDistribution[] demand = new MultiINIDistribution[] {
                new MultiINIDistribution(new Distribution[] { new BinomialDist(1, 1.0) }, new double[] { 0.0 }, new double[] { 1.0 })
        };
        demand[0].discretizeDistributions();
        TransitionProbabilityImpl transition = new TransitionProbabilityImpl(demand, randomOutcomeFunction, stateSpace);

        State initial = stateSpace[0].getState(new StateDescriptorImpl(0, new int[] { 0 }));
        Action action = new ActionImpl(initial, new int[] { 1 });
        State finalState = stateSpace[1].getState(new StateDescriptorImpl(1, new int[] { 0 }));

        assertTrue(transition.getTransitionProbability(initial, action, finalState) > 0.0);
        assertTrue(transition.getFinalStates(initial, action).size() >= 1);
    }

    @Test(expected = NullPointerException.class)
    public void testGenerateFinalStatesThrowsWhenNotImplemented() {
        MultiINIDistribution[] demand = new MultiINIDistribution[] {
                new MultiINIDistribution(new Distribution[] { new BinomialDist(1, 1.0) }, new double[] { 0.0 }, new double[] { 1.0 })
        };
        TransitionProbabilityImpl transition = new TransitionProbabilityImpl(demand, randomOutcomeFunction, stateSpace);

        State initial = stateSpace[0].getState(new StateDescriptorImpl(0, new int[] { 0 }));
        Action action = new ActionImpl(initial, new int[] { 0 });
        transition.generateFinalStates(initial, action);
    }
}