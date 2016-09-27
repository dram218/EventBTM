import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DependencyRelation implements Serializable{//,Comparable<DependencyRelation> {
	String typeString;
	String firstString;
	String secondString;
	int firstPosition;
	int secondPosition;
	public DependencyRelation(String relation){
		Pattern pattern;
		Matcher matcher;
		pattern = Pattern.compile("(.+?)[(](.+?)[,][\\s+](.+?)[)]");
		matcher = pattern.matcher(relation);
		String tmp1, tmp2;
//		System.out.println(relation);
		if (matcher.find()) {

			typeString = matcher.group(1);
			tmp1 = matcher.group(2);
			tmp2 = matcher.group(3);
			firstString = tmp1.substring(0,tmp1.lastIndexOf("-"));
			firstPosition = Integer.valueOf(tmp1.substring(tmp1.lastIndexOf("-") + 1));
			secondString = tmp2.substring(0,tmp2.lastIndexOf("-"));
			secondPosition = Integer.valueOf(tmp2.substring(tmp2.lastIndexOf("-") + 1));
			//System.out.println("1"+firstString);
		}
	}
	
	public String getFirstStr(){
		return firstString+"-"+firstPosition;
	}
	
	public String getSecondStr(){
		return secondString+"-"+secondPosition;
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {

		stream.defaultWriteObject();

		stream.writeObject(typeString);
		stream.writeObject(firstString);
		stream.writeObject(secondString);
		stream.writeObject(firstPosition);
		stream.writeObject(secondPosition);
	}

	private void readObject(ObjectInputStream stream) throws IOException,ClassNotFoundException {

		stream.defaultReadObject();
		typeString = (String)stream.readObject();
		firstString = (String)stream.readObject();
		secondString = (String)stream.readObject();
		firstPosition = (Integer)stream.readObject();
		secondPosition = (Integer)stream.readObject();
	}

	/*@Override
	public String toString() {
		// TODO Auto-generated method stub
		String relationStr=typeString+"("+getFirstStr()+","+getSecondStr()+")";
		return relationStr;
	}*/

/*	@Override
	public int compareTo(DependencyRelation o) {
		// TODO Auto-generated method stub
		DependencyRelation s1 = this;
		  DependencyRelation s2 = (DependencyRelation) o;
		  if (s1.firstPosition> s2.firstPosition)
		   return 1;
		  else if(s1.firstPosition<s2.firstPosition)
			  return -1;
		  else if(s1.secondPosition>s2.secondPosition)
			  return 1;
		  else
			  return -1;
	}*/
	
}

