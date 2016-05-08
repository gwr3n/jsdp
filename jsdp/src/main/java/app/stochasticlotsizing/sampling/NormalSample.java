package app.stochasticlotsizing.sampling;

import java.util.Random;

import umontreal.iro.lecuyer.probdist.NormalDist;

public class NormalSample {
	
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

    public static void main(String args[]){
        NormalSample.test();
    }

}
