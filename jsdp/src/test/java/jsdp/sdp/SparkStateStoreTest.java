/**
 * jsdp: A Java Stochastic Dynamic Programming Library
 *
 * MIT License
 *
 * Copyright (c) 2016 Roberto Rossi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jsdp.sdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link SparkStateStore} and the {@link HashType#SPARK_RDD} integration path
 * through {@link StateSpace}.
 */
public class SparkStateStoreTest {

    private JavaSparkContext sc;

    @Before
    public void setUp() {
        try {
            SparkConf conf = new SparkConf()
                    .setAppName("SparkStateStoreTest")
                    .setMaster("local");
            sc = new JavaSparkContext(conf);
            sc.setLogLevel("ERROR");
        } catch (UnsupportedOperationException e) {
            Assume.assumeTrue("Skipping SparkStateStoreTest: Spark is incompatible with this JDK version: " + e.getMessage(), false);
        }
    }

    @After
    public void tearDown() {
        if (sc != null) {
            sc.close();
        }
    }

    // -----------------------------------------------------------------------
    // Minimal serializable State stand-in used only within this test class
    // -----------------------------------------------------------------------

    private static final class MockState extends State {
        private static final long serialVersionUID = 1L;
        private final int id;

        MockState(int period, int id) {
            super(period);
            this.id = id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof MockState)) return false;
            MockState o = (MockState) obj;
            return this.period == o.period && this.id == o.id;
        }

        @Override
        public int hashCode() {
            return 31 * period + id;
        }
    }

    // -----------------------------------------------------------------------
    // SparkStateStore unit tests
    // -----------------------------------------------------------------------

    @Test
    public void testGetReturnsNullOnEmptyStore() {
        SparkStateStore<Integer> store = new SparkStateStore<>(sc);
        assertNull(store.get(99));
    }

    @Test
    public void testPutAndGetRoundTrip() {
        SparkStateStore<Integer> store = new SparkStateStore<>(sc);
        MockState state = new MockState(1, 42);
        store.put(42, state);
        State retrieved = store.get(42);
        assertNotNull(retrieved);
        assertEquals(state, retrieved);
    }

    @Test
    public void testGetReturnsFirstMatchingEntry() {
        SparkStateStore<Integer> store = new SparkStateStore<>(sc);
        MockState s1 = new MockState(1, 10);
        MockState s2 = new MockState(2, 20);
        store.put(10, s1);
        store.put(20, s2);
        assertEquals(s1, store.get(10));
        assertEquals(s2, store.get(20));
        assertNull(store.get(99));
    }

    @Test
    public void testEntrySetReflectsAllPuts() {
        SparkStateStore<Integer> store = new SparkStateStore<>(sc);
        MockState s1 = new MockState(1, 1);
        MockState s2 = new MockState(1, 2);
        MockState s3 = new MockState(1, 3);
        store.put(1, s1);
        store.put(2, s2);
        store.put(3, s3);
        Set<Map.Entry<Integer, State>> entries = store.entrySet();
        assertEquals(3, entries.size());
    }

    @Test
    public void testEntrySetIsEmptyInitially() {
        SparkStateStore<Integer> store = new SparkStateStore<>(sc);
        assertEquals(0, store.entrySet().size());
    }

    // -----------------------------------------------------------------------
    // HashType.SPARK_RDD integration test via StateSpace constructor
    // -----------------------------------------------------------------------

    private static final class MockStateSpace extends StateSpace<Integer> {

        MockStateSpace(int period, JavaSparkContext sc) {
            super(period, HashType.SPARK_RDD, sc);
        }

        @Override
        public State getState(Integer descriptor) {
            State existing = stateStore.get(descriptor);
            if (existing != null) return existing;
            State created = new MockState(this.period, descriptor);
            stateStore.put(descriptor, created);
            return created;
        }

        @Override
        public Iterator<State> iterator() {
            return stateStore.entrySet().stream().map(Map.Entry::getValue).iterator();
        }
    }

    @Test
    public void testHashTypeSparkRddConstructsSparkStore() {
        MockStateSpace space = new MockStateSpace(2, sc);
        assertEquals(2, space.getPeriod());
        assertNotNull(space.getState(7));
        assertEquals(space.getState(7), space.getState(7));
    }

    @Test
    public void testEntrySetViaStateSpaceMatchesInsertedStates() {
        MockStateSpace space = new MockStateSpace(1, sc);
        space.getState(100);
        space.getState(200);
        assertEquals(2, space.entrySet().size());
    }

    @Test
    public void testDefaultMapStateStoreRejectsSparkRdd() {
        try {
            new DefaultMapStateStore<Integer>(HashType.SPARK_RDD);
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException for SPARK_RDD");
    }
}
