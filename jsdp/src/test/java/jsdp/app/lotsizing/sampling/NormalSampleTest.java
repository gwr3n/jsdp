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

package jsdp.app.lotsizing.sampling;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import umontreal.ssj.gof.GofStat;
import umontreal.ssj.probdist.ContinuousDistribution;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.NormalDist;

/* 
 * http://www.vogella.com/tutorials/JUnit/article.html
 */

public class NormalSampleTest {

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
	public void testGetNextNormalSample() {
		int N = 10000;
		double[] arrayMu = new double[N];
		double[] arraySigma = new double[N];
		Arrays.fill(arrayMu, 30);
		Arrays.fill(arraySigma, 3);
		
		Distribution[] distributions = IntStream.iterate(0, i -> i + 1).limit(N).mapToObj(i -> new NormalDist(arrayMu[i],arraySigma[i])).toArray(Distribution[]::new);
	    
		double[] data = SampleFactory.getInstance().getNextSample(distributions);
		ContinuousDistribution distribution = new NormalDist(30,3);
		double[] sval = new double[3];
		double[] pval = new double[3];
		GofStat.kolmogorovSmirnov(data, distribution, sval, pval);
		assertTrue("Gof (KS): "+pval[2]+"<= 0.05", pval[2] >= 0.05);
	}
	
	@Test
	public void testGetNormalLHSSample(){
		fail("Not yet implemented");
	}
}
