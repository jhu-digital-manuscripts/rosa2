package rosa.search.core;


public interface ParserInput {
	/**
	 * @return if there is more input
	 */
	public boolean more();

	/**
	 * @return the next character and iterate
	 * @throws ParseException .
	 */

	public char next() throws ParseException;
	
	/**
	 * @return the next character without iterating.
	 * @throws ParseException .
	 */
	public char peek() throws ParseException;
	
	/**
	 * Sets the mark to the next character.
	 */
	public void mark();
	
	/**
	 * Clear the mark.
	 */
	public void unmark();
	
	/**
	 * Rewind to mark.
	 */
	public void rewind();
	
	/**
	 * @return string starting at mark and ending at 
	 *         char returned by last next 
	 */
	public String marked();
}
