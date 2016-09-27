import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;


public class Event{
	static HashMap<String, ArrayList<String>> CilinMap=null;
	static HashMap<String, ArrayList<String>> InverCilinMap=null;
	static int triggerClassLen=5;
	static int argumentClassLen=5;

	int docIndex;
	int order;
	double aveOrder;
	LinkedList<Event> allEventsInPatter;
	int isPattern;
	LinkedList<Event> eventsInPattern;
	int indexInPattern=-1;
	ArrayList<String> patternSubClassList;
	ArrayList<String> patternPreClassList;
	ArrayList<String> patternObjClassList;
	
	Set<String> disSubSet;
	Set<String> disPreSet;
	Set<String> disObjSet;
	Event originalEvent;
	int isChanged=0;
	Event patternEvent;

	String docName;
	String Subject;
	int SubjectNo;
	ArrayList<String> SubjectClassList;
	String SubjectNer;
	String Predicate;
	int PredicateNo;
	ArrayList<String> PredicateClassList;
	String Object;//����
	int ObjectNo;
	ArrayList<String> ObjectClassList;
	String ObjectNer;
	int LineNo;
	int eventType;//0: s,p,o  1:s,p  2:p,o
	String eventPattern;
	
	public Event(){
		
	}
	
	public Event(String words,String docName){
		this.docName=docName;
		isPattern=0;
		
		String[] terms=words.split("##");
		String tmpstr="";
		String[] eventStr=terms[0].split(",");
		tmpstr=eventStr[0];
		this.Subject=tmpstr.substring(0,tmpstr.indexOf("("));
		if(this.Subject!="\"\""&&LdaGibbsSampling.docSet.wordsList.indexOf(this.Subject)==-1)
			LdaGibbsSampling.docSet.wordsList.add(this.Subject);
		tmpstr=eventStr[1];
		this.Predicate=tmpstr.substring(0,tmpstr.indexOf("("));
		if(this.Predicate!="\"\""&&LdaGibbsSampling.docSet.wordsList.indexOf(this.Predicate)==-1)
			LdaGibbsSampling.docSet.wordsList.add(this.Predicate);
		tmpstr=eventStr[2];
		this.Object=tmpstr.substring(0,tmpstr.indexOf("("));
		if(this.Object!="\"\""&&LdaGibbsSampling.docSet.wordsList.indexOf(this.Object)==-1)
			LdaGibbsSampling.docSet.wordsList.add(this.Object);
		
		this.LineNo=Integer.valueOf(terms[2]);
		this.eventType=Integer.valueOf(terms[3]);
		
		this.eventPattern=terms[1];
		
	}

	public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof Event) {
        	Event other = (Event) o;
    		if (this.eventPattern.equals(other.eventPattern))
    			return true;
        	
        }
        return false;
    }

    public int hashCode() {
        return (this.SubjectNer+this.Predicate+this.ObjectNer).hashCode();

    }


    static class ArgInEvent{
    	String word;
    	int wordIndex;
		public ArgInEvent(String word, int wordIndex) {
			super();
			this.word = word;
			this.wordIndex = wordIndex;
		}
    	
    }
    public String changeStr(){
    	
		String str="";
		str+=this.originalEvent.Subject+this.originalEvent.patternEvent.patternSubClassList+","+this.originalEvent.Predicate+this.originalEvent.patternEvent.patternPreClassList+","+this.originalEvent.Object+this.originalEvent.patternEvent.patternObjClassList;
    	str+=":"+this.originalEvent.patternEvent.toString()+"\t";
    	str+=this.Subject+this.patternEvent.patternSubClassList+","+this.Predicate+this.patternEvent.patternPreClassList+","+this.Object+this.patternEvent.patternObjClassList;
    	str+=":"+this.patternEvent+"\t"+this.docName;
    	return str;
    }

    public String toString(){
    	return this.Subject+","+this.Predicate+","+this.Object;
    }

	public int crossInLine(Event event){
		int flag=0;
		if (this.docName==event.docName&&this.LineNo==event.LineNo) {
			ArrayList<Integer> positions1=new ArrayList<Integer>();
			ArrayList<Integer> positions2=new ArrayList<Integer>();
			positions1.add(this.SubjectNo);
			positions1.add(this.PredicateNo);
			positions1.add(this.ObjectNo);
			positions2.add(this.SubjectNo);
			positions2.add(this.PredicateNo);
			positions2.add(this.ObjectNo);
			Collections.sort(positions1);
			Collections.sort(positions2);
			if (positions1.get(0)==positions2.get(0)&&positions1.get(1)==positions2.get(1)&&positions1.get(2)==positions2.get(2)) {
				flag=1;
			}
		}
		return flag;
	}
	public Event(String words,String docName,int patternFlag){

		this.docName=docName;
		isPattern=0;
		
		String[] terms=words.split(",");
		String[] nerTermStrings;
		String tmpstr="";
		if(terms[0].equals("\"\"")==false){
			tmpstr=terms[0].substring(0,terms[0].lastIndexOf("-"));
			nerTermStrings=tmpstr.split("_");
			this.Subject=nerTermStrings[0];
			/*if(nerTermStrings[1].equals("O"))
				this.SubjectNer=this.Subject;
			else
				this.SubjectNer=nerTermStrings[1];*/
			this.SubjectNer=this.Subject;
			this.SubjectNo=Integer.valueOf(terms[0].substring(terms[0].lastIndexOf("-")+1));
		}
		else{ 
			this.Subject=terms[0];
			this.SubjectNer="\"\"";
		}
		
		if(terms[1].equals("\"\"")==false){
			this.Predicate=terms[1].substring(0,terms[1].lastIndexOf("-"));
			this.PredicateNo=Integer.valueOf(terms[1].substring(terms[1].lastIndexOf("-")+1));
		}
		else {
			this.Predicate=terms[1];
			this.PredicateNo=Integer.valueOf(terms[1].substring(terms[1].lastIndexOf("-")+1));
		}
		
		//System.out.println(terms[2]+" "+"\"\"");
		if(terms[2].equals("\"\"")==false){
			tmpstr=terms[2].substring(0,terms[2].lastIndexOf("-"));
			nerTermStrings=tmpstr.split("_");
			this.Object=nerTermStrings[0];
			/*if(nerTermStrings[1].equals("O"))
				this.ObjectNer=this.Object;
			else
				this.ObjectNer=nerTermStrings[1];*/
			this.ObjectNer=this.Object;
			this.ObjectNo=Integer.valueOf(terms[2].substring(terms[2].lastIndexOf("-")+1));
			
		}
		else {
			this.Object=terms[2];
			this.ObjectNer="\"\"";
		}
		/*setArgsClass();*/
		this.LineNo=Integer.valueOf(terms[3]);
		this.eventType=Integer.valueOf(terms[4]);
		
//		try {
//			originalEvent=(Event)this.clone();
//		} catch (CloneNotSupportedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		this.eventPattern=this.Subject+","+this.Predicate+","+this.Object;
	}
	
	public Event(String[] args){

		this.Subject=args[0];
		this.Predicate=args[1];
		this.Object=args[2];
	}
}
