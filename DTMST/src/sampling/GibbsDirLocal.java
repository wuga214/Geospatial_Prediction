package sampling;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import delaunay.BowyerWatson;
import regressions.KNN;
import structure.DEdge;
import structure.DPoint;
import utils.InstancesToPoints;
import utils.RegressionProblem;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.Resample;

/**
 * @author Wuga
 * Need to embed Gibbs voronoi merging classifier into WEKA Classifier to run cross validation for later convenience
 * Trained model by maximize likelihood, choose model by RMSE and output is also RMSE
 */
public class GibbsDirLocal {
	public ModelManager Manager;
	public Instances oregData;
	public Instances validating;
	public int iteration;
	public SampleManager samples;
	public double currentSampleWeight;
	public HashMap<Integer, HashSet<Integer>> voronoiMapping;
	
	
	public GibbsDirLocal(Instances data, Instances valid, int iter, int lablenum, SampleManager samp) throws Exception{
		Manager=new ModelManager(data,lablenum);
		oregData=data;
		validating=valid;
		iteration=iter;
		samples=samp;
		Manager.findNearestNeighbour(validating);
		ArrayList<DPoint> points=InstancesToPoints.transfer(Manager.modeledData);
		BowyerWatson bw=new BowyerWatson(-200,-200,300,300,points);
		HashSet<DEdge> full_edges=bw.getPrunEdges();
		voronoiMapping = getVoronoiMapping(full_edges, points);
	}
	
	//Gibbs Sampling outer iterations: number of iterations and dimension selection without random selection(tuning around)
	
	/**
	 * Here the parameter need is for plot, which is not related to Gibbs Samping!!
	 * @param whole data
	 * @throws Exception
	 */
	public void Sampling(Instances wholedata, boolean debug) throws Exception{
		//System.out.println("Number of training data:"+oregData.numInstances());
		//Gibbs Sampling outer layer: iteration of full dimensions route
		for(int i=0;i<iteration;i++){
			//System.out.println("iteration:"+(i+1));
			//System.out.println("Number of Segmentations:"+Manager.segmentations.size());
			//Gibbs Samping inner layer: sampling for each dimension
			for(int j=0;j<oregData.numInstances();j++){
				//taking sample value of the dimension through singleDimensionSampling() function
				int sdsIndex=singleDimensionSampling(j);
				Manager.flipCellAssignment(j, sdsIndex, oregData);
				Manager.removeEmptySegments();
				//only sample 10 models!
//				System.out.println(sdsIndex);
//				for(int q=0;q<Manager.segmentations.size();q++){
//					System.out.println("Seg_"+q+" contains "+Manager.segmentations.get(q).cells.toString());
//				}
			}
			if(i%5.0==0.0){
				if((iteration-i)<=200){
					samples.addSample(Manager.deepCopySegmentations());
				}
				
				if(debug==true){
					Instances labeled = new Instances(wholedata);
					for (int k = 0; k < wholedata.numInstances(); k++) {
						//bug founded here! the training instance value is changed to segmentation index!!!!
						double clsLabel = Manager.classifyInstance(wholedata.instance(k));
						labeled.instance(k).setClassValue(clsLabel);
					}
					// save labeled data
					BufferedWriter writer = new BufferedWriter(
							new FileWriter("outputs/Gibbs/iteration_"+i+".arff"));
					writer.write(labeled.toString());
					writer.newLine();
					writer.flush();
					writer.close();
				}
			}
		}
	}
	
	//Single Dimension Conditional Distribution, Sample value of single dimension given values from other dimensions fixed.
	public int singleDimensionSampling(int dimIndex) throws Exception{
		int segSize=Manager.segmentations.size();
		double[] logLikelihood=new double[segSize];
		double largest=-Double.MAX_VALUE;
		double partition=0;
		double alpha=0.01;
		//for(int i=0;i<segSize;i++){
		HashSet<Integer> voronoineighbors = voronoiMapping.get(dimIndex);
		int[] subsegindexes = new int[voronoineighbors.size()+1];
		int j=0;
		for(int k:voronoineighbors){
			subsegindexes[j]=Manager.segmentTracker[k];
			j++;
		}
		subsegindexes[j]= segSize-1;
		double n=Manager.segmentTracker.length;
		for(int i:subsegindexes){
			Manager.flipCellAssignment(dimIndex, i, oregData);
			double ni=Manager.segmentations.get(i).cells.size();
			if(ni==1){
				logLikelihood[i]=Manager.getLogLikelihood(validating,i)+Math.log(alpha/(n-1+alpha));
			}else{
				logLikelihood[i]=Manager.getLogLikelihood(validating,i)+Math.log((ni-1)/(n-1+alpha));
			}
			if(logLikelihood[i]>largest){
				largest=logLikelihood[i];
			}
		}
		double exps=0;
		for(int i=0;i<logLikelihood.length;i++){
			exps+=Math.exp(logLikelihood[i]-largest);
		}
		partition=largest+Math.log(exps);
		//log likelihood now tune into likelihood, even still using name loglikelihood
		double[] likeLihood=new double[logLikelihood.length];
		double sum=0;
		for(int i=0;i<likeLihood.length;i++){
			likeLihood[i]=Math.exp(logLikelihood[i]-partition);
			sum+=likeLihood[i];
		}
		
		//Uniformly distributed random value
		//cumulative density larger than this value, then the last candidate added is selected as sample 
		double rand=Math.random();
		double cumuDensity=0;
		int sampleIndex=-1;
		for(int i=0;i<segSize;i++){
			cumuDensity+=likeLihood[i];
			if(rand<=(cumuDensity/sum)){
				sampleIndex=i;
				break;
			}
		}
//		if(sampleIndex==-1){
//			System.out.println("encounter error that sampled index is -1, current cumuDensity is "+cumuDensity);
//			System.out.println("Log Likelihood:"+Arrays.toString(logLikelihood));
//			}
		//System.out.println("Sample Logged Likelihood:"+currentSampleWeight);
		return sampleIndex;
		/*
		 * 1 find sampled segmentation index;
		 * 2 update ModelManger by the selected segmentation index;
		 * 3 clean up empty segmentations
		 */
	}
	
	public HashMap<Integer, HashSet<Integer>> getVoronoiMapping(HashSet<DEdge> edges, ArrayList<DPoint> points){
		HashMap<Integer, HashSet<Integer>> voronoi_neighbor_mapping = new HashMap<Integer, HashSet<Integer>>();
		for(DPoint point:points){
			HashSet<Integer> neighbor_index=new HashSet<Integer>(); 
			for(DEdge edge:edges){
				if(edge.contains(point)){
					neighbor_index.add((int)edge.p[0].value);
					neighbor_index.add((int)edge.p[1].value);
				}
			}
			neighbor_index.remove((int)point.value);
			voronoi_neighbor_mapping.put((int)point.value, neighbor_index);
		}
		return voronoi_neighbor_mapping;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			RegressionProblem cp;
	        SampleManager samp=new SampleManager();
			cp = new RegressionProblem("data/box.arff");
	        Resample filter=new Resample();
	        filter.setOptions(new String[]{"-Z","20","-no-replacement","-S","1"});
	        filter.setInputFormat(cp.getData());
	        Instances newTrain = Filter.useFilter(cp.getData(), filter); 
	        filter.setOptions(new String[]{"-Z","20","-no-replacement","-S","2"});
	        Instances newTest = Filter.useFilter(cp.getData(), filter); 
	        GibbsDirLocal gb=new GibbsDirLocal(newTrain, newTrain, 1000,newTrain.numInstances(), samp);
	        gb.Sampling(cp.getData(),true);
	        gb.Manager.writeFile("Gibbs");
	        samp.showSampleSize();
	        samp.createBaggingModel(newTrain);
	        samp.batchPrediction(cp.getData());
	        System.out.println("All results are under output/Gibbs folder");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
