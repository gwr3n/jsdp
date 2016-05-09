package jsdp.app.stochasticlotsizing.sampling;

import java.util.Random;

import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.randvar.UniformGen;
import umontreal.ssj.randvarmulti.IIDMultivariateGen;
import umontreal.ssj.randvarmulti.RandomMultivariateGen;
import umontreal.ssj.rng.MRG32k3aL;
import umontreal.ssj.rng.RandomStream;

public class NormalSample {
	
	static RandomStream stream;
	
	static {
		stream = new MRG32k3aL();
	}
	
	public static void resetNextSubstream(){
		stream.resetNextSubstream();
	}
	
	public static void resetStartStream(){
		stream.resetStartStream();
	}
	
	public static double[] getNextNormalSample(double[] mu, double[] sigma){
		if(mu.length != sigma.length) 
			throw new NullPointerException();
		UniformGen uniform = new UniformGen(stream);
		RandomMultivariateGen generator = new IIDMultivariateGen(uniform, mu.length);
		double[] p = new double[mu.length];
		generator.nextPoint(p);
		double[] sample = new double[mu.length];
		for(int i = 0; i < p.length; i++){
			sample[i] = NormalDist.inverseF(mu[i], sigma[i], p[i]);
		}
		return sample;
	}
	
	public static double[][] getNormalLHSSample(double[] mu, double[] sigma, int points, long seed){
        if(mu.length != sigma.length) return null;
        double[][] sampledProbabilities = LHSampling.latin_random(mu.length, points, seed);
        double[][] normalSample = new double[points][mu.length];
        for(int i = 0; i < points; i++){
            for(int j = 0; j < mu.length; j++){
                normalSample[i][j] = NormalDist.inverseF01(sampledProbabilities[j][i])*sigma[j]+mu[j];
            }
        }
        return normalSample;
    }
    
    public static int[][] getNormalLHSSampleInt(double[] mu, double[] sigma, int points, long seed){
        if(mu.length != sigma.length) return null;
        double[][] sampledProbabilities = LHSampling.latin_random(mu.length, points, seed);
        int[][] normalSample = new int[points][mu.length];
        for(int i = 0; i < points; i++){
            for(int j = 0; j < mu.length; j++){
                normalSample[i][j] = (int)Math.round(NormalDist.inverseF01(sampledProbabilities[j][i])*sigma[j]+mu[j]);
            }
        }
        return normalSample;
    }
}
