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

/**
 * An abstract lightweight descriptor to uniquely identify a {@code State}. {@code StateDescriptor}
 * must implement method {@code hashCode()}, which will be used by the {@code Hashtable} used
 * to store states.
 * 
 * @author Roberto Rossi
 *
 */
public abstract class StateDescriptor{
	
	protected int period;
	
	@Override
	public abstract boolean equals(Object descriptor);
	@Override
	public abstract int hashCode();
	
	public StateDescriptor(int period){
	   this.period = period;
	}
	
	/**
	 * Returns the period associated with this {@code StateDescriptor}.
	 * @return the period associated with this {@code StateDescriptor}. 
	 */
	public int getPeriod(){
		return period;
	}
}