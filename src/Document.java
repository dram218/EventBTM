import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Document {
		String docName;
		int docIndex;
		int[] docWords;
		ArrayList<Event> events=new ArrayList<Event>();
		
		int[] originalDocWords;
		ArrayList<Event> allNewEvents=new ArrayList<Event>();
		ArrayList<Integer> eventFlags=new ArrayList<Integer>();
		
		int[] virtulTerms;
/*		public Document filtDoc(Documents filtedDocs,ArrayList<Integer> termIndexs2remove,Documents unfiltDocs){
			Document filtedDoc=new Document();
			filtedDoc.docName=this.docName;
			filtedDoc.docIndex=this.docIndex;
			filtedDoc.events=new ArrayList<Event>();
			ArrayList<Integer> docWords1=new ArrayList<Integer>();
			for(int i=0;i<docWords.length;i++){
				if (termIndexs2remove.contains(this.docWords[i])) 
					continue;
				else {
					filtedDoc.events.add(this.events.get(i));
					Event eeEvent=unfiltDocs.indexToTermMap.get(this.docWords[i]);
					if(!filtedDocs.termToIndexMap.containsKey(eeEvent)){
						int newIndex=filtedDocs.termToIndexMap.size();
						filtedDocs.termToIndexMap.put(eeEvent, newIndex);
						filtedDocs.indexToTermMap.add(eeEvent);
						filtedDocs.termCountMap.put(eeEvent, new Integer(1));
						docWords1.add(newIndex);
					}else {
						docWords1.add(filtedDocs.termToIndexMap.get(eeEvent));
						filtedDocs.termCountMap.put(eeEvent, filtedDocs.termCountMap.get(eeEvent) + 1);
					}
				}
			}
			filtedDoc.docWords=new int[docWords1.size()];
			for(int i=0;i<docWords1.size();i++){
				filtedDoc.docWords[i]=docWords1.get(i);
			}
			return filtedDoc;
		}*/

		public Document(String docName, Documents docs) throws Exception, Exception {
			this.docName = docName;
			this.docIndex=docs.docs.size();
			// Read file and initialize word index array
			ArrayList<Integer> docWords1=new ArrayList<Integer>();
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(docName), "utf8"));

			String buf = "";

			int newIndex=0;
			int order=0;
			while ((buf = br.readLine()) != null)
			{
								
				buf=buf.trim();
				if (buf.length() == 0)		continue;
				
				String[] items={buf};
				if(docs.tokenFlag==1){
					items=buf.split(" ");
					for(int i=0;i<items.length;i++){
						items[i]=items[i].substring(0,items[i].indexOf("_"));
					}
				}
				
				

				try{
					for(String item:items){
						if (docs.tokenFlag==1&&docs.stopWords.contains(item)) {
							continue;
						}
						/*else {
							if (item.contains("自流")) {
								continue;
							}
						}*/
						order++;
						Event eeEvent=null;
						if (docs.tokenFlag==1) {
							eeEvent=new Token(item, docName);
						}else {
							if (docs.patternFlag==1) {
								eeEvent=new Event(item,docName);
							}else {
								eeEvent=new Event(item, docName, 0);
							}
							
						}
						eeEvent.order=order;
						eeEvent.docIndex=this.docIndex;
						
						this.events.add(eeEvent);
						
						
						String baseEvent=null;
						if((baseEvent=docs.getBaseEvent(eeEvent))==null){
							baseEvent=eeEvent.eventPattern;
							newIndex = docs.termToIndexMap.size();
							docs.termToIndexMap.put(baseEvent, newIndex);
							docs.indexToTermMap.add(baseEvent);
							
							docs.termCountMap.put(baseEvent, new Integer(1));
							docWords1.add(newIndex);
							
							
							docs.eventInTerms.put(baseEvent, new ArrayList<Event>());
							docs.eventInTerms.get(baseEvent).add(eeEvent);
							docs.averOrderofTerms.put(baseEvent, 0.0);
							
							
						}else {
							docWords1.add(docs.termToIndexMap.get(baseEvent));
							docs.termCountMap.put(baseEvent,docs.termToIndexMap.get(baseEvent)+1);
							
							docs.eventInTerms.get(baseEvent).add(eeEvent);
						}
					
					}
				}
				catch(Exception eee){
					System.err.println(docName);
					System.err.println(buf);
					eee.printStackTrace();}
			}
			br.close();
			docWords=new int[docWords1.size()*docs.dup];
			for(int i=0;i<docWords1.size();i++){
//				docWords[i]=docWords1.get(i);
				for(int j=0;j<docs.dup;j++){
					docWords[i+docWords1.size()*j]=docWords1.get(i);
				}
			}
			if (docs.bitTerm!=0) {//use virtulTerms to construct new doc representation
				virtulTerms=new int[docWords1.size()*(docWords1.size()-1)/2];
				int c=0;
				for(int i=0;i<docWords1.size()-1;i++){
//					docWords[i]=docWords1.get(i);
					for(int j=i+1;j<docWords1.size();j++){
						virtulTerms[c++]=docWords1.get(i)*docs.magofDict+docWords1.get(j);
					}
				}
			}else{
				virtulTerms=docWords;
			}
			
			docWords1.clear();
			docWords1=null;
			

		}
		
		
		public boolean isStopword(String word) {
			// TODO Auto-generated method stub
			if(LdaGibbsSampling.stopList.indexOf(word)>-1)
				return true;
			return false;
		}
		
		public void tokenizeAndLowerCase(String line,ArrayList<String> words){
			String[] wordList=line.split("\\s");
			String tmpStr="";
			for(int i=0;i<wordList.length;i++){
				tmpStr=wordList[i].trim();
				System.out.println(tmpStr);
				if(tmpStr.length()<=1) continue;
				if(ContainNonLetter(tmpStr)==true) continue;
				words.add(tmpStr);
			}
			
		}
		
		public boolean isNoiseWord(String string) {
			// TODO Auto-generated method stub
			string = string.toLowerCase().trim();
			Pattern MY_PATTERN = Pattern.compile(".*[a-zA-Z]+.*");
			Matcher m = MY_PATTERN.matcher(string);
			// filter @xxx and URL
			if (string.matches(".*www\\..*") || string.matches(".*\\.com.*")|| string.matches(".*http:.*"))
				return true;
			if (!m.matches()) {
				return true;
			} else
				return false;
		}
		
		/**
		 * �ж��ַ����Ƿ���ڷ���ĸ
		 * @param word
		 * @return
		 */
		public boolean ContainNonLetter(String word){
			char ch;
			for(int i=0;i<word.length();i++){
				ch=word.charAt(i);
				if(Character.isLetter(ch)==false){
					return true;
				}
			}
			return false;
		}	
		
		
		HashMap<Integer, Double> featureValueofDoc=new HashMap<Integer, Double>();
		
	}