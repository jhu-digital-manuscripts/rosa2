package rosa.search.core;


public class ParseException extends Exception {
	private static final long serialVersionUID = 1L;

	public ParseException(String message, Throwable cause) {
		super(message, cause);
	}

    public ParseException(ParserInput input, String message) {
        super(message + "\n" + input);
    }
    
	/**
	 * @param input
	 * @param type of object being parsed
	 * @param message about the parse failure
	 */
    public ParseException(ParserInput input, String type, String message) {
        super("Parsing " + type + "\n" + message + "\n" + input);
    }
}
