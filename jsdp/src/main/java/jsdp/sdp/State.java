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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Stream;

public abstract class State implements Comparable<State>{
	
	protected int period;
	protected ArrayList<Action> permissibleActions;
	protected Action noAction;
	
	public Enumeration<Action> getPermissibleActions() {
		Enumeration<Action> e = Collections.enumeration(this.permissibleActions);
		return e;
	}
	
	public Stream<Action> getPermissibleActionsStream(){
		return this.permissibleActions.stream();
	}
	
	public Stream<Action> getPermissibleActionsParallelStream(){
		return this.permissibleActions.parallelStream();
	}
	
	public Action getNoAction(){
		return noAction;
	}
	
	public int getPeriod(){
		return this.period;
	}
	
	public abstract int compareTo(State state);
	public abstract boolean equals(Object state);
	public abstract int hashCode();
	protected abstract void buildActionList();
}
