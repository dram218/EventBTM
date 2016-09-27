import java.awt.RenderingHints.Key;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.buaa.edu.wordsimilarity.WordSimilarity;

public class Documents {
	ArrayList<Document> docs;
	
	Map<String, Integer> termToIndexMap;
	ArrayList<String> indexToTermMap;
	Map<String, Integer> termCountMap=null;
	float[][] similarScore=null;
	HashMap<String, HashMap<String, Float>> WNsimilarityScore=null;
	Map<String,ArrayList<Event>> eventInTerms;
	Map<String,Double> averOrderofTerms;
	ArrayList<String> wordsList;
	
	WordVectors wvectors=null;
	
//	int[][] coTermDocCount;
	float[][] coTermDocCount;
	int[] termDocCount;
	int[][] preOrderCount; 
	ArrayList<ArrayList<Integer>> classDocsLists;
	
//	ArrayList<Event> allPatterns=new ArrayList<Event>();
	ArrayList<Integer> allConfindPatterns=new ArrayList<Integer>();
	ArrayList<Integer> curConfindPatterns=new ArrayList<Integer>();
	int allEventsNum=0,remainEventsNum=0,allNewEventNum=0;
	static Double seedPatternThreshold=3.0;
	Hashtable<String,ArrayList<ArrayList<DependencyRelation>>> allRelation=null;
	public String getEventPattern(Event event){
		return event.eventPattern;
	}

	
	public String getBaseEvent(Event event){

		//System.out.println(this.eventInTerms.size());
		//System.out.println(event.eventPattern);
		if(this.eventInTerms.containsKey(event.eventPattern))
				return event.eventPattern;		
		return null;
	}

	
	public Documents() {
		docs = new ArrayList<Document>();
		termToIndexMap = new HashMap<String, Integer>();
		indexToTermMap = new ArrayList<String>();
		termCountMap = new HashMap<String, Integer>();
		
		eventInTerms = new HashMap<String,ArrayList<Event>>();
		averOrderofTerms = new HashMap<String,Double>();
		wordsList=new ArrayList<String>();
	}
	public float[][] sparseSimMatrix(){
		float[][] sparseSim=new float[indexToTermMap.size()][];
		float[] tempSim=new float[indexToTermMap.size()];
		for(int i=0;i<indexToTermMap.size();i++){
			int premer=0;
			for(int j=0;j<indexToTermMap.size();j++){
				if (similarScore[i][j]>=simThreshold) {
					//similarScore[i][premer]=j+similarScore[i][j];
					tempSim[premer]=j+similarScore[i][j];
					premer++;
				}
			}
			/*if(premer<indexToTermMap.size()){
				similarScore[i][premer]=-0.1f;
			}*/
			sparseSim[i]=new float[premer+1];
			for(int j=0;j<premer;j++){
				sparseSim[i][j]=tempSim[j];
			}
			sparseSim[i][premer]=-0.1f;
		}
		return sparseSim;
	}
	 
