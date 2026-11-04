package rosa.archive.aor;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.IdLocations;

/**
 * Resolves the targets of AoR internal references ({@code <target ref="…">}) to a page of
 * a book in the collection.
 *
 * <p>A ref is an annotation or page id, {@code {book}:{page token}[:{m|u|s…}NNNN]}, whose
 * page token is the library's original scan name and differs per book
 * ({@code 00000027}, {@code 615_d_7_f035r}, {@code earbm_stc_11402_0039}, {@code 84}).
 * The collection's {@code id_locations.csv} ({@link IdLocations}) says which page each id
 * is on; this class looks the ref up there, forgiving the usual slips (an annotation
 * suffix on a page id, dots in the book id, a differently padded number) and, when the
 * sheet does not have the ref, falls back to a folio at the end of the token
 * ({@code FolgersHa2:Ha2.007r}, {@code BLC120b4:c_120_b_(2)_f019v}).
 *
 * <p>Image lists are read on demand through the function given to the constructor and
 * cached, so a resolver can be shared by every page of a collection.
 */
public final class AorReferenceResolver {

    /**
     * A resolved target.
     *
     * @param bookId  the book
     * @param imageId the image id as in the image list, e.g. {@code RCPD1266.037v.tif}
     * @param index   the 0-based position of the image in the book's image list
     */
    public record Target(String bookId, String imageId, int index) {

        /**
         * The name of the page in viewer URLs: the image id without extension and without
         * the {@code {book}.} prefix ({@code 037v}), or {@code i{index}} when the image is
         * not named after the book. Mirrors {@code canvasNames.js} in rosa-viewer.
         */
        public String canvasName() {
            String base = stripExtension(imageId);
            String prefix = bookId + ".";
            return base.startsWith(prefix) && base.length() > prefix.length()
                    ? base.substring(prefix.length())
                    : "i" + index;
        }
    }

    /**
     * The outcome of resolving a ref: the target, or the reason it could not be resolved.
     */
    public record Resolution(Target target, String reason) {
        public boolean resolved() {
            return target != null;
        }
    }

    /** An annotation suffix such as {@code :m0001} or {@code :u0012}. */
    private static final Pattern ANNOTATION_SUFFIX = Pattern.compile("^(.*):([a-z]+\\d{3,4})$");
    /** A page token ending in digits: the digits may be padded differently in the sheet. */
    private static final Pattern TRAILING_NUMBER = Pattern.compile("^(.*?)(\\d+)$");
    /** A folio at the end of a page token. */
    private static final Pattern FOLIO = Pattern.compile("(\\d{3}[rv])$");
    /** A page written as a folio or page number, e.g. {@code 35r}, {@code 131v}, {@code 3}. */
    private static final Pattern FOLIO_PAGE = Pattern.compile("^(\\d+)([rv]?)$");
    /** A flyleaf named the way image labels were first written, e.g. {@code front matter 5r}. */
    private static final Pattern FLYLEAF_PAGE = Pattern.compile("^(front|end) ?matter (\\d+)([rv])$", Pattern.CASE_INSENSITIVE);
    /** A pastedown, e.g. {@code front matter pastedown}. */
    private static final Pattern PASTEDOWN_PAGE = Pattern.compile("^(front|end) ?matter pastedown$", Pattern.CASE_INSENSITIVE);
    /** A cover, e.g. {@code binding front cover}. */
    private static final Pattern COVER_PAGE = Pattern.compile("^binding (front|back) ?cover$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final IdLocations locations;
    private final Map<String, String> booksByKey = new HashMap<>();
    private final Function<String, ImageList> imageLists;
    private final Map<String, Optional<ImageList>> imageCache = new HashMap<>();
    private final Map<String, Set<Integer>> numberWidths = new TreeMap<>();

    /**
     * @param locations  the collection's {@code id_locations.csv}, or null when it has none
     * @param bookIds    the ids of the books in the collection
     * @param imageLists reads a book's image list; may return null for a book without one
     */
    public AorReferenceResolver(IdLocations locations, Collection<String> bookIds,
                                Function<String, ImageList> imageLists) {
        this.locations = locations;
        this.imageLists = imageLists;
        for (String book : bookIds) {
            booksByKey.put(bookKey(book), book);
        }
        if (locations != null) {
            for (String id : locations.asMap().keySet()) {
                String[] parts = withoutAnnotation(id).split(":", 2);
                if (parts.length == 2) {
                    String book = booksByKey.get(bookKey(parts[0]));
                    Matcher m = TRAILING_NUMBER.matcher(parts[1]);
                    if (book != null && m.matches()) {
                        numberWidths.computeIfAbsent(book, _ -> new LinkedHashSet<>()).add(m.group(2).length());
                    }
                }
            }
        }
    }

    /**
     * Makes a book's image list known without reading it, for callers that already have it.
     */
    public void putImages(String bookId, ImageList images) {
        imageCache.put(bookId, Optional.ofNullable(images));
    }

    /**
     * The page a ref points to, if it can be resolved.
     */
    public Optional<Target> resolve(String ref) {
        return Optional.ofNullable(resolution(ref).target());
    }

    /**
     * The page a ref points to, or why it cannot be resolved.
     */
    public Resolution resolution(String ref) {
        String r = ref == null ? "" : WHITESPACE.matcher(ref).replaceAll("").trim();
        if (r.isEmpty()) {
            return fail("empty ref");
        }
        String base = withoutAnnotation(r);
        String[] parts = base.split(":", 2);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            return fail("not of the form book:page");
        }
        String book = booksByKey.get(bookKey(parts[0]));
        if (book == null) {
            return fail("unknown book '" + parts[0] + "'");
        }
        String token = parts[1];

        if (locations != null) {
            for (String key : candidateKeys(r, base, parts[0], book, token)) {
                IdLocations.IdLocation loc = locations.get(key);
                if (loc == null) {
                    continue;
                }
                String locBook = booksByKey.get(bookKey(loc.book()));
                if (locBook == null) {
                    return fail("id_locations.csv puts '" + key + "' in unknown book '" + loc.book() + "'");
                }
                if (loc.page() == null || loc.page().isBlank()) {
                    return fail("id_locations.csv has no page for '" + key + "'");
                }
                Target target = imageForPage(locBook, loc.page());
                return target != null ? new Resolution(target, null)
                        : fail("id_locations.csv page '" + loc.page() + "' is not an image of " + locBook);
            }
        }

        Matcher folio = FOLIO.matcher(stripExtension(token));
        if (folio.find()) {
            Target target = imageForPage(book, folio.group(1));
            if (target != null) {
                return new Resolution(target, null);
            }
        }
        return fail(locations == null ? "no id_locations.csv" : "not in id_locations.csv");
    }

