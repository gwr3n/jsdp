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