	public void GetSimilarScore() throws Exception{
		File simScoreFile=new File("simScoreFile"+tokenFlag+patternFlag+".obj");//+LdaGibbsSampling.topics.replaceAll("\\|", "_")
		
		if (simScoreFile.exists()) {
			ObjectInputStream objectIn=new ObjectInputStream(new FileInputStream(simScoreFile));
			similarScore=(float[][])objectIn.readObject();
			objectIn.close();
			return;
		}
		
		similarScore=new float[indexToTermMap.size()][indexToTermMap.size()];
		
//		if(new File("WordVectors.obj").exists()){
//			wvectors=(WordVectors)DeSerielizeObject("WordVectors.obj");
//		}
		if(new File("WordVectors.obj").exists()){
			wvectors=(WordVectors)DeSerielizeObject("WordVectors.obj");
		}
		else{
			wvectors=new WordVectors();
			wvectors.ReadWordVector();
		}
		System.out.println("Read Word Embedding Over!");
		
		float[] word1=new float[wvectors.vectorLen]; 
		float[] word2=new float[wvectors.vectorLen]; 
		float[] tmpWord=new float[wvectors.vectorLen]; 

/*		if(new File("WNSimilarScore.obj").exists()){
			this.WNsimilarityScore=(HashMap<String, HashMap<String, Float>>)DeSerielizeObject("WNSimilarScore.obj");
		}
		else {
			this.WNsimilarityScore=new HashMap<String, HashMap<String, Float>>();
		}
		WordNetTool.initWordNet();		
*/		
		for(int i=0;i<indexToTermMap.size();i++){
			similarScore[i][i]=1;
			for(int j=i+1;j<indexToTermMap.size();j++){
				String term1=this.indexToTermMap.get(i);
				String term2=this.indexToTermMap.get(j);
				
				if (tokenFlag==1) {
					float score=CalSimilarScoreByWord(term1, term2);
					similarScore[i][j]=score;
					similarScore[j][i]=score;
					continue;
				}
				if (patternFlag==0) {
					//float score=CalSimilarScoreByCSMultiply(eventInTerms.get(term1).get(0), eventInTerms.get(term2).get(0));
					Event event1=eventInTerms.get(term1).get(0),event2=eventInTerms.get(term2).get(0);
					Event event1_1=null,event1_2=null,event2_1=null,event2_2=null;
					if (event1.Subject.equals("\"\"")) {
						event1_1=new Event(new String[]{event1.Object,event1.Predicate,"\"\""});
					}else if (event1.Object.equals("\"\"")) {
						event1_1=event1;
					}else{
						event1_1=new Event(new String[]{event1.Subject,event1.Predicate,"\"\""});
						event1_2=new Event(new String[]{event1.Object,event1.Predicate,"\"\""});
					}
					if (event2.Subject.equals("\"\"")) {
						event2_1=new Event(new String[]{event2.Object,event2.Predicate,"\"\""});
					}else if (event2.Object.equals("\"\"")) {
						event2_1=event2;
					}else{
						event2_1=new Event(new String[]{event2.Subject,event2.Predicate,"\"\""});
						event2_2=new Event(new String[]{event2.Object,event2.Predicate,"\"\""});
					}
					float score=0,sim11=-1,sim12=-1,sim21=-1,sim22=-1;
					sim11=CalSimilarScoreByCSMultiply(event1_1,event2_1);
					sim12=CalSimilarScoreByCSMultiply(event1_1,event2_2);
					sim21=CalSimilarScoreByCSMultiply(event1_2,event2_1);
					sim22=CalSimilarScoreByCSMultiply(event1_2,event2_2);
					score=Math.max(sim11, Math.max(sim12, Math.max(sim21, sim22)));
					similarScore[i][j]=score>0?score:0;
					similarScore[j][i]=similarScore[i][j];
					continue;
				}
				ArrayList<Event> events1=this.eventInTerms.get(term1);
				ArrayList<Event> events2=this.eventInTerms.get(term2);
				
				
//				float score=CalSimilarScore(event1, event2);
//				float score=CalSimilarScoreByCS(event1, event2,word1,word2,tmpWord);
				
				
				float score=0.0f;
				for(int kk=0;kk<events1.size();kk++){
					for(int kkk=0;kkk<events2.size();kkk++){
						Event event1=events1.get(kk);
						Event event2=events2.get(kkk);
						float tmp=CalSimilarScoreByCSMultiply(event1, event2);
						//System.out.println(event1.toString()+","+event2.toString()+":"+tmp);
						if(tmp>score)
							score=tmp;
					}
				}				
				
				similarScore[i][j]=score;
				similarScore[j][i]=score;
				//if(score>0.4)	System.out.println(term1+"\t"+term2+"\t"+score);
			}
			//System.out.println(i);
		}
		System.out.println("CalSimilarScore Over!");
		if (!simScoreFile.exists()) {
			ObjectOutputStream objectOut=new ObjectOutputStream(new FileOutputStream(simScoreFile));
			objectOut.writeObject(similarScore);
			objectOut.close();
		}
//		if(new File("WordVectors.obj").exists()==false){
//			SerielizeObject(this.wvectors, "WordVectors.obj");
//		}
		
/*		if(new File("WNSimilarScore.obj").exists()==false){
			SerielizeObject(this.WNsimilarityScore, "WNSimilarScore.obj");
		}
*/	}
	public float CalSimilarScoreByWord(String str1,String str2){
		float[] word1=new float[wvectors.vectorLen]; 
		float[] word2=new float[wvectors.vectorLen]; 
		if(wvectors.vectors.containsKey(str1)==false){
			//System.out.println(event1.SubjectNer);
		}
		else{
			for(int i=0;i<wvectors.vectors.get(str1).size();i++){
				word1[i]=wvectors.vectors.get(str1).get(i);
			}
		}
		if(wvectors.vectors.containsKey(str2)==false){
			//System.out.println(event1.SubjectNer);
		}
		else{
			for(int i=0;i<wvectors.vectors.get(str2).size();i++){
				word2[i]=wvectors.vectors.get(str2).get(i);
			}
		}
		return GetVectorScore(word1, word2);
	}
	public float CalSimilarScoreByCS(Event event1,Event event2,float[] word1,float[] word2,float[] tmpWord){
		float score=0;
		if(event1.Subject.equals("\"\"")==false){
			String[] subjectStr=event1.Subject.split("\\s");
			for(int ii=0;ii<wvectors.vectorLen;ii++) tmpWord[ii]=1.0f;
			for(int ii=0;ii<subjectStr.length;ii++){
				if(wvectors.vectors.containsKey(subjectStr[ii])==false)
					System.out.println(subjectStr[ii]);
				else
					for(int i=0;i<wvectors.vectorLen;i++){
						tmpWord[i]*=wvectors.vectors.get(subjectStr[ii]).get(i);
				}
			}
			for(int i=0;i<wvectors.vectorLen;i++){
				word1[i]=0.2f*tmpWord[i];
			}
		}
		if(event1.Predicate.equals("\"\"")==false){
			if(wvectors.vectors.containsKey(event1.Predicate)==false)
				System.out.println(event1.Predicate);
			else
				for(int i=0;i<wvectors.vectors.get(event1.Predicate).size();i++){
					tmpWord[i]=wvectors.vectors.get(event1.Predicate).get(i);
			}	
			for(int i=0;i<wvectors.vectorLen;i++){
				word1[i]+=0.6f*tmpWord[i];
			}
		}
		if(event1.Object.equals("\"\"")==false){
			String[] objectStr=event1.Object.split("\\s");
			for(int ii=0;ii<wvectors.vectorLen;ii++) tmpWord[ii]=1.0f;
			for(int ii=0;ii<objectStr.length;ii++){
				if(wvectors.vectors.containsKey(objectStr[ii])==false)
					System.out.println(objectStr[ii]);
				else
					for(int i=0;i<wvectors.vectorLen;i++){
						tmpWord[i]*=wvectors.vectors.get(objectStr[ii]).get(i);
				}
			}
			for(int i=0;i<wvectors.vectorLen;i++){
				word1[i]+=0.2f*tmpWord[i];
			}
		}		
		
		if(event2.Subject.equals("\"\"")==false){
			String[] subjectStr=event2.Subject.split("\\s");
			for(int ii=0;ii<wvectors.vectorLen;ii++) tmpWord[ii]=1.0f;
			for(int ii=0;ii<subjectStr.length;ii++){
				if(wvectors.vectors.containsKey(subjectStr[ii])==false)
					System.out.println(subjectStr[ii]);
				else
					for(int i=0;i<wvectors.vectorLen;i++){
						tmpWord[i]*=wvectors.vectors.get(subjectStr[ii]).get(i);
				}
			}
			for(int i=0;i<wvectors.vectorLen;i++){
				word2[i]=0.2f*tmpWord[i];
			}
		}
		if(event1.Predicate.equals("\"\"")==false){
			if(wvectors.vectors.containsKey(event2.Predicate)==false)
				System.out.println(event2.Predicate);
			else
				for(int i=0;i<wvectors.vectors.get(event2.Predicate).size();i++){
					tmpWord[i]=wvectors.vectors.get(event2.Predicate).get(i);
			}	
			for(int i=0;i<wvectors.vectorLen;i++){
				word2[i]+=0.6f*tmpWord[i];
			}
		}
		if(event2.Object.equals("\"\"")==false){
			String[] objectStr=event2.Object.split("\\s");
			for(int ii=0;ii<wvectors.vectorLen;ii++) tmpWord[ii]=1.0f;
			for(int ii=0;ii<objectStr.length;ii++){
				if(wvectors.vectors.containsKey(objectStr[ii])==false)
					System.out.println(objectStr[ii]);
				else
					for(int i=0;i<wvectors.vectorLen;i++){
						tmpWord[i]*=wvectors.vectors.get(objectStr[ii]).get(i);
				}
			}
			for(int i=0;i<wvectors.vectorLen;i++){
				word2[i]+=0.2f*tmpWord[i];
			}
		}		
		score=GetVectorScore(word1, word2);
		return score;
		
	}
	
