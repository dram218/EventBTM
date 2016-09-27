import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;



public class LdaGibbsSampling {
	public static String sourceTextPath="";
	public static String originalDocsPath = "topicEvents/";//1.5test(nophrase)\\Events"originalDocsPath";Event12-27\\event(���Ҷ�ʧ��Ԫ-3����) C:\\Documents and Settings\\Administrator\\����\\1.7����\\Events(1)
	public static String resultPath = "reducedResult/";//resultPath
	public static String parameterFile= "parameterFile";
	public static ArrayList<String> stopList=new ArrayList<String>();
	public static Documents docSet = new Documents();
	public static String topics="016|020|027|057|019|028|079|058|047|084";//
	

	public static class modelparameters {
		int topicNum = 10;
		float alpha =  50.0f/topicNum;// 50.0f/topicNum; // usual value is 50 / K
		float beta = 0.1f;// usual value is 0.1
		int iteration = 2000;
		int saveStep = 200;
		int beginSaveIters = 1000;
		
		int ldaType=0;//0 LDA,1 GLDA use GPU or not
		double simThreshold=0.6;
		int tokenFlag=0;
		int patternFlag=0;
		int samplingType=1;
		int dup=1;
		int biterm=2;//nobiterm for 0,biterm for 1,probiterm for 2
		int magofDict=100000;
		int tcM=20;
		int sparseMatrix=0;
		double lambda=1;
		int magofTopic=100;
	}

