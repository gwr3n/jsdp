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

import java.util.Map;
import java.util.Set;

/**
 * Storage abstraction for the state space backing store.
 * Implementations provide the concrete storage strategy used by {@code StateSpace}.
 * The default implementation is {@code DefaultMapStateStore}, which wraps the
 * existing {@code HashType}-based in-memory and MapDB maps.
 * Alternative implementations (e.g. Apache Spark RDDs) can be supplied via the
 * {@code StateSpace(int, StateStore)} protected constructor.
 *
 * @author Roberto Rossi
 *
 * @param <SD> the state descriptor type for the problem at hand.
 */
public interface StateStore<SD> {

   /**
    * Returns the {@code State} associated with the given descriptor,
    * or {@code null} if no mapping exists.
    *
    * @param descriptor the state descriptor.
    * @return the stored {@code State}, or {@code null}.
    */
   State get(SD descriptor);

   /**
    * Associates the given descriptor with the given {@code State}.
    *
    * @param descriptor the state descriptor.
    * @param state      the state to store.
    */
   void put(SD descriptor, State state);

   /**
    * Returns the entry set of all stored descriptor-to-state mappings.
    *
    * @return a set of key-value entries.
    */
   Set<Map.Entry<SD, State>> entrySet();
}
