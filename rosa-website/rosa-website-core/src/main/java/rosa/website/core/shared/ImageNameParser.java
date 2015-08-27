package rosa.website.core.shared;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public class ImageNameParser {
    private static final RegExp PAGE_PATTER = RegExp.compile("([a-zA-Z]*)(\\d+)(r|v|R|V)");

    /**
     *
     * @param page page name that includes page number and recto/verso designation
     *             EX: #r, ##v, etc
     * @return standard format with three digit page number: ###r OR ###v
     * @throws NullPointerException if {@param page} is NULL
     */
    public static String toStandardName(String page) {
        if (page == null) {
            return null;
        }
        String[] parts = page.replaceAll(" ", ".").split("\\.");

        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (isNamePart(part)) {
                sb.append(part.equals("frontmatter") || part.equals("endmatter") ?
                        part.replace("matter", " matter") : part);
                sb.append(' ');
            } else if (isFolio(part)) {
                sb.append(folio(part));
                sb.append(' ');
            }
        }

        return sb.toString().trim();
    }

    // GWT does not support String.format()
    private static String folio(String part) {
        if (part == null || part.isEmpty() || !PAGE_PATTER.test(part)) {
            return "";
        }

        MatchResult m = PAGE_PATTER.exec(part);
        int n = Integer.parseInt(m.getGroup(2));

        StringBuilder page = new StringBuilder(m.getGroup(1));

        if (n < 10) {
            page.append("00");
        } else if (n < 100) {
            page.append("0");
        }
        page.append(n);
        page.append(m.getGroup(3));

        return page.toString();
    }

    /**
     * There are several standard names to sections of a book. Front matter, body matter,
     * and end matter.
     *
     * @param part string to test
     * @return is this string part of an image name?
     */
    private static boolean isNamePart(String part) {
        return part.equalsIgnoreCase("frontmatter")
                || part.equalsIgnoreCase("endmatter")
                || part.equalsIgnoreCase("front")
                || part.equalsIgnoreCase("end")
                || part.equalsIgnoreCase("matter");
    }

    private static boolean isFolio(String part) {
        return PAGE_PATTER.test(part);
    }

}
