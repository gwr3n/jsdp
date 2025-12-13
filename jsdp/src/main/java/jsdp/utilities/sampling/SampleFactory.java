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

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.randvar.UniformGen;
import umontreal.ssj.randvar.UniformIntGen;
import umontreal.ssj.rng.MRG32k3aL;
import umontreal.ssj.rng.RandomStream;

public class SampleFactory {
	
	private static RandomStream stream = new MRG32k3aL();
	
   // Factory parameters and state
   public static int M = 1000; // batch size for LHS
   private static double[][] currentLHSBatch = null; // shape: [d][M]
   private static int nextBatchIndex = 0;
	
	/**
	 * Reinitializes the stream to the beginning of its next substream.
	 */
	public static void resetNextSubstream(){
		stream.resetNextSubstream();
	}
	
	/**
	 * Reinitializes the stream to its initial state.
	 */
	public static void resetStartStream(){
		stream.resetStartStream();
	}
	
	/**
	 * Implements Simple Random Sampling
	 * @param distributions array of distributions to be sampled
	 * @return a Simple Random Sample for the distributions in {@code distributions}
	 */
	public static double[] getNextSample(Distribution[] distributions){
		UniformGen uniform = new UniformGen(stream);
		return IntStream.iterate(0, i -> i + 1)
		                .limit(distributions.length).mapToDouble(
		                      i -> distributions[i].inverseF(uniform.nextDouble()))
		                .toArray();
	}
	
	/**
    * Implements LHS factory: returns one sample vector at a time from an internal LHS batch.
    * Regenerates a new batch of size M when exhausted or when dimension changes.
    * @param distributions array of distributions to be sampled
    * @return a Simple Random Sample for the distributions in {@code distributions} using LHS batching
    */
   public static double[] getNextLHSample(Distribution[] distributions){
      // Regenerate when no batch, exhausted, or dimension changed
      if (currentLHSBatch == null
          || nextBatchIndex >= M
          || currentLHSBatch.length != distributions.length) {
         currentLHSBatch = getNextLHSample(distributions, M);
         nextBatchIndex = 0;
      }
      // Extract column nextBatchIndex across all dimensions
      double[] sample = new double[distributions.length];
      for (int d = 0; d < distributions.length; d++) {
         sample[d] = currentLHSBatch[d][nextBatchIndex];
      }
      nextBatchIndex++;
      return sample;
   }
	
	/**
	 * Implements Latin Hypercube Sampling as originally introduced in 
	 * 
	 * McKay, M.D.; Beckman, R.J.; Conover, W.J. (May 1979). 
	 * "A Comparison of Three Methods for Selecting Values of Input Variables 
	 * in the Analysis of Output from a Computer Code". 
	 * Technometrics 21 (2): 239–245.
	 * 
	 * @param distributions array of distributions to be sampled 
	 * @param samples number of samples
	 * @return a Latin Hypercube Sample for the distributions in {@code distributions}
	 */
	public static double[][] getNextLHSample(Distribution[] distributions, int samples){
		double x[][] = new double[distributions.length][samples];
		x = IntStream.iterate(0, d -> d + 1)
		             .limit(distributions.length)
		             .mapToObj(
		                   d -> DoubleStream.iterate(0, i -> i + 1.0/samples)
		                                    .limit(samples)
		                                    .map(i -> distributions[d].inverseF(i + UniformGen.nextDouble(stream, 0, 1.0/samples)))
		                                    .toArray())
		             .toArray(double[][]::new);	
		for(int i = 0; i < x.length; i++){
			shuffle(x[i]);
		}
		return x;
	}
	
	/**
	 * Returns an unbiased Fisher–Yates shuffle of {@code sample}.
	 * 
	 * @param sample the original sample.
	 * @return a random shuffle of {@code sample}.
	 */
	private static double[] shuffle(double[] sample){
		for(int i = 0; i < sample.length - 1; i++){
			int j = UniformIntGen.nextInt(stream, i, sample.length - 1);
			double temp = sample[i];
			sample[i] = sample[j];
			sample[j] = temp;
		}
		return sample;
	}
}
