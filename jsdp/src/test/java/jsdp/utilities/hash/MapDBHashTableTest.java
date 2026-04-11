package jsdp.utilities.hash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
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

    @Test
    public void testHeapShardedStorage() {
        MapDBHashTable<String, Integer> shardedTable =
                new MapDBHashTable<>("heapShardedTest", MapDBHashTable.Storage.HEAP_SHARDED);
        shardedTable.put("a", 42);
        assertEquals(Integer.valueOf(42), shardedTable.get("a"));
        assertTrue(shardedTable.containsKey("a"));
        assertEquals(1, shardedTable.size());
        shardedTable.clear();
    }

    @Test
    public void testMemoryShardedStorage() {
        MapDBHashTable<String, Integer> shardedTable =
                new MapDBHashTable<>("memShardedTest", MapDBHashTable.Storage.MEMORY_SHARDED);
        shardedTable.put("b", 99);
        assertEquals(Integer.valueOf(99), shardedTable.get("b"));
        assertEquals(1, shardedTable.size());
        shardedTable.clear();
    }

    @Test
    public void testDiskStorage() {
        // Snapshot existing files in tables/ so we only delete what this test creates
        File tablesDir = new File("tables");
        Set<String> filesBefore = tablesDir.exists() && tablesDir.listFiles() != null
                ? new HashSet<>(Arrays.stream(tablesDir.listFiles()).map(File::getName)
                        .collect(java.util.stream.Collectors.toSet()))
                : Collections.emptySet();

        MapDBHashTable<String, Integer> diskTable =
                new MapDBHashTable<>("diskTest", MapDBHashTable.Storage.DISK);
        try {
            diskTable.put("c", 7);
            assertEquals(Integer.valueOf(7), diskTable.get("c"));
            assertTrue(diskTable.containsKey("c"));
        } finally {
            diskTable.db.close();
            // Delete only the files this test created
            if (tablesDir.exists() && tablesDir.listFiles() != null) {
                for (File f : tablesDir.listFiles()) {
                    if (!filesBefore.contains(f.getName())) {
                        f.delete();
                    }
                }
            }
        }
    }

    @AfterClass
    public static void cleanUpTablesDirectory() throws Exception {
        // Safety net: remove any residual diskTest DB files left by this test class
        File tablesDir = new File("tables");
        if (tablesDir.exists() && tablesDir.listFiles() != null) {
            for (File f : tablesDir.listFiles()) {
                if (f.getName().startsWith("diskTest")) {
                    Files.deleteIfExists(f.toPath());
                }
            }
            // Remove the tables directory itself if it is now empty
            String[] remaining = tablesDir.list();
            if (remaining != null && remaining.length == 0) {
                tablesDir.delete();
            }
        }
    }

    @Test
    public void testPutAllAndKeySet() {
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 10);
        source.put("y", 20);
        heapTable.putAll(source);
        assertEquals(2, heapTable.size());
        Set<String> keys = heapTable.keySet();
        assertTrue(keys.contains("x"));
        assertTrue(keys.contains("y"));
    }
}
