package rosa.website.pizan.client;

import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.resources.client.TextResource;
import rosa.website.model.csv.CSVType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HistoryConfig {
    private static final WebsiteConfig CONFIG = WebsiteConfig.INSTANCE;
    private static final PizanHtml HTMLS = PizanHtml.INSTANCE;

    private static Set<String> htmlPages;
    private static Set<String> csvPages;

    /**
     * @param history history token
     * @return does the history token belong to a static HTML page?
     */
    public static boolean isValidHtmlPage(String history) {
        if (htmlPages == null) {
            htmlPages = new HashSet<>(Arrays.asList(CONFIG.htmlHistory().split(",")));
        }

        return htmlPages.contains(history);
    }

    /**
     * @param history history token
     * @return does the history token belong to a CSV data page?
     */
    public static boolean isValidCsvPage(String history) {
        if (csvPages == null) {
            csvPages = new HashSet<>(Arrays.asList(CONFIG.csvHistory().split(",")));
        }

        return csvPages.contains(history);
    }

    /**
     * Get an external HTML resource according to its name. If no resource
     * is found associated with a given name, NULL is returned.
     *
     * @param history name associated with the HTML resource
     * @return external text resource, or NULL if name not found
     */
    public static ExternalTextResource getHtml(String history) {
        // TODO make more dynamic or interact with config? Cache these resources (here or in the Activity)?
        switch (history) {
            case "contact":
                return HTMLS.contactHtml();
            case "partners":
                return HTMLS.partnersHtml();
            case "terms":
                return HTMLS.termsHtml();
            case "pizan":
                return HTMLS.pizanHtml();
            case "names":
                return HTMLS.properNamesHtml();
            case "works":
                return HTMLS.worksHtml();
            default:
                return null;
        }
    }

    /**
     * Get an external HTML resource containing prose description of a
     * CSV data set.
     *
     * @param history name associated with a particular CSV
     * @return HTML description, or NULL if not found
     */
    public static ExternalTextResource getCsvDescription(String history) {
        switch (history) {
            case "chars":
                return HTMLS.characterNamesHtml();
            case "corpus":
                return HTMLS.collectionDataHtml();
            case "illustrations":
                return HTMLS.illustrationTitlesHtml();

            default:
                return null;
        }
    }

    public static TextResource getHomeHtml() {
        return HTMLS.homeHtml();
    }

    /**
     * Get the CsvType associated with a history token.
     *
     * @param history history token
     * @return the csv type
     */
    public static CSVType getCsvType(String history) {
        switch (history) {
            case "corpus":
                return CSVType.COLLECTION_DATA;
            case "data":
                return CSVType.COLLECTION_BOOKS;
            case "illustrations":
                return CSVType.ILLUSTRATIONS;
            case "chars":
                return CSVType.CHARACTERS;
            case "sections":
                return CSVType.NARRATIVE_SECTIONS;
            default:
                return null;
        }
    }
}
