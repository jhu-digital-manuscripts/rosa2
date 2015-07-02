package rosa.website.viewer.client.jsviewer.util;

import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;

import java.util.ArrayList;
import java.util.List;

public class Util {

    public static String extractText(Node n) {
        StringBuffer buf = new StringBuffer();
        extractText(n, buf);

        return buf.toString();
    }

    private static void extractText(Node n, StringBuffer buf) {
        if (n.getNodeType() == Node.TEXT_NODE) {
            buf.append(n.getNodeValue());
        }

        for (n = n.getFirstChild(); n != null; n = n.getNextSibling()) {
            extractText(n, buf);
        }
    }

    public static Node getFirstChild(Node n, String name) {
        for (n = n.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeName().equals(name)) {
                return n;
            }
        }

        return null;
    }

    public static String getFirstElementValue(Element parent, String name) {
        NodeList l = parent.getElementsByTagName(name);

        if (l.getLength() == 0) {
            return null;
        } else {
            return extractText(l.item(0));
        }
    }

    public static String[] getElementValues(Element parent, String name) {
        NodeList l = parent.getElementsByTagName(name);
        String[] values = new String[l.getLength()];

        for (int i = 0; i < values.length; i++) {
            values[i] = extractText(l.item(i));
        }

        return values;
    }

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
     * @param csv
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

    /**
     * Popup window and display the given html. Name must not contain spaces or
     * special chars.
     * 
     * @param name
     * @param width
     * @param height
     * @param html
     * @param opts
     */
    public static native void popupWindowHTML(String name, int width, int height, String html, String opts) /*-{
                                                                                                            var w = $wnd.open('', name, "height=" + height + ",width=" + width + "," + opts);
                                                                                                            w.document.write(html);
                                                                                                            w.document.close();
                                                                                                            w.focus();
                                                                                                            }-*/;

    public static native void popupWindowURL(String name, int width, int height, String url, String opts) /*-{
                                                                                                          var w = $wnd.open(url, name, "height=" + height + ",width=" + width + "," + opts);
                                                                                                          w.document.close();
                                                                                                          w.focus();
                                                                                                          }-*/;

    public static native String appLastModified() /*-{
                                                  return $doc.lastModified;
                                                  }-*/;

    private static final int findStartOfLastSequenceOfDigits(String s) {
        int last = -1;

        for (int i = s.length() - 1; i >= 0; i--) {
            if (Character.isDigit(s.charAt(i))) {
                last = i;
            } else {
                break;
            }
        }

        return last;
    }

    public static int compareStringsPossiblyEndingWithNumbers(String s1, String s2) {
        int i1 = findStartOfLastSequenceOfDigits(s1);
        int i2 = findStartOfLastSequenceOfDigits(s2);

        if (i1 == -1 || i2 == -1) {
            return s1.compareTo(s2);
        }

        int compare = s1.substring(0, i1).compareTo(s2.substring(0, i2));

        if (compare == 0) {
            int n1 = Integer.parseInt(s1.substring(i1));
            int n2 = Integer.parseInt(s2.substring(i2));

            return n1 - n2;
        }

        return compare;
    }

    /**
     * Parse arguments encoded with toToken.
     * 
     * @param token
     * @return
     */
    public static List<String> parseTokenArguments(String token) {
        List<String> args = new ArrayList<String>(4);

        StringBuilder arg = new StringBuilder();

        // Arguments are after the prefix

        int i = token.indexOf(';');

        if (i == -1) {
            return args;
        }

        boolean escaped = false;

        for (i++; i < token.length(); i++) {
            char c = token.charAt(i);

            if (escaped) {
                arg.append(c);
                escaped = false;
            } else {
                if (c == '-') {
                    escaped = true;
                } else if (c == ';') {
                    args.add(arg.toString());
                    arg.setLength(0);
                } else {
                    arg.append(c);
                }
            }
        }

        if (arg.length() > 0) {
            args.add(arg.toString());
        }

        return args;
    }

    /**
     * Build up a token with the given arguments. Null arguments are ignored.
     * The prefix is returned if there are no arguments.
     * 
     * @param prefix
     * @param args
     * @return
     */

    public static String toToken(String prefix, String... args) {
        if (args.length == 0) {
            return prefix;
        }

        StringBuilder sb = new StringBuilder(prefix);

        // Arguments separated by ';'.
        // Escape ';' and '\' with '\'.

        for (String arg : args) {
            if (arg == null) {
                continue;
            }

            sb.append(';');
            sb.append(arg.replaceAll("(;|-)", "-$1"));
        }

        return sb.toString();
    }
}
