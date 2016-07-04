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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jsdp.sdp.Recursion.OptimisationDirection;

/**
 * Stores the best action and its value.
 * 
 * @author Roberto Rossi
 *
 */
public class BestActionRepository {
   
   static final Logger logger = LogManager.getLogger(BestActionRepository.class.getName());
   
   Action bestAction = null;
   double bestValue;
   OptimisationDirection direction;
   
   public BestActionRepository(OptimisationDirection direction) {
      this.direction = direction;
      bestValue = direction == OptimisationDirection.MIN ? Double.MAX_VALUE : Double.MIN_VALUE;
   }

   /**
    * Compares {@code currentAction} and {@code currentValue} to the best action currently stored and updates
    * values stored accordingly.
    * 
    * @param currentAction the action.
    * @param currentValue the action expected value.
    */
   public synchronized void update(Action currentAction, double currentValue){
      if(currentAction == null)
         throw new NullPointerException("Current action cannot be null");
      switch(direction){
      case MIN:
         if(bestAction == null || Double.isNaN(this.bestValue) || currentValue < bestValue){
            bestValue = currentValue;
            bestAction = currentAction;
         }
         break;
      case MAX:
         if(bestAction == null || Double.isNaN(this.bestValue) || currentValue > bestValue){
            bestValue = currentValue;
            bestAction = currentAction;
         }
         break;
      }
   }
   
   /**
    * Returns the best action stored.
    * 
    * @return the best action stored.
    */
   public Action getBestAction(){
      if(Double.isNaN(this.bestValue))
         logger.error("Number of samples probably too low");
      return this.bestAction;
   }
   
   /**
    * Returns the value associated with the best action stored.
    * 
    * @return the value associated with the best action stored.
    */
   public double getBestValue(){
       if(Double.isNaN(this.bestValue))
            logger.error("Number of samples probably too low");
      return this.bestValue;
   }
}