	public float[] VectorMultiply(float[] word1,float[] word2){
		float[] tmpWord=new float[wvectors.vectorLen];
		for(int i=0;i<wvectors.vectorLen;i++){
			tmpWord[i]+=word1[i]*word2[i];
		}
		return tmpWord;
	}
	
	public float[][] DicalMultiply(float[] word1,float[] word2){
		float[][] multVector=new float[wvectors.vectorLen][wvectors.vectorLen];
		for(int i=0;i<wvectors.vectorLen;i++){
			for(int j=0;j<wvectors.vectorLen;j++){
				multVector[i][j]=word1[i]*word2[j];
			}
		}
		return multVector;
	}
	
	public float[] VectorAndMatrixMultiply(float[] word1,float[][] word2){
		float[] result=new float[wvectors.vectorLen];
		float tmp=0.0f;
		for(int i=0;i<wvectors.vectorLen;i++){
			tmp=0.0f;
			for(int j=0;j<wvectors.vectorLen;j++){
				tmp+=word1[i]*word2[j][i];
			}
			result[i]=tmp;
		}
		return result;
	}
	public float CalSimilarScoreByCSMultiply(Event event1,Event event2){
		if (event1==null||event2==null) {
			return -1;
		}
		if (tokenFlag==1) {
			float[] word1=new float[wvectors.vectorLen]; 
			float[] word2=new float[wvectors.vectorLen]; 
			if(wvectors.vectors.containsKey(event1.eventPattern)==false){
				//System.out.println(event1.SubjectNer);
			}
			else{
				for(int i=0;i<wvectors.vectors.get(event1.eventPattern).size();i++){
					word1[i]=wvectors.vectors.get(event1.eventPattern).get(i);
				}
			}
			if(wvectors.vectors.containsKey(event2.eventPattern)==false){
				//System.out.println(event1.SubjectNer);
			}
			else{
				for(int i=0;i<wvectors.vectors.get(event2.eventPattern).size();i++){
					word2[i]=wvectors.vectors.get(event2.eventPattern).get(i);
				}
			}
			return GetVectorScore(word1, word2);
		}
		
		float score=0;
		float[] word1=new float[wvectors.vectorLen]; 
		float[] word2=new float[wvectors.vectorLen]; 
		float[] word3=new float[wvectors.vectorLen]; 
		float[] word4=new float[wvectors.vectorLen]; 
		float[] word5=new float[wvectors.vectorLen]; 
		float[] word6=new float[wvectors.vectorLen]; 
		float[] tmpWord1=null; 
		float[] tmpWord2=null; 
		
		int event1Subj=0,event1Pred=0,event1Obj=0,event2Subj=0,event2Pred=0,event2Obj=0;
		
		for(int i=0;i<wvectors.vectorLen;i++){word1[i]=1;word2[i]=1;word3[i]=1;}
		if(event1.Subject.equals("\"\"")==false){
			if(wvectors.vectors.containsKey(event1.Subject)==false){
				//System.out.println(event1.SubjectNer);
			}
			else{
				for(int i=0;i<wvectors.vectors.get(event1.Subject).size();i++){
					word1[i]=wvectors.vectors.get(event1.Subject).get(i);
				}
				event1Subj=1;
			}
		}
		
		if(event1.Predicate.equals("\"\"")==false){
			if(wvectors.vectors.containsKey(event1.Predicate)==false){
				//System.out.println(event1.Predicate);
			}
			else{
				for(int i=0;i<wvectors.vectors.get(event1.Predicate).size();i++){
					word2[i]=wvectors.vectors.get(event1.Predicate).get(i);
				}
				event1Pred=1;
			}			
		}
		
		if(event1.Object.equals("\"\"")==false){
			if(wvectors.vectors.containsKey(event1.Object)==false){
				//System.out.println(event1.ObjectNer);
			}
			else{
				for(int i=0;i<wvectors.vectors.get(event1.Object).size();i++){
					word3[i]=wvectors.vectors.get(event1.Object).get(i);
				}
				event1Obj=1;
			}			
		}

		

		for(int i=0;i<wvectors.vectorLen;i++){word4[i]=1;word5[i]=1;word6[i]=1;}
		if(event2.Subject.equals("\"\"")==false){
			if(wvectors.vectors.containsKey(event2.Subject)==false){
				//System.out.println(event2.SubjectNer);
			}
			else{
				for(int i=0;i<wvectors.vectors.get(event2.Subject).size();i++){
					word4[i]=wvectors.vectors.get(event2.Subject).get(i);
				}
				event2Subj=1;
			}
		}
		
		if(event2.Predicate.equals("\"\"")==false){
			if(wvectors.vectors.containsKey(event2.Predicate)==false){
				//System.out.println(event2.Predicate);
			}
			else{
				for(int i=0;i<wvectors.vectors.get(event2.Predicate).size();i++){
					word5[i]=wvectors.vectors.get(event2.Predicate).get(i);
				}
				event2Pred=1;
			}			
		}
		
		if(event2.Object.equals("\"\"")==false){
			if(wvectors.vectors.containsKey(event2.Object)==false){
				//System.out.println(event2.ObjectNer);
			}
			else{
				for(int i=0;i<wvectors.vectors.get(event2.Object).size();i++){
					word6[i]=wvectors.vectors.get(event2.Object).get(i);
				}
				event2Obj=1;
			}			
		}
		
		if(event1Subj==1&&event2Subj==1&&event1Pred==1&&event2Pred==1&&event1Obj==1&&event2Obj==1){
			tmpWord1=VectorAndMatrixMultiply(word2,DicalMultiply(word1,word3));
			tmpWord2=VectorAndMatrixMultiply(word5,DicalMultiply(word4,word6));
			score=GetVectorScore(tmpWord1, tmpWord2);
			return score;
		}
		else if(event1Subj==1&&event2Subj==1&&event1Pred==1&&event2Pred==1){
			tmpWord1=VectorMultiply(word1,word2);
			tmpWord2=VectorMultiply(word4,word5);
			score=GetVectorScore(tmpWord1, tmpWord2);
			/*float score2=0.0f;
			if(event1Obj==1){
				tmpWord1=VectorMultiply(word3,word2);
				tmpWord2=VectorMultiply(word4,word5);
				score2=GetVectorScore(tmpWord1, tmpWord2);
				score=score>score2?score:score2;
				tmpWord1=VectorMultiply(word1,word3);
				tmpWord2=VectorMultiply(word4,word5);
				score2=GetVectorScore(tmpWord1, tmpWord2);
				score=score>score2?score:score2;
			}
			else if(event2Obj==1){
				tmpWord1=VectorMultiply(word1,word2);
				tmpWord2=VectorMultiply(word6,word5);
				score2=GetVectorScore(tmpWord1, tmpWord2);
				score=score>score2?score:score2;
				tmpWord1=VectorMultiply(word1,word2);
				tmpWord2=VectorMultiply(word4,word5);
				score2=GetVectorScore(tmpWord1, tmpWord2);
				score=score>score2?score:score2;
			}*/
			
			return score;
		}
		else if(event1Pred==1&&event2Pred==1&&event1Obj==1&&event2Obj==1){
			tmpWord1=VectorMultiply(word2,word3);
			tmpWord2=VectorMultiply(word5,word6);
			score=GetVectorScore(tmpWord1, tmpWord2);
			/*float score2=0.0f;
			if(event1Subj==1){
				tmpWord1=VectorMultiply(word1,word2);
				tmpWord2=VectorMultiply(word5,word6);
				score2=GetVectorScore(tmpWord1, tmpWord2);
				score=score>score2?score:score2;
				tmpWord1=VectorMultiply(word1,word3);
				tmpWord2=VectorMultiply(word5,word6);
				score2=GetVectorScore(tmpWord1, tmpWord2);
				score=score>score2?score:score2;
			}
			else if(event2Subj==1){
				tmpWord1=VectorMultiply(word2,word3);
				tmpWord2=VectorMultiply(word5,word4);
				score2=GetVectorScore(tmpWord1, tmpWord2);
				score=score>score2?score:score2;
				tmpWord1=VectorMultiply(word2,word3);
				tmpWord2=VectorMultiply(word6,word4);
				score2=GetVectorScore(tmpWord1, tmpWord2);
				score=score>score2?score:score2;
			}*/
			return score;
		}
		else{
			//System.out.println("haha"+event1.eventPattern+tmpWord1+" "+event2.eventPattern+tmpWord2);
			return 0;
		}
	}
	
