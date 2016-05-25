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

import jsdp.sdp.Action;
import jsdp.sdp.State;

public class sS_Action extends Action {
	
	int intAction;
	
	public static double actionToOrderQuantity(int action){
		return action*sS_State.getStepSize();
	}
	
	public static int orderQuantityToAction(double orderQty){
		return (int) Math.round(orderQty/sS_State.getStepSize());
	}
	
	public sS_Action(State state, int intAction){
		this.state = state;
		this.intAction = intAction;
	}
	
	public int getIntAction(){
		return this.intAction;
	}
	
	@Override
	public boolean equals(Object action){
		if(action instanceof sS_Action)
			return this.intAction == ((sS_Action)action).intAction;
		else
			return false;
	}
	
	@Override
	public int hashCode(){
		String hash = "";
        hash = (hash + intAction);
        return hash.hashCode();
	}
	
	@Override
	public String toString(){
		return state+"\tAction: "+ actionToOrderQuantity(this.intAction);
	}
}
