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

package jsdp.utilities.hash;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.mapdb.*;

/**
 * A wrapper for class {@code HTreeMap} from http://www.mapdb.org
 * 
 * http://www.mapdb.org/doc/quick-start/
 * http://www.mapdb.org/doc/htreemap/
 * 
 * @author Roberto Rossi
 *
 * @param <K> the hash table key type
 * @param <V> the hash table value type
 */
public class MapDBHashTable<K,V> implements Map<K,V>{

   protected DB db;
   protected HTreeMap<K,V> table;
   
   private static int shardConcurrency = 16;
   
   public enum Storage {
      HEAP,
      HEAP_SHARDED,
      MEMORY,
      MEMORY_SHARDED,
      DISK
   };
   
   @SuppressWarnings("unchecked")
   public MapDBHashTable(String name, Storage hashTableStorage){
      switch(hashTableStorage){
      case HEAP:
         db = DBMaker.heapDB().make();
         this.table = (HTreeMap<K,V>)db.hashMap(name).create();
         break;
      case HEAP_SHARDED:
         this.table =  (HTreeMap<K,V>)DBMaker.heapShardedHashMap(shardConcurrency).create();
         break;
      case MEMORY:
         db = DBMaker.memoryDB().make();
         //db = DBMaker.memoryDirectDB().make();
         this.table = (HTreeMap<K,V>)db.hashMap(name).create();
         break;
      case MEMORY_SHARDED:
         this.table =  (HTreeMap<K,V>)DBMaker.memoryShardedHashMap(shardConcurrency).create();
         break;
      case DISK:
         File f = new File("tables");
         if (!(f.exists() && f.isDirectory())) {
            f.mkdir();
         }
         String uuid = UUID.randomUUID().toString();
         db = DBMaker.fileDB("tables/"+name+uuid+".db").fileMmapEnableIfSupported()
               .allocateStartSize(50 * 1024*1024)  // 50MB
               .make();
         this.table = (HTreeMap<K,V>)db.hashMap(name).create();
         break;
      }
      
   }
   
   @SuppressWarnings("deprecation")
   @Override
   protected void finalize() throws Throwable {
      super.finalize();
      db.close();
    }
   
   @Override
   public int size() {
      return this.table.size();
   }

   @Override
   public boolean isEmpty() {
      return this.table.isEmpty();
   }

   @Override
   public boolean containsKey(Object key) {
      return this.table.containsKey(key);
   }

   @Override
   public boolean containsValue(Object value) {
      return this.table.containsValue(value);
   }

   @Override
   public V get(Object key) {
      return this.table.get(key);
   }

   @Override
   public V put(K key, V value) {
      return this.table.put(key, value);
   }

   @Override
   public V remove(Object key) {
      return this.table.remove(key);
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      this.table.putAll(m);
   }

   @Override
   public void clear() {
      this.table.clear();
   }

   @SuppressWarnings("unchecked")
   @Override
   public Set<K> keySet() {
      return (Set<K>)this.table.keySet();
   }

   @SuppressWarnings("unchecked")
   @Override
   public Collection<V> values() {
      return (Collection<V>)this.table.values();
   }

   @SuppressWarnings("unchecked")
   @Override
   public Set<java.util.Map.Entry<K, V>> entrySet() {
      return (Set<java.util.Map.Entry<K, V>>)this.table.entrySet();
   }
}
