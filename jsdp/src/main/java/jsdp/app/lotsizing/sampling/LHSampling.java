package jsdp.app.lotsizing.sampling;

import java.util.Random;

public class LHSampling {

	public static double[][] latin_random_matrix ( int dim_num, int point_num, long seed ){
        Random rnd = new Random(seed);
        double x[][] = new double[dim_num][point_num];
        int [] perm;

        for (int i = 0; i < dim_num; i++ )
        {
            perm = perm_random ( point_num, rnd );

        for (int j = 0; j < point_num; j++ )
        {
          x[i][j] = perm[j] - 1;
        }
      }
      return x;
    }
	
    public static double[][] latin_random ( int dim_num, int point_num, long seed ){
        Random rnd = new Random(seed);
        double x[][] = new double[dim_num][point_num];
        int [] perm;

        for (int i = 0; i < dim_num; i++ )
        {
            perm = perm_random ( point_num, rnd );

        for (int j = 0; j < point_num; j++ )
        {
          x[i][j] = ( ( ( double ) ( perm[j] - 1 ) ) + rnd.nextDouble() ) / ( ( double ) point_num );
        }
      }
      return x;
    }

    static int getUniform(int a, int b, Random rnd) {
        double randDbl = rnd.nextDouble();
        randDbl = (1.0 - randDbl) * ((double) (Math.min(a, b)) - 0.5)
                  + randDbl * ((double) (Math.max(a, b)) + 0.5);

        randDbl = Math.round(randDbl);

        randDbl = Math.max(randDbl, Math.min(a, b));
        randDbl = Math.min(randDbl, Math.max(a, b));

        return (int)randDbl;
    }

    static int[] perm_random ( int point_num, Random rnd){
        int[] perm = new int[point_num];

        for (int i = 0; i < point_num; i++) {
            perm[i] = i + 1;
        }

        for (int i = 1; i <= point_num; i++) {
            int j = getUniform(i, point_num, rnd);
            int swap = perm[i - 1];
            perm[i - 1] = perm[j - 1];
            perm[j - 1] = swap;
        }

        return perm;
    }

    static void test(){
        int dimNumber = 4;
        int pointNumber = 4;
        long seed = 1112;

        double[][] latin = latin_random(dimNumber, pointNumber, seed);

        for(int j = 0; j < pointNumber; j++){
            for(int i = 0; i < dimNumber; i++){
                System.out.print((""+latin[i][j]).substring(0,5)+ "\t");
            }
            System.out.println();
        }
    }

    public static void main(String args[]){
        LHSampling.test();
    }
}
