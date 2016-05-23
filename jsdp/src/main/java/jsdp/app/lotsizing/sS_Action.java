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
	
	int orderQty;
	
	public sS_Action(State state, int orderQty){
		this.state = state;
		this.orderQty = orderQty;
	}
	
	public int getOrderQuantity(){
		return this.orderQty;
	}
	
	@Override
	public boolean equals(Object action){
		if(action instanceof sS_Action)
			return this.orderQty == ((sS_Action)action).orderQty;
		else
			return false;
	}
	
	@Override
	public int hashCode(){
		String hash = "";
        hash = (hash + orderQty);
        return hash.hashCode();
	}
	
	@Override
	public String toString(){
		return state+"\tAction: "+this.orderQty;
	}
}
