package app.stochasticlotsizing.sampling;

import java.util.Random;

import umontreal.iro.lecuyer.probdist.NormalDist;
import umontreal.iro.lecuyer.probdist.PoissonDist;
import umontreal.iro.lecuyer.randvar.UniformGen;
import umontreal.iro.lecuyer.randvarmulti.IIDMultivariateGen;
import umontreal.iro.lecuyer.randvarmulti.RandomMultivariateGen;
import umontreal.iro.lecuyer.rng.MRG32k3aL;
import umontreal.iro.lecuyer.rng.RandomStream;

public class NormalSample {
	
	public static void main(String args[]){
		stream.resetStartStream();
		for(double e : NormalSample.getNextNormalSample(new double[]{30,20,30}, new double[]{10,20,30})){
			System.out.print(e + " ");
		}
		System.out.println();
		stream.resetNextSubstream();
		for(double e : NormalSample.getNextNormalSample(new double[]{10,20,30}, new double[]{10,20,30})){
			System.out.print(e + " ");
		}
	}
	
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
	
	public static double[][] getNormalSample(double[] mu, double[] sigma, int points, long seed){
		Random generator = new Random(seed);
        if(mu.length != sigma.length) return null;
        double[][] normalSample = new double[points][mu.length];
        for(int i = 0; i < points; i++){
            for(int j = 0; j < mu.length; j++){
                normalSample[i][j] = NormalDist.inverseF01(generator.nextDouble())*sigma[j]+mu[j];
            }
        }
        return normalSample;
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

    static void test(){
        double[] mu = {20,50,40,15,60,30};
        double[] sigma = new double[mu.length];
        for(int i = 0; i< sigma.length; i++) sigma[i] = mu[i]*0.2;
        int pointNumber = 10;
        long seed = 212311;

        double[][] latinNormal = getNormalSample(mu, sigma, pointNumber, seed);

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
