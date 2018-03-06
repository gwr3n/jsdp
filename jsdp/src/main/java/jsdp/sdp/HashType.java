package jsdp.sdp;

/**
 * Available hash mappings. Note that ConcurrentHashMap should not be used with our
 * forward recursion code, as this leads to deadlocks. It is instead fine to 
 * use ConcurrentHashMap in the context of our backward recursion code. 
 * 
 * http://blog.jooq.org/2015/03/04/avoid-recursion-in-concurrenthashmap-computeifabsent
 * https://bugs.openjdk.java.net/browse/JDK-8074374
 * http://stackoverflow.com/q/28840047/521799
 * 
 * @author Roberto Rossi
 *
 */

public enum HashType {
   HASHTABLE,
   CONCURRENT_HASHMAP,
   THASHMAP,
   MAPDB_HEAP,
   MAPDB_HEAP_SHARDED,
   MAPDB_MEMORY,
   MAPDB_MEMORY_SHARDED,
   MAPDB_DISK
}
