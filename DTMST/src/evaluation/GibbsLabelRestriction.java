package evaluation;

import java.io.IOException;
import java.util.ArrayList;

import regressions.EProblemList;
import regressions.GibbsFixedSegSize;
import regressions.Problems;
import utils.RandomPermutation;
import utils.RegressionProblem;
import weka.classifiers.Evaluation;
import weka.core.Instances;

/**
 * @author Wuga
 *
 *	We try to evaluate label restriction version of Gibbs Sampling.
 *	How the performance is impacted when number of Latent classes is fixed?
 * 	For each setting of number of latent classes, we run 10 times to see the mean and variance of the error
 *  For each predictive problem, this code generate a curve to show the performance changes based on the restriction.
 *  The output is under output folder, which is CSV format data. Please run python code to generate the plot
 */
public class GibbsLabelRestriction {
	
	public ArrayList<double[][]>  errors;
	public int MaxRestriction=10;
	public int MinRestriction=1;
	public int TrainingPercent=30;
	public int EvalIterationNumber=10;
	public String GibbsIterationNumber="2000";
	
	public GibbsLabelRestriction(int Max, int Min, int TrainP, int EvalI, int GibbI){
		MaxRestriction=Max;
		MinRestriction=Min;
		TrainingPercent=TrainP;
		EvalIterationNumber=EvalI;
		GibbsIterationNumber=Integer.toString(GibbI);
		errors=new ArrayList<double[][]>();
	}
	
	public void run() throws Exception{
		Problems pbs=new Problems();
		for(EProblemList name:EProblemList.values()){
			System.out.println("Working on problem:"+name);
			double [][] error_record=new double[MaxRestriction-MinRestriction+1][2];
			RegressionProblem cp=pbs.createRegressionProblem(name);
			cp.normalizeData();
			RandomPermutation randPerm=new RandomPermutation();
			randPerm.getRandomPermutation(cp.getData());
			Instances data=randPerm.permutated;
			int count=0;
			for(int i=MinRestriction;i<=MaxRestriction;i++){
				System.out.println("Iteration:"+(count+1));
				double mean=0;
				double CI=0;
				double[] temp_record=new double[EvalIterationNumber];
				for(int j=0;j<EvalIterationNumber;j++){
					randPerm.getRandomPermutation(data);
					data=randPerm.permutated;
					int trainSize = (int) Math.round(data.numInstances() * (double)TrainingPercent/ 100);
					int testSize = data.numInstances() - (int) Math.round(data.numInstances() * 0.5);
					Instances newTrain = new Instances(data, 0, trainSize);
					Instances newTest = new Instances(data, testSize, testSize);
					GibbsFixedSegSize classifier=new GibbsFixedSegSize();
					classifier.setOptions(new String[]{"-I",GibbsIterationNumber,"-L",Integer.toString(i)});
					classifier.buildClassifier(newTrain);
					Evaluation eval = new Evaluation(newTrain);
		            eval.evaluateModel(classifier, newTest);
		            temp_record[j]=eval.rootMeanSquaredError();
				}
				mean=getMean(temp_record);
				CI=getCI(temp_record,mean);
				error_record[count][0]=mean;
				error_record[count][1]=CI;
				count++;
			}
			errors.add(error_record);
		}
	}
	
	public void CSVPacker(){
		String mean_list="";
		String CI_list="";
		for(int i=0;i<errors.size();i++){
			String[] mean_temp=new String[errors.get(0).length];
			String[] CI_temp=new String[errors.get(0).length];
			for(int j=0;j<errors.get(0).length;j++){
				mean_temp[j]=Double.toString(errors.get(i)[j][0]);
				CI_temp[j]=Double.toString(errors.get(i)[j][1]);
			}
			mean_list+= String.join(",", mean_temp)+'\n';
			CI_list+= String.join(",", CI_temp)+'\n';
		}
		System.out.println(mean_list);
		System.out.println(CI_list);
	}
	
	public static double getMean(double[] results){
		double sum=0;
		for(double x:results){
			sum+=x;
		}
		return sum/results.length;
	}
	
	public static double getCI(double[] results, double mean){
		double var=0;
		for(double x:results){
			var+=Math.pow(x-mean, 2);
		}
		return Math.sqrt(var/results.length)/Math.sqrt(results.length);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			GibbsLabelRestriction eval= new GibbsLabelRestriction(10, 2, 30, 30, 2000);
			eval.run();
			eval.CSVPacker();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
