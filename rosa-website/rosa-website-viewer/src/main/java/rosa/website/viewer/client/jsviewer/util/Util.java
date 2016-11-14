package rosa.website.viewer.client.jsviewer.util;

import java.util.ArrayList;
import java.util.List;

public class Util {
    public static String[] parseCSV(String csv) {
        List<String> vals = new ArrayList<String>();
        boolean quoted = false;

        StringBuffer val = new StringBuffer();

        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);

            if (c == '\"') {
                if (quoted) {
                    quoted = false;
                } else {
                    quoted = true;
                }

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

        return (String[]) vals.toArray(new String[] {});
    }

    /**
     * Does not handle newlines in cell values. We ensure cells do not have
     * newlines on client side.
     * 
     * @param csv CSV data as a string
     * @return table
     */
    public static String[][] parseCSVTable(String csv) {
        String[] rows = csv.split("\n");
        String[][] result = new String[rows.length][];

        for (int row = 0; row < rows.length; row++) {
            result[row] = Util.parseCSV(rows[row]);
        }

        return result;
    }
}
