import edu.mit.jwi.item.POS;

public class WordNetLemma implements Comparable<WordNetLemma> {
	public String _token = "";
	public POS _pos = null; 
	public WordNetLemma(String word) {
	_token = word; _pos = null;
	}
	public WordNetLemma(String word, POS pos) {
	this(word);
	_pos = pos;
	}

	public boolean isPureWordForm() {
	return _pos == null;
	}

	public String toString() {
		if (isPureWordForm()) 
			return _token;
		else
			return _token + "." + _pos.toString();
		}

	public int compareTo(WordNetLemma o) {
		// TODO Auto-generated method stub
		return this.toString().compareTo(o.toString());
	}

	public boolean equals(Object o) {
		return (_token.equals(((WordNetLemma)o)._token) && _pos.equals(((WordNetLemma)o)._pos));
	}

	public int hashCode() {
		int result, c; 
		result = 17;
		c = _token.hashCode();
		result = 37 * result + c;
		c = _pos.hashCode();
		result = 37 * result + c;
		return result;
		}
	}