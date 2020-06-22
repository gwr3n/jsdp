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

package jsdp.utilities.sampling;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cern.colt.list.IntArrayList;

import umontreal.ssj.gof.GofStat;
import umontreal.ssj.probdist.ChiSquareDist;
import umontreal.ssj.probdist.DiscreteDistributionInt;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.EmpiricalDist;
import umontreal.ssj.probdist.PoissonDist;

public class PoissonSampleTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testResetNextSubstream() { 
		assertTrue("This method delegates to RandomStream",true);
	}

	@Test
	public void testResetStartStream() {
		assertTrue("This method delegates to RandomStream",true);
	}

	@Test
	public void testGetNextPoissonSampleGof() {
		int N = 10000;
		double[] array = new double[N];
		Arrays.fill(array, 30);
		Distribution[] distributions = IntStream.iterate(0, i -> i + 1).limit(N).mapToObj(i -> new PoissonDist(array[i])).toArray(Distribution[]::new);
	    
		SampleFactory.resetStartStream();
		
		int[] samples = Arrays.stream(SampleFactory.getNextSample(distributions)).mapToInt(i -> (int) Math.round(i)).toArray();
		IntArrayList list = new IntArrayList(samples);
		DiscreteDistributionInt distribution = new PoissonDist(30);
		int[] categories = new int[1];
		double chi2Statistic = GofStat.chi2(
				list, 
				distribution, 
				0, 
				(int) PoissonDist.inverseF(30, 0.999), 
				5, 
				categories);
		assertTrue("Gof (Chi^2): "+chi2Statistic+">="+ChiSquareDist.inverseF(categories[0]-1, 0.95), 
				chi2Statistic <= ChiSquareDist.inverseF(categories[0]-1, 0.95));
	}

	@Test
	public void testVarianceReduction(){
		int N = 200;
		int randomVariables = 10;
		
		DiscreteDistributionInt distribution = new PoissonDist(30);
		
		double[] arraylambda = new double[randomVariables];
		
		Arrays.fill(arraylambda, distribution.getMean());
		
		Distribution[] distributions = IntStream.iterate(0, i -> i + 1)
												.limit(randomVariables)
												.mapToObj(i -> new PoissonDist(arraylambda[i]))
												.toArray(Distribution[]::new);
	    
		SampleFactory.resetStartStream();
		
		double[][] dataLHS = SampleFactory.getNextLHSample(distributions, N);
		
		SampleFactory.resetNextSubstream();
		
		double[][] dataSRS = IntStream.iterate(0, d -> d + 1)
									.limit(randomVariables)
									.mapToObj(d -> DoubleStream.iterate(0, i -> i + 1)
															   .limit(N).map(i -> SampleFactory.getNextSample(distributions)[d]).toArray()
									).toArray(double[][]::new);
		
		
		for(int i = 0; i < randomVariables; i++) Arrays.sort(dataLHS[i]);
		EmpiricalDist empLHS[] = IntStream.iterate(0, i -> i + 1)
										  .limit(randomVariables)
									      .mapToObj(i -> new EmpiricalDist(dataLHS[i]))
									      .toArray(EmpiricalDist[]::new);
		for(int i = 0; i < randomVariables; i++) Arrays.sort(dataSRS[i]);
		EmpiricalDist empSRS[] = IntStream.iterate(0, i -> i + 1)
										  .limit(randomVariables)
										  .mapToObj(i -> new EmpiricalDist(dataSRS[i]))
										  .toArray(EmpiricalDist[]::new);
		
		for(int i = 0; i < randomVariables; i++)
			assertTrue("LHS Mean: "+empLHS[i].getMean()+"\tSRS Mean: "+empSRS[i].getMean(), 
					Math.abs(distribution.getMean()-empLHS[i].getMean()) <= Math.abs(distribution.getMean()-empSRS[i].getMean()));

	}

}