    /**
     * The keys to try in the sheet, most literal first: the ref as written, without its
     * annotation suffix, with the book id as the archive spells it, and with the trailing
     * number padded to the widths the sheet uses for that book.
     */
    private Set<String> candidateKeys(String ref, String base, String bookAsWritten, String book, String token) {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(ref);
        keys.add(base);
        keys.add(book + ":" + token);
        Matcher m = TRAILING_NUMBER.matcher(token);
        if (m.matches() && m.group(2).length() <= 9) {
            int n = Integer.parseInt(m.group(2));
            for (int width : numberWidths.getOrDefault(book, Set.of())) {
                String padded = m.group(1) + String.format(Locale.ROOT, "%0" + width + "d", n);
                keys.add(book + ":" + padded);
                keys.add(bookAsWritten + ":" + padded);
            }
        }
        return keys;
    }

    /**
     * The image a page of the sheet names: the image id itself, a folio or page number
     * ({@code 35r} is {@code {book}.035r.tif}), a flyleaf, pastedown or cover as the image
     * labels first named them ({@code front matter 5r} is
     * {@code {book}.frontmatter.flyleaf.005r.tif}), or the image's current label.
     */
    private Target imageForPage(String book, String page) {
        ImageList list = images(book);
        if (list == null) {
            return null;
        }
        String p = normalize(page);
        String pBase = stripExtension(p);
        Set<String> wanted = new LinkedHashSet<>();
        wanted.add(pBase);
        wanted.add(book.toLowerCase(Locale.ROOT) + "." + pBase);
        Matcher m = FOLIO_PAGE.matcher(pBase);
        if (m.matches() && m.group(1).length() <= 9) {
            wanted.add(book.toLowerCase(Locale.ROOT) + "." + String.format(Locale.ROOT, "%03d", Integer.parseInt(m.group(1))) + m.group(2));
        }
        m = FLYLEAF_PAGE.matcher(pBase);
        if (m.matches() && m.group(2).length() <= 9) {
            wanted.add(book.toLowerCase(Locale.ROOT) + "." + m.group(1).toLowerCase(Locale.ROOT) + "matter.flyleaf."
                    + String.format(Locale.ROOT, "%03d", Integer.parseInt(m.group(2))) + m.group(3));
        }
        m = PASTEDOWN_PAGE.matcher(pBase);
        if (m.matches()) {
            wanted.add(book.toLowerCase(Locale.ROOT) + "." + m.group(1).toLowerCase(Locale.ROOT) + "matter.pastedown");
        }
        m = COVER_PAGE.matcher(pBase);
        if (m.matches()) {
            wanted.add(book.toLowerCase(Locale.ROOT) + ".binding." + m.group(1).toLowerCase(Locale.ROOT) + "cover");
        }

        int index = 0;
        for (BookImage image : list.getImages()) {
            String id = image.getId();
            if (wanted.contains(stripExtension(id).toLowerCase(Locale.ROOT))) {
                return new Target(book, id, index);
            }
            index++;
        }
        index = 0;
        for (BookImage image : list.getImages()) {
            if (image.getName() != null && normalize(image.getName()).equals(normalize(p))) {
                return new Target(book, image.getId(), index);
            }
            index++;
        }
        return null;
    }

    private ImageList images(String book) {
        return imageCache.computeIfAbsent(book, b -> Optional.ofNullable(imageLists.apply(b))).orElse(null);
    }

    private static Resolution fail(String reason) {
        return new Resolution(null, reason);
    }

    private static String withoutAnnotation(String ref) {
        Matcher m = ANNOTATION_SUFFIX.matcher(ref);
        return m.matches() ? m.group(1) : ref;
    }

    /** Books are matched ignoring case and dots: {@code RCPD126.6} is {@code RCPD1266}. */
    private static String bookKey(String book) {
        return book == null ? "" : book.replace(".", "").toLowerCase(Locale.ROOT);
    }

    private static String normalize(String s) {
        return WHITESPACE.matcher(s.trim()).replaceAll(" ").toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(".tif") ? name.substring(0, name.length() - 4) : name;
    }
}
