package rosa.archive.core.util;

import java.util.ArrayList;
import java.util.List;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTitles;

/**
 * Resolves the references held in a book's illustration tagging spreadsheet
 * ({@code <book>.imagetag.csv}) against the collection level lookup tables
 * {@code illustration_titles.csv} and {@code character_names.csv}.
 *
 * <p>The "Illustration title" and "Characters" columns hold comma separated
 * entries. A purely numeric entry is a reference into the corresponding lookup
 * table; anything else is a literal value that is used as it stands.
 *
 * <p>An unresolvable numeric reference is a data error and causes an
 * {@link IllegalStateException}. Silently emitting the raw id would surface bare
 * numbers to readers.
 */
public final class IllustrationRefs {

    private IllustrationRefs() {
    }

    /**
     * Checks whether a spreadsheet entry is a reference into a lookup table
     * rather than a literal value.
     *
     * @param value the entry
     * @return {@code true} if the entry is a numeric reference
     */
    public static boolean isReference(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resolves the illustration titles of an illustration to their display text.
     *
     * @param collection   the collection holding {@code illustration_titles.csv}
     * @param book         the book the illustration belongs to
     * @param illustration the illustration
     * @return the resolved titles, in spreadsheet order
     * @throws IllegalStateException if a numeric title reference cannot be resolved
     */
    public static List<String> resolveTitles(BookCollection collection, Book book, Illustration illustration) {
        List<String> result = new ArrayList<>();
        String[] titleIds = illustration.getTitles();

        if (titleIds == null) {
            return result;
        }

        IllustrationTitles titles = collection.getIllustrationTitles();

        for (String titleId : titleIds) {
            if (titleId == null || titleId.isBlank()) {
                continue;
            }

            if (!isReference(titleId)) {
                result.add(titleId.trim());
                continue;
            }

            if (titles == null) {
                throw new IllegalStateException("Cannot resolve illustration title ID '" + titleId
                        + "': no illustration_titles.csv loaded for collection '" + collection.getId() + "'");
            }

            String resolved = titles.getTitleById(titleId);
            if (resolved == null || resolved.isBlank()) {
                throw new IllegalStateException("Cannot resolve illustration title ID '" + titleId + "' in "
                        + context(collection, book, illustration));
            }

            result.add(resolved.trim());
        }

        return result;
    }

    /**
     * Resolves the characters of an illustration to a single display name each.
     *
     * @param collection   the collection holding {@code character_names.csv}
     * @param book         the book the illustration belongs to
     * @param illustration the illustration
     * @return the resolved character names, in spreadsheet order
     * @throws IllegalStateException if a numeric character reference cannot be resolved
     * @see CharacterName#getDisplayName()
     */
    public static List<String> resolveCharacterDisplayNames(BookCollection collection, Book book,
                                                            Illustration illustration) {
        List<String> result = new ArrayList<>();

        for (CharacterRef ref : resolveCharacters(collection, book, illustration)) {
            switch (ref) {
                case Known(CharacterName name) -> {
                    String display = name.getDisplayName();
                    result.add(display == null ? name.getId() : display);
                }
                case Literal(String value) -> result.add(value);
            }
        }

        return result;
    }

    /**
     * Resolves the characters of an illustration to every known name of each
     * character. Intended for search indexing, where all name variants should
     * match.
     *
     * @param collection   the collection holding {@code character_names.csv}
     * @param book         the book the illustration belongs to
     * @param illustration the illustration
     * @return all names of the referenced characters, plus any literal values
     * @throws IllegalStateException if a numeric character reference cannot be resolved
     */
    public static List<String> resolveAllCharacterNames(BookCollection collection, Book book,
                                                        Illustration illustration) {
        List<String> result = new ArrayList<>();

        for (CharacterRef ref : resolveCharacters(collection, book, illustration)) {
            switch (ref) {
                case Known(CharacterName name) -> result.addAll(name.getAllNames());
                case Literal(String value) -> result.add(value);
            }
        }

        return result;
    }

    /** A resolved character entry: either a known character or a literal value. */
    private sealed interface CharacterRef {
    }

    private record Known(CharacterName name) implements CharacterRef {
    }

    private record Literal(String value) implements CharacterRef {
    }

    /**
     * Resolves each character entry to either a known character or a literal value.
     */
    private static List<CharacterRef> resolveCharacters(BookCollection collection, Book book,
                                                        Illustration illustration) {
        List<CharacterRef> result = new ArrayList<>();
        String[] charIds = illustration.getCharacters();

        if (charIds == null) {
            return result;
        }

        CharacterNames charNames = collection.getCharacterNames();

        for (String charId : charIds) {
            if (charId == null || charId.isBlank()) {
                continue;
            }

            if (!isReference(charId)) {
                result.add(new Literal(charId.trim()));
                continue;
            }

            if (charNames == null) {
                throw new IllegalStateException("Cannot resolve character ID '" + charId
                        + "': no character_names.csv loaded for collection '" + collection.getId() + "'");
            }

            CharacterName name = charNames.getCharacterName(charId);
            if (name == null) {
                throw new IllegalStateException("Cannot resolve character ID '" + charId + "' in "
                        + context(collection, book, illustration));
            }

            result.add(new Known(name));
        }

        return result;
    }

    private static String context(BookCollection collection, Book book, Illustration illustration) {
        return "collection '" + (collection == null ? null : collection.getId())
                + "', book '" + (book == null ? null : book.getId())
                + "', page '" + illustration.getPage()
                + "', illustration '" + illustration.getId() + "'";
    }
}
