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

	public CSVSpreadSheet(File file, int mincols, int maxcols,
			List<String> errors) throws IOException {
		this(new FileReader(file), mincols, maxcols, errors);
	}

	public int size() {
		return table.length;
	}

	public String get(int row, int col) {
		// Check for ragged rows
		if (col >= table[row].length) {
			return "";
		}

		return table[row][col];
	}

	public String set(int row, int col, String value) {
		// Check for ragged rows
		if (col >= table[row].length) {
			return "";
		}

		return table[row][col] = Normalizer.normalize(value, Form.NFC);
	}

	public void serialize(Writer out) throws IOException {
		CSV.write(out, table);
	}
}
