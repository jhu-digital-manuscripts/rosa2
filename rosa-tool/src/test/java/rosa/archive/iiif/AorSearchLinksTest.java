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
    void buildsAdvancedSearchUrl() {
        assertEquals("https://example.org/aor/#aor/search?q=people:'Plutarch'&type=advanced",
                links.href("people", "Plutarch"));
        assertEquals("https://example.org/aor/#aor/search?q=locations:'Troy'&type=advanced",
                links.href("locations", "Troy"));
    }

    @Test
    void spacesBecomePlusAndWhitespaceIsCleaned() {
        assertEquals("https://example.org/aor/#aor/search?q=people:'Dionysius+of+Halicarnassus'&type=advanced",
                links.href("people", " Dionysius  of\nHalicarnassus "));
    }

    @Test
    void escapesQuotesAndBackslashesForTheQueryGrammar() {
        // ' is escaped as \' in the grammar; \ is percent-encoded, ' is not
        assertEquals("https://example.org/aor/#aor/search?q=people:'O%5C'Neill'&type=advanced",
                links.href("people", "O'Neill"));
        assertEquals("https://example.org/aor/#aor/search?q=books:'a%5C%5Cb'&type=advanced",
                links.href("books", "a\\b"));
    }

    @Test
    void percentEncodesReservedAndNonAsciiCharacters() {
        assertEquals("https://example.org/aor/#aor/search?q=books:'Tom+%26+Jerry+%2B+%3F%3D%23'&type=advanced",
                links.href("books", "Tom & Jerry + ?=#"));
        assertEquals("https://example.org/aor/#aor/search?q=people:'%C3%89rasme'&type=advanced",
                links.href("people", "Érasme"));
        // Characters route.js keeps readable
        assertEquals("https://example.org/aor/#aor/search?q=books:'De+re:+a,b/c'&type=advanced",
                links.href("books", "De re: a,b/c"));
    }

    @Test
    void blankValuesHaveNoLink() {
        assertNull(links.href("people", null));
        assertNull(links.href("people", "  "));
    }

    @Test
    void requiresViewerUrlAndCollection() {
        assertThrows(IllegalArgumentException.class, () -> new AorSearchLinks(" ", "aor"));
        assertThrows(IllegalArgumentException.class, () -> new AorSearchLinks("https://x/", null));
    }
}
