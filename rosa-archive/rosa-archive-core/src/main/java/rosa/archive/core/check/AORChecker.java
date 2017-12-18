package rosa.archive.core.check;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookReferenceSheet;
import rosa.archive.model.ReferenceSheet;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Specialized checker for ensuring logical consistency in AOR data.
 * This class is NOT meant to check for structural errors in the data.
 */
public class AORChecker {
    private static final List<String> LANGS = Arrays.asList("EN", "EL", "FR", "IT", "LA", "ES");

    public static class ResultSet {
        final List<String> errors = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();

        boolean pass() {
            return errors.isEmpty();
        }

        // Combine this result set with another
        void combine(ResultSet other) {
            if (other == null) {
                return;
            }
            if (!other.errors.isEmpty()) {
                this.errors.addAll(other.errors);
            }
            if (!other.warnings.isEmpty()) {
                this.warnings.addAll(other.warnings);
            }
        }
    }

    public static ResultSet checkAORTranscriptions(BookCollection collection, Book book) {
        ResultSet results = new ResultSet();

        book.getAnnotatedPages().forEach(page -> {
            results.combine(checkPage(page));
            results.combine(checkReferences(collection, book));
        });

        return results;
    }

    private static ResultSet checkPage(AnnotatedPage page) {
        ResultSet result = new ResultSet();

        /*
             Make sure that the transcription file name matches the associated image name:
             FolgersHa2.aor.033v.xml must match with FolgersHa2.033v.tif
                 OR
             0000033v.xml must match with 0000033v.tif
         */
        if (page.getId().contains("aor")) {
            String[] nameParts = page.getId().replaceAll("\\.aor", "").split("\\.");
            String[] imageParts = page.getPage().split("\\.");

            if (nameParts.length < 2) {
                result.errors.add("Transcription name has invalid format. (" + page.getId() + ")");
            } else if (imageParts.length < 2) {
                result.errors.add("Image associated with transcription has invalid name. (" + page.getPage() + ")");
            } else if (!nameParts[nameParts.length - 2].equals(imageParts[imageParts.length - 2])) {
                result.errors.add("Transcription name does not match image name. ["
                        + page.getId() + " / " + page.getPage() + "]");
            }
        } else {
            if (!page.getId().equals(page.getPage())) {
                result.errors.add("Transcription name does not match image name. ["
                        + page.getId() + " / " + page.getPage() + "]");
            }
        }

        for (Marginalia marg : page.getMarginalia()) {
            for (MarginaliaLanguage lang : marg.getLanguages()) {
                for (Position pos : lang.getPositions()) {
                    // Make sure if text attr appears, language attr appears as well
                    boolean badXrefText = pos.getxRefs().stream()
                            .filter(xref -> xref.getText() != null && !xref.getText().equals(""))
                            .anyMatch(xRef -> xRef.getLanguage() == null || xRef.getLanguage().equals(""));
                    // Make sure any language attr is one of the acceptable values
                    badXrefText = badXrefText || pos.getxRefs().stream()
                            .filter(xRef -> xRef.getLanguage() != null && !xRef.getLanguage().equals(""))
                            .anyMatch(xRef -> !LANGS.contains(xRef.getLanguage().toUpperCase()));

                    if (badXrefText) {
                        result.errors.add("X-ref has associated text, but no language. [" + page.getId() + "]");
                    }
                }
            }

        }

        return result;
    }

    private static ResultSet checkReferences(BookCollection collection, Book parent) {
        ResultSet results = new ResultSet();

        ReferenceSheet people = collection.getPeopleRef();
        ReferenceSheet locations = collection.getLocationsRef();
        BookReferenceSheet books = collection.getBooksRef();

        if (people == null || locations == null || books == null) {
            return results;
        }

        for (AnnotatedPage page : parent.getAnnotatedPages()) {
            String sig = parent.getId() + ":" + page.getPage();

            for (Marginalia marg : page.getMarginalia()) {
                for (MarginaliaLanguage lang : marg.getLanguages()) {
                    for (Position pos : lang.getPositions()) {
                        for (String book : pos.getBooks()) {
                            if (book == null || book.isEmpty()) {
                                results.warnings.add("Book reference is blank. [" + sig + ":Marginalia:" + pos.getPlace());
                            } else if (!books.containsKey(book)) {
                                results.warnings.add("Book reference found in annotation not present in reference sheets. " +
                                        "[" + sig + ":Marginalia:" + pos.getPlace() + ":" + book + "]");
                            }
                        }

                        for (String person : pos.getPeople()) {
                            if (person == null || person.isEmpty()) {
                                results.warnings.add("Person reference is blank. [" + sig + ":Marginalia:" + pos.getPlace());
                            } else if (!people.containsKey(person)) {
                                results.warnings.add("Person reference found in annotation not present in reference sheets. " +
                                        "[" + sig + ":Marginalia:" + pos.getPlace() + ":" + person + "]");
                            }
                        }

                        for (String loc : pos.getLocations()) {
                            if (loc == null || loc.isEmpty()) {
                                results.warnings.add("Location reference is blank. [" + sig + ":Marginalia:" + pos.getPlace());
                            } else if (!locations.containsKey(loc)) {
                                results.warnings.add("Location reference found in annotation not present in reference sheets. " +
                                        "[" + sig + ":Marginalia:" + pos.getPlace() + ":" + loc + "]");
                            }
                        }
                    }
                }
            }
        }

        return results;
    }

}
