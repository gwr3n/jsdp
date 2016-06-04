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

package jsdp.utilities.probdist;

import java.util.Arrays;

import umontreal.ssj.probdistmulti.MultinomialDist;

/**
 * A "safe" implementation of a MultinomialDist; this implementation
 * rather than throwing an exception, returns a probability 0.0 for states 
 * that are outside the multinomial support (i.e. states with negative values). 
 * 
 * @author Roberto Rossi
 *
 */
public class SafeMultinomialDist extends MultinomialDist{
   
   /**
    * Creates a `MultinomialDist` object with parameters @f$n@f$ and
    * (@f$p_1@f$,…,@f$p_d@f$) such that @f$\sum_{i=1}^d p_i = 1@f$. We
    * have @f$p_i = @f$ `p[i-1]`.
    * 
    * @param n number of trials
    * @param p multinomial probabilities
    */
   public SafeMultinomialDist (int n, double p[]) {
      super(n, p);
   }
   
   @Override
   public double prob (int x[]) {
      if(Arrays.stream(x).filter(element -> element < 0).findAny().isPresent())
         return 0;
      else
         return super.prob(x);
   }

   @Override
   public double cdf (int x[]) {
      if(Arrays.stream(x).filter(element -> element < 0).findAny().isPresent())
         return 0;
      else
         return super.cdf(x);
   }
}
