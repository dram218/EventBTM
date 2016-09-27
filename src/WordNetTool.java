import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.IRAMDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.mit.jwi.morph.WordnetStemmer;
import edu.sussex.nlp.jws.AdaptedLesk;
import edu.sussex.nlp.jws.ICFinder;
import edu.sussex.nlp.jws.JWS;
import edu.sussex.nlp.jws.JiangAndConrath;
import edu.sussex.nlp.jws.Lin;
import edu.sussex.nlp.jws.Resnik;


public class WordNetTool {
	static IDictionary dict;
	static String fileUrl="D:/Program Files (x86)/WordNet/2.1/dict";
	static String dir = "D:/Program Files (x86)/WordNet";
	static	JWS ws = new JWS(dir, "2.1", "ic-bnc.dat");
//	static	JiangAndConrath jcn = ws.getJiangAndConrath();
	static AdaptedLesk 	lesk = null;//ws.getAdaptedLesk();;
	static String verString="2.1";
	static String icfile= dir+"/" + verString + "/WordNet-InfoContent-"+ verString + "/ic-semcor.dat";
	static Lin lin =ws.getLin();

	

	public static double GetSimilarScoreByLin(String word1,String word2,String pos){
// all senses
/*		TreeMap<String, Double> 	scores2	=	lin.lin(word1,word2,POS);			
		for(String s : scores2.keySet())
			System.out.println(s + "\t" + scores2.get(s));
*/
		
// specific senses
//		System.out.println("\nspecific pair\t=\t" + lin.lin("apple", 1, "banana", 1, "n"));
		if(word1.equals(word2)) return 1;
		return  lin.max(word1, word2, pos);
	}
	
	public static double GetSimilarScoreByLesk(String word1,String word2,String pos){
		//lesk.useStopList(false);			// by default, the stop list and the lemmatiser
		//lesk.useLemmatiser(false);		// are used to check for 'non content' words in overlaps - here, you can turn these defaults off
		//System.out.println("Adapted Lesk (Extended Gloss Overlaps)\n");// all senses
		TreeMap<String, Double> 	scores3	=	lesk.lesk("film", "movie", "n");	// all senses
		//TreeMap<String, Double> 	scores3	=	lesk.lesk("apple", 1, "banana", "n"); 		// fixed;all
		//TreeMap<String, Double> 	scores3	=	lesk.lesk("apple", "banana", 2, "n"); 		// all;fixed
		/*IIndexWord	word3	= ws.getDictionary().getIndexWord("film", POS.NOUN);
		IIndexWord	word4	= ws.getDictionary().getIndexWord("movie", POS.NOUN);
		System.out.println(word3.toString());
		System.out.println(word4.toString());
		for(String s : scores3.keySet())			System.out.println(s + "\t" + scores3.get(s));
		*///specific senses
		//System.out.println("\nspecific pair\t=\t" + lesk.lesk("apple", 1, "current", 1, "n") );
		//max.
		return lesk.max(word1, word2, pos);
	}
	
	public static double GetSimilarScoreByResnik(String word1,String word2,String pos) throws Exception{
		ICFinder 			icfinder 			=	new ICFinder(icfile);
		Resnik res = new Resnik(dict, icfinder);
// res(1) specific senses for both words
/*		double resscore = res.res("apple", 1, "banana", 2, "n"); // "word1", sense#, "word2", sense#, "POS"
		System.out.println("specific senses");
		if(resscore != 0) // 0 is an error code i.e it means that something isn't right e.g. words are not in WordNet, wrong POS etc
		{
			System.out.println("res:\t" + formatter.format(resscore));
		}
		System.out.println();
*/
// res(2) all senses: a value (score) of 0 is an error code for a pair
/*		TreeMap<String,Double> map = res.res("apple", "banana", "n"); // "word1", "word2", "POS"
		System.out.println("all senses");
		for(String pair : map.keySet())
		{
			System.out.println(pair + "\t" + formatter.format(map.get(pair)));
		}
		System.out.println();
*/
// max value (i.e. highest score!) : get the highest score for 2 words
		return res.max(word1, word2, pos); // "word1", "word2", "POS
	}
	
