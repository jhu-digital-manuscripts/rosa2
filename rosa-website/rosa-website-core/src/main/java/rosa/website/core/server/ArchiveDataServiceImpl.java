package rosa.website.core.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.lang3.math.NumberUtils;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.ImageListSerializer;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.model.*;
import rosa.website.core.client.ArchiveDataService;
import rosa.website.model.csv.BookDataCSV;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CSVRow;
import rosa.website.model.csv.CharacterNamesCSV;
import rosa.website.model.csv.CollectionCSV;
import rosa.website.model.csv.CSVType;
import rosa.website.model.csv.IllustrationTitleCSV;
import rosa.website.model.csv.NarrativeSectionsCSV;
import rosa.website.model.select.BookSelectData;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArchiveDataServiceImpl extends RemoteServiceServlet implements ArchiveDataService {
    private static final Logger logger = Logger.getLogger("");
    private static final String DEFAULT_LANGUAGE = "en";

    private static final int MAX_CACHE_SIZE = 1000;
    private final ConcurrentMap<String, Object> objectCache = new ConcurrentHashMap<>(MAX_CACHE_SIZE);

    private static final ImageListSerializer imageListSerializer = new ImageListSerializer();

    private Store archiveStore;

    /** No-arg constructor needed to make GWT RPC work. */
    public ArchiveDataServiceImpl() {}

    /**
     * Use for testing.
     *
     * @param store a Store for accessing the archive.
     */
    ArchiveDataServiceImpl(Store store) {
        this.archiveStore = store;
    }

    @Override
    public void init() {
        logger.info("Initializing ArchiveDataService.");
        Injector injector = Guice.createInjector(new ArchiveCoreModule());

        String path = getServletContext().getInitParameter("archive-path");
        if (path == null || path.isEmpty()) {
            logger.warning("'archive-path' not specified. Using default value [/mnt]");
            path = "/mnt";
        }

        ByteStreamGroup base = new FSByteStreamGroup(path);
        this.archiveStore = new StoreImpl(injector.getInstance(SerializerSet.class),
                injector.getInstance(BookChecker.class), injector.getInstance(BookCollectionChecker.class), base);
        logger.info("Archive Store set.");
    }

    @Override
    public CSVData loadCSVData(String collection, String lang, CSVType type) throws IOException {
        logger.info("Loading CSV data. [" + collection + ":" + lang + ":" + type + "]");

        switch (type) {
            case COLLECTION_DATA:
                return loadCollectionData(collection, lang);
            case COLLECTION_BOOKS:
                return loadCollectionBookData(collection, lang);
            case ILLUSTRATIONS:
                return loadIllustrationTitles(collection);
            case CHARACTERS:
                return loadCharacterNames(collection);
            case NARRATIVE_SECTIONS:
                return loadNarrativeSections(collection);
            default:
                throw new IllegalArgumentException("CSV type not found.");
        }
    }

    @Override
    public CollectionCSV loadCollectionData(String collection, String lang) throws IOException {
        // collection_data.csv | collection_data_fr.csv
        logger.info("Loading collection_data. [" + collection + ":" + lang + "]");
        String key = CollectionCSV.class + "." + collection + "." + lang;

        Object obj = objectCache.get(key);
        if (obj != null) {
            return (CollectionCSV) obj;
        }

        BookCollection col = loadBookCollection(collection);

        if (col == null) {
            logger.severe("No book collection found. [" + collection + "]");
            return null;
        }
        lang = checkLang(lang);

        List<CSVRow> entries = new ArrayList<>();
        for (String bookName : col.books()) {
            Book book = loadBook(col, bookName);

            if (book == null) {
                continue;
            }

            if (book.getBookMetadata(lang) != null) {
                BookMetadata md = book.getBookMetadata(lang);
                BookText rose = null;

                if (md.getTexts() != null) {
                    for (BookText text : md.getTexts()) {
                        if (text == null) {
                            continue;
                        }
                        // TODO Needs to be abstracted out to distinguish rose/pizan/etc?
                        if (text.getId() != null && text.getId().equals("rose")) {
                            rose = text;
                            break;
                        }
                    }
                }

                boolean hasRose = rose != null;
                int[] illusCount = countPagesWithIllustrations(book.getIllustrationTagging());

            /*
                Roman de la Rose collection counts only those book texts with the identifier 'rose' as only
                those texts contain Roman de la Rose material.
                Pizan and AoR do not care about this, and will count all book texts.
             */
                entries.add(new CSVRow(
                        bookName, md.getCommonName(), md.getOrigin(), md.getMaterial(),
                        String.valueOf(hasRose ? rose.getNumberOfPages() : md.getNumberOfPages()),
                        String.valueOf(md.getHeight()),
                        String.valueOf(md.getWidth()),
                        String.valueOf(hasRose ? rose.getLeavesPerGathering() : -1),
                        String.valueOf(hasRose ? rose.getLinesPerColumn() : -1),
                        String.valueOf(hasRose ? rose.getNumberOfIllustrations() : md.getNumberOfIllustrations()),
                        String.valueOf(md.getYearStart()),
                        String.valueOf(md.getYearEnd()),
                        String.valueOf(hasRose ? rose.getColumnsPerPage() : -1),
                        String.valueOf(md.getTexts() == null ? 0 : md.getTexts().length),
                        String.valueOf(illusCount[0]),
                        String.valueOf(illusCount[1])
                ));
            } else {
                // If there is no metadata, add a row with only the book's name
                entries.add(new CSVRow(
                        bookName, "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""
                ));
            }
        }

        Collections.sort(entries, new Comparator<CSVRow>() {
            @Override
            public int compare(CSVRow o1, CSVRow o2) {
                if (o1 == null && o2 != null) { // first is NULL
                    return -1;
                } else if (o1 != null && o2 == null) { // second is NULL
                    return 1;
                } else if (o1 == null) { // both are NULL
                    return 0;
                }

                return o1.getValue(CollectionCSV.Column.NAME)
                        .compareToIgnoreCase(o2.getValue(CollectionCSV.Column.NAME));
            }
        });
        CollectionCSV result = new CollectionCSV(collection, entries);

        updateCache(key, result);
        return result;
    }

    @Override
    public BookDataCSV loadCollectionBookData(String collection, String lang) throws IOException {
        // books.csv | books_fr.csv
        final String key = BookDataCSV.class.toString() + "." + collection + "." + lang;

        Object obj = objectCache.get(key);
        if (obj != null) {
            return (BookDataCSV) obj;
        }
        BookCollection col = loadBookCollection(collection);

        if (col == null) {
            logger.severe("No book collection found. [" + collection + "]");
            return null;
        }

        lang = checkLang(lang);

        List<CSVRow> entries = new ArrayList<>();
        for (String bookName : col.books()) {
            Book book = loadBook(collection, bookName);
            if (book == null) {
                continue;
            }

            entries.add(rowForBookData(book, lang));
        }

        Collections.sort(entries, new Comparator<CSVRow>() {
            @Override
            public int compare(CSVRow o1, CSVRow o2) {
                if (o1 == null && o2 != null) { // first is NULL
                    return -1;
                } else if (o1 != null && o2 == null) { // second is NULL
                    return 1;
                } else if (o1 == null) { // both are NULL
                    return 0;
                }

                return o1.getValue(BookDataCSV.Column.COMMON_NAME)
                        .compareToIgnoreCase(o2.getValue(BookDataCSV.Column.COMMON_NAME));
            }
        });
        BookDataCSV result = new BookDataCSV(collection, entries);
        updateCache(key, result);

        return result;
    }

    @Override
    public BookSelectList loadBookSelectionData(String collection, SelectCategory category, String lang)
            throws IOException{
        String key = BookSelectList.class.toString() + "." + collection + "." + category + "." + lang;

        Object list = objectCache.get(key);
        if (list != null) {
            return (BookSelectList) list;
        }

        BookCollection col = loadBookCollection(collection);

        if (col == null) {
            logger.severe("No book collection found. [" + collection + "]");
            return null;
        }

        lang = checkLang(lang);

        List<BookSelectData> entries = new ArrayList<>();
        for (String bookName : col.books()) {
            Book book = loadBook(collection, bookName);
            if (book == null) {
                continue;
            }

            entries.add(new BookSelectData(
                    rowForBookData(book, lang),
                    book.getTranscription() != null,
                    book.getIllustrationTagging() != null,
                    book.getAutomaticNarrativeTagging() != null || book.getManualNarrativeTagging() != null,
                    false // No bibliography in current books
            ));
        }
        // Pre sort results?
        BookSelectList result = new BookSelectList(category, collection, entries);
        updateCache(key, result);

        return result;
    }

    @Override
    public IllustrationTitleCSV loadIllustrationTitles(String collection) throws IOException {
        String key = IllustrationTitleCSV.class + "." + collection;

        Object csv = objectCache.get(key);
        if (csv != null) {
            return (IllustrationTitleCSV) csv;
        }

        BookCollection col = loadBookCollection(collection);
        if (col == null) {
            logger.severe("Failed to load book collection.");
            return null;
        }
        IllustrationTitles titles = col.getIllustrationTitles();

        List<CSVRow> entries = new ArrayList<>();
        Map<String, List<Integer>> positions = new HashMap<>();
        Map<String, Integer> frequencies = new HashMap<>();

        getIllustrationFrequencyAndPosition(positions, frequencies, col);

        // Transform the maps into CSVEntries
        for (String id : titles.getAllIds()) {
            String title = titles.getTitleById(id);

            int frequency = frequencies.get(title) == null ? 0 : frequencies.get(title);

            int average_position = -1;
            if (frequency > 0) {
                average_position = positions.get(title) == null ? -1 : sum(positions.get(title)) / frequency;
            }

            entries.add(new CSVRow(
                    String.valueOf(average_position),
                    title,
                    String.valueOf(frequency)
            ));
        }

        // Sort entries by location
        Collections.sort(entries, new Comparator<CSVRow>() {
            @Override
            public int compare(CSVRow o1, CSVRow o2) {
                if (o1 == null && o2 != null) { // first is NULL
                    return -1;
                } else if (o1 != null && o2 == null) { // second is NULL
                    return 1;
                } else if (o1 == null) { // both are NULL
                    return 0;
                }

                String o1_val = o1.getValue(IllustrationTitleCSV.Column.LOCATION);
                String o2_val = o2.getValue(IllustrationTitleCSV.Column.LOCATION);

                try {
                    int o1_loc = Integer.parseInt(o1_val);
                    int o2_loc = Integer.parseInt(o2_val);

                    return o1_loc - o2_loc;
                } catch (NumberFormatException e) {
                    return o1.getValue(IllustrationTitleCSV.Column.LOCATION)
                            .compareTo(o2.getValue(IllustrationTitleCSV.Column.LOCATION));
                }
            }
        });

        IllustrationTitleCSV result = new IllustrationTitleCSV(titles.getId(), entries);
        updateCache(key, result);

        return result;
    }

    @Override
    public BookCollection loadBookCollection(String collection) throws IOException {
        Object obj = objectCache.get(collection);
        if (obj != null) {
            return (BookCollection) obj;
        }

        List<String> errors = new ArrayList<>();
        BookCollection col = null;

        try {
            col = archiveStore.loadBookCollection(collection, errors);
            checkErrors(errors);

            updateCache(collection, col);
        } catch (Exception e) {
            // TODO dont catch Exception...
            logger.log(Level.SEVERE, "An error has occurred while loading a collection. [" + collection + "]", e);
        }

        return col;
    }

    @Override
    public Book loadBook(String collection, String book) throws IOException {
        return loadBook(loadBookCollection(collection), book);
    }

    @Override
    public String loadPermissionStatement(String collection, String book, String lang) throws IOException {
        return loadBook(collection, book).getPermission(lang).getPermission();
    }

    @Override
    public String loadImageListAsString(String collection, String book) throws IOException {
        String key = ImageList.class + "." + String.class + "." + collection + "." + book;

        Object str = objectCache.get(key);
        if (str != null) {
            return (String) str;
        }
        Book b = loadBook(collection, book);

        if (b == null || b.getImages() == null || b.getImages().getImages() == null
                || b.getImages().getImages().isEmpty()) {
            return "";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        imageListSerializer.write(b.getImages(), out);

        updateCache(key, out.toString());
        return out.toString();
    }

    @Override
    public ImageList loadImageList(String collection, String book) throws IOException {
        String key = ImageList.class + "." + collection + "." + book;

        Object list = objectCache.get(key);
        if (list != null) {
            return (ImageList) list;
        }
        Book b = loadBook(collection, book);

        if (b == null || b.getImages() == null || b.getImages().getImages() == null
                || b.getImages().getImages().isEmpty()) {
            return null;
        }

        updateCache(key, b.getImages());
        return b.getImages();
    }

    private CharacterNamesCSV loadCharacterNames(String collection) throws IOException {
        logger.info("Loading character_names.csv for collection [" + collection + "]");
        String key = CharacterNamesCSV.class + "." + collection;

        Object obj = objectCache.get(key);
        if (obj != null) {
            return (CharacterNamesCSV) obj;
        }

        BookCollection col = loadBookCollection(collection);

        List<CSVRow> entries = new ArrayList<>();

        CharacterNames names = col.getCharacterNames();
        for (String id : names.getAllCharacterIds()) {
            CharacterName name = names.getCharacterName(id);
            entries.add(new CSVRow(id, name.getNameInLanguage("en"), name.getNameInLanguage("fr")));
        }

        CharacterNamesCSV csv = new CharacterNamesCSV(names.getId(), entries);
        updateCache(key, csv);

        return csv;
    }

    private NarrativeSectionsCSV loadNarrativeSections(String collection) throws IOException {
        logger.info("Loading narrative sections. [" + collection + "]");
        String key = NarrativeSectionsCSV.class + "." + collection;

        Object obj = objectCache.get(key);
        if (obj != null) {
            return (NarrativeSectionsCSV) obj;
        }

        BookCollection col = loadBookCollection(collection);

        List<CSVRow> entries = new ArrayList<>();
        NarrativeSections sections = col.getNarrativeSections();

        if (sections == null) {
            return null;
        }

        for (NarrativeScene scene : sections.asScenes()) {
            if (scene == null) {
                continue;
            }

            entries.add(new CSVRow(
                    scene.getId(),
                    scene.getDescription(),
                    scene.getCriticalEditionStart() + "-" + scene.getCriticalEditionEnd()
            ));
        }

        NarrativeSectionsCSV csv = new NarrativeSectionsCSV(sections.getId(), entries);
        updateCache(key, csv);

        return csv;
    }

    private CSVRow rowForBookData(Book book, String lang) throws IOException {
        if (book == null) {
            return null;
        }

        if (book.getBookMetadata(lang) != null) {
            BookMetadata md = book.getBookMetadata(lang);
            BookText rose = null;

            for (BookText text : md.getTexts()) {
                if (text != null && text.getId() != null && text.getId().equals("rose")) {
                    rose = text;
                }
            }

            boolean hasRose = rose != null;

            return new CSVRow(
                    book.getId(),
                    md.getRepository(),
                    md.getShelfmark(),
                    md.getCommonName(),
                    md.getCurrentLocation(),
                    md.getDate(),
                    md.getOrigin(),
                    md.getType(),
                    String.valueOf(hasRose ? rose.getNumberOfIllustrations() : md.getNumberOfIllustrations()),
                    String.valueOf(hasRose ? rose.getNumberOfPages() : md.getNumberOfPages())
            );
        } else {
            return new CSVRow(book.getId(), "", "", "", "", "", "", "", "", "");
        }

    }

    private Book loadBook(BookCollection collection, String book) throws IOException {
        String key = collection.getId() + "." + book;

        Object obj = objectCache.get(key);
        if (obj != null) {
            return (Book) obj;
        }

        List<String> errors = new ArrayList<>();
        Book b = null;

        try {
            b = archiveStore.loadBook(collection, book, errors);
            checkErrors(errors);

            updateCache(key, b);
        } catch (Exception e) {
            // TODO dont catch Exception...
            logger.log(Level.SEVERE, "An error has occurred while loading a book. [" + collection + ":" + book + "]", e);
        }

        return b;
    }

    /**
     * Find the number of times each illustration in the collection is used through
     * all of the books in the collection.
     *
     * @param positions map associating illustration titles with all positions it has in books
     * @param frequency map associating illustration titles with the frequency it appears in books
     * @param collection the collection of books
     */
    private void getIllustrationFrequencyAndPosition(Map<String, List<Integer>> positions,
                                                     Map<String, Integer> frequency, BookCollection collection) {
        IllustrationTitles titles = collection.getIllustrationTitles();

        for (String name : collection.books()) {
            try {
                Book book = loadBook(collection, name);
                if (book == null || book.getIllustrationTagging() == null) {
                    continue;
                }

                int first_page_in_book = findFirstPage(book.getImages());

                for (Illustration ill : book.getIllustrationTagging()) {
                    // Get the frequency that each illustration is used across all books
                    for (String title : ill.getTitles()) {
                        // Resolve numeric IDs to appropriate title
                        if (NumberUtils.isNumber(title)) {
                            title = titles.getTitleById(title);
                        }

                        // If already present, increment
                        if (frequency.containsKey(title)) {
                            frequency.put(title, frequency.get(title) + 1);
                        } else {
                            frequency.put(title, 1);
                        }

                        // Calculate the average position of this illustration across all books
                        int this_position = findPageIndex(ill.getPage(), book.getImages()) - first_page_in_book;

                        if (positions.containsKey(title)) {
                            positions.get(title).add(this_position);
                        } else {
                            positions.put(title, new ArrayList<>(Collections.singletonList(this_position)));
                        }
                    }
                }
            } catch (IOException e) {
                // Skip this book if it fails to load
                logger.log(Level.SEVERE, "Failed to load book. [" + name + "]: ", e);
            }
        }
    }

    /**
     * Find the index of the first body matter page in a list of images. This assumes
     * that the image list in the book is already sorted. If no body matter images
     * are found, -1 is returned.
     *
     * @param images list of images in a book
     * @return the index in the image list, or -1 if no body matter pages are found
     */
    private int findFirstPage(ImageList images) {
        int first_page = 0;

        for (BookImage image : images) {
            if (image.getLocation() == BookImageLocation.BODY_MATTER) {
                return first_page;
            }

            first_page++;
        }

        return -1;
    }

    /**
     * Find the index of a page in an image list by searching for the image name.
     *
     * @param imageName name of image to look for
     * @param images image list
     * @return index of the page, or -1 if not found
     */
    private int findPageIndex(String imageName, ImageList images) {
        int position = 0;

        for (BookImage image : images) {
            if (image.getName().equals(imageName)) {
                return position;
            }

            position++;
        }

        return -1;
    }

    private int sum(List<Integer> ints) {
        int sum = 0;

        for (int i : ints) {
            sum += i;
        }

        return sum;
    }

    /**
     * Count the number of pages with one illustration and the number of pages
     * with more than one illustration.
     *
     * @param tagging illustration tagging for a book
     * @return array: {pages with one, pages with more than one}
     */
    private int[] countPagesWithIllustrations(IllustrationTagging tagging) {
        if (tagging == null) {
            return new int[] {-1, -1};
        }

        int one = 0;
        int more = 0;

        // Counting number of illustrations per page
        Map<String, Integer> page_count = new HashMap<>();
        for (Illustration ill : tagging) {
            String page = ill.getPage();

            if (page_count.containsKey(page)) {
                page_count.put(page, page_count.get(page) + 1);
            } else {
                page_count.put(page, 1);
            }
        }

        // Count number of pages with one/more illustrations. Any entry into the 'page_count' map
        // will have at least one.
        for (Map.Entry<String, Integer> entry : page_count.entrySet()) {
            if (entry.getValue().equals(1)) {
                one++;
            } else {
                more++;
            }
        }

        return new int[] {one, more};
    }

    /**
     * @param lang language code
     * @return the valid language code
     */
    private String checkLang(String lang) {
        if (lang == null || lang.isEmpty()) {
            return DEFAULT_LANGUAGE;
        }

        return lang;
    }

    private void checkErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Error loading book collection.\n");
            for (String s : errors) {
                sb.append(s);
                sb.append('\n');
            }
            logger.warning(sb.toString());
        }
    }

    /**
     * Update the object cache with a new object. Do nothing if either the key or
     * value is NULL.
     *
     * @param key .
     * @param value .
     */
    private void updateCache(String key, Object value) {
        if (key == null || value == null) {
            return;
        }

        if (objectCache.size() > MAX_CACHE_SIZE) {
            objectCache.clear();
        }

        objectCache.putIfAbsent(key, value);
    }
}
