package rosa.archive.core.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse comma separated values with '\' used as an escape. Spaces between
 * values are ignored.
 *
 * Copied from earlier CSV class in rosa1 project.
 */
public class CSV {
    public static void normalize(File file) throws IOException {
        String[][] table = CSV.parseTable(new FileReader(file));
        CSV.normalizeWhiteSpaceAndCharacters(table);

        FileWriter out = new FileWriter(file);
        CSV.write(out, table);
        out.close();
    }

    /**
     *
     * @param csv
     *          a csv row of data
     * @return
     *          the columns of a row in a String array
     */
    public static String[] parse(String csv) {
        List<String> vals = new ArrayList<>();
        boolean quoted = false;

        StringBuilder val = new StringBuilder();

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

        return vals.toArray(new String[vals.size()]);
    }

    /**
     * Properly escape a String so that it may be used as a single cell value in a CSV.
     *
     * <p>
     *     CSV files rely on commas (',') to separate values laid out in a table format.
     *     However, it is possible for a single cell in the table to contain zero or more commas
     *     as part if its data. These must be taken into account while parsing or writing CSV
     *     data. Other special characters must be taken into consideration as well for similar
     *     reasons.
     * </p>
     * <p>
     *     This method takes the value to be put in a single cell in the CSV file and surrounds
     *     the data with quotation marks ("value goes here") if a special character is encountered
     *     in the data string. If no special characters are found within the data string, the
     *     original string is returned.
     * </p>
     *
     * @param val
     *          value to escape
     * @return
     *          escaped String
     */
    public static String escape(String val) {
        if (val == null) {
            return "";
        }

        val = val.replaceAll("\\\"", "\"\"");

        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val + "\"";
        } else {
            return val;
        }
    }

    /**
     *
     * @param input
     *          reader
     * @return
     *          2D String array holding the CSV cell data
     * @throws IOException if inaccessible
     */
    public static String[][] parseTable(Reader input) throws IOException {
        boolean quoted = false;
        List<List<String>> table = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        List<String> row = new ArrayList<>();

        for (;;) {
            int char_in = input.read();
            int last = -1;

            if (char_in == -1) {
                break;
            } else if (quoted) {
                if (char_in == '\"') {
                    quoted = false;
                } else {
                    cell.append((char) char_in);
                }
            } else if (char_in == '\n') {
                row.add(cell.toString().trim());
                cell.setLength(0);

                table.add(new ArrayList<>(row));
                row.clear();
            } else if (char_in == '\"') {
                if (char_in == last) {
                    cell.append((char) char_in);
                }

                quoted = true;
            } else if (char_in == ',') {
                row.add(cell.toString().trim());
                cell.setLength(0);
            } else if (char_in == '\r') {
                // do nothing
            } else {
                cell.append((char) char_in);
            }

            last = char_in;
        }

        if (cell.length() > 0) {
            row.add(cell.toString().trim());
        }

        if (row.size() > 0) {
            table.add(row);
        }

        String[][] result = new String[table.size()][];

        for (int i = 0; i < table.size(); i++) {
            result[i] = table.get(i).toArray(new String[table.get(i).size()]);
        }

        return result;
    }

    /**
     * Trim whitespace from each value and convert contiguous whitespace in a
     * value to a single space and also normalize characters.
     *
     * @param table
     *          csv data
     */
    public static void normalizeWhiteSpaceAndCharacters(String[][] table) {
        for (String[] row : table) {
            for (int i = 0; i < row.length; i++) {
                row[i] = row[i].replaceAll("\\s+", " ").trim();
                row[i] = Normalizer.normalize(row[i], Form.NFC);
            }
        }
    }

    /**
     *
     * @param out
     *          writer
     * @param table
     *          output data
     * @throws IOException if inaccessible
     */
    public static void write(Writer out, String[]... table) throws IOException {
        for (String[] row : table) {
            for (int i = 0; i < row.length; i++) {
                out.append(escape(row[i]));

                if (i != row.length - 1) {
                    out.append(',');
                }
            }

            out.append('\n');
        }

        out.flush();
    }
}
