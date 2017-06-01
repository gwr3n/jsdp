package jsdp.app.inventory.capital;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class CapitalFlowBatch {
   
   /**
    * zip -d jsdp-0.0.1-SNAPSHOT.jar 'META-INF/*.SF' 'META-INF/*.RSA' 'META-INF/*SF'
    * @param args
    */
   
   public static void main(String args[]){
      /**
       * Fixed parameters
       */
      double h = 1;
      
      int initialInventory = 0;
      
      /**
       * Variable parameters
       */
      
      double[] K = {10,15};
      double[] v = {1,2};
      double[] S = {5,10};
      
      double[] p = {2,4};
      double[] b = {0.05,0.2};
      
      int initialCapital[] = {0,20};
      
      double[][] demands = {{7,7,7,7,7,7},
                            {2,3,4,5,6,7},
                            {8,7,6,5,4,3},
                            {5,6,7,8,7,6},//7,6,5,4,5,6
                            {8,5,2,1,2,5},
                            {8,4,1,3,1,3},
                            {1,3,8,4,8,7},
                            {1,4,7,3,5,8},
                            {3,8,4,4,6,2},
                            {3,1,5,8,4,4}
                            };
      
      writeToFile("./"+CapitalFlowBatch.class.getSimpleName() + "_results.csv", getHeadersString());
      
      for(int demand = 0; demand < demands.length; demand++){
         for(int fixedOrderingCost = 0; fixedOrderingCost < K.length; fixedOrderingCost++){
            for(int proportionalOrderingCost = 0; proportionalOrderingCost < v.length; proportionalOrderingCost++){
               for(int sellingPrice = 0; sellingPrice < S.length; sellingPrice++){
                  for(int penaltyCost = 0; penaltyCost < p.length; penaltyCost++){
                     for(int interest = 0; interest < b.length; interest++){
                        for(int c = 0; c < initialCapital.length; c++){
                           /**
                            * Instance
                            */
                           
                           CapitalFlow cf = new CapitalFlow(K[fixedOrderingCost],
                                                            v[proportionalOrderingCost],
                                                            S[sellingPrice],
                                                            h,
                                                            p[penaltyCost],
                                                            b[interest],
                                                            demands[demand],
                                                            initialInventory,
                                                            initialCapital[c]
                                                            );
                           
                           double[] result = cf.runInstance();
                           
                           String out = K[fixedOrderingCost]+",\t"+
                                        v[proportionalOrderingCost]+",\t"+
                                        S[sellingPrice]+",\t"+
                                        h+",\t"+
                                        p[penaltyCost]+",\t"+
                                        b[interest]+",\t"+
                                        demand+",\t"+
                                        initialInventory+",\t"+
                                        initialCapital[c]+",\t"+
                                        result[0]+",\t"+
                                        result[1];
                           
                           writeToFile("./"+CapitalFlowBatch.class.getSimpleName() + "_results.csv", out);
                        }
                     }
                  }
               }
            }
         }
      }
   }
   
   public static String getHeadersString(){
      return "K,v,S,h,p,b,demand,I0,B0,ETC,Time(sec)";
   }
   
   public static void writeToFile(String fileName, String str){
      File results = new File(fileName);
      try {
         FileOutputStream fos = new FileOutputStream(results, true);
         OutputStreamWriter osw = new OutputStreamWriter(fos);
         osw.write(str+"\n");
         osw.close();
      } catch (FileNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
}