	public static void main(String[] args) throws IOException{
		System.out.println(GetSimilarScoreByLin("apple", "apple", "n"));
		System.in.read();
		initWordNet();
		if(IsSameSynset("child","kid"))
			System.out.println("true");
		else 
			System.out.println("false");
		
		initWordNet();
		String token = "leave"; 
		POS pos = POS.VERB;
		WordnetStemmer stemmer = new WordnetStemmer(dict);
		List<String> wordforms = stemmer.findStems(token, pos);
		LinkedHashSet<WordNetLemma> lemmas = new LinkedHashSet<WordNetLemma>();
		for (String wordform : wordforms) {
			lemmas.add(new WordNetLemma(wordform, pos));
		}
		Map<WordNetLemma, List<LinkedHashSet<WordNetLemma>>> antonyms = getAntonymousSynsetLemmas(lemmas, dict);
		System.out.println(antonyms);
		
	}
	
	public static Map<WordNetLemma, List<LinkedHashSet<WordNetLemma>>> getAntonymousSynsetLemmas(LinkedHashSet<WordNetLemma> lemmas, IDictionary _wordnet) {
		Map<WordNetLemma, List<LinkedHashSet<WordNetLemma>>> synonyms = new TreeMap<WordNetLemma, List<LinkedHashSet<WordNetLemma>>>();
		for (WordNetLemma lemma : lemmas) {
			List<LinkedHashSet<WordNetLemma>> partial = getAntonymousSynsetLemmas(lemma, _wordnet);
			// merge
			synonyms.put(lemma, partial);
		}
		return synonyms;
	}

	static List<LinkedHashSet<WordNetLemma>> getAntonymousSynsetLemmas(WordNetLemma lemma, IDictionary _wordnet) {
		List<LinkedHashSet<WordNetLemma>> antonymyLemmas = new LinkedList<LinkedHashSet<WordNetLemma>>();
		IIndexWord indexWord = _wordnet.getIndexWord(lemma._token, lemma._pos);
		if (indexWord == null)
			return antonymyLemmas;
		List<IWordID> wordIDs = indexWord.getWordIDs();
		for (IWordID wordID : wordIDs) {
			IWord word = _wordnet.getWord(wordID); // get the @word
													// corresponding to @wordID
			LinkedHashSet<WordNetLemma> antonymyLemmasOfOneSynset = new LinkedHashSet<WordNetLemma>();
			List<IWordID> antonymousWordIDs = word.getRelatedWords(edu.mit.jwi.item.Pointer.ANTONYM);
			for (IWordID antonymousWordID : antonymousWordIDs) {
				// one corresponding @antonymousWord
				IWord antonymousWord = _wordnet.getWord(antonymousWordID);
				// add this
				String antonymousLemmaToken = antonymousWord.getLemma();
				POS antonymousLemmaPOS = antonymousWord.getPOS();
				WordNetLemma antonymousLemma = new WordNetLemma(antonymousLemmaToken, antonymousLemmaPOS);
				antonymyLemmasOfOneSynset.add(antonymousLemma);
			}
			antonymyLemmas.add(antonymyLemmasOfOneSynset);
		}
		return antonymyLemmas;
	}

	public static void initWordNet() throws IOException{
		dict=new Dictionary(new URL("file", null, fileUrl));
		dict.open();
	}
	 public static void getSynonyms(String wordName,POS wordPos) throws IOException{
		initWordNet();
		// look up first sense of the word "go"
		IIndexWord idxWord = dict.getIndexWord(wordName, wordPos);
		IWordID wordID = idxWord.getWordIDs().get(0); // 1st meaning
		IWord word = dict.getWord(wordID);
		ISynset synset = word.getSynset(); // ISynset是一个词的同义词集的接口

		// iterate over words associated with the synset
		for (IWord w : synset.getWords())
			System.out.println(w.getLemma());// 打印同义词集中的每个同义词
	 }

	 public static void getWordInfoInWordNet(String wordName,POS wordPos){
		IIndexWord idxWord = dict.getIndexWord(wordName, wordPos);// 获取一个索引词，（dog,名词）
		for (int i = 0; i < idxWord.getWordIDs().size(); i++) {
			IWordID wordID = idxWord.getWordIDs().get(0);// 获取dog第一个词义ID
			IWord word = dict.getWord(wordID); // 获取该词
			System.out.println("Id = " + wordID);

			System.out.println(" 词元 = " + word.getLemma());
			System.out.println(" 注解 = " + word.getSynset().getGloss());
		}
	 }
	 
	 public static boolean IsSameSynset(String wordName1,String wordName2) throws IOException{
		IIndexWord idxWord; 
		boolean isSame=false;
		//for(int j=0;j<POS.values().length;j++){
			try {
				idxWord = dict.getIndexWord(wordName1, POS.NOUN);
				IWordID wordID;
				IWord word;
				ISynset synset;
				for (int i = 0; i < idxWord.getWordIDs().size(); i++) {
					wordID = idxWord.getWordIDs().get(0); // 1st meaning
					word = dict.getWord(wordID);
					synset = word.getSynset(); // ISynset是一个词的同义词集的接口

					// iterate over words associated with the synset
					for (IWord w : synset.getWords())
						if (w.getLemma().equals(wordName2))
							isSame = true;
				}
			} catch (Exception ee) {
				isSame = false;
			}
			if (isSame)		return isSame;		
		//}
			try {
				idxWord = dict.getIndexWord(wordName1, POS.VERB);
				IWordID wordID;
				IWord word;
				ISynset synset;
				for (int i = 0; i < idxWord.getWordIDs().size(); i++) {
					wordID = idxWord.getWordIDs().get(0); // 1st meaning
					word = dict.getWord(wordID);
					synset = word.getSynset(); // ISynset是一个词的同义词集的接口

					// iterate over words associated with the synset
					for (IWord w : synset.getWords())
						if (w.getLemma().equals(wordName2))
							isSame = true;
				}
			} catch (Exception ee) {
				isSame = false;
			}
			if (isSame)		return isSame;		
		return isSame;
		
	 }
	 public static void getHypernyms() throws IOException{
		// 获取指定的synset
		IIndexWord idxWord = dict.getIndexWord("article", POS.NOUN);// 获取dog的IndexWord
		IWordID wordID = idxWord.getWordIDs().get(0); // 取出第一个词义的词的ID号
		IWord word = dict.getWord(wordID); // 获取词
		ISynset synset = word.getSynset(); // 获取该词所在的Synset

		//  获取hypernyms
		List<ISynsetID> hypernyms = synset.getRelatedSynsets(Pointer.HYPERNYM);// 通过指针类型来获取相关的词集，其中Pointer类型为HYPERNYM
		// print out each hypernyms id and synonyms
		List<IWord> words;
		for (ISynsetID sid : hypernyms) {
			words = dict.getSynset(sid).getWords(); // 从synset中获取一个Word的list
			System.out.print(sid + "{");
			for (Iterator<IWord> i = words.iterator(); i.hasNext();) {
				System.out.print(i.next().getLemma());
				if (i.hasNext())
					System.out.print(", ");
			}
			System.out.println("}");
		}
	}

	 public static void testRAMDictionary() throws IOException, InterruptedException{

		File wnDir=new File(fileUrl);
		IRAMDictionary dict = new RAMDictionary(wnDir, ILoadPolicy.NO_LOAD);
		dict.open();
		// 周游WordNet
		System.out.print("没装载前：\n");
		trek(dict);
		// now load into memor
		System.out.print("\nLoading Wordnet into memory...");
		long t = System.currentTimeMillis();
		dict.load(true);
		System.out.printf("装载时间：done(%1d msec)\n", System.currentTimeMillis() - t);

		// 装载后在周游
		System.out.print("\n装载后：\n");
		trek(dict);
	}
	 
	/*
	 * this method is Achieved to trek around the WordNet
	 */
	public static void trek(IDictionary dict) {
		int tickNext = 0;
		int tickSize = 20000;
		int seen = 0;
		System.out.print("Treking across Wordnet");
		long t = System.currentTimeMillis();
		for (POS pos : POS.values()) { // 遍历所有词性
			for (Iterator<IIndexWord> i = dict.getIndexWordIterator(pos); i
					.hasNext();) {
				// 遍历某一个词性的所有索引
				for (IWordID wid : i.next().getWordIDs()) {
					// 遍历每一个词的所有义项
					seen += dict.getWord(wid).getSynset().getWords().size();// 获取某一个synsets所具有的词
					if (seen > tickNext) {
						System.out.print(".");
						tickNext = seen + tickSize;
					}
				}
			}
		}

		System.out.printf("done (%1d msec)\n", System.currentTimeMillis() - t);
		System.out.println("In my trek I saw " + seen + " words");
	}
	    
	
	
}
