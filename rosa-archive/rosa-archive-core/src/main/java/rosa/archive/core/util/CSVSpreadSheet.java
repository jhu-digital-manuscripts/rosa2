package rosa.archive.core.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.List;

/**
 * Access CSV spreadsheet as a table. All text is normalized to NFC. All
 * whitespace is normalized to a single space.
 */
public class CSVSpreadSheet {
	private final String[][] table;

    /**
     * @param input input reader
     * @param mincols minimum number of columns expected
     * @param maxcols maximum number of columns expected
     * @param errors list of errors
     * @throws IOException if the reader is inaccessible
     */
	public CSVSpreadSheet(Reader input, int mincols, int maxcols,
			List<String> errors) throws IOException {
		this.table = CSV.parseTable(input);
		CSV.normalizeWhiteSpaceAndCharacters(table);

		if (errors != null) {
			for (int row = 0; row < table.length; row++) {
				if (table[row].length > maxcols) {
					errors.add("Row " + row + " has too many columns");
				}

				if (table[row].length < mincols) {
					errors.add("Row " + row + " has too few columns");
				}
			}
		}
	}

    /**
     * @param file file containing the data
     * @param mincols minimum number of columns expected
     * @param maxcols maximum number of columns expected
     * @param errors list of errors
     * @throws IOException if the file is inaccessible
     */
	public CSVSpreadSheet(File file, int mincols, int maxcols,
			List<String> errors) throws IOException {
		this(new FileReader(file), mincols, maxcols, errors);
	}

    /**
     * @return number of rows
     */
	public int size() {
		return table.length;
	}

    /**
     * @param row row index
     * @param col column index
     * @return contents of a cell
     */
	public String get(int row, int col) {
		// Check for ragged rows
		if (col >= table[row].length) {
			return "";
		}

		return table[row][col];
	}

    /**
     * @param index row index
     * @return array containing the contents of a row
     */
    public String[] row(int index) {
        if (index > table.length) {
            return new String[] {};
        }
        return table[index];
    }

    /**
     * Set the contents of a cell. The value is normalized first.
     * If the row and column indices are out of bounds, no cell
     * in the spreadsheet will change.
     *
     * @param row row index
     * @param col column index
     * @param value value to set
     * @return the value that was set, or an empty string if out of bounds
     */
	public String set(int row, int col, String value) {
		// Check for ragged rows
		if (col >= table[row].length) {
			return "";
		}

		return table[row][col] = Normalizer.normalize(value, Form.NFC);
	}

    /**
     * Write this spread sheet out to the archive.
     *
     * @param out writer
     * @throws IOException if the writer is inaccessible
     */
	public void serialize(Writer out) throws IOException {
		CSV.write(out, table);
	}
}
