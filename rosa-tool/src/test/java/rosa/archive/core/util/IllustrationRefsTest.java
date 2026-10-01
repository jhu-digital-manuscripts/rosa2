package rosa.archive.core.util;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTitles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests resolution of illustration tagging references against the collection
 * level lookup tables.
 */
class IllustrationRefsTest {

    private BookCollection collection;
    private Book book;

    @BeforeEach
    void setUp() {
        collection = new BookCollection();
        collection.setId("rose");

        IllustrationTitles titles = new IllustrationTitles();
        titles.setData(Map.of("8", "Portrait of Haine", "11", "Portrait of Covoitise"));
        collection.setIllustrationTitles(titles);

        CharacterNames charNames = new CharacterNames();
        charNames.addCharacterName(character("50", "Haine", "Hayne, Haïne", "Hate"));
        charNames.addCharacterName(character("90", "Oiseuse", null, null));
        charNames.addCharacterName(character("91", "Pitie", null, "Pitie"));
        collection.setCharacterNames(charNames);

        book = new Book();
        book.setId("Francais802");
    }

    private CharacterName character(String id, String siteName, String french, String english) {
        CharacterName name = new CharacterName();
        name.setId(id);
        if (siteName != null) {
            name.addName(siteName, "Site name");
        }
        if (french != null) {
            name.addName(french, "French variant");
        }
        if (english != null) {
            name.addName(english, "English name");
        }
        return name;
    }

    private Illustration illustration(String[] titles, String[] characters) {
        Illustration illus = new Illustration();
        illus.setId("2");
        illus.setPage("2r");
        illus.setTitles(titles);
        illus.setCharacters(characters);
        return illus;
    }

    @Nested
    @DisplayName("Title references")
    class Titles {
        @Test
        void numericReferenceIsResolved() {
            Illustration illus = illustration(new String[]{"8"}, new String[0]);
            assertEquals(List.of("Portrait of Haine"), IllustrationRefs.resolveTitles(collection, book, illus));
        }

        @Test
        void referencesAreResolvedInSpreadsheetOrder() {
            Illustration illus = illustration(new String[]{"11", "8"}, new String[0]);
            assertEquals(List.of("Portrait of Covoitise", "Portrait of Haine"),
                    IllustrationRefs.resolveTitles(collection, book, illus));
        }

        @Test
        void literalIsPassedThroughAndMixedWithReferences() {
            Illustration illus = illustration(new String[]{"8", "The Trinity"}, new String[0]);
            assertEquals(List.of("Portrait of Haine", "The Trinity"),
                    IllustrationRefs.resolveTitles(collection, book, illus));
        }

        @Test
        void blankEntriesAreSkipped() {
            Illustration illus = illustration(new String[]{"", "  ", "8"}, new String[0]);
            assertEquals(List.of("Portrait of Haine"), IllustrationRefs.resolveTitles(collection, book, illus));
        }

        @Test
        void unknownReferenceFails() {
            Illustration illus = illustration(new String[]{"999"}, new String[0]);
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> IllustrationRefs.resolveTitles(collection, book, illus));
            assertTrue(e.getMessage().contains("999"), e.getMessage());
            assertTrue(e.getMessage().contains("Francais802"), e.getMessage());
        }

        @Test
        void missingLookupTableFails() {
            collection.setIllustrationTitles(null);
            Illustration illus = illustration(new String[]{"8"}, new String[0]);
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> IllustrationRefs.resolveTitles(collection, book, illus));
            assertTrue(e.getMessage().contains("illustration_titles.csv"), e.getMessage());
        }
    }

    @Nested
    @DisplayName("Character references")
    class Characters {
        @Test
        void displayNameCombinesSiteNameAndEnglishName() {
            Illustration illus = illustration(new String[0], new String[]{"50"});
            assertEquals(List.of("Haine (Hate)"),
                    IllustrationRefs.resolveCharacterDisplayNames(collection, book, illus));
        }

        @Test
        void displayNameFallsBackToSiteNameAlone() {
            Illustration illus = illustration(new String[0], new String[]{"90"});
            assertEquals(List.of("Oiseuse"),
                    IllustrationRefs.resolveCharacterDisplayNames(collection, book, illus));
        }

        @Test
        void displayNameIsNotRepeatedWhenNamesMatch() {
            Illustration illus = illustration(new String[0], new String[]{"91"});
            assertEquals(List.of("Pitie"),
                    IllustrationRefs.resolveCharacterDisplayNames(collection, book, illus));
        }

        @Test
        void literalsAreMixedWithReferencesInOrder() {
            Illustration illus = illustration(new String[0], new String[]{"50", "Jalous' wife", "90"});
            assertEquals(List.of("Haine (Hate)", "Jalous' wife", "Oiseuse"),
                    IllustrationRefs.resolveCharacterDisplayNames(collection, book, illus));
        }

        @Test
        void allNamesIncludesEveryVariant() {
            Illustration illus = illustration(new String[0], new String[]{"50", "dancers"});
            List<String> names = IllustrationRefs.resolveAllCharacterNames(collection, book, illus);
            assertTrue(names.containsAll(List.of("Haine", "Hayne, Haïne", "Hate", "dancers")),
                    "Expected every name variant and the literal: " + names);
        }

        @Test
        void unknownReferenceFails() {
            Illustration illus = illustration(new String[0], new String[]{"999"});
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> IllustrationRefs.resolveCharacterDisplayNames(collection, book, illus));
            assertTrue(e.getMessage().contains("999"), e.getMessage());
        }

        @Test
        void missingLookupTableFails() {
            collection.setCharacterNames(null);
            Illustration illus = illustration(new String[0], new String[]{"50"});
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> IllustrationRefs.resolveAllCharacterNames(collection, book, illus));
            assertTrue(e.getMessage().contains("character_names.csv"), e.getMessage());
        }

        @Test
        void literalsNeedNoLookupTable() {
            collection.setCharacterNames(null);
            Illustration illus = illustration(new String[0], new String[]{"dancers", "musician"});
            assertEquals(List.of("dancers", "musician"),
                    IllustrationRefs.resolveCharacterDisplayNames(collection, book, illus));
        }
    }

    @Test
    void isReferenceOnlyAcceptsDigits() {
        assertTrue(IllustrationRefs.isReference("50"));
        assertEquals(false, IllustrationRefs.isReference("50a"));
        assertEquals(false, IllustrationRefs.isReference("Jalous' wife"));
        assertEquals(false, IllustrationRefs.isReference(""));
        assertEquals(false, IllustrationRefs.isReference(null));
    }
}