	public float GetVectorScore(float[] word1,float[] word2){
		float score=0.0f;
		for(int i=0;i<wvectors.vectorLen;i++){
			score+=word1[i]*word2[i];
		}
		
		float ss=0.0f;
		for(int i=0;i<word1.length;i++)
			ss+=word1[i]*word1[i];
		ss=(float)Math.sqrt(ss);
		
		score/=ss;
		
		for(int i=0;i<word2.length;i++)
			ss+=word2[i]*word2[i];
		ss=(float)Math.sqrt(ss);
		
		score/=ss;
		
		return score;
	}
	
	public float GetWNScore(String w1,String w2){
		if(this.WNsimilarityScore.containsKey(w1)){
			if(this.WNsimilarityScore.get(w1).containsKey(w2)){
				return this.WNsimilarityScore.get(w1).get(w2);
			}
		}
		if(this.WNsimilarityScore.containsKey(w2)){
			if(this.WNsimilarityScore.get(w2).containsKey(w1)){
				return this.WNsimilarityScore.get(w2).get(w1);
			}
		}
		
		return -1;
		
	}
	
	public float CalWordSimilar(String word1,String word2,String pos){
		double tmp=0;
		
		if(word1.equals("\"\"")||word2.equals("\"\"")) return 0;
		
		tmp=GetWNScore(word1, word2);
		if(tmp==-1){
			tmp=WordNetTool.GetSimilarScoreByLin(word1,word2,pos);
			if(this.WNsimilarityScore.containsKey(word1))
				this.WNsimilarityScore.get(word1).put(word2, (float)tmp);
			else{
				this.WNsimilarityScore.put(word1, new HashMap<String,Float>());
				this.WNsimilarityScore.get(word1).put(word2, (float)tmp);
			}
		}
		return (float) tmp;
	}
	
