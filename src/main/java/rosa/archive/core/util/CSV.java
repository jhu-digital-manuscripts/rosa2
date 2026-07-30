package rosa.archive.core.util;

import java.io.IOException;
import java.io.Reader;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses comma-separated values with standard quoting. Double-quotes
 * are used to escape commas and newlines within a value.
 */
public final class CSV {

    private CSV() {}

    /**
     * Parses a single CSV row into an array of column values.
     *
     * @param csv the CSV row string
     * @return the parsed column values
     */
    public static String[] parse(String csv) {
        List<String> vals = new ArrayList<>();
        boolean quoted = false;
        var val = new StringBuilder();

        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);

            if (c == '\"') {
                quoted = !quoted;
                if (i > 0 && csv.charAt(i - 1) == '\"') {
                    val.append(c);
                }
            } else if (quoted) {
                val.append(c);
            } else if (c == ',') {
                vals.add(val.toString().trim());
                val.setLength(0);
            } else {
                val.append(c);
            }
        }

        vals.add(val.toString().trim());
        return vals.toArray(new String[0]);
    }

    /**
     * Escapes a value for CSV output, quoting if it contains commas,
     * double-quotes, or newlines.
     *
     * @param val the value to escape
     * @return the escaped value
     */
    public static String escape(String val) {
        if (val == null) {
            return "";
        }

        val = val.replaceAll("\\\"", "\"\"");

        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val + "\"";
        }
        return val;
    }

    /**
     * Parses an entire CSV file into a two-dimensional string array.
     *
     * @param input  the reader
     * @return the parsed table
     * @throws IOException if reading fails
     */
    public static String[][] parseTable(Reader input) throws IOException {
        boolean quoted = false;
        List<List<String>> table = new ArrayList<>();
        var cell = new StringBuilder();
        List<String> row = new ArrayList<>();

        for (;;) {
            int charIn = input.read();

            if (charIn == -1) {
                break;
            } else if (quoted) {
                if (charIn == '\"') {
                    quoted = false;
                } else {
                    cell.append((char) charIn);
                }
            } else if (charIn == '\n') {
                row.add(cell.toString().trim());
                cell.setLength(0);
                table.add(new ArrayList<>(row));
                row.clear();
            } else if (charIn == '\"') {
                quoted = true;
            } else if (charIn == ',') {
                row.add(cell.toString().trim());
                cell.setLength(0);
            } else if (charIn == '\r') {
                // skip
            } else {
                cell.append((char) charIn);
            }
        }

        if (!cell.isEmpty()) {
            row.add(cell.toString().trim());
        }
        if (!row.isEmpty()) {
            table.add(row);
        }

        String[][] result = new String[table.size()][];
        for (int i = 0; i < table.size(); i++) {
            result[i] = table.get(i).toArray(new String[0]);
        }
        return result;
    }

    /**
     * Normalizes whitespace and characters in the table (NFC normalization,
     * contiguous whitespace collapsed to single space).
     *
     * @param table the table to normalize in place
     */
    public static void normalizeWhiteSpaceAndCharacters(String[][] table) {
        for (String[] row : table) {
            for (int i = 0; i < row.length; i++) {
                row[i] = row[i].replaceAll("\\s+", " ").trim();
                row[i] = Normalizer.normalize(row[i], Form.NFC);
            }
        }
    }
}
