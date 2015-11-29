package evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import regressions.Algorithms;
import regressions.EProblemList;
import regressions.ERegressionList;
import regressions.MAPofBMA;
import regressions.Problems;
import utils.RegressionProblem;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.Resample;

public class CrossValidations {
	
	public static Evaluation crossValidation(Object classifier, Instances data,int folds) throws Exception{
		Evaluation eval = new Evaluation(data);
		for(int n=0;n<folds;n++){
			Classifier clsCopy = Classifier.makeCopy((Classifier)classifier);
			Instances train=data.trainCV(folds, n);
			Instances valid=data.testCV(folds, n);
			clsCopy.buildClassifier(train);
	        eval.evaluateModel(clsCopy, valid);
		}
//	      System.out.println();
//	      System.out.println("=== Setup run ===");
//	      System.out.println("Classifier: " + classifier.getClass().getName() + " " + Utils.joinOptions(((Classifier) classifier).getOptions()));
//	      System.out.println("Dataset: " + data.relationName());
//	      System.out.println("Folds: " + folds);
//	      System.out.println();
//	      System.out.println(eval.toSummaryString("=== " + folds + "-fold Cross-validation run ===", false));
	      return eval;
	}
	
	public static List<FoldRecord> batchCrossValidation(RegressionProblem cp) throws Exception{
		Instances data=cp.getData();
		Resample filter=new Resample();
		filter.setOptions(new String[]{"-Z","30","-no-replacement","-S","1"});
		filter.setInputFormat(data);
		Instances newTrain = Filter.useFilter(data, filter);
		filter.setOptions(new String[]{"-Z","30","-no-replacement","-S","3"});
        Instances newTest = Filter.useFilter(data, filter);
		Algorithms algo=new Algorithms();
		List<FoldRecord> probresults= new ArrayList<FoldRecord>();
		for(ERegressionList name:ERegressionList.values()){
			Map<String, Set<String>> params=algo.getDefaultClassifiersParameters(name);
			List<String> settings=SettingExtender.generateModels(params);
			List<FoldRecord> foldresult=new ArrayList<FoldRecord>();
			for(String setting:settings){
				Classifier classifier=algo.createClassifier(name);
				classifier.setOptions(setting.split("\\s+"));
				Evaluation eval=crossValidation(classifier,newTrain,10);
				FoldRecord record=new FoldRecord(name,setting,eval.correlationCoefficient(),eval.rootMeanSquaredError());
				foldresult.add(record);
			}
			Collections.sort(foldresult);
			FoldRecord bestSetting=foldresult.get(0);
			Classifier classifier=algo.createClassifier(bestSetting.name);
			classifier.setOptions(bestSetting.settings.split("\\s+"));
			classifier.buildClassifier(newTrain);
			Evaluation eval=new Evaluation(newTrain);
			eval.evaluateModel(classifier, newTest);
		      System.out.println();
		      System.out.println("=== Setup run ===");
		      System.out.println("Classifier: " + classifier.getClass().getName() + " " + Utils.joinOptions(((Classifier) classifier).getOptions()));
		      System.out.println("Dataset: " + data.relationName());
		      System.out.println();
		      System.out.println(eval.toSummaryString("=== test dataset result ===", false));
			probresults.add(new FoldRecord(bestSetting.name,bestSetting.settings,eval.correlationCoefficient(),eval.rootMeanSquaredError()));
			//#!
			// Here I need to apply best settings to whole training data and add that result into CVoutput list,
			// To be continue
		}
		return probresults;
	}
	
	public static CVOutput autobatchCrossValidation() throws Exception{
		CVOutput cvout=new CVOutput();
		Problems pbs=new Problems();
		for(EProblemList name:EProblemList.values()){
			RegressionProblem cp=pbs.createRegressionProblem(name);
			cvout.add(batchCrossValidation(cp),name.toString());
		}
		return cvout;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
//			RegressionProblem cp = new RegressionProblem("data/tobs-averages.arff");
//			Instances data=cp.getData();
//			Resample filter=new Resample();
//			filter.setOptions(new String[]{"-Z","30","-no-replacement","-S","1"});
//			filter.setInputFormat(cp.getData());
//			Instances newTrain = Filter.useFilter(cp.getData(), filter);
//			MAPofBMA classifier=new MAPofBMA(26,-124,24,70);
//			classifier.setOptions(new String[]{"-I","1"});
//			Evaluation ave=crossValidation(classifier,newTrain,10);
			CVOutput cvout=autobatchCrossValidation();
			System.out.println(cvout.getTable());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
