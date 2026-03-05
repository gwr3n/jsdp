package jsdp.utilities.hash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MapDBHashTableTest {

    private MapDBHashTable<String, Integer> heapTable;
    private MapDBHashTable<String, Integer> memoryTable;

    @Before
    public void setUp() {
        heapTable = new MapDBHashTable<>("heapTest", MapDBHashTable.Storage.HEAP);
        memoryTable = new MapDBHashTable<>("memoryTest", MapDBHashTable.Storage.MEMORY);
    }

    @After
    public void tearDown() throws Throwable {
        // MapDB resources should ideally be closed, but we'll let finalize or JVM
        // handle it
        // Or we could explicitly call a close method if it existed.
        heapTable.clear();
        memoryTable.clear();
    }

    @Test
    public void testPutAndGet() {
        heapTable.put("key1", 100);
        assertEquals(Integer.valueOf(100), heapTable.get("key1"));

        memoryTable.put("key2", 200);
        assertEquals(Integer.valueOf(200), memoryTable.get("key2"));
    }

    @Test
    public void testContainsKeyAndValue() {
        heapTable.put("key1", 100);
        assertTrue(heapTable.containsKey("key1"));
        assertFalse(heapTable.containsKey("missingKey"));
        assertTrue(heapTable.containsValue(100));
        assertFalse(heapTable.containsValue(999));
    }

    @Test
    public void testRemove() {
        heapTable.put("key1", 100);
        assertEquals(Integer.valueOf(100), heapTable.remove("key1"));
        assertFalse(heapTable.containsKey("key1"));
        assertEquals(0, heapTable.size());
    }

    @Test
    public void testSizeAndClear() {
        heapTable.put("k1", 1);
        heapTable.put("k2", 2);
        heapTable.put("k3", 3);
        assertEquals(3, heapTable.size());

        heapTable.clear();
        assertEquals(0, heapTable.size());
        assertTrue(heapTable.isEmpty());
    }
}
