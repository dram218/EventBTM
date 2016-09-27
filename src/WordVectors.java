import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;


public class WordVectors implements Serializable{
	HashMap<String, ArrayList<Float>> vectors=new HashMap<String,ArrayList<Float>>();
	int wordCount;
	int vectorLen;
	
	public WordVectors(){
		
	}

	private void writeObject(ObjectOutputStream stream) throws Exception{
		stream.defaultWriteObject();
		
		stream.writeObject(vectors);
		stream.writeObject(wordCount);
		stream.writeObject(vectorLen);
		
	}
	
	private void readObject(ObjectInputStream stream) throws Exception, ClassNotFoundException{
		stream.defaultReadObject();
		vectors=(HashMap<String,ArrayList<Float>>)stream.readObject();
		wordCount=(Integer)stream.readObject();
		vectorLen=(Integer)stream.readObject();
	}
	
/*	public void ReadWordVector() throws Exception, FileNotFoundException{
		BufferedReader br1 = new BufferedReader(new InputStreamReader(new FileInputStream("ACL2012_wordVectorsTextFile\\vocab.txt"), "utf-8"));
		BufferedReader br2 = new BufferedReader(new InputStreamReader(new FileInputStream("ACL2012_wordVectorsTextFile\\wordVectors.txt"), "utf-8"));
		String word = "";
		String vecString="";
		String[] wordVectors;
		
		while ((word = br1.readLine()) != null)
		{
			word=word.trim();
			vecString=br2.readLine().trim();
			wordVectors=vecString.split("\\s");
			if(vectorLen!=wordVectors.length){
				System.out.println(vectorLen+","+wordVectors.length);
				vectorLen=wordVectors.length;
			}
			
			ArrayList<Float> vecValues=new ArrayList<Float>(wordVectors.length);
			for(int i=0;i<wordVectors.length;i++)
				vecValues.add(Float.valueOf(wordVectors[i]));
			
			vectors.put(word, vecValues);	
			
		}
		
		wordCount=vectors.size();
		br1.close();
		br2.close();
	}*/
	
	public void ReadWordVector() throws Exception, FileNotFoundException{
		BufferedReader br1 = new BufferedReader(new InputStreamReader(new FileInputStream("ChiVectors(50skip3).bin"), "utf-8"));//EngVectorsnewq100.dat
		String word = "";
		String vecString="";
		String[] wordVectors;
		int lineno=0;//System.out.println("haha "+br1.readLine()+br1.readLine()+br1.readLine());
		while ((vecString = br1.readLine()) != null)
		{	
			vecString=vecString.trim();
			//System.out.println(vecString);
			wordVectors=vecString.split("\\s");
			if(lineno==0){
				lineno++;
				wordCount=Integer.valueOf(wordVectors[0]);
				vectorLen=Integer.valueOf(wordVectors[1]);
				continue;
			}
			
			word=wordVectors[0];
			/*
			if(vectorLen!=wordVectors.length){
				System.out.println(vectorLen+","+wordVectors.length);
				vectorLen=wordVectors.length;
			}*/
			
			if(LdaGibbsSampling.docSet.wordsList.indexOf(word)==1)
				continue;
			ArrayList<Float> vecValues=new ArrayList<Float>(wordVectors.length);
			for(int i=1;i<wordVectors.length;i++)
				vecValues.add(Float.valueOf(wordVectors[i]));
			
			vectors.put(word, vecValues);	
			//System.out.print(word+":");for(int kk=0;kk<10;kk++){System.out.print(" "+wordVectors[kk]);}System.out.println();
			
		}
		
		
		br1.close();
	}
	
}
