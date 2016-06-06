package jsdp.utilities.probdist;

import java.util.Arrays;
import java.util.stream.IntStream;

import jsdp.sdp.impl.multivariate.StateImpl;

import umontreal.ssj.probdist.DiscreteDistribution;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdistmulti.DiscreteDistributionIntMulti;

/**
 * This class implements a multivariate distribution built from independently 
 * non-identically distributed random variables
 * 
 * @author Roberto Rossi
 *
 */
public class MultiINIDistribution extends DiscreteDistributionIntMulti{
   Distribution[] distributions;
   double[] supportLowerBounds; 
   double[] supportUpperBounds;
   DiscreteDistribution[] discreteDistributions;
   
   /**
    * Creates a new instance of a multivariate distribution built from independently 
    * non-identically distributed random variables. 
    * 
    * @param distributions the constituting independent discrete distributions. 
    * @param supportLowerBounds support lower bound of each distribution
    * @param supportUpperBounds support upper bound of each distribution
    */
   public MultiINIDistribution(Distribution[] distributions, double[] supportLowerBounds, double[] supportUpperBounds){
      this.dimension = distributions.length;
      this.distributions = Arrays.copyOf(distributions, distributions.length);
      this.supportLowerBounds = Arrays.copyOf(supportLowerBounds, supportLowerBounds.length);
      this.supportUpperBounds = Arrays.copyOf(supportUpperBounds, supportUpperBounds.length);
   }
   
   public void discretizeDistributions(){
      this.discreteDistributions = IntStream.iterate(0, i -> i + 1)
                                            .limit(this.dimension)
                                            .mapToObj(i -> DiscreteDistributionFactory.getTruncatedDiscreteDistribution(distributions[i], supportLowerBounds[i], supportUpperBounds[i], StateImpl.getStepSize()[i]))
                                            .toArray(DiscreteDistribution[]::new);
   }

   @Override
   public double prob(int[] x) {
      double prob = 1;
      for(int i = 0; i < this.dimension; i++){
         prob *= this.discreteDistributions[i].prob(x[i]);
      }
      return prob;
   }

   @Override
   public double[] getMean() {
      return Arrays.stream(this.discreteDistributions).mapToDouble(d -> d.getMean()).toArray();
   }

   @Override
   public double[][] getCovariance() {
      double[][] covMatrix = new double[this.dimension][this.dimension];
      for(int i = 0; i < this.dimension; i++){
         covMatrix[i][i] = this.discreteDistributions[i].getVariance();
      }
      return covMatrix;
   }

   @Override
   public double[][] getCorrelation() {
      double[][] corrMatrix = new double[this.dimension][this.dimension];
      for(int i = 0; i < this.dimension; i++){
         corrMatrix[i][i] = 1.0;
      }
      return corrMatrix;
   }
   
   
}
