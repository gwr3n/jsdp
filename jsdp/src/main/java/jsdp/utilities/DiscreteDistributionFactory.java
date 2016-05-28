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

package jsdp.utilities;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import umontreal.ssj.probdist.ContinuousDistribution;
import umontreal.ssj.probdist.DiscreteDistribution;
import umontreal.ssj.probdist.DiscreteDistributionInt;
import umontreal.ssj.probdist.Distribution;

/**
 * {@code DiscreteDistributionFactory} converts a {@code ContinuousDistribution} 
 * or a {@code DiscreteDistributionInt} from the {@code umontreal.ssj.probdist} 
 * package to a {@code DiscreteDistribution}.
 *  
 * @author Roberto Rossi
 *
 */
public class DiscreteDistributionFactory {

	/**
	 * A method to discretize and truncate a {@code Distribution}.
	 * 
	 * @param distribution original {@code DiscreteDistributionInt} to be discretized and truncated
	 * @param supportLB support lower bound
	 * @param supportUB support upper bound
	 * @param stepSize discretization step
	 * @return a discretized and truncated {@code DiscreteDistribution}
	 */
	public static DiscreteDistribution getTruncatedDiscreteDistribution(Distribution distribution, 
																		double supportLB, 
																		double supportUB, 
																		double stepSize){
		if(distribution instanceof DiscreteDistributionInt){
			if(stepSize != 1) 
				throw new NullPointerException("Factor must be 1 for Poisson");
			return truncatedDiscreteDistributionInt((DiscreteDistributionInt) distribution, supportLB, supportUB);
		}else if(distribution instanceof ContinuousDistribution){
			return discretizeTruncatedContinuousDistribution((ContinuousDistribution) distribution, supportLB, supportUB, stepSize);
		}else{
			throw new NullPointerException("Unknown distribution");
		}
	}
	
	/**
	 * A method to discretize a {@code DiscreteDistributionInt}.
	 * 
	 * @param distribution original {@code DiscreteDistributionInt} to be discretized
	 * @param supportLB support lower bound
	 * @param supportUB support upper bound
	 * @return a discretized {@code DiscreteDistribution}
	 */
	private static DiscreteDistribution truncatedDiscreteDistributionInt(DiscreteDistributionInt distribution, 
																	     double supportLB, 
																	     double supportUB){
		int[] demandValues = IntStream.iterate(supportLB >= 0 ? (int) Math.round(supportLB) : 0, n -> n + 1)
									  .limit((supportUB >= 0 ? (int) Math.round(supportUB) : 0) + 1)
									  .toArray();
		double[] demandProbabilities = Arrays.stream(demandValues)
											 .mapToDouble(d -> distribution.prob(d)/
													 		   (distribution.cdf(supportUB)-distribution.cdf(supportLB-1)))
											 .toArray();
		
		return new DiscreteDistribution(demandValues, 
										demandProbabilities, 
										demandValues.length);
	}
	
	/**
	 * A method to discretize a {@code ContinuousDistribution}.
	 * 
	 * @param distribution original {@code ContinuousDistribution} to be discretized
	 * @param supportLB support lower bound
	 * @param supportUB support upper bound
	 * @param stepSize discretization step
	 * @return a discretized {@code DiscreteDistribution}
	 */
	private static DiscreteDistribution discretizeTruncatedContinuousDistribution(ContinuousDistribution distribution, 
																		 double supportLB,
																		 double supportUB, 
																		 double stepSize){
		double[] demandValues = DoubleStream.iterate(supportLB, n -> n + stepSize)
											.limit((int) Math.ceil((supportUB - supportLB)/stepSize) + 1)
											.toArray();
		double[] demandProbabilities = Arrays.stream(demandValues)
											 .map(d -> (distribution.cdf(d+0.5*stepSize) - distribution.cdf(d-0.5*stepSize))/
													   (distribution.cdf(supportUB+0.5*stepSize)-distribution.cdf(supportLB-0.5*stepSize)))
											 .toArray();
		return new DiscreteDistribution(demandValues, demandProbabilities, demandValues.length);
	}
}

