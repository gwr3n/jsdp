package jsdp.sdp.impl.univariate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.State;
import jsdp.sdp.StateSpaceIterator;

public class StateSpaceImplTest {

    private Function<State, ArrayList<Action>> buildActionList;
    private Function<State, Action> idempotentAction;

    @Before
    public void setUp() {
        StateImpl.setStateBoundaries(1.0, -2.0, 2.0);
        buildActionList = state -> {
            ArrayList<Action> actions = new ArrayList<>();
            actions.add(new ActionImpl(state, 0));
            actions.add(new ActionImpl(state, 1));
            return actions;
        };
        idempotentAction = state -> new ActionImpl(state, 0);
    }

    @Test
    public void testGetStateExistsAndDeterministicIterator() {
        StateSpaceImpl stateSpace = new StateSpaceImpl(0, buildActionList, idempotentAction, HashType.HASHTABLE);

        StateDescriptorImpl descriptor = new StateDescriptorImpl(0, 0);
        assertFalse(stateSpace.exists(descriptor));

        State state = stateSpace.getState(descriptor);
        assertTrue(stateSpace.exists(descriptor));
        assertEquals(state, stateSpace.getState(descriptor));

        StateSpaceIterator iterator = (StateSpaceIterator) stateSpace.iterator();
        int seen = 0;
        while (iterator.hasNext()) {
            iterator.next();
            seen++;
        }
        assertEquals(StateImpl.getMaxIntState() - StateImpl.getMinIntState() + 1, seen);
    }

    @Test
    public void testSamplingIteratorAndSampleGeneration() {
        StateSpaceImpl stateSpace = new StateSpaceImpl(
                1,
                buildActionList,
                idempotentAction,
                HashType.HASHTABLE,
                SamplingScheme.STRATIFIED_SAMPLING,
                3,
                1.0);

        assertTrue(stateSpace.iterator() instanceof StateSpaceSampleIteratorImpl);

        StateSpaceSampleIteratorImpl iterator = new StateSpaceSampleIteratorImpl(
                stateSpace,
                SamplingScheme.SIMPLE_RANDOM_SAMPLING,
                3,
                1.0);
        int[] sample = iterator.getNextSample(3);
        assertTrue(sample.length >= 1);

        int[] jensen = iterator.getNextJensensSample(3);
        assertEquals(3, jensen.length);
    }

    @Test
    public void testSizedConstructorUsesDeterministicIterator() {
        StateSpaceImpl stateSpace = new StateSpaceImpl(
                1,
                buildActionList,
                idempotentAction,
                HashType.HASHTABLE,
                32,
                0.75f);

        StateDescriptorImpl descriptor = new StateDescriptorImpl(1, 1);
        assertFalse(stateSpace.exists(descriptor));
        assertTrue(stateSpace.getState(descriptor) instanceof StateImpl);

        StateSpaceIterator iterator = (StateSpaceIterator) stateSpace.iterator();
        int seen = 0;
        while (iterator.hasNext()) {
            assertTrue(iterator.next() instanceof StateImpl);
            seen++;
        }

        assertEquals(StateImpl.getMaxIntState() - StateImpl.getMinIntState() + 1, seen);
        assertFalse(iterator.hasNext());
        assertNull(iterator.next());
    }

    @Test
    public void testStratifiedSamplingAndIteratorExhaustion() {
        StateImpl.setStateBoundaries(1.0, 0.0, 5.0);

        StateSpaceImpl stateSpace = new StateSpaceImpl(
                1,
                buildActionList,
                idempotentAction,
                HashType.HASHTABLE,
                SamplingScheme.STRATIFIED_SAMPLING,
                3,
                1.0);

        assertTrue(stateSpace.iterator() instanceof StateSpaceSampleIteratorImpl);

        StateSpaceSampleIteratorImpl stratifiedIterator = new StateSpaceSampleIteratorImpl(
                stateSpace,
                SamplingScheme.STRATIFIED_SAMPLING,
                3,
                1.0);
        int[] stratified = stratifiedIterator.getNextStratifiedSample(3);
        assertTrue(stratified.length >= 1);
        assertTrue(stratified.length <= 3);

        StateSpaceSampleIteratorImpl sampledIterator = new StateSpaceSampleIteratorImpl(
                stateSpace,
                SamplingScheme.JENSENS_PARTITIONING,
                3,
                1.0);
        while (sampledIterator.hasNext()) {
            assertTrue(sampledIterator.next() instanceof StateImpl);
        }

        assertFalse(sampledIterator.hasNext());
        assertNull(sampledIterator.next());
    }

    @Test(expected = NullPointerException.class)
    public void testSampleGenerationRejectsOversizedSamples() {
        StateSpaceImpl stateSpace = new StateSpaceImpl(1, buildActionList, idempotentAction, HashType.HASHTABLE);
        StateSpaceSampleIteratorImpl iterator = new StateSpaceSampleIteratorImpl(
                stateSpace,
                SamplingScheme.SIMPLE_RANDOM_SAMPLING,
                3,
                1.0);
        int stateSpaceSize = StateImpl.getMaxIntState() - StateImpl.getMinIntState() + 1;
        iterator.getNextSample(stateSpaceSize + 1);
    }

    @Test
    public void testSixArgSizedConstructor() {
        StateSpaceImpl stateSpace = new StateSpaceImpl(0, buildActionList, idempotentAction, HashType.HASHTABLE, 64, 0.75f);
        StateDescriptorImpl descriptor = new StateDescriptorImpl(0, 0);
        // exercises the sized (6-arg) constructor; state lookup should work normally
        assertFalse(stateSpace.exists(descriptor));
        stateSpace.getState(descriptor);
        assertTrue(stateSpace.exists(descriptor));
    }

    @Test
    public void testGetNextStratifiedSample() {
        // boundaries: step=1, min=0, max=5 → intStates 0..5 (6 states)
        StateImpl.setStateBoundaries(1.0, 0.0, 5.0);
        StateSpaceImpl stateSpace = new StateSpaceImpl(1, buildActionList, idempotentAction, HashType.HASHTABLE);
        StateSpaceSampleIteratorImpl iterator = new StateSpaceSampleIteratorImpl(
                stateSpace, SamplingScheme.STRATIFIED_SAMPLING, 3, 1.0);
        int[] sample = iterator.getNextStratifiedSample(3);
        assertTrue(sample.length >= 1);
    }

    @Test
    public void testSamplingIteratorExhaustionHasNextFalseNextNull() {
        // small state space: step=1, min=0, max=2 → 3 states
        StateImpl.setStateBoundaries(1.0, 0.0, 2.0);
        StateSpaceImpl stateSpace = new StateSpaceImpl(
                1, buildActionList, idempotentAction, HashType.HASHTABLE,
                SamplingScheme.JENSENS_PARTITIONING, 3, 1.0);
        StateSpaceIterator iterator = (StateSpaceIterator) stateSpace.iterator();
        while (iterator.hasNext()) {
            iterator.next();
        }
        assertFalse(iterator.hasNext());
        // next() after exhaustion returns null without throwing
        State result = iterator.next();
        assertEquals(null, result);
    }
}
