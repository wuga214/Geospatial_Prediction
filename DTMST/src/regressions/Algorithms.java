package regressions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.Sets;
import weka.classifiers.Classifier;

public class Algorithms {

	//singleton impl
	private static Algorithms instance;
	public synchronized static Algorithms getInstance(){
		if(instance == null)
			instance = new Algorithms();

		return instance;
	}
	public Algorithms(){}

	//create classifier to be used in the simulations 
	public Classifier createClassifier(ERegressionList EType) {
		Classifier ret = null;

		switch (EType) {
		case REGRESSION_TREE :
			ret = new FRT();
			break;

		case GAUSSIAN_PROCESS :
			ret = new GP();
			break;

		case KNN :
			ret = new KNN();
			break;
			
		case MINIMUM_SPANNING_TREE_MERGING :
			ret = new MAPofBMA(-200,-200,300,300);
			break;
		}

		return ret;
	}
	
	public Map<String,Set<String>> getDefaultClassifiersParameters(ERegressionList etype){
		Map<String,Set<String>> param = new HashMap<String,Set<String>>();

		switch (etype) {
		case REGRESSION_TREE :
			param.put("-M",Sets.newHashSet("-M 1","-M 2","-M 3","-M 4","-M 5"));
			param.put("-V",Sets.newHashSet("-V 0.001","-V 0.01","-V 0.1","-V 1","-V 10"));
			break;

		case GAUSSIAN_PROCESS :
			param.put("-L",Sets.newHashSet("-L 0.01","-L 0.1","-L 1","-L 10","-L 100"));
			param.put("-N", Sets.newHashSet("-N 1"));
			param.put("-K",Sets.newHashSet("-K weka.classifiers.functions.supportVector.RBFKernel","-K weka.classifiers.functions.supportVector.PolyKernel"));
			break;
			
		case KNN :
			param.put("-H",Sets.newHashSet("-K 1","-K 2","-K 3","-K 4","-K 5"));
			param.put("-F",Sets.newHashSet("-F"));
			break;

		case MINIMUM_SPANNING_TREE_MERGING :
			param.put("-I",Sets.newHashSet("-I 30","-I 60","-I 100","-I 130","-I 160","-I 200","-I 230","-I 260","-I 300","-I 330","-I 360","-I 400","-I 430","-I 460","-I 500"));
			param.put("-F",Sets.newHashSet("-F"));
			break;
		}

		return param;

	}
	
}