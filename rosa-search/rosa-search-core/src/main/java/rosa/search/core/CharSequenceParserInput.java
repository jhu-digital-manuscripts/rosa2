package rosa.search.core;


public class CharSequenceParserInput implements ParserInput {
	private final CharSequence input;
	private int next;
	private int mark;
	
	public CharSequenceParserInput(CharSequence input) {
		this.input = input;
		this.next = 0;
		this.mark = -1;
	}
	
	public boolean more() {
		return next < input.length();
	}

	public char next() throws ParseException {
		if (next == input.length()) {
			throw new ParseException(this, "Premature end of input");
		}
		
		return input.charAt(next++);
	}

	public char peek() throws ParseException {
		if (next == input.length()) {
			throw new ParseException(this, "Premature end of input");
		}
		
		return input.charAt(next);
	}

	public void mark() {
		mark = next;
	}

	public void unmark() {
		mark = -1;
	}
	
	public void rewind() {
		next = mark;
	}

	public String marked() {
		return input.subSequence(mark, next).toString();
	}
	
	public String toString() {
		if (next < input.length()) {
			return input.subSequence(0, next) + " >" + input.charAt(next)
			       + "< " + input.subSequence(next + 1, input.length());
		} else {
			return input.toString();
		}
	}
}
