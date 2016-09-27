import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class LdaModel {

	int[][] doc;// word index array
	int V, K, M;// vocabulary size, topic number, document number
	int[][] z;// topic label array
	float alpha; // doc-topic dirichlet prior parameter
	float beta; // topic-word dirichlet prior parameter
	int[][] nmk;// given document m, count times of topic k. M*K
	float[][] nkt;// given topic k, count times of term t. K*V
	int[] nmkSum;// Sum for each row in nmk
	float[] nktSum;// Sum for each row in nkt
	float[][] phi;// Parameters for topic-word distribution K*V
	float[][] theta;// Parameters for doc-topic distribution M*K
	int iterations;// Times of iterations
	int saveStep;// The number of iterations between two saving
	int beginSaveIters;// Begin save model at this iteration
	float[][] similarScore;
	int ldaType;//0 LDA,1 GLDA
	double simThreshold;
//	List<List<Integer>> sortedTerms;
	int tokenFlag=1;//0 event,1 token
	int patternFlag=1;
	int samplingType=0;
	int dup=1;
	int bitTerm=0;
	int magofDict;
	int sparseMatrix=1;
	double lambda=1;
	int magofTopic;
	boolean[][] proBiterm;
	float[][] gamma;
	int[][] nmkBiterm;//counter of assign k as biterm in m
	int[] nmkBitermSum;
	
	public LdaModel(LdaGibbsSampling.modelparameters modelparam) {
		// TODO Auto-generated constructor stub
		alpha = modelparam.alpha;
		beta = modelparam.beta;
		iterations = modelparam.iteration;
		K = modelparam.topicNum;
		saveStep = modelparam.saveStep;
		beginSaveIters = modelparam.beginSaveIters;
		
		ldaType=modelparam.ldaType;
		simThreshold=modelparam.simThreshold;
		tokenFlag=modelparam.tokenFlag;
		patternFlag=modelparam.patternFlag;
		samplingType=modelparam.samplingType;
		dup=modelparam.dup;
		bitTerm=modelparam.biterm;
		magofDict=modelparam.magofDict;
		sparseMatrix=modelparam.sparseMatrix;
		lambda=modelparam.lambda;
		magofTopic=modelparam.magofTopic;
	}

	public void initializeModel(Documents docSet) {
		// TODO Auto-generated method stub
		
		M = docSet.docs.size();
		V = docSet.termToIndexMap.size();
		nmk = new int[M][K];
		nkt = new float[K][V];
		nmkSum = new int[M];
		nktSum = new float[K];
		phi = new float[K][V];
		theta = new float[M][K];
		
		similarScore=docSet.similarScore;

		// initialize documents index array
		doc = new int[M][];
		for (int m = 0; m < M; m++) {
			// Notice the limit of memory
			int N = docSet.docs.get(m).virtulTerms.length;
			doc[m] = new int[N];
			for (int n = 0; n < N; n++) {
				doc[m][n] = docSet.docs.get(m).virtulTerms[n];
			}
		}
		

		if (bitTerm==2) {//generete hyperparameter for proBiterm
			proBiterm=new boolean[M][];
			gamma=new float[M][];
			nmkBiterm=new int[M][K];
			nmkBitermSum=new int[M];
			for (int m = 0; m < M; m++) {
				int N = doc[m].length;
				proBiterm[m]=new boolean[N];
				gamma[m] = new float[N];
				for (int n = 0; n < doc[m].length; n++) {
					int wi=doc[m][n]/magofDict;
					int wj=doc[m][n]%magofDict;
					gamma[m][n] = (float)Math.min(Math.max(similarScore[wi][wj],0)+0.3,1);
					/*if (gamma[m][n]<1) {
						System.out.println(similarScore[wi][wj]);
					}*/
				}
			}
		}
		if (sparseMatrix==1) {//compress matrix for GPU
			similarScore=docSet.sparseSimMatrix();
		}
		
		// initialize topic lable z for each word
		z = new int[M][];
		for (int m = 0; m < M; m++) {
			int N = doc[m].length;
			z[m] = new int[N];
			for (int n = 0; n < N; n++) {
				if (bitTerm!=0) {
					int wi=doc[m][n]/magofDict;
					int wj=doc[m][n]%magofDict;
					if (bitTerm==2) {
						proBiterm[m][n]=Math.random()<gamma[m][n];//use same topic
						int zi=(int)(Math.random()*K),zj=proBiterm[m][n]?zi:((int)(Math.random()*K));
						nmkBiterm[m][zi]+=proBiterm[m][n]?1:0;
						nmkBitermSum[m]+=proBiterm[m][n]?1:0;
						z[m][n]=zi*magofTopic+zj;
						nkt[zi][wi]++;
						nkt[zj][wj]++;
						nktSum[zi]++;
						nktSum[zj]++;
						nmk[m][zi]++;
						nmk[m][zj]++;
						nmkSum[m]+=2;
						
						continue;
					}
					
				}
				
				int initTopic = (int) (Math.random() * K);// From 0 to K �C 1
				z[m][n] = initTopic;
				// number of words in doc m assigned to topic initTopic add 1
				nmk[m][initTopic]++;
				// total number of words assigned to topic initTopic add 1
				addkt(m, n, initTopic);
				/*if (ldaType==0){ 
					nktSum[initTopic]++;
					// number of terms doc[m][n] assigned to topic initTopic add 1
					nkt[initTopic][doc[m][n]]++;
				}
				if (ldaType==1){
					nktSum[initTopic]+=addToNktSum(m,n,initTopic);
					//nktSum[initTopic]++;
					nkt[initTopic][doc[m][n]]+=addToNktSum(m,n,initTopic);
					addToNktSum(m,n,initTopic);
				}*/
				nmkSum[m]++;
			}
			// total number of words in document m is N
			//nmkSum[m] = N;
		}
		
/*		for(int t=0;t<K;t++){
			for(int v=0;v<V;v++){
				nktSum[t]+=addToNktSum(v,t);
			}
		}*/
		
/*		for(int v1=0;v1<V;v1++){
			for(int v2=0;v2<V;v2++){
				similarScore[v1][v2]=docSet.similarScore[v1][v2];
			}
		}
*/	
		
	}

	public void inferenceModel(Documents docSet) throws IOException {
		// TODO Auto-generated method stub
		if (iterations < saveStep + beginSaveIters) {
			System.err
					.println("Error: the number of iterations should be larger than "
							+ (saveStep + beginSaveIters));
			System.exit(0);
		}
		for (int i = 0; i < iterations; i++) {
			System.out.println("Iteration " + i);
			if ((i >= beginSaveIters)
					&& (((i - beginSaveIters) % saveStep) == 0)) {
				// Saving the model
				System.out.println("Saving model at iteration " + i + " Over! ");
				// Firstly update parameters
				updateEstimatedParameters(docSet);
				
				// Secondly print model variables
				saveIteratedModel(i, docSet);
			}

			// Use Gibbs Sampling to update z[][]
			for (int m = 0; m < M; m++) {
				int N = doc[m].length;
				for (int n = 0; n < N; n++) {
					// Sample from p(z_i|z_-i, w)
					int newTopic = sampleTopicZ(m, n);
					z[m][n] = newTopic;
				}
			}
		}
	}
	private void updateEstimatedParameters(Documents docSet){
		float[][] ktCount=null;
		float[] ktSum=null;
		if (ldaType==0) {
			ktCount=nkt;
			ktSum=nktSum;
		}else {
			ktCount=new float[K][V];
			ktSum=new float[K];
			for (int m = 0; m < M; m++) {
				int N = doc[m].length;
				for (int n = 0; n < N; n++) {
					// Sample from p(z_i|z_-i, w)
					if (bitTerm==1) {
						int wi=doc[m][n]/magofDict;
						int wj=doc[m][n]%magofDict;
						ktCount[z[m][n]][wi]++;
						ktCount[z[m][n]][wj]++;
						ktSum[z[m][n]]+=2;
					}else if (bitTerm==0){
						ktCount[z[m][n]][doc[m][n]]++;
						ktSum[z[m][n]]++;
					}else {
						int zi=z[m][n]/magofTopic;
						int zj=z[m][n]%magofTopic;
						int wi=doc[m][n]/magofDict;
						int wj=doc[m][n]%magofDict;
						ktCount[zi][wi]++;
						ktCount[zj][wj]++;
						ktSum[zi]++;
						ktSum[zj]++;
					}
				}
			}
		}
		
		for (int k = 0; k < K; k++) {
			for (int t = 0; t < V; t++) {
				phi[k][t] = (ktCount[k][t] + beta) / (ktSum[k] + V * beta);
			}
		}
		for (int m = 0; m < M; m++) {
			for (int k = 0; k < K; k++) {
				theta[m][k] = (nmk[m][k]-(bitTerm==2?nmkBiterm[m][k]:0) + alpha) / (nmkSum[m]-(bitTerm==2?nmkBitermSum[m]:0) + K * alpha);
			}
		}
	}
	private void updateEstimatedParameters() {
		// TODO Auto-generated method stub
		for (int k = 0; k < K; k++) {
			for (int t = 0; t < V; t++) {
				phi[k][t] = (nkt[k][t] + beta) / (nktSum[k] + V * beta);
			}
		}

		for (int m = 0; m < M; m++) {
			for (int k = 0; k < K; k++) {
				theta[m][k] = (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
			}
		}
	}
	
	private void addkt(int m,int n,int k){
		if (bitTerm==1) {
			int wi=doc[m][n]/magofDict;
			int wj=doc[m][n]%magofDict;
			if (ldaType==0) {
				nkt[k][wi]++;
				nkt[k][wj]++;
				nktSum[k]+=2;
			}else{
				float tmpfenwi=0,tmpfenwj=0;
				if (sparseMatrix==1) {
					for(int v=0;v<V;v++){
						if (similarScore[wi][v]<0) 
							break;
						int index=(int)similarScore[wi][v];
						double simScore=similarScore[wi][v]-index;
						if (simScore==0) {
							index--;
							simScore=1;
						}
						tmpfenwi+=simScore*lambda;//*nkt[k][v];
						if (samplingType==1) {
							nkt[k][index]+=simScore*lambda;
						}
					}
					for(int v=0;v<V;v++){
						if (similarScore[wj][v]<0) 
							break;
						int index=(int)similarScore[wj][v];
						double simScore=similarScore[wj][v]-index;
						if (simScore==0) {
							index--;
							simScore=1;
						}
						tmpfenwj+=simScore*lambda;//*nkt[k][v];
						if (samplingType==1) {
							nkt[k][index]+=simScore*lambda;
						}
					}

				}else {
					for(int v=0;v<V;v++){
						if(similarScore[wi][v]>=simThreshold){
							tmpfenwi+=similarScore[wi][v]*lambda;//*nkt[k][v];
							if (samplingType==1) {
								nkt[k][v]+=similarScore[wi][v]*lambda;
							}
						}
						if(similarScore[wj][v]>=simThreshold){
							tmpfenwj+=similarScore[wj][v]*lambda;//*nkt[k][v];
							if (samplingType==1) {
								nkt[k][v]+=similarScore[wj][v]*lambda;
							}
						}
					}
				}
				
				if (samplingType==0) {
					nkt[k][wi]+=tmpfenwi;
					nkt[k][wj]+=tmpfenwj;
				}
				nktSum[k]+=tmpfenwi+tmpfenwj;
			}
			
		}else {
			if (ldaType==0) {
				nkt[k][doc[m][n]]++;
				nktSum[k]++;
			}else {
				float tmpfen=0;
				for(int v=0;v<V;v++){
					if (sparseMatrix==1) {
						if (similarScore[doc[m][n]][v]<0) 
							break;
						int index=(int)similarScore[doc[m][n]][v];
						double simScore=similarScore[doc[m][n]][v]-index;
						if (simScore==0) {
							index--;
							simScore=1;
						}
						tmpfen+=simScore*lambda;//*nkt[k][v];
						if (samplingType==1) {
							nkt[k][index]+=simScore*lambda;
						}
						continue;
					}
					if(similarScore[doc[m][n]][v]>=simThreshold){
						tmpfen+=similarScore[doc[m][n]][v]*lambda;//*nkt[k][v];
						if (samplingType==1) {
							nkt[k][v]+=similarScore[doc[m][n]][v]*lambda;
						}
					}
				}
				if (samplingType==0) {
					nkt[k][doc[m][n]]+=tmpfen;
				}
				nktSum[k]+=tmpfen;
			}
		}
	}
	private void subtractkt(int m,int n,int k){
		if (bitTerm==1) {
			int wi=doc[m][n]/magofDict;
			int wj=doc[m][n]%magofDict;
			if (ldaType==0) {
				nkt[k][wi]--;
				nkt[k][wj]--;
				nktSum[k]-=2;
			}else{
				float tmpfenwi=0,tmpfenwj=0;
				if (sparseMatrix==1) {
					for(int v=0;v<V;v++){
						if (similarScore[wi][v]<0) 
							break;
						int index=(int)similarScore[wi][v];
						double simScore=similarScore[wi][v]-index;
						if (simScore==0) {
							index--;
							simScore=1;
						}
						tmpfenwi+=simScore*lambda;//*nkt[k][v];
						if (samplingType==1) {
							nkt[k][index]-=simScore*lambda;
						}
					}
					for(int v=0;v<V;v++){
						if (similarScore[wj][v]<0) 
							break;
						int index=(int)similarScore[wj][v];
						double simScore=similarScore[wj][v]-index;
						if (simScore==0) {
							index--;
							simScore=1;
						}
						tmpfenwj+=simScore*lambda;//*nkt[k][v];
						if (samplingType==1) {
							nkt[k][index]-=simScore*lambda;
						}
					}

				}else {
					for(int v=0;v<V;v++){
						if(similarScore[wi][v]>=simThreshold){
							tmpfenwi+=similarScore[wi][v]*lambda;//*nkt[k][v];
							if (samplingType==1) {
								nkt[k][v]-=similarScore[wi][v]*lambda;
							}
						}
						if(similarScore[wj][v]>=simThreshold){
							tmpfenwj+=similarScore[wj][v]*lambda;//*nkt[k][v];
							if (samplingType==1) {
								nkt[k][v]-=similarScore[wj][v]*lambda;
							}
						}
					}
				}
				
				if (samplingType==0) {
					nkt[k][wi]-=tmpfenwi;
					nkt[k][wj]-=tmpfenwj;
				}
				nktSum[k]-=(tmpfenwi+tmpfenwj);
			}
			
		}else {
			if (ldaType==0) {
				nkt[k][doc[m][n]]--;
				nktSum[k]--;
			}else {
				float tmpfen=0;
				for(int v=0;v<V;v++){
					if (sparseMatrix==1) {
						if (similarScore[doc[m][n]][v]<0) 
							break;
						int index=(int)similarScore[doc[m][n]][v];
						double simScore=similarScore[doc[m][n]][v]-index;
						if (simScore==0) {
							index--;
							simScore=1;
						}
						tmpfen+=simScore*lambda;//*nkt[k][v];
						if (samplingType==1) {
							nkt[k][index]-=simScore*lambda;
						}
						continue;
					}
					if(similarScore[doc[m][n]][v]>=simThreshold){
						tmpfen+=similarScore[doc[m][n]][v]*lambda;//*nkt[k][v];
						if (samplingType==1) {
							nkt[k][v]-=similarScore[doc[m][n]][v]*lambda;
						}
					}
				}
				if (samplingType==0) {
					nkt[k][doc[m][n]]-=tmpfen;
				}
				nktSum[k]-=tmpfen;
			}
		}
	}
	private float addToNktSum(int m,int n,int k){
		float tmpfen=0;
		for(int v=0;v<V;v++){
			if(similarScore[doc[m][n]][v]>=simThreshold){
				tmpfen+=similarScore[doc[m][n]][v];//*nkt[k][v];
				if (samplingType==1) {
					nkt[k][v]+=similarScore[doc[m][n]][v];
				}
			}
		}
//		if(simThreshold==1)		System.out.println("equals: "+tmpfen);
		if (samplingType==0) {
			nkt[k][doc[m][n]]+=tmpfen;
		}
		nktSum[k]+=tmpfen;
		return tmpfen;
	}
	
	private float subtractFromNktSum(int m,int n,int k){
		/*float tmpfen=0;
		for(int v=0;v<V;v++){
			if(similarScore[doc[m][n]][v]>=simThreshold) tmpfen+=similarScore[doc[m][n]][v]*nkt[k][v];
		}
		return tmpfen;*/
		float tmpfen=0;
		for(int v=0;v<V;v++){
			if(similarScore[doc[m][n]][v]>=simThreshold){
				tmpfen+=similarScore[doc[m][n]][v];//*nkt[k][v];
				if (samplingType==1) {
					nkt[k][v]-=similarScore[doc[m][n]][v];
				}
			}
		}
//		if(simThreshold==1)		System.out.println("equals: "+tmpfen);
		if (samplingType==0) {
			nkt[k][doc[m][n]]-=tmpfen;
		}
		nktSum[k]-=tmpfen;
		return tmpfen;
	}
	
	private float addToNktSum(int v,int k){
		float tmpfen=0;
		for(int i=0;i<V;i++){
			if(similarScore[v][i]>=simThreshold) tmpfen+=similarScore[v][i]*nkt[k][i];
		}
		return tmpfen;
	}
	
	
	private float CalNktSum(int k){
		float tmpfen=0;
		for(int v=0;v<V;v++){
			for(int j=0;j<V;j++){
				if(similarScore[v][j]>=simThreshold) tmpfen+=similarScore[v][j]*nkt[k][j];
			}
		}
		return tmpfen;
	}

	private int sampleTopicZ(int m, int n) {
		// TODO Auto-generated method stub
		// Sample from p(z_i|z_-i, w) using Gibbs upde rule

		// Remove topic label for w_{m,n}
		if (bitTerm==2) {
			int zi=z[m][n]/magofTopic;
			int zj=z[m][n]%magofTopic;
			int wi=doc[m][n]/magofDict;
			int wj=doc[m][n]%magofDict;
			nkt[zi][wi]--;
			nkt[zj][wj]--;
			nktSum[zi]--;
			nktSum[zj]--;
			nmk[m][zi]--;
			nmk[m][zj]--;
			nmkSum[m]-=2;
			nmkBiterm[m][zi]-=proBiterm[m][n]?1:0;
			nmkBitermSum[m]-=proBiterm[m][n]?1:0;
			//proBiterm[m][n]=Math.random()<gamma[m][n];//use same topic
			//int zi=(int)(Math.random()*K),zj=proBiterm[m][n]?zi:((int)(Math.random()*K));
			//z[m][n]=zi*magofTopic+zj;
			
			//calculate g(zi,zj,lb)
			float[][][] gfunction=new float[2][K][K];
			for (int ki = 0; ki < K; ki++) {
				for (int kj = 0; kj < K; kj++) {
					gfunction[0][ki][kj]=(nmk[m][ki]-nmkBiterm[m][ki]+alpha)/(nmkSum[m]-nmkBitermSum[m]+K*alpha)*(nmk[m][kj]-nmkBiterm[m][kj]+alpha+(ki==kj?1:0))/(nmkSum[m]-nmkBitermSum[m]+K*alpha+1);
					/*if (ki==kj) {
						gfunction[0][ki][kj]=(nmk[m][ki]-nmkBiterm[m][ki]+alpha)/(nmkSum[m]-nmkBitermSum[m]+K*alpha)*(nmk[m][kj]-nmkBiterm[m][kj]+alpha+1)/(nmkSum[m]-nmkBitermSum[m]+K*alpha+1);	
					}else {
						gfunction[0][ki][kj]=(nmk[m][ki]-nmkBiterm[m][ki]+alpha)/(nmkSum[m]-nmkBitermSum[m]+K*alpha)*(nmk[m][kj]-nmkBiterm[m][kj]+alpha)/(nmkSum[m]-nmkBitermSum[m]+K*alpha+1);		
					}*/
				}
			}
			for (int ki = 0; ki < K; ki++) {
				gfunction[1][ki][ki]=(nmk[m][ki]-nmkBiterm[m][ki]+alpha)/(nmkSum[m]-nmkBitermSum[m]+K*alpha);
			}
			
			//sample for indicator lb
			float plb1=0,plb0=0;
			if (zi!=zj) {//plb1=0;
				proBiterm[m][n]=false;
			}else {
				plb1=gfunction[1][zi][zi]*gamma[m][n];
				plb0=gfunction[0][zi][zi]*(1-gamma[m][n]);
				proBiterm[m][n]=(Math.random()*(plb1+plb0))<plb1;
			}
			
			//sample for zi,zj
			float[][] pzij=new float[K][K];
			float preSum=0;
			for (int ki = 0; ki < K; ki++) {
				for (int kj = 0; kj < K; kj++) {
					if (kj == 0) {
						if (ki>0) {
							preSum=pzij[ki-1][K-1];
						}
					}else {
						preSum=pzij[ki][kj-1];
					}

					pzij[ki][kj]=(preSum)+(nkt[ki][wi]+beta)/(nktSum[ki] + V*beta)*(nkt[kj][wj]+beta+(wi==wj?1:0))/(nktSum[kj] + V*beta+(ki==kj?1:0))*gfunction[proBiterm[m][n]?1:0][ki][kj];
					
				}
			}
			double rd=Math.random()*pzij[K-1][K-1];
			boolean flag=true;
			for (int ki = 0; ki < K&&flag; ki++) {
				for (int kj = 0; kj < K&&flag; kj++) {
					if (rd<pzij[ki][kj]) {
						zi=ki;
						zj=kj;
						flag=false;
					}
				}
			}
			
			/*if (proBiterm[m][n]) {
				System.out.println("haha"+rd+"haha"+zi+"haha"+zj);
			}*/
			nkt[zi][wi]++;
			nkt[zj][wj]++;
			nktSum[zi]++;
			nktSum[zj]++;
			nmk[m][zi]++;
			nmk[m][zj]++;
			nmkSum[m]+=2;
			nmkBiterm[m][zi]+=proBiterm[m][n]?1:0;
			nmkBitermSum[m]+=proBiterm[m][n]?1:0;
			return zi*magofTopic+zj;
		}else{
			int oldTopic = z[m][n];
			nmk[m][oldTopic]--;
			nmkSum[m]--;
			subtractkt(m, n, oldTopic);
		}
		/*if (ldaType==0) {
			nkt[oldTopic][doc[m][n]]--;
			nktSum[oldTopic]--;
		}
		if(ldaType==1){
//			nktSum[oldTopic]-=subtractFromNktSum(m,n,oldTopic);
			nkt[oldTopic][doc[m][n]]-=addToNktSum(m,n,oldTopic);
			nktSum[oldTopic]-=addToNktSum(m,n,oldTopic);
			subtractFromNktSum(m,n,oldTopic);
		}*/

		// Compute p(z_i = k|z_-i, w)
		double[] p = new double[K];
		for (int k = 0; k < K; k++) {
			
			if (bitTerm==1) {
				int wi=doc[m][n]/magofDict;
				int wj=doc[m][n]%magofDict;
				p[k]=(nkt[k][wi]+beta)/(nktSum[k] + V*beta)*(nkt[k][wj]+beta+(wi==wj?1:0))/(nktSum[k] + V*beta+1)*(nmk[m][k]+alpha)/(nmkSum[m]+K*alpha);
			}else if(bitTerm==0){
				p[k]=(nkt[k][doc[m][n]]+beta)/(nktSum[k] + V*beta)*(nmk[m][k]+alpha)/(nmkSum[m]+K*alpha);
			}else{
				
			}
			/*if(ldaType==1){
				float tmpfen=addToNktSum(m,n,k);				
				p[k]=(tmpfen+beta)/(nktSum[k] + V*beta)*(nmk[m][k]+alpha)/(nmkSum[m]+K*alpha);
			}*/

		}

		// Sample a new topic label for w_{m, n} like roulette
		// Compute cumulated probability for p
		for (int k = 1; k < K; k++) {
			p[k] += p[k - 1];
		}
		
		double u =  Math.random() * p[K - 1]; // p[] is unnormalised
		int newTopic;
		for (newTopic = 0; newTopic < K; newTopic++) {
			if (u < p[newTopic]) {
				break;
			}
		}

		if(newTopic==50)
			System.out.println("error");
		// Add new topic label for w_{m, n}
		nmk[m][newTopic]++;
		nmkSum[m]++;
		addkt(m, n, newTopic);
		/*if(ldaType==0){
			nktSum[newTopic]++;
			nkt[newTopic][doc[m][n]]++;
		}
		if(ldaType==1){
			nktSum[newTopic]+=addToNktSum(m,n,newTopic);
			nkt[newTopic][doc[m][n]]+=addToNktSum(m,n,newTopic);
			addToNktSum(m,n,newTopic);
		}*/

		return newTopic;
	}

	public void saveIteratedModel(int iters, Documents docSet)
			throws IOException {
		// TODO Auto-generated method stub
		// lda.params lda.phi lda.theta lda.tassign lda.twords
		// lda.params
		String resPath = LdaGibbsSampling.resultPath;
		String modelName = "lda_" + iters;
		ArrayList<String> lines = new ArrayList<String>();
		lines.add("alpha = " + alpha);
		lines.add("beta = " + beta);
		lines.add("topicNum = " + K);
		lines.add("docNum = " + M);
		lines.add("termNum = " + V);
		lines.add("iterations = " + iterations);
		lines.add("saveStep = " + saveStep);
		lines.add("beginSaveIters = " + beginSaveIters);
		// FileUtil.writeLines(resPath + modelName + ".params", lines);??

		// lda.phi K*V
		 BufferedWriter writer = new BufferedWriter(new FileWriter(resPath + modelName + ".phi"));
		for (int i = 0; i < K; i++) {
			for (int j = 0; j < V; j++) {
				writer.write(phi[i][j] + "\t");
			}
			writer.write("\n");
		}
		writer.close();

		// lda.theta M*K
		writer = new BufferedWriter(new FileWriter(resPath + modelName +".theta"));
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < K; j++) {
				writer.write(theta[i][j] + "\t");
			}
			writer.write("\n");
		}
		writer.close();

		// lda.tassign
		writer = new BufferedWriter(new FileWriter(resPath + modelName
				+ ".tassign"));
		for (int m = 0; m < M; m++) {
			for (int n = 0; n < doc[m].length; n++) {
				writer.write(doc[m][n] + ":" + z[m][n] + "\t");
			}
			writer.write("\n");
		}
		writer.close();

		// lda.twords phi[][] K*V
		writer = new BufferedWriter(new FileWriter(resPath + modelName
				+ ".twords"));
		int topNum = 100; // Find the top 20 topic words in each topic
		for (int i = 0; i < K; i++) {
			List<Integer> tWordsIndexArray = new ArrayList<Integer>();
			for (int j = 0; j < V; j++) {
				tWordsIndexArray.add(new Integer(j));
			}
			Collections.sort(tWordsIndexArray, new LdaModel.TwordsComparable(
					phi[i]));
			writer.write("topic " + i + "\t:\t");
			
			if (tokenFlag==1) {
				for (int t = 0; t < topNum; t++) {
					writer.write(docSet.indexToTermMap.get(tWordsIndexArray.get(t)).toString()+ " " + phi[i][tWordsIndexArray.get(t)] + "\t");
				}
				writer.write("\n");
				continue;
			}
			if (patternFlag==0) {
				for (int t = 0; t < topNum; t++) {
					writer.write(docSet.indexToTermMap.get(tWordsIndexArray.get(t)).toString()+ " " + phi[i][tWordsIndexArray.get(t)] + "\t");
				}
				writer.write("\n");
				continue;
			}
			ArrayList<Integer>	docs=getTopKDocInTopic(i,10);
			for (int t = 0,count=0; t< V&&count < topNum; t++) {
				String eventPattern=docSet.indexToTermMap.get(tWordsIndexArray.get(t)).toString();
				String newEventPattern=getTopicEventPattern(docs,eventPattern);
				if(newEventPattern.equals("")) continue;
				count++;
				writer.write(newEventPattern+ " " + phi[i][tWordsIndexArray.get(t)] + "\t");
				writer.write("\n");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public class TwordsComparable implements Comparator<Integer> {

		public float[] sortProb; // Store probability of each word in topic k

		public TwordsComparable(float[] sortProb) {
			this.sortProb = sortProb;
		}

		@Override
		public int compare(Integer o1, Integer o2) {
			// TODO Auto-generated method stub
			// Sort topic word index according to the probability of each word
			// in topic k
			if (sortProb[o1] > sortProb[o2])
				return -1;
			else if (sortProb[o1] < sortProb[o2])
				return 1;
			else
				return 0;
		}
	}
	
	/*public void topicCoherence(Documents docSet,int topM){
		float[] tc=new float[K];
		for (int i = 0; i < K; i++) {
			List<Integer> tWordsIndexArray = new ArrayList<Integer>();
			for (int j = 0; j < V; j++) {
				tWordsIndexArray.add(new Integer(j));
			}
			Collections.sort(tWordsIndexArray, new LdaModel.TwordsComparable(
					phi[i]));

			for (int m = 1; m <= topM-1; m++) {
				for (int j = 0; j <m; j++) {
//					System.out.println(docSet.coTermDocCount[tWordsIndexArray.get(m)][tWordsIndexArray.get(j)]+"  "+docSet.termDocCount[tWordsIndexArray.get(j)]+docSet.indexToTermMap.get(m)+"  "+docSet.indexToTermMap.get(j));
					tc[i]+=Math.log(((float)(docSet.coTermDocCount[tWordsIndexArray.get(m)][tWordsIndexArray.get(j)]+1))/docSet.termDocCount[tWordsIndexArray.get(j)]);
				}
			}
			System.out.println("topic coherence of topic "+i+": "+tc[i]);
		}
		
	}*/
	public double topicCoherence(Documents docSet,int topM){
		PrintWriter writer=null,chainWriter=null,graphChainWriter=null,edgeWriter=null;
		try {
			writer=new PrintWriter(new OutputStreamWriter(new FileOutputStream(LdaGibbsSampling.resultPath+"topic Coherenct.txt", true), "utf8"));
			//writer.println(new Date());
			chainWriter=new PrintWriter(new OutputStreamWriter(new FileOutputStream(LdaGibbsSampling.resultPath+"eventChains.txt", false), "utf8"));
			chainWriter.println(new Date());
			graphChainWriter=new PrintWriter(new OutputStreamWriter(new FileOutputStream(LdaGibbsSampling.resultPath+"eventChainsByGraph.txt", false), "utf8"));
			graphChainWriter.println(new Date());
			edgeWriter=new PrintWriter(new OutputStreamWriter(new FileOutputStream(LdaGibbsSampling.resultPath+"eventEdges.txt", false), "utf8"));
			edgeWriter.println(new Date());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		float[] tc=new float[K];
		float AvgTC=0;

		for (int i = 0; i < K; i++) {
			List<Integer> tWordsIndexArray = new ArrayList<Integer>();
			for (int j = 0; j < V; j++) {
				tWordsIndexArray.add(new Integer(j));
			}
			Collections.sort(tWordsIndexArray, new LdaModel.TwordsComparable(
					phi[i]));
//			for (int j = 0; j < V; j++){
//				sortedTerms[i][j]=tWordsIndexArray.get(j);
//			}
			
			for (int m = 1; m <= topM-1; m++) {
				for (int j = 0; j <m; j++) {
//					System.out.println(docSet.coTermDocCount[tWordsIndexArray.get(m)][tWordsIndexArray.get(j)]+"  "+docSet.termDocCount[tWordsIndexArray.get(j)]+docSet.indexToTermMap.get(m)+"  "+docSet.indexToTermMap.get(j));
					tc[i]+=Math.log(((float)(docSet.coTermDocCount[tWordsIndexArray.get(m)][tWordsIndexArray.get(j)]+1))/docSet.termDocCount[tWordsIndexArray.get(j)]);
				}
			}
			System.out.println("topic coherence of topic "+i+": "+tc[i]);
			AvgTC+=tc[i];
			if(writer!=null)
				writer.println("topic coherence of topic "+i+": "+tc[i]);
			
			
			/*ArrayList<Integer>	topdocs=getTopKDocInTopic(i,20);
			
			float[] tWordsIMPScore=new float[topM];
			for(int m=0;m<topM;m++){
				int eventIndex=tWordsIndexArray.get(m);
				float eventTopicProb=phi[i][eventIndex];
				float eventInDocProb=0.0f;
				int eventInDocCount=0;
				int totalEventInDocCount=0;
				for(int ii=0;ii<docSet.docs.size();ii++){
					
					if(topdocs.contains(ii)==false) continue;
					totalEventInDocCount++;
					int AllEventCountIndoc=docSet.docs.get(ii).docWords.length;
					int curEventCountIndoc=0;
					for(int kk=0;kk<docSet.docs.get(ii).docWords.length;kk++){
						if(docSet.docs.get(ii).docWords[kk]==eventIndex)
							curEventCountIndoc++;
					}
					eventInDocCount+=(curEventCountIndoc>0)?1:0;
					float docTopicProb=theta[ii][i];
					if(AllEventCountIndoc>0){
						eventInDocProb+=curEventCountIndoc*1.0/AllEventCountIndoc*docTopicProb;	
					}					
				}
				
				tWordsIMPScore[m]=eventTopicProb*eventInDocProb*(eventInDocCount*1.0f/totalEventInDocCount);	
				System.out.println(tWordsIMPScore[m]);
			}
			List<Integer> tImpWordsIndexArray = new ArrayList<Integer>();
			for (int j = 0; j < topM; j++) {
				tImpWordsIndexArray.add(new Integer(j));				
			}
			Collections.sort(tImpWordsIndexArray, new LdaModel.TwordsComparable(tWordsIMPScore));
			int chainLen=10;
			
			
			ArrayList<Integer> tTopImpWordsIndex=new ArrayList<Integer>();
			for(int j=0,count=0;j<topM&&count<chainLen;j++){
				boolean found=false;
				for(int kk=0;kk<tTopImpWordsIndex.size();kk++){
					if(docSet.similarScore[tWordsIndexArray.get(tImpWordsIndexArray.get(j))][tTopImpWordsIndex.get(kk)]>0.5){
						found=true;
						break;
					}
				}
				if(found==false){
					tTopImpWordsIndex.add(tWordsIndexArray.get(tImpWordsIndexArray.get(j)));
					System.out.println(docSet.indexToTermMap.get(tWordsIndexArray.get(tImpWordsIndexArray.get(j)))+","+tWordsIMPScore[tImpWordsIndexArray.get(j)]);
					count++;
				}
			}
			
			//ArrayList<Integer> sortedIndexesInHead=getEventChain(docSet, topM, tWordsIndexArray,i);
			edgeWriter.print("topic " + i + "\t:\t");
//			ArrayList<Integer> topoEvents=getEventChainByGraph(docSet, topM, tWordsIndexArray, i,edgeWriter);
			ArrayList<Integer> topoEvents=getEventChainByGraph(docSet,chainLen,tTopImpWordsIndex,tWordsIMPScore, i,edgeWriter);
			edgeWriter.print("\n");
			chainWriter.print("topic " + i + "\t:\t");
			graphChainWriter.print("topic " + i + "\t:\t");
			for (int t = 0; t < chainLen; t++) {
				//chainWriter.write(docSet.indexToTermMap.get(sortedIndexesInHead.get(t)).toString()+ " " + phi[i][sortedIndexesInHead.get(t)] + " " +docSet.averOrderofTerms.get(docSet.indexToTermMap.get(sortedIndexesInHead.get(t)))+"\t");
				graphChainWriter.write(docSet.indexToTermMap.get(topoEvents.get(t)).toString()+ " " + phi[i][topoEvents.get(t)] + " " +"\t");
			}
			chainWriter.print("\n");
			graphChainWriter.print("\n");*/
		}
		
		AvgTC/=K;
		System.out.println("Average topic coherence : "+AvgTC);
		if(writer!=null)
			writer.println("Average topic coherence : "+AvgTC);
	
		/*double klDivergence=calculateKLDivergence();
		System.out.println("Average KL-divergence : "+klDivergence);
		if(writer!=null)
			writer.println("Average KL-divergence : "+klDivergence);
		
		double perplexity=calculatePerplexity(docSet);
		System.out.println("perplexity : "+perplexity);
		if(writer!=null)
			writer.println("perplexity : "+perplexity);*/

		if(writer!=null){
			writer.close();
		}
		if(chainWriter!=null){
			chainWriter.close();
		}
		if(graphChainWriter!=null){
			graphChainWriter.close();
		}
		if(edgeWriter!=null){
			edgeWriter.close();
		}
		return AvgTC;
	}
	
	
	public double calculatePerplexity(Documents docSet){
		double result=0.0;
		int wordCount=0;
		for (int m = 0; m < M; m++) {			
			for (int n = 0; n < docSet.docs.get(m).docWords.length/dup; n++) {
				int termIndex=docSet.docs.get(m).docWords[n];
				double tmpResult=0.0;
				for(int k=0;k<K;k++){
					double topicDoc=theta[m][k];
					double topicTerm=phi[k][termIndex];
					tmpResult+=topicDoc*topicTerm;					
				}
				result+=Math.log(tmpResult);
			}
			wordCount+=docSet.docs.get(m).docWords.length/dup;
		}
		
		System.out.println("wordcount+"+wordCount);
		return Math.exp(-1*result/wordCount);
	}
	public double calculateKLDivergence(){
		double result=0.0;
		int count=0;
		for (int i = 0; i < K; i++){
			for(int j=0;j<K;j++){
				if(i==j) continue;
				double KLDiverItoJ=0;
				for (int k = 0; k < V; k++) {
					KLDiverItoJ+=Math.log(phi[i][k]/phi[j][k])*phi[i][k];
				}
				System.out.println(i+" to "+j+": "+KLDiverItoJ);
				count++;
				result+=KLDiverItoJ;
			}
		}
		return result/count;
	}
	public void classfyDocs(Documents docSet){
		/*ArrayList<ArrayList<Integer>> classDocsLists=new ArrayList<ArrayList<Integer>>(K);
		for(int j=0;j<K;j++){
			classDocsLists.add(new ArrayList<Integer>());
		}
		for(int i=0;i<M;i++){
			int topicOfDoc=0;
			for(int j=0;j<K;j++){
				if (theta[i][j]>=theta[i][topicOfDoc]) {
					topicOfDoc=j;
				}
			}
			classDocsLists.get(topicOfDoc).add(i);
		}
		docSet.classDocsLists=classDocsLists;*/
		
	}
	
	public ArrayList<Integer> getTopKDocInTopic(int topicID,int K){
		ArrayList<Integer> docs=new ArrayList<Integer>(K);
		MinHeap heap=new MinHeap(K);
		for(int i=0;i<M;i++){
			heap.AddNew(new DocTopicDistribution(i,theta[i][topicID]));			
		}
		for(int i=0;i<K;i++){
			docs.add(heap.array[i].docID);
		}
		return docs;
	}
	
	public String getTopicEventPattern(ArrayList<Integer> docs,String eventPattern){
		String newPattern="";
		ArrayList<Event> eventList=LdaGibbsSampling.docSet.eventInTerms.get(eventPattern);
		ArrayList<String> subs=new ArrayList<String>();
		ArrayList<String> preds=new ArrayList<String>();
		ArrayList<String> objs=new ArrayList<String>();
		for(int i=0;i<eventList.size();i++){
			Event curEvent=eventList.get(i);
			if(docs.indexOf(curEvent.docIndex)==-1) continue;
			if(curEvent.Subject!="\"\""&&subs.contains(curEvent.Subject)==false) subs.add(curEvent.Subject);
			if(curEvent.Predicate!="\"\""&&preds.contains(curEvent.Predicate)==false) preds.add(curEvent.Predicate);
			if(curEvent.Object!="\"\""&&objs.contains(curEvent.Object)==false) objs.add(curEvent.Object);
		}
		
		if(preds.size()==0) return "";
		String subStr="";
		String predStr="";
		String objStr="";
		if(subs.size()>0){
			for(int i=0;i<subs.size()-1;i++){
				subStr+=subs.get(i)+",";
			}
			subStr+=subs.get(subs.size()-1);
		}
		else subStr="\"\"";
		
		if(preds.size()>0){
			for(int i=0;i<preds.size()-1;i++){
				predStr+=preds.get(i)+",";
			}
			predStr+=preds.get(preds.size()-1);
		}
		else predStr="\"\"";
		
		if(objs.size()>0){
			for(int i=0;i<objs.size()-1;i++){
				objStr+=objs.get(i)+",";
			}
			objStr+=objs.get(objs.size()-1);
			}
		else objStr="\"\"";
		
		String[] tmpStr=eventPattern.split("\\[|\\]");	
		newPattern="{";
		newPattern+="["+subStr+"]["+tmpStr[3];
		newPattern+="]},{["+predStr+"]["+tmpStr[7];
		newPattern+="]},{["+objStr+"]["+tmpStr[11];
		newPattern+="]}";
		
		return newPattern;
	}
	
	public ArrayList<Integer> getEventChain(Documents docSet,int chainLen,List<Integer> sortedIndexes,int topicK){
		ArrayList<Integer> sortedIndexesInHead=new ArrayList<>(sortedIndexes.subList(0, chainLen));
		Collections.sort(sortedIndexesInHead, new OrderCompare(docSet,topicK));
		return sortedIndexesInHead;
	}
	public ArrayList<Integer> getEventChainByGraph(Documents docSet,int chainLen,List<Integer> termIndexesofK,float[] tWordsIMPScore,int topicK,PrintWriter edgeWriter){
		int[][] preOrderCount=docSet.preOrderCount;	
		ArrayList<Edge> edges=new ArrayList<Edge>();
		ArrayList<GraphNode> verts=new ArrayList<GraphNode>();
		ArrayList<ArrayList<Integer>> linksList=new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> inDegrees=new ArrayList<Integer>();
		
		for (int i = 0; i < chainLen; i++) {			
			verts.add(new GraphNode(i,termIndexesofK.get(i),"",tWordsIMPScore[i]));
			linksList.add(new ArrayList<Integer>());
			inDegrees.add(0);			
		}
		
		
		for (int i = 0; i < chainLen; i++) {
			for(int j=i+1;j<chainLen;j++){
				int countij=preOrderCount[termIndexesofK.get(i)][termIndexesofK.get(j)];
				int countji=preOrderCount[termIndexesofK.get(j)][termIndexesofK.get(i)];
				/*if (countij>countji) {
					edges.add(new Edge(termIndexesofK.get(i), termIndexesofK.get(j), countij-countji));
				}else if(countij<countji){
					edges.add(new Edge(termIndexesofK.get(j), termIndexesofK.get(i), countji-countij));
				}*/
				if (countij==0&&countji==0) {
					continue;
				}
				double itoj=(countij+1)/docSet.termCountMap.get(docSet.indexToTermMap.get(termIndexesofK.get(i)));
				double jtoi=(countji+1)/docSet.termCountMap.get(docSet.indexToTermMap.get(termIndexesofK.get(j)));
				if(Math.abs(itoj/jtoi-1)<0.3) 
					continue;
				
				if (countij>countji) {
					edges.add(new Edge(i, j, itoj));
					linksList.get(i).add(j);
					inDegrees.set(j, inDegrees.get(j)+1);
				}else if(countij<countji){
					edges.add(new Edge(j, i, jtoi));
					linksList.get(j).add(i);
					inDegrees.set(i, inDegrees.get(i)+1);
				}
				
				edgeWriter.print(docSet.indexToTermMap.get(termIndexesofK.get(i))+"->"+docSet.indexToTermMap.get(termIndexesofK.get(j))+" "+preOrderCount[i][j]+"--");
				edgeWriter.print(docSet.indexToTermMap.get(termIndexesofK.get(j))+"->"+docSet.indexToTermMap.get(termIndexesofK.get(i))+" "+preOrderCount[j][i]+"\t");
				
			}
		}
		TermOrderGraph graph=new TermOrderGraph();
		graph.verts=verts;
		graph.linksList=linksList;
		graph.edges=edges;
		graph.inDegrees=inDegrees;
		ArrayList<GraphNode> nodeList=graph.convert2DAG().topologySort();
		ArrayList<Integer> vertList=new ArrayList<Integer>();
		for(GraphNode indexNode:nodeList){
			vertList.add(indexNode.index);
		}
		return vertList;
		
	}
	class Edge{
		int start,end;
		double w;
		public Edge(int start, int end, double w) {
			super();
			this.start = start;
			this.end = end;
			this.w = w;
		}
		
	}
	class TermOrderGraph{
		ArrayList<GraphNode> verts;
		ArrayList<ArrayList<Integer>> linksList;
		ArrayList<Integer> inDegrees;
		ArrayList<Edge> edges;
		public TermOrderGraph convert2DAG(){
			TermOrderGraph dagGraph=new TermOrderGraph();
			/*ArrayList<Integer> verts=this.verts;
			ArrayList<Edge> newEdges=new ArrayList<Edge>();
			ArrayList<Integer> inDegrees=new ArrayList<Integer>();
			ArrayList<ArrayList<Integer>> linksList=new ArrayList<ArrayList<Integer>>();*/
			dagGraph.verts=this.verts;
			dagGraph.edges=new ArrayList<Edge>();
			dagGraph.inDegrees=new ArrayList<Integer>();;
			dagGraph.linksList=new ArrayList<ArrayList<Integer>>();
			for (int i = 0; i < verts.size(); i++) {
				dagGraph.linksList.add(new ArrayList<Integer>());
				dagGraph.inDegrees.add(0);
			}
			Collections.sort(this.edges, new Comparator<Edge>() {
				@Override
				public int compare(Edge o1, Edge o2) {
					// TODO Auto-generated method stub
					if (o1.w>o2.w) {
						return -1;
					}else if(o1.w<o2.w){
						return 1;
					}
					return 0;
				}
			});
			for(Edge edge:this.edges){
				if(!dagGraph.isConnect(edge.end, edge.start)){
					dagGraph.edges.add(edge);
					dagGraph.linksList.get(edge.start).add(edge.end);
					dagGraph.inDegrees.set((edge.end),dagGraph.inDegrees.get(edge.end)+1);
				}
			}
			return dagGraph;
		}
		private boolean isConnect(int i,int j){
			List<Integer> indexesInDepth=new ArrayList<Integer>();
			List<Integer> path=new ArrayList<Integer>();
			path.add(i);
			int depth=1;
			while(depth>0){
				if (indexesInDepth.size()==depth) {
					indexesInDepth.set(depth-1, indexesInDepth.get(depth-1)+1);
				}else {
					indexesInDepth.add(0);
					path.add(0);
				}
				if(indexesInDepth.get(depth-1)>=linksList.get(path.get(depth-1)).size()){
					indexesInDepth.remove(depth-1);
					path.remove(depth);
					depth--;
				}else{
					int curVert=linksList.get(path.get(depth-1)).get(indexesInDepth.get(depth-1));
					path.set(depth, curVert);
					depth++;
					if (curVert==j) {
						return true;
					}
				}
			}
			return false;
		}
		public ArrayList<GraphNode> topologySort(){
			Queue<GraphNode> queue=new LinkedList<GraphNode>();
			ArrayList<GraphNode> result=new ArrayList<GraphNode>();
			ArrayList<Integer> tempInDegrees=new ArrayList<Integer>(inDegrees);
			for(int i=0;i<tempInDegrees.size();i++){
				if (tempInDegrees.get(i)==0) {
					queue.add(this.verts.get(i));
				}
			}

			Collections.sort((LinkedList<GraphNode>)queue, new GraphNodeComparator()); 
			
			while(queue.isEmpty()==false){
				GraphNode curNode=queue.poll();
				result.add(curNode);
				for(int nextIndex:linksList.get(curNode.id)){
					tempInDegrees.set(nextIndex, tempInDegrees.get(nextIndex)-1);
					if (tempInDegrees.get(nextIndex)==0) {
						queue.add(this.verts.get(nextIndex));
					}
				}
			
				Collections.sort((LinkedList<GraphNode>)queue, new GraphNodeComparator()); 

			}	
			return result;
		}
	}
	
	
	class GraphNode{
		int id;
		int index;
		float importance;
		String eventPattern;
		public GraphNode(){}
		public GraphNode(int _id,int _index,String _eventPattern,float _importance){
			this.id=_id;
			this.index=_index;
			this.importance=_importance;
			this.eventPattern=_eventPattern;
		}
	}
	
	class GraphNodeComparator implements Comparator<GraphNode> {
		  @Override

		  public int compare(GraphNode o1, GraphNode o2 ) {
			  if(o1.importance>o2.importance)
				  return 1;
			  else if (o1.importance<o2.importance)
				  return -1;
			  else
				  return 0;

		  }
		}
	
	class OrderCompare implements Comparator<Integer>{
		Documents docSet;
		ArrayList<Integer> classDocsList;
		public OrderCompare(Documents docSet,int topicK){
			this.docSet=docSet;
			this.classDocsList=docSet.classDocsLists.get(topicK);
		}
		@Override
		public int compare(Integer o1, Integer o2) {
			// TODO Auto-generated method stub
			double aveOrder1,aveOrder2;
			if ((aveOrder1=docSet.averOrderofTerms.get(docSet.indexToTermMap.get(o1)))==0.0) {
				List<Event> events1=docSet.eventInTerms.get(docSet.indexToTermMap.get(o1));
				int order1=0,count1=0;
				for(Event event:events1){
					if (classDocsList.contains(event.docIndex)){
						order1+=event.LineNo;
						count1++;
					}
				}
				aveOrder1=count1!=0?((double)order1)/count1:V;
				//docSet.indexToTermMap.get(o1).aveOrder=aveOrder1;
				docSet.averOrderofTerms.put(docSet.indexToTermMap.get(o1), aveOrder1);
			}
			if ((aveOrder2=docSet.averOrderofTerms.get(docSet.indexToTermMap.get(o2)))==0.0) {
				List<Event> events2=docSet.eventInTerms.get(docSet.indexToTermMap.get(o2));
				int order2=0,count2=0;
				for(Event event:events2){
					if (classDocsList.contains(event.docIndex)){
						order2+=event.LineNo;
						count2++;
					}
				}
				aveOrder2=count2!=0?((double)order2)/count2:V;
				docSet.averOrderofTerms.put(docSet.indexToTermMap.get(o2),aveOrder2);
			}	
			if(aveOrder1>aveOrder2)
				return 1;
			else if(aveOrder1<aveOrder2)
				return -1;
			return 0;	
		}
		
	}
}