	public static void LoadStopWords() throws Exception{
		//ReadLines("stoplist.txt",stopList);
		BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream("stopwords.txt"),"utf8"));
		String word=null;
		while((word=reader.readLine())!=null){
			Documents.stopWords.add(word);
		}
		reader.close();
	}
	public static void ReadLines(String fileName,ArrayList<String> paramLines) throws Exception, FileNotFoundException{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "GBK"));
		String buf = "";
		
		while ((buf = br.readLine()) != null)
		{
			buf=buf.trim();
			if (buf.length() == 0)		continue;
			paramLines.add(buf);
		}
		br.close();
	}
	
	public static void WriteLines(String fileName,ArrayList<String> lines) throws Exception, FileNotFoundException{
		FileWriter out = new FileWriter(fileName, false);
		for(int i=0;i<lines.size();i++){
			out.write(lines.get(i));
			out.write("\n");
			out.flush();
		}
		out.close();
	}

	/**
	 * Get parameters from configuring file. If the configuring file has value
	 * in it, use the value. Else the default value in program will be used
	 * 
	 * @param ldaparameters
	 * @param parameterFile
	 * @return void
	 * @throws Exception 
	 * @throws FileNotFoundException 
	 */
	private static void getParametersFromFile(modelparameters ldaparameters,String parameterFile) throws FileNotFoundException, Exception {
		// TODO Auto-generated method stub
		ArrayList<String> paramLines = new ArrayList<String>();
		ReadLines(parameterFile, paramLines);
		for (String line : paramLines) {
			String[] lineParts = line.split("\t");
			switch (parameters.valueOf(lineParts[0])) {
			case alpha:
				ldaparameters.alpha = Float.valueOf(lineParts[1]);
				break;
			case beta:
				ldaparameters.beta = Float.valueOf(lineParts[1]);
				break;
			case topicNum:
				ldaparameters.topicNum = Integer.valueOf(lineParts[1]);
				break;
			case iteration:
				ldaparameters.iteration = Integer.valueOf(lineParts[1]);
				break;
			case saveStep:
				ldaparameters.saveStep = Integer.valueOf(lineParts[1]);
				break;
			case beginSaveIters:
				ldaparameters.beginSaveIters = Integer.valueOf(lineParts[1]);
				break;
			}
		}
	}

	public enum parameters {
		alpha, beta, topicNum, iteration, saveStep, beginSaveIters;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		/*System.out.println(new Date());
		System.out.println(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
		System.exit(0);*/
		//System.out.println(args[0]+","+args[1]);
		
		LoadStopWords();
		
		modelparameters ldaparameters = new modelparameters();
		docSet.tokenFlag=ldaparameters.tokenFlag;
		docSet.patternFlag=ldaparameters.patternFlag;
		docSet.dup=ldaparameters.dup;
		docSet.bitTerm=ldaparameters.biterm;
		docSet.magofDict=ldaparameters.magofDict;
		docSet.simThreshold=ldaparameters.simThreshold;
		LdaGibbsSampling.originalDocsPath=ldaparameters.tokenFlag==1?"posText/":ldaparameters.patternFlag==1?"topicEvents/":"Events(3)/";//"posText/"
		LdaGibbsSampling.originalDocsPath=args[0]+"/"+LdaGibbsSampling.originalDocsPath;
		//LdaGibbsSampling.originalDocsPath="C:/Documents and Settings/Administrator/桌面/1.5test(nophrase)/Events/";
		System.out.println(new File(LdaGibbsSampling.originalDocsPath).getParent());
		//System.out.println(new File(new File(LdaGibbsSampling.originalDocsPath).getParent()+"/posText/"));System.exit(0);
		LdaGibbsSampling.resultPath=args[1]+"/result";
		LdaGibbsSampling.resultPath+="_"+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+"/";
		if (new File(LdaGibbsSampling.resultPath).exists()==false) {
			new File(LdaGibbsSampling.resultPath).mkdir();
		}
		
		/*ldaparameters.simThreshold=new Float(args[2]);
		ldaparameters.ldaType=new Integer(args[3]);
		
		ldaparameters.iteration=new Integer(args[4]);
		ldaparameters.topicNum=new Integer(args[5]);*/
		//get the parameters from specific file
//		getParametersFromFile(ldaparameters, parameterFile);
		

		docSet.readDocs(originalDocsPath);
		Documents unfiltedDocs=docSet;
		docSet.GetSimilarScore();
		/*PrintWriter simWriter=new PrintWriter(new OutputStreamWriter(new FileOutputStream("simEvents.txt"),"utf8"));
		double maxsim=0;
		for(int i=0;i<docSet.indexToTermMap.size();i++){
			for (int j = i+1; j < docSet.indexToTermMap.size(); j++) {
				if (docSet.similarScore[i][j]>maxsim) {
					simWriter.println(docSet.indexToTermMap.get(i)+"   "+docSet.indexToTermMap.get(j));
					maxsim=docSet.similarScore[i][j];
				}
			}
		}System.out.println(maxsim);System.exit(0);
		simWriter.close();*/
//		docSet=docSet.filtDocs();
//		if (ldaparameters.ldaType==1||ldaparameters.biterm==2){
//			docSet.GetSimilarScore();
//			/*if (ldaparameters.sparseMatrix==1) {
//				docSet.sparseSimMatrix();
//			}*/
//		}

		
		
		System.out.println("wordMap size " + docSet.termToIndexMap.size());
		LdaModel model = new LdaModel(ldaparameters);
		System.out.println("1 Initialize the model");
		model.initializeModel(docSet);
		System.out.println("2 Learning and Saving the model");
		model.inferenceModel(docSet);
		System.out.println("3 Output the final model");
		model.saveIteratedModel(ldaparameters.iteration, docSet);
		System.out.println("Done!");
		int sparseEventNum=0;
		for(Integer count:unfiltedDocs.termCountMap.values()){
			if (count==1) {
				sparseEventNum++;
			}
		}
		System.out.println(unfiltedDocs.termToIndexMap.size()+"##"+sparseEventNum+"##"+docSet.termToIndexMap.size());
		System.out.println(docSet.termToIndexMap.size());
		System.out.println(docSet.allEventsNum);
		/*docSet.similarScore=null;
		System.gc();*/
		PrintStream out=System.out,classifyStream=new PrintStream(LdaGibbsSampling.resultPath+"/"+"classifyResult.txt");
		System.setOut(classifyStream);
		docSet.classPipeline(model);
		System.setOut(out);
		classifyStream.close();
		//System.exit(0);
		docSet.countTermDoc();
		
		PrintWriter rsWriter=new PrintWriter(new OutputStreamWriter(new FileOutputStream(LdaGibbsSampling.resultPath+"result.txt"), "utf8"));
		rsWriter.println("topicNum="+ldaparameters.topicNum);
		rsWriter.println("alpha="+ldaparameters.alpha);
		rsWriter.println("beta="+ldaparameters.beta);
		rsWriter.println("iteration="+ldaparameters.iteration);
		rsWriter.println("ldaType="+ldaparameters.ldaType);
		rsWriter.println("simThreshold="+ldaparameters.simThreshold);
		rsWriter.println("tokenFlag="+ldaparameters.tokenFlag);
		rsWriter.println("patternFlag="+ldaparameters.patternFlag);
		rsWriter.println("samplingType="+ldaparameters.samplingType);
		rsWriter.println("dup="+ldaparameters.dup);
		rsWriter.println("biterm="+ldaparameters.biterm);
		rsWriter.println("tcM="+ldaparameters.tcM);
		rsWriter.println("lambda="+ldaparameters.lambda);
		rsWriter.println("docNum##dictionary size##sparseTermNum##allTermNum##avgDocLen"+docSet.docs.size()+"##"+unfiltedDocs.termToIndexMap.size()+"##"+sparseEventNum+"##"+docSet.allEventsNum+"##"+((double)docSet.allEventsNum)/docSet.docs.size());
		for(int topM=ldaparameters.tcM;topM<=100;topM+=5){
			double avgTc=model.topicCoherence(docSet, topM);
			rsWriter.println("Average topic coherence of top "+topM+"="+avgTc);
		}
		rsWriter.println("Average KL-divergence : "+model.calculateKLDivergence());
		rsWriter.println("perplexity : "+model.calculatePerplexity(unfiltedDocs));
		rsWriter.close();
		System.exit(0);
//		model.classfyDocs(docSet);
//		docSet.countTermDoc();
//		model.topicCoherence(docSet, 10);
//		model.topicCoherence(docSet, 40);
//		model.topicCoherence(docSet, 50);
		
		PrintWriter writer=new PrintWriter(new OutputStreamWriter(new FileOutputStream(LdaGibbsSampling.resultPath+"patternStatisticConvergeNew.txt"), "utf8"));
		ArrayList<Integer> indexlist=new ArrayList<Integer>();
		int allTermCount=0;
		for(int i=0;i<docSet.termToIndexMap.keySet().size();i++){
			indexlist.add(i);
			allTermCount+=docSet.termCountMap.get(docSet.indexToTermMap.get(i));
		}
		System.out.println(allTermCount);
		Collections.sort(indexlist, new Comparator<Integer>() {

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
		});
		int eventPatternNum=1000;
		for(int i=0;i<eventPatternNum;i++){
			int order=indexlist.get(i);
			int patternCount=docSet.termCountMap.get(docSet.indexToTermMap.get(order));
			writer.println(docSet.indexToTermMap.get(order).toString()+"\t"+patternCount+"\t"+((double)patternCount)/docSet.termCountMap.size());			
//			writer.println(docSet.indexToTermMap.get(order).eventsInPattern.get(0).toString()+"\t"+patternCount+"\t"+((double)patternCount)/docSet.termCountMap.size());
//			writer.println(docSet.indexToTermMap.get(order).toString()+"\t"+patternCount+"\t"+((double)patternCount)/docSet.termCountMap.size());					
		}
		writer.close();
		
	}
}
