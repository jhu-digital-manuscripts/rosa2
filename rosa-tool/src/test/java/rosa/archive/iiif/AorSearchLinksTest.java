package rosa.archive.iiif;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AorSearchLinks}. The expected values are what the viewer's route
 * parser (rosa-viewer/src/site/history/route.js and advancedQuery.js) reads back.
 */
class AorSearchLinksTest {

    private final AorSearchLinks links = new AorSearchLinks("https://example.org/aor/", "aor");

    @Test
    void buildsAdvancedSearchUrlForAPhrase() {
        assertEquals("https://example.org/aor/#aor/search?q=people:'%22Plutarch%22'&type=advanced",
                links.href("people", "Plutarch"));
        assertEquals("https://example.org/aor/#aor/search?q=locations:'%22Troy%22'&type=advanced",
                links.href("locations", "Troy"));
    }

    @Test
    void spacesBecomePlusAndWhitespaceIsCleaned() {
        assertEquals("https://example.org/aor/#aor/search?q=people:'%22Dionysius+of+Halicarnassus%22'&type=advanced",
                links.href("people", " Dionysius  of\nHalicarnassus "));
    }

    @Test
    void escapesQuotesAndBackslashesForTheQueryGrammar() {
        // ' is escaped as \' in the grammar; \ is percent-encoded, ' is not
        assertEquals("https://example.org/aor/#aor/search?q=people:'%22O%5C'Neill%22'&type=advanced",
                links.href("people", "O'Neill"));
        assertEquals("https://example.org/aor/#aor/search?q=books:'%22a%5C%5Cb%22'&type=advanced",
                links.href("books", "a\\b"));
    }

    @Test
    void percentEncodesReservedAndNonAsciiCharacters() {
        assertEquals("https://example.org/aor/#aor/search?q=books:'%22Tom+%26+Jerry+%2B+%3F%3D%23%22'&type=advanced",
                links.href("books", "Tom & Jerry + ?=#"));
        assertEquals("https://example.org/aor/#aor/search?q=people:'%22%C3%89rasme%22'&type=advanced",
                links.href("people", "Érasme"));
        // Characters route.js keeps readable
        assertEquals("https://example.org/aor/#aor/search?q=books:'%22De+re:+a,b/c%22'&type=advanced",
                links.href("books", "De re: a,b/c"));
    }

    @Test
    void buildsPageUrls() {
        assertEquals("https://example.org/aor/#aor/RCPD1266/037v/image", links.pageHref("RCPD1266", "037v"));
        assertEquals("https://example.org/aor/#aor/RCPD1266/i14/image", links.pageHref("RCPD1266", "i14"));
        assertEquals("https://example.org/aor/#aor/RCP10549-50v1/misc.insert.07/image",
                links.pageHref("RCP10549-50v1", "misc.insert.07"));
        assertEquals("https://example.org/aor/#aor/A%20B/1r/image", links.pageHref("A B", "1r"));
    }

    @Test
    void blankValuesHaveNoLink() {
        assertNull(links.href("people", null));
        assertNull(links.href("people", "  "));
        assertNull(links.href("people", " \"\" "));
    }

    @Test
    void dropsDoubleQuotesFromTheValue() {
        // The value is itself quoted to search as a phrase; quotes inside it would end it
        assertEquals("https://example.org/aor/#aor/search?q=books:'%22The+Prince%22'&type=advanced",
                links.href("books", "\"The\" Prince"));
    }

    @Test
    void requiresViewerUrlAndCollection() {
        assertThrows(IllegalArgumentException.class, () -> new AorSearchLinks(" ", "aor"));
        assertThrows(IllegalArgumentException.class, () -> new AorSearchLinks("https://x/", null));
    }
}
