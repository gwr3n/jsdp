package jsdp.sdp.impl.multivariate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
        StateImpl.setStateBoundaries(new double[] { 1.0 }, new double[] { -1.0 }, new double[] { 2.0 });

        buildActionList = state -> {
            ArrayList<Action> actions = new ArrayList<>();
            actions.add(new ActionImpl(state, new int[] { 0 }));
            actions.add(new ActionImpl(state, new int[] { 1 }));
            return actions;
        };
        idempotentAction = state -> new ActionImpl(state, new int[] { 0 });
    }

    @Test
    public void testPeriodZeroIteratorIsNullAndStateCachingWorks() {
        StateSpaceImpl stateSpace = new StateSpaceImpl(
                0,
                buildActionList,
                idempotentAction,
                SamplingScheme.NONE,
                10,
                HashType.HASHTABLE,
                1.0);

        StateDescriptorImpl descriptor = new StateDescriptorImpl(0, new int[] { 0 });
        assertFalse(stateSpace.exists(descriptor));

        State state = stateSpace.getState(descriptor);
        assertTrue(stateSpace.exists(descriptor));
        assertEquals(state, stateSpace.getState(descriptor));
        assertNull(stateSpace.iterator());
    }

    @Test
    public void testDeterministicIteratorAndSamplingIterator() {
        StateSpaceImpl deterministicStateSpace = new StateSpaceImpl(
                1,
                buildActionList,
                idempotentAction,
                SamplingScheme.NONE,
                10,
                HashType.HASHTABLE,
                1.0);

        int count = 0;
        for (@SuppressWarnings("unused") State ignored : deterministicStateSpace) {
            count++;
        }
        assertEquals(4, count);

        StateSpaceImpl sampledStateSpace = new StateSpaceImpl(
                1,
                buildActionList,
                idempotentAction,
                SamplingScheme.SIMPLE_RANDOM_SAMPLING,
                3,
                HashType.HASHTABLE,
                1.0);

        assertTrue(sampledStateSpace.iterator() instanceof StateSpaceSampleIteratorImpl);
        StateSpaceSampleIteratorImpl sampleIterator = new StateSpaceSampleIteratorImpl(
                sampledStateSpace,
                SamplingScheme.JENSENS_PARTITIONING,
                3,
                1.0);
        int[] jensenSample = sampleIterator.getNextJensensSample(3);
        assertEquals(3, jensenSample.length);
        assertNotNull(sampleIterator.next());
    }

    @Test
    public void testPlainAndSizedConstructorsUseDeterministicIterators() {
        StateSpaceImpl plainStateSpace = new StateSpaceImpl(1, buildActionList, idempotentAction, HashType.HASHTABLE);
        StateSpaceImpl sizedStateSpace = new StateSpaceImpl(1, buildActionList, idempotentAction, HashType.HASHTABLE, 32, 0.75f);

        int plainCount = 0;
        for (@SuppressWarnings("unused") State ignored : plainStateSpace) {
            plainCount++;
        }
        assertEquals(4, plainCount);

        StateSpaceIterator iterator = (StateSpaceIterator) sizedStateSpace.iterator();
        int sizedCount = 0;
        while (iterator.hasNext()) {
            assertNotNull(iterator.next());
            sizedCount++;
        }
        assertEquals(4, sizedCount);
        assertFalse(iterator.hasNext());
        assertNull(iterator.next());
    }

    @Test
    public void testStratifiedSamplingAndSampleIteratorExhaustion() {
        StateSpaceImpl sampledStateSpace = new StateSpaceImpl(
                1,
                buildActionList,
                idempotentAction,
                SamplingScheme.STRATIFIED_SAMPLING,
                2,
                HashType.HASHTABLE,
                1.0);

        assertTrue(sampledStateSpace.iterator() instanceof StateSpaceSampleIteratorImpl);

        StateSpaceSampleIteratorImpl stratifiedIterator = new StateSpaceSampleIteratorImpl(
                sampledStateSpace,
                SamplingScheme.STRATIFIED_SAMPLING,
                2,
                1.0);
        int[] stratifiedSample = stratifiedIterator.getNextStratifiedSample(2);
        assertEquals(2, stratifiedSample.length);

        StateSpaceSampleIteratorImpl exhaustedIterator = new StateSpaceSampleIteratorImpl(
                sampledStateSpace,
                SamplingScheme.JENSENS_PARTITIONING,
                2,
                1.0);
        while (exhaustedIterator.hasNext()) {
            assertNotNull(exhaustedIterator.next());
        }

        assertFalse(exhaustedIterator.hasNext());
        assertNull(exhaustedIterator.next());
    }
}
