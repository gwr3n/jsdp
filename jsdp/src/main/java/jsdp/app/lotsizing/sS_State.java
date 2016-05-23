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

package jsdp.app.lotsizing;

import java.util.ArrayList;

import jsdp.sdp.Action;
import jsdp.sdp.State;

public class sS_State extends State {
	
	private int initialInventory;
	
	//Factor must be 1 for discrete distributions
	public static double factor = 1;
	public static int minInventory = -100;
	public static int maxInventory = 150;
	
	public sS_State(sS_StateDescriptor descriptor){
		this.period = descriptor.getPeriod();
		this.initialInventory = descriptor.getInitialInventory();
		this.buildActionList();
	}
	
	/**
	 * Implement inventory normalisation!
	 */
	public int getInitialInventory(){
		return this.initialInventory;
	}
	
	@Override
	protected void buildActionList(){
		this.noAction = new sS_Action(this, 0);
		this.permissibleActions = new ArrayList<Action>();
		for(int i = this.initialInventory; i <= maxInventory; i++){
			permissibleActions.add(new sS_Action(this, i - this.initialInventory));
		}
	}
	
	@Override
	public boolean equals(Object state){
		if(state instanceof sS_State)
			return this.period == ((sS_State)state).period && this.initialInventory == ((sS_State)state).initialInventory;
		else return false;
	}
	
	@Override
	public int hashCode(){
		String hash = "";
        hash = (hash + period) + initialInventory;
        return hash.hashCode();
	}

	@Override
	public int compareTo(State state) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public String toString(){
		return "Period: "+this.period+"\tInventory:"+this.initialInventory;
	}
}
