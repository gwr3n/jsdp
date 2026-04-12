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

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gnu.trove.map.hash.THashMap;
import jsdp.utilities.hash.MapDBHashTable;
import jsdp.utilities.hash.MapDBHashTable.Storage;

/**
 * Default {@code StateStore} implementation backed by a {@code Map}.
 * The concrete {@code Map} type is selected from a {@code HashType} enum value,
 * preserving all pre-existing storage options (in-memory and MapDB variants).
 *
 * @author Roberto Rossi
 *
 * @param <SD> the state descriptor type for the problem at hand.
 */
public class DefaultMapStateStore<SD> implements StateStore<SD> {

   private final Map<SD, State> map;

   /**
    * Constructs a store backed by the {@code Map} type corresponding to {@code hash}.
    *
    * @param hash the hash storage type.
    */
   public DefaultMapStateStore(HashType hash) {
      if(hash == HashType.SPARK_RDD)
         throw new IllegalArgumentException("Use SparkStateStore for HashType.SPARK_RDD");
      switch (hash) {
         case HASHTABLE:
            map = new Hashtable<>();
            break;
         case CONCURRENT_HASHMAP:
            map = Collections.synchronizedMap(new ConcurrentHashMap<>());
            break;
         case THASHMAP:
            map = Collections.synchronizedMap(new THashMap<>());
            break;
         case MAPDB_HEAP:
            map = new MapDBHashTable<>("states", Storage.HEAP);
            break;
         case MAPDB_HEAP_SHARDED:
            map = new MapDBHashTable<>("states", Storage.HEAP_SHARDED);
            break;
         case MAPDB_MEMORY:
            map = new MapDBHashTable<>("states", Storage.MEMORY);
            break;
         case MAPDB_MEMORY_SHARDED:
            map = new MapDBHashTable<>("states", Storage.MEMORY_SHARDED);
            break;
         case MAPDB_DISK:
            map = new MapDBHashTable<>("states", Storage.DISK);
            break;
         default:
            throw new NullPointerException("HashType not available");
      }
   }

   /**
    * Constructs a sized store backed by the {@code Map} type corresponding to {@code hash}.
    * Size hint and load factor are honoured by the Java collection types; MapDB variants
    * ignore them (MapDB manages sizing internally).
    *
    * @param hash                    the hash storage type.
    * @param stateSpaceSizeLowerBound initial capacity hint.
    * @param loadFactor              the load factor for the internal map.
    */
   public DefaultMapStateStore(HashType hash, int stateSpaceSizeLowerBound, float loadFactor) {
      if(hash == HashType.SPARK_RDD)
         throw new IllegalArgumentException("Use SparkStateStore for HashType.SPARK_RDD");
      switch (hash) {
         case HASHTABLE:
            map = new Hashtable<>(stateSpaceSizeLowerBound, loadFactor);
            break;
         case CONCURRENT_HASHMAP:
            map = Collections.synchronizedMap(new ConcurrentHashMap<>(stateSpaceSizeLowerBound, loadFactor));
            break;
         case THASHMAP:
            map = Collections.synchronizedMap(new THashMap<>(stateSpaceSizeLowerBound, loadFactor));
            break;
         case MAPDB_HEAP:
            map = new MapDBHashTable<>("states", Storage.HEAP);
            break;
         case MAPDB_HEAP_SHARDED:
            map = new MapDBHashTable<>("states", Storage.HEAP_SHARDED);
            break;
         case MAPDB_MEMORY:
            map = new MapDBHashTable<>("states", Storage.MEMORY);
            break;
         case MAPDB_MEMORY_SHARDED:
            map = new MapDBHashTable<>("states", Storage.MEMORY_SHARDED);
            break;
         case MAPDB_DISK:
            map = new MapDBHashTable<>("states", Storage.DISK);
            break;
         default:
            throw new NullPointerException("HashType not available");
      }
   }

   @Override
   public State get(SD descriptor) {
      return map.get(descriptor);
   }

   @Override
   public void put(SD descriptor, State state) {
      map.put(descriptor, state);
   }

   @Override
   public Set<Map.Entry<SD, State>> entrySet() {
      return map.entrySet();
   }
}