	public float CalSimilarScore(Event event1,Event event2) throws Exception{
/*		float score=0;
		if(event1.SubjectNer.equals("\"\"")==false&&event1.SubjectNer.equals(event2.SubjectNer)) score++;
		if(event1.Predicate.equals(event2.Predicate)) score++;
		if(event1.ObjectNer.equals("\"\"")==false&&event1.ObjectNer.equals(event2.ObjectNer)) score++;
		score=score/3;
*/		
		float score=0;
		double tmp=0,tmp1=0,tmp2=0,max=0;
		int count=0;
		
		if(event1.Predicate.equals("\"\"")==false&&event2.Predicate.equals("\"\"")==false){
			tmp=CalWordSimilar(event1.Predicate,event2.Predicate,"v");
			score+=0.6*tmp;	
		}
		tmp1=CalWordSimilar(event1.Subject,event2.Subject,"n");
		tmp2=CalWordSimilar(event1.Subject,event2.Object,"n");
		if(tmp1>tmp2){
			tmp=CalWordSimilar(event1.Object,event2.Object,"n");
			score+=0.2*tmp1+0.2*tmp;			
		}
		else{
			tmp=CalWordSimilar(event1.Object,event2.Subject,"n");
			score+=0.2*tmp2+0.2*tmp;			
		}
		return score;
	}
	
	public void readDocs(String docsPath) throws Exception {
		//int i=0;
		File[] files=new File(docsPath).listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				// TODO Auto-generated method stub 016|020|027|057
				if(name.matches("^("+LdaGibbsSampling.topics+")[0-9]{3}(\\.txt)?"))//050|009|051|  088|112|000|010|045|047|016|020|027|057
					return true;
				return false;
			}
		});
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				// TODO Auto-generated method stub
				return o1.getName().compareTo(o2.getName());
			}
		});
		//HashMap<String, Integer> docNums=new HashMap<String, Integer>();
		for (File docFile : files) {
			/*String topicId=docFile.getName().substring(0,3);
			Integer count=null;
			if ((count=docNums.get(topicId))!=null) {
				docNums.put(topicId, count+1);
			}else {
				docNums.put(topicId, 1);
			}
			if (true) {
				continue;
			}*/
			Document doc = new Document(docFile.getAbsolutePath(),this);
			docs.add(doc);
			
			allEventsNum+=doc.events.size();
			remainEventsNum+=doc.events.size();
			//System.out.println("eventsNums:"+allEventsNum);
			//System.out.println(LdaGibbsSampling.originalDocsPath+docFile.getName().toString());
		}
		/*List<Entry<String, Integer>> topicNumEntrys=new ArrayList<Entry<String, Integer>>(docNums.entrySet());
		Collections.sort(topicNumEntrys, new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1,
					Entry<String, Integer> o2) {
				// TODO Auto-generated method stub
				return o1.getValue()-o2.getValue();
			}
		});
		for(Entry<String, Integer> entry:topicNumEntrys){
			System.out.println(entry.getKey()+"  "+entry.getValue());
		}*/
	}
