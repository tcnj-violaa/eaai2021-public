import java.util.Arrays;
import java.util.Random;

// mostly from http://modelai.gettysburg.edu/2013/cfr/
// modified according to pdf to perform regret-matching 
// for both players--instead of average strategy of action 1 tending to 
// lean toward 1 to exploit the opponent's bias towards action 0,
// both reach an equilibrium and result in roughly 1/3 chance for every action.
public class DualRPSTrainer {
        
    public static final int ROCK = 0, PAPER = 1, SCISSORS = 2, NUM_ACTIONS = 3;
    public static final Random random = new Random();
    double[] regretSum = new double[NUM_ACTIONS], 
             strategy = new double[NUM_ACTIONS], 
             strategySum = new double[NUM_ACTIONS]; 
             //oppStrategy = { 0.4, 0.3, 0.3 }; 

    double[] oppRegretSum = new double[NUM_ACTIONS],
    	     oppStrategy = new double[NUM_ACTIONS],
	     oppStrategySum = new double[NUM_ACTIONS];
    private double[] getStrategy() {
        double normalizingSum = 0;
        for (int a = 0; a < NUM_ACTIONS; a++) {
            strategy[a] = regretSum[a] > 0 ? regretSum[a] : 0;
            normalizingSum += strategy[a];
        }
        for (int a = 0; a < NUM_ACTIONS; a++) {
            if (normalizingSum > 0)
              strategy[a] /= normalizingSum;
            else
              strategy[a] = 1.0 / NUM_ACTIONS;
            strategySum[a] += strategy[a];
        }
        return strategy;
    }
    

    private double[] getOppStrategy() {
        double normalizingSum = 0;
        for (int a = 0; a < NUM_ACTIONS; a++) {
            oppStrategy[a] = oppRegretSum[a] > 0 ? oppRegretSum[a] : 0;
            normalizingSum += oppStrategy[a];
        }
        for (int a = 0; a < NUM_ACTIONS; a++) {
            if (normalizingSum > 0)
              oppStrategy[a] /= normalizingSum;
            else
              oppStrategy[a] = 1.0 / NUM_ACTIONS;
            oppStrategySum[a] += oppStrategy[a];
        }
        return oppStrategy;
    }
    

    public int getAction(double[] strategy) {
        double r = random.nextDouble();
        int a = 0;
        double cumulativeProbability =  0;
        while (a < NUM_ACTIONS - 1) {
            cumulativeProbability += strategy[a];
            if (r < cumulativeProbability)
                break;
            a++;
        }
        return a;
    }
    
    public void train(int iterations) {
        double[] actionUtility = new double[NUM_ACTIONS];
        double[] oppActionUtility = new double[NUM_ACTIONS];

	for (int i = 0; i < iterations; i++) {
            double[] strategy = getStrategy();
            double[] oppStrategy = getOppStrategy();
	    int myAction = getAction(strategy);
            int otherAction = getAction(oppStrategy);
            actionUtility[otherAction] = 0;
            actionUtility[otherAction == NUM_ACTIONS - 1 ? 0 : otherAction + 1] = 1;
            actionUtility[otherAction == 0 ? NUM_ACTIONS - 1 : otherAction - 1] = -1;

	    oppActionUtility[myAction] = 0;
            oppActionUtility[myAction == NUM_ACTIONS - 1 ? 0 : myAction + 1] = 1;
            oppActionUtility[myAction == 0 ? NUM_ACTIONS - 1 : myAction - 1] = -1;

            for (int a = 0; a < NUM_ACTIONS; a++){
                regretSum[a] += actionUtility[a] - actionUtility[myAction];
		oppRegretSum[a] += oppActionUtility[a] - oppActionUtility[otherAction];
		}
        }
    }
    
    public double[] getAverageStrategy() {
        double[] avgStrategy = new double[NUM_ACTIONS];
        double normalizingSum = 0;
        for (int a = 0; a < NUM_ACTIONS; a++)
            normalizingSum += strategySum[a];
        for (int a = 0; a < NUM_ACTIONS; a++) 
            if (normalizingSum > 0)
                avgStrategy[a] = strategySum[a] / normalizingSum;
            else
                avgStrategy[a] = 1.0 / NUM_ACTIONS;
        return avgStrategy;
    }

     public double[] getOppAverageStrategy() {
        double[] avgStrategy = new double[NUM_ACTIONS];
        double normalizingSum = 0;
        for (int a = 0; a < NUM_ACTIONS; a++)
            normalizingSum += oppStrategySum[a];
        for (int a = 0; a < NUM_ACTIONS; a++) 
            if (normalizingSum > 0)
                avgStrategy[a] = oppStrategySum[a] / normalizingSum;
            else
                avgStrategy[a] = 1.0 / NUM_ACTIONS;
        return avgStrategy;
    }

    public static void main(String[] args) {
        DualRPSTrainer trainer = new DualRPSTrainer();
        trainer.train(1000000);
        System.out.println(Arrays.toString(trainer.getAverageStrategy()));
        System.out.println(Arrays.toString(trainer.getOppAverageStrategy()));
    }

}
