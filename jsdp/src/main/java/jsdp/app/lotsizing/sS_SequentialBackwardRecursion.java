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

import java.util.Enumeration;

import org.apache.logging.log4j.Logger;

import jsdp.sdp.Action;
import umontreal.ssj.probdist.Distribution;

import org.apache.logging.log4j.LogManager;

public class sS_SequentialBackwardRecursion extends sS_BackwardRecursion {
	static final Logger logger = LogManager.getLogger(sS_BackwardRecursion.class.getName());
	
	public sS_SequentialBackwardRecursion(
			Distribution[] demand,
			double fixedOrderingCost, 
			double proportionalOrderingCost, 
			double holdingCost,
			double penaltyCost){
		super(demand,fixedOrderingCost,proportionalOrderingCost,holdingCost,penaltyCost);
	}
	
	@Override
	protected void recurse(int period){
		for(int i = sS_State.maxIntState; i >= sS_State.minIntState; i--){
			sS_StateDescriptor stateDescriptor = new sS_StateDescriptor(period, i);
			sS_State state = (sS_State) ((sS_StateSpace)this.getStateSpace()[period]).getState(stateDescriptor);
			Action bestAction = null;
			double bestCost = Double.MAX_VALUE;
			Enumeration<Action> actions = state.getPermissibleActions();
			while(actions.hasMoreElements()){
				Action currentAction = actions.nextElement();
				double currentCost = this.getCostRepository().getExpectedCost(state, currentAction, this.getTransitionProbability());
				if(currentCost < bestCost){
					bestCost = currentCost;
					bestAction = currentAction;
				}
			}
			this.getCostRepository().setOptimalExpectedCost(state, bestCost);
			this.getCostRepository().setOptimalAction(state, bestAction);
			logger.trace("Period["+period+"]\tInventory: "+i+"\tOrder: "+sS_Action.actionToOrderQuantity(((sS_Action)bestAction).intAction)+"\tCost: "+bestCost);
			
			if(((sS_Action)bestAction).getIntAction() > 0){
				int initialAction = ((sS_Action)bestAction).getIntAction();
				while(--i >= sS_State.minIntState){
					stateDescriptor = new sS_StateDescriptor(period, i);
					state = (sS_State) ((sS_StateSpace)this.getStateSpace()[period]).getState(stateDescriptor);
					double orderingCostIncrement = sS_Action.actionToOrderQuantity(((sS_Action)bestAction).intAction+1-initialAction)*this.proportionalOrderingCost;
					this.getCostRepository().setOptimalExpectedCost(state, bestCost+orderingCostIncrement);
					bestAction = new sS_Action(state, ((sS_Action)bestAction).intAction+1); 
					this.getCostRepository().setOptimalAction(state, bestAction);
					logger.trace("Period["+period+"]\tInventory: "+i+"\tOrder: "+sS_Action.actionToOrderQuantity(((sS_Action)bestAction).intAction)+"\tCost: "+bestCost);
				}
			}
		}
	}
}