/*	public Documents filtDocs(){
		ArrayList<Event> terms2remove=new ArrayList<Event>();
		ArrayList<Integer> termIndexs2remove=new ArrayList<Integer>();
		for(Event event:termCountMap.keySet()){
			int termCount=0;
			if ((termCount=termCountMap.get(event))<2) {
				terms2remove.add(event);
				termIndexs2remove.add(this.termToIndexMap.get(event));
			}
		}
		System.err.println("removed event num:"+terms2remove.size());
		Documents filtedDocs=new Documents();
//		filtedDocs.
		for(Document doc:this.docs){
			filtedDocs.docs.add(doc.filtDoc(filtedDocs,termIndexs2remove,this));
		}
		return filtedDocs;
	}*/
	public void countTermDoc(){
//		coTermDocCount=new int[indexToTermMap.size()][indexToTermMap.size()];
		if (similarScore!=null) {
			coTermDocCount=similarScore;
			for(int i=0;i<indexToTermMap.size();i++){
				for(int j=0;j<indexToTermMap.size();j++){
					coTermDocCount[i][j]=0;
				}
			}
		}else {
			coTermDocCount=new float[indexToTermMap.size()][indexToTermMap.size()];
		}
		
		termDocCount=new int[indexToTermMap.size()];
//		preOrderCount=new int[indexToTermMap.size()][indexToTermMap.size()];
		for(Document doc:docs){
			HashMap<Integer, Integer> termCountsOfDoc=new HashMap<Integer, Integer>();
			for (int i = 0; i < doc.docWords.length/dup; i++) {
				Integer termCount=null;
				if ((termCount=termCountsOfDoc.get(doc.docWords[i]))==null) {
					termCountsOfDoc.put(doc.docWords[i], 1);
				}else {
					termCountsOfDoc.put(doc.docWords[i], termCount+1);
				}
				/*for(int j=i+1;j<doc.docWords.length;j++){
					preOrderCount[doc.docWords[i]][doc.docWords[j]]++;
				}*/
			}
			ArrayList<Integer> termIndexsOfDoc=new ArrayList<Integer>(termCountsOfDoc.keySet());
			for (int i = 0; i < termIndexsOfDoc.size(); i++) {
				termDocCount[termIndexsOfDoc.get(i)]++;
				for (int j = i+1; j < termIndexsOfDoc.size(); j++) {
					coTermDocCount[termIndexsOfDoc.get(i)][termIndexsOfDoc.get(j)]++;
					coTermDocCount[termIndexsOfDoc.get(j)][termIndexsOfDoc.get(i)]++;
				}
			}
		}
		
	}

	/**
	 * 闁跨喐鏋婚幏閿嬪瘹闁跨喐鏋婚幏鐑芥晸娓氥儳銆嬮幏鐑芥晸閺傘倖瀚归柨鐔告灮閹风兘鏁撻崣顐＄串閹风兘鏁撻弬銈嗗闁跨喐鏋婚幏锟�
	 * @param fileName
	 * @return
	 * @throws Exception
	 * @throws IOException
	 */
	public static Object DeSerielizeObject(String fileName) throws Exception, IOException{
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
		Object obj = in.readObject();
		in.close();
		return obj;
	}
	
	/**
	 * 闁跨喐鏋婚幏鐑芥晸閺傘倖瀚归柨鐔告灮閹风兘鏁撶悰妤呮交閹风兘鏁撻弬銈嗗閸曠喖鏁撻敓锟�
	 * @param obj闁跨喐鏋婚幏鐑芥晸閺傘倖瀚归柨鐔告灮閹风兘鏁撻弬銈嗗閾诲拋鍎為弸姘舵晸閺傘倖瀚归柨鐔告灮閹风兘鏁撶徊鐖塸lements Serializable闁跨喐鏋婚幏鐑芥晸閺傘倖瀚归柨鐔告灮閹峰嘲鍟搘riteObject闁跨喐鏋婚幏绌渆adObject闁跨喐鏋婚幏鐑芥晸閺傘倖瀚�
	 * @param fileName闁跨喐鏋婚幏鐑芥晸閺傘倖瀚归惄顕�鏁撻弬銈嗗闁跨喍鑼庣涵閿嬪闁跨喐鏋婚幏锟�
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void SerielizeObject(Object obj,String fileName) throws FileNotFoundException, IOException{
		ObjectOutputStream oo = null;
		oo = new ObjectOutputStream(new FileOutputStream(fileName));
		oo.writeObject(obj);		
		oo.close();
	}

	public void changedEventsOut()throws Exception{
		PrintWriter writer=null;
		writer=new PrintWriter(new OutputStreamWriter(new FileOutputStream("changedEvents2.txt"),"utf8"));
		for(Document doc:docs){
			for(Event event:doc.events){
				if (event.isChanged==1) {
					writer.println(event.changeStr());
				}
			}
		}
		writer.close();
	}

	public class TermCountComparator implements Comparator<Integer>{
		Documents docSet;
		public TermCountComparator(Documents docSet){
			this.docSet=docSet;
		}
		@Override
		public int compare(Integer o1, Integer o2) {
			// TODO Auto-generated method stub
			if(docSet.termCountMap.get(docSet.indexToTermMap.get(o1))>docSet.termCountMap.get(docSet.indexToTermMap.get(o2)))
				return -1;
			else if(docSet.termCountMap.get(docSet.indexToTermMap.get(o1))<docSet.termCountMap.get(docSet.indexToTermMap.get(o2))){
				return 1;
			}
			return 0;
		}
		
	}
	
//	ArrayList<Integer> features=new ArrayList<Integer>();
//	int[] featureFlags=new int[indexToTermMap.size()];
	int[] term2Feature=null;
	static abstract class FeatureSelector{
		abstract int featureSelect(int featureIndex);
	}
	static class PhiSelector extends FeatureSelector{
		HashSet<Integer> highPhiTermSet=new HashSet<Integer>();
		public PhiSelector(LdaModel lda,int topK){
			for (int i = 0; i < lda.K; i++) {
				List<Integer> tWordsIndexArray = new ArrayList<Integer>();
				for (int j = 0; j < lda.V; j++) {
					tWordsIndexArray.add(new Integer(j));
				}
				Collections.sort(tWordsIndexArray, lda.new TwordsComparable(lda.phi[i]));
				for(int k=0;k<topK;k++){
					highPhiTermSet.add(tWordsIndexArray.get(k));
				}
			}
		}
		@Override
		int featureSelect(int featureIndex) {
			// TODO Auto-generated method stub
			if (highPhiTermSet.contains(featureIndex)) {
				return 1;
			}
			return 0;
		}
		
	}
	public void featureSelect(FeatureSelector selector){
		term2Feature=new int[indexToTermMap.size()];
		termDocCount=new int[indexToTermMap.size()];//df
		for(Document doc:docs){
			HashMap<Integer, Double> termCountsOfDoc=doc.featureValueofDoc;
			for (int i = 0; i < doc.docWords.length/dup; i++) {
				Double termCount=null;
				if ((termCount=termCountsOfDoc.get(doc.docWords[i]))==null) {
					termCountsOfDoc.put(doc.docWords[i], 1.0);
					termDocCount[doc.docWords[i]]++;
				}else {
					termCountsOfDoc.put(doc.docWords[i], termCount+1);
				}
			}
			
		}
		int featureIndex=1;
		for(int i=0;i<indexToTermMap.size();i++){
//			if (termDocCount[i]>4) {
//			if (termCountMap.get(indexToTermMap.get(i))>7) {
			if (selector.featureSelect(i)==1) {
				term2Feature[i]=featureIndex++;
			}
		}
	}
	
	private int binarySearch(List<Integer> features,int termIndex){
		int low=0,high=features.size()-1;
		while(low<=high){
			int mid=(low+high)/2;
			if (features.get(mid)==termIndex) {
				return termIndex;
			}
			if (features.get(mid)>termIndex) {
				high--;
			}else {
				low++;
			}
		}
		return -1;
	}
	public void featureWeight(LdaModel ldaModel){
		HashSet<Integer> featureSet=new HashSet<Integer>();
		for(int i=0;i<indexToTermMap.size();i++){
			if (term2Feature[i]>0) {
				featureSet.add(i);
			}
		}
		for(Document doc:docs){
			HashMap<Integer, Double> featureValueofDoc=doc.featureValueofDoc;
			/*List<Integer> termsofdoc=new LinkedList<Integer>(featureValueofDoc.keySet());
			for(Integer featureIndex:termsofdoc){
				if (term2Feature[featureIndex]==0) {
					featureValueofDoc.remove(featureIndex);
				}
			}*/
			
			double normalizer=0.0;
			if(this.tokenFlag==1){
				
			}
			else{
			for(Integer featureIndex:featureSet){
				if (term2Feature[featureIndex]>0){
					double weight=0;//featureValueofDoc.get(featureIndex)*Math.log(docs.size()/termDocCount[featureIndex]);
					//if(this.tokenFlag==0){
						for(Integer tempfeatureIndex:featureValueofDoc.keySet()){
							if(Float.isNaN(similarScore[featureIndex][tempfeatureIndex])==false)
								if(similarScore[featureIndex][tempfeatureIndex]>0.6)
									weight+=similarScore[featureIndex][tempfeatureIndex]*featureValueofDoc.get(tempfeatureIndex);
							//weight=similarScore[featureIndex][tempfeatureIndex]>weight?similarScore[featureIndex][tempfeatureIndex]:weight;
						}
						//System.out.println("weight1:"+weight);
						//weight/=doc.docWords.length;
						weight/=doc.docWords.length;
					//}
					/*else{
						for(Integer tempfeatureIndex:featureValueofDoc.keySet()){
							if(Float.isNaN(similarScore[featureIndex][tempfeatureIndex])==false)
								if(featureIndex==tempfeatureIndex)
								weight=featureValueofDoc.get(tempfeatureIndex);
							//weight=similarScore[featureIndex][tempfeatureIndex]>weight?similarScore[featureIndex][tempfeatureIndex]:weight;
						}
					}*/
					//System.out.println("weight2:"+weight);
					//閺夊啴鍣哥拫鍐╂殻,1娴ｅ灝绶辨稉宥呮倱缁鍩嗘妯活洤閻滃檼erm閻ㄥ嫬灏崚鍡楀婢х偛銇囬敍灞藉綁閹广垻鐓╅梼纰夌吹2鐎甸�涚艾salient閻ㄥ墖erm閸旂姴銇囬弶鍐櫢
					float[] termPhi=new float[ldaModel.K];
					double weightPre=1.0;
					double max1=0.0,max2=0.0;
					for (int i = 0; i < termPhi.length; i++) {
						termPhi[i]=ldaModel.phi[i][featureIndex];
						//weightPre=termPhi[i]>weight?termPhi[i]:weightPre;
						if (termPhi[i]>max2) {
							if (termPhi[i]>max1) {
								max2=max1;
								max1=termPhi[i];
							}else {
								max2=termPhi[i];
							}
						}
					}
//					weightPre=max1/max2>100?Math.log10(max1/max2):1;
//					weightPre*=(max1>0.001?Math.log10(1000*max1):1);
//					weightPre=Math.log10(max1/max2);
//					System.out.println(indexToTermMap.get(featureIndex)+max1+" "+max2);
					weight*=weightPre;
					//System.out.println("weight3:"+weight);
					featureValueofDoc.put(featureIndex,weight);
					normalizer+=weight*weight;
				}
			}
			}
			//System.out.println("normalizer:"+normalizer);
			normalizer=Math.sqrt(normalizer);
			HashMap<Integer, Double> temp=new HashMap<Integer, Double>();
			for(Integer featureIndex:featureValueofDoc.keySet()){
				if (term2Feature[featureIndex]>0){
					temp.put(term2Feature[featureIndex],featureValueofDoc.get(featureIndex)/normalizer);//
				}
				//System.out.println("featureIndex:"+featureValueofDoc.get(featureIndex));
			}
			doc.featureValueofDoc=temp;
		}
		
	}
	
	public void outputData(String trainDataFile,String testDataFile) throws UnsupportedEncodingException, FileNotFoundException{
		PrintWriter trainWriter=new PrintWriter(new OutputStreamWriter(new FileOutputStream(trainDataFile),"utf8"));
		PrintWriter testWriter=new PrintWriter(new OutputStreamWriter(new FileOutputStream(testDataFile),"utf8"));
		HashMap<String, List<Document>> classDocsMap=new HashMap<String, List<Document>>();
		HashMap<String, Integer> lableClassMap=new HashMap<String,Integer>();
		{
			String[] topics=LdaGibbsSampling.topics.split("\\|");
			for(int i=0;i<topics.length;){
				lableClassMap.put(topics[i],i++);
			}
		}
		
/*{		int i=0;
		lableClassMap.put("016", i++);
		lableClassMap.put("020", i++);
		lableClassMap.put("027", i++);
		lableClassMap.put("057", i++);
		lableClassMap.put("019", i++);
		lableClassMap.put("028", i++);
		lableClassMap.put("079", i++);
		lableClassMap.put("058", i++);
		lableClassMap.put("047", i++);
		lableClassMap.put("084", i++);
}*/	
//	"016|020|027|057|019|028|079|058|047|084";
		int[] lables=new int[101];
		int curLable=0;
		for(Document doc:docs){
			String labelStr=new File(doc.docName).getName().substring(0,3);
			List<Document> docsofClass=null;
			if ((docsofClass=classDocsMap.get(labelStr))==null) {
				docsofClass=new ArrayList<Document>();
				classDocsMap.put(labelStr, docsofClass);
			}
			docsofClass.add(doc);
			/*int lableNum=Integer.parseInt(labelStr);
			if (lables[lableNum]==0) {
				curLable++;
				lables[lableNum]=curLable;
			}
			writer.println(lables[lableNum]+" "+featuresStr(doc.featureValueofDoc));*/
			/*int lable=-1;
			switch (labelStr) {
			case "010":
				lable=0;
				break;
			case "016":
				lable=1;
				break;
			case "020":
				lable=2;
				break;
			case "027":
				lable=3;
				break;
			case "057":
				lable=4;
				break;
			default:
				break;
			}
			trainWriter.println(lable+" "+featuresStr(doc.featureValueofDoc));*/
		}
		for(String lableStr:classDocsMap.keySet()){
			List<Document> docsofClass=classDocsMap.get(lableStr);
			int m=docsofClass.size()/10;
			/*for(int i=0;i<docsofClass.size();i++){
				if ((Math.random()*(docsofClass.size()-i))<m) {
					testWriter.println(lableClassMap.get(lableStr)+" "+featuresStr(docsofClass.get(i).featureValueofDoc));
					m--;
				}else {
					trainWriter.println(lableClassMap.get(lableStr)+" "+featuresStr(docsofClass.get(i).featureValueofDoc));
				}
			}*/
			int i=0;
			for(i=0;i<docsofClass.size()/10;i++){
				testWriter.println(lableClassMap.get(lableStr)+" "+featuresStr(docsofClass.get(i).featureValueofDoc));
			}
			for(i=0;i<docsofClass.size();i++){
				trainWriter.println(lableClassMap.get(lableStr)+" "+featuresStr(docsofClass.get(i).featureValueofDoc));
			}
		}
		trainWriter.close();
		testWriter.close();
	}
	public String featuresStr(HashMap<Integer, Double> features){
		StringBuffer featureStr=new StringBuffer();
		ArrayList<Integer> featureIndexs=new ArrayList<Integer>(features.keySet());
		Collections.sort(featureIndexs);
		for(Integer index:featureIndexs){
			featureStr.append(index+":"+features.get(index)+" ");
		}
		return featureStr.toString();
	}
	public void svmRun(String trainDataFile,String testDataFile,String predictFile) throws IOException{
		String[] trainParas={"-t","2","-c","10","-v","10",trainDataFile,LdaGibbsSampling.resultPath+"model.txt"};
		svm_train train=new svm_train();
		train.main(trainParas);
		String[] predictParas={testDataFile,LdaGibbsSampling.resultPath+"model.txt",predictFile};
		svm_predict predict=new svm_predict();
//		predict.main(predictParas);
	}
	
	static HashSet<String> stopWords=new HashSet<String>();
	public void classPipeline(LdaModel ldaModel) throws Exception{
		//System.out.print("error4");
		featureSelect(new PhiSelector(ldaModel,20));
		//System.out.print("error2");
		featureWeight(ldaModel);
		//System.out.print("error3");
		if(this.tokenFlag==0) featureExtend();
		//System.out.print("error0");
		outputData(LdaGibbsSampling.resultPath+"trainData.txt", LdaGibbsSampling.resultPath+"testData.txt");
		svmRun(LdaGibbsSampling.resultPath+"trainData.txt", LdaGibbsSampling.resultPath+"testData.txt", LdaGibbsSampling.resultPath+"predict.txt");
		//System.out.print("error1");
	}
	HashMap<String, List<Document>> testDocs=new HashMap<String, List<Document>>();
	int tokenFlag;
	int patternFlag;
	int dup;
	int bitTerm;
	int magofDict;
	double simThreshold;
	public Documents(int tokenFlag,int patternFlag){
		this();
		this.tokenFlag=tokenFlag;
		this.patternFlag=patternFlag;
	}
	public void featureExtend() throws Exception{
		int featureIndex=0;
		for(int i=term2Feature.length-1;i>=0;i--){
			if (term2Feature[i]>0) {
				featureIndex=term2Feature[i];
				break;
			}
		}
		final Documents docsEx=new Documents(1, 0);
		docsEx.dup=1;
		docsEx.readDocs(new File(LdaGibbsSampling.originalDocsPath).getParent()+"/posText/");
		docsEx.featureSelect(new FeatureSelector() {
			@Override
			int featureSelect(int featureIndex) {
				// TODO Auto-generated method stub
				if(docsEx.termDocCount[featureIndex]>2){
					return 1;
				}
				return 0;
			}
		});
		
		for(int docIndex=0;docIndex<docs.size();docIndex++){
			Document doc=docs.get(docIndex),docEx=docsEx.docs.get(docIndex);
			HashMap<Integer, Double> featureValueofDoc=doc.featureValueofDoc;
			double normalizer=0.0;
			for(int i:docEx.featureValueofDoc.keySet()){
				if (docsEx.term2Feature[i]>0) {
					double weight=docEx.featureValueofDoc.get(i)*Math.log(docsEx.docs.size()/docsEx.termDocCount[i]);
					featureValueofDoc.put(docsEx.term2Feature[i]+featureIndex, weight);
				}
			}
			
		}
	}
}
