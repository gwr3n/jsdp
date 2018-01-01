package jsdp.app.inventory.capital;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jsdp.sdp.Action;
import jsdp.sdp.HashType;
import jsdp.sdp.State;
import jsdp.sdp.impl.univariate.SamplingScheme;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.PoissonDist;

/**
 * Implementation of the model discussed in 
 * 
 * Z. Chen, R. Rossi, R. Zhang, "Single item stochastic lot sizing problem 
 * considering capital flow and business overdraft," arxiv:1706.05663
 * 
 * @author Roberto Rossi
 *
 */
public class CapitalFlow {
   
   static final Logger logger = LogManager.getLogger(CapitalFlow.class.getName());
   
   double K = 10;
   double v = 1;
   double S = 5;
   double h = 1;
   double p = 2;
   double b = 0.05;
   
   double lambda[] = {2,3,4,5,6,7};  
   
   int maxOrderQuantity = 50;
   
   int initialInventory = 0;
   int initialCapital = 0;
   
   CF_ForwardRecursion recursion;
   
   public CapitalFlow(){
      
   }
   
   public CapitalFlow(double K,
                      double v,
                      double S,
                      double h,
                      double p,
                      double b,
                      double[] lambda,
                      int initialInventory,
                      int initialCapital){
      this.K = K;
      this.v = v;
      this.S = S;
      this.h = h;
      this.p = p;
      this.b = b;
      
      this.lambda = Arrays.copyOf(lambda, lambda.length);
      
      this.initialInventory = initialInventory;
      this.initialCapital = initialCapital;
   }
   
   private CF_ForwardRecursion buildModel(){
      /*******************************************************************
       * Model definition
       */
      
      // State space
      CF_Action.setMaxOrderQuantity(maxOrderQuantity);
      
      // Actions
      Function<State, ArrayList<Action>> buildActionList = s -> {
         CF_State state = (CF_State) s;
         ArrayList<Action> feasibleActions = new ArrayList<Action>();
         feasibleActions.addAll(CF_Action.generateOrderQuantities(state).mapToObj(action -> new CF_Action(state, action)).collect(Collectors.toList()));
         return feasibleActions;
      };
      
      // Immediate Value Function
      
      jsdp.sdp.ImmediateValueFunction<State, Action, Double> immediateValueFunction = (initialState, action, finalState) -> {
         CF_State is = (CF_State) initialState;
         CF_Action a = (CF_Action) action;
         CF_State fs = (CF_State) finalState;
         int demand = is.getInventory() + a.getOrderQuantity() - fs.getInventory();
         
         double revenue = this.S*Math.min(demand + Math.max(-is.getInventory(), 0), a.getOrderQuantity() + Math.max(is.getInventory(), 0));
         double orderingCost = a.getOrderQuantity() > 0 ? this.K + a.getOrderQuantity()*this.v : 0;
         double inventoryCost = this.h*Math.max(fs.getInventory(), 0) + this.p*Math.max(-fs.getInventory(), 0);
         double borrowingCost = this.b*Math.max(-fs.getCapital(), 0);
         return revenue - orderingCost - inventoryCost - borrowingCost;
      };
      
      // Immediate Value Function
      
      ImmediateValueFunction<State, Action, Integer, Double> immediateValueFunctionRandomOutcome = (initialState, action, demand) -> {
         CF_State is = (CF_State) initialState;
         CF_Action a = (CF_Action) action;
         int finalInventory = is.getInventory() + a.getOrderQuantity() - demand;
         
         double revenue = this.S*Math.min(demand + Math.max(-is.getInventory(), 0), a.getOrderQuantity() + Math.max(is.getInventory(), 0));
         double orderingCost = a.getOrderQuantity() > 0 ? this.K + a.getOrderQuantity()*this.v : 0;
         double inventoryCost = this.h*Math.max(finalInventory, 0) + this.p*Math.max(-finalInventory, 0);
         
         int finalCapital = is.getCapital() + (int) Math.round(revenue - orderingCost - inventoryCost);
         
         double borrowingCost = this.b*Math.max(-finalCapital, 0);
         return revenue - orderingCost - inventoryCost - borrowingCost;
      };
      
      /**
       * THashMap
       */
      int stateSpaceSizeLowerBound = 10000000;
      float loadFactor = 0.8F;
      double discountFactor = 1.0;
      
      Distribution[] distributions = IntStream.iterate(0, i -> i + 1)
                                              .limit(lambda.length)
                                              .mapToObj(i -> new PoissonDist(lambda[i]))
                                              .toArray(Distribution[]::new);
      
      /**
       * Sampling scheme
       */
      SamplingScheme samplingScheme = SamplingScheme.NONE;
      int sampleSize = 50;                                     // This is the sample size used to determine a state value function
      double reductionFactorPerStage = 20;
      
      CF_ForwardRecursion recursion = new CF_ForwardRecursion(distributions,
                                                              immediateValueFunction,
                                                              immediateValueFunctionRandomOutcome,
                                                              buildActionList,
                                                              discountFactor,
                                                              HashType.HASHTABLE,
                                                              stateSpaceSizeLowerBound,
                                                              loadFactor,
                                                              samplingScheme,
                                                              sampleSize,
                                                              reductionFactorPerStage);
      return recursion;
   }
   
   public double[] runInstance(){
      recursion = buildModel();
      
      int period = 0;
      
      CF_StateDescriptor initialState = new CF_StateDescriptor(period, 
                                                               initialInventory,
                                                               initialCapital);
      
      recursion.runForwardRecursionMonitoring(((CF_StateSpace)recursion.getStateSpace()[initialState.getPeriod()]).getState(initialState));
      double EFC = recursion.getExpectedCapital(initialState) + initialCapital;
      long percent = recursion.getMonitoringInterfaceForward().getPercentCPU();
      logger.info("---");
      logger.info("Expected final capital: "+EFC);
      logger.info("Optimal initial action: "+recursion.getOptimalAction(initialState).toString());
      logger.info("Time elapsed: "+recursion.getMonitoringInterfaceForward().getTime());
      logger.info("Cpu usage: "+percent+"% ("+Runtime.getRuntime().availableProcessors()+" cores)");
      logger.info("---");
      
      return new double[]{EFC,recursion.getMonitoringInterfaceForward().getTime()};
   }
   
   public static void main(String args[]){
      CapitalFlow cf = new CapitalFlow();
      cf.runInstance();
   }
}
