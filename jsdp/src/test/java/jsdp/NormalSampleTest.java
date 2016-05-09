package jsdp;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import app.stochasticlotsizing.sampling.NormalSample;

public class NormalSampleTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSequentialSampling() {
		NormalSample.resetStartStream();
		for(double e : NormalSample.getNextNormalSample(new double[]{30,20,30}, new double[]{10,20,30})){
			System.out.print(e + " ");
		}
		System.out.println();
		NormalSample.resetNextSubstream();
		for(double e : NormalSample.getNextNormalSample(new double[]{10,20,30}, new double[]{10,20,30})){
			System.out.print(e + " ");
		}
	}
	
	@Test
	public void testLHS(){
        double[] mu = {20,50,40,15,60,30};
        double[] sigma = new double[mu.length];
        for(int i = 0; i< sigma.length; i++) sigma[i] = mu[i]*0.2;
        int pointNumber = 10;
        long seed = 212311;

        double[][] latinNormal = NormalSample.getNormalSample(mu, sigma, pointNumber, seed);

        for(int j = 0; j < pointNumber; j++){
            for(int i = 0; i < mu.length; i++){
                System.out.print((""+latinNormal[j][i]).substring(0,5)+ "\t");
            }
            System.out.println();
        }
        System.out.println();
        for(int j = 0; j < pointNumber; j++){
        	System.out.print("[");
            for(int i = 0; i < mu.length-1; i++){
                System.out.print((""+Math.round(latinNormal[j][i]))+ ",\t");
            }
            System.out.println((""+Math.round(latinNormal[j][mu.length-1]))+ "],");
        }

    }

}
