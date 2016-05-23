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

import umontreal.ssj.probdist.DiscreteDistribution;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.probdist.PoissonDist;

public class DiscreteDistributionFactory {

	public static DiscreteDistribution getTruncatedDiscreteDistribution(Distribution distribution, double supportLB, double supportUB, double factor){
		if(distribution instanceof PoissonDist){
			if(factor != 1) throw new NullPointerException("Factor must be 1 for Poisson");
			return truncatePoisson(distribution.getMean(), supportLB, supportUB);
		}else if(distribution instanceof NormalDist){
			return discretizeTruncatedNormal(distribution.getMean(), distribution.getStandardDeviation(), supportLB, supportUB, factor);
		}else{
			throw new NullPointerException("Unknown distribution");
		}
	}
	
	private static DiscreteDistribution truncatePoisson(double lambda, double supportLB, double supportUB){
		int[] demandValues = IntStream.iterate(supportLB >= 0 ? (int) Math.round(supportLB) : 0, n -> n + 1).limit((supportUB >= 0 ? (int) Math.round(supportUB) : 0) + 1).toArray();
		PoissonDist distribution = new PoissonDist(lambda);
		double[] demandProbabilities = Arrays.stream(demandValues).mapToDouble(d -> distribution.prob(d)/(distribution.cdf(supportUB)-distribution.cdf(supportLB-1))).toArray();
		
		return new DiscreteDistribution(demandValues, demandProbabilities, demandValues.length);
	}
	
	private static DiscreteDistribution discretizeTruncatedNormal(double mu, double sigma, double supportLB, double supportUB, double stepSize){
		double[] demandValues = DoubleStream.iterate(supportLB, n -> n + stepSize).limit((int) Math.ceil((supportUB - supportLB)/stepSize) + 1).toArray();
		NormalDist distribution = new NormalDist(mu, sigma);
		double[] demandProbabilities = Arrays.stream(demandValues).map(
				d -> (distribution.cdf(d+0.5*stepSize) - distribution.cdf(d-0.5*stepSize))/(distribution.cdf(supportUB+0.5*stepSize)-distribution.cdf(supportLB-0.5*stepSize))).toArray();
		
		return new DiscreteDistribution(demandValues, demandProbabilities, demandValues.length);
	}
}

