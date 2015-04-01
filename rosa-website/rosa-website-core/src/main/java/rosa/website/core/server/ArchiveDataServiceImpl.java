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
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.model.*;
import rosa.website.core.client.ArchiveDataService;
import rosa.website.model.csv.BookDataCSV;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CSVEntry;
import rosa.website.model.csv.CharacterNamesCSV;
import rosa.website.model.csv.CollectionCSV;
import rosa.website.model.csv.CsvType;
import rosa.website.model.csv.IllustrationTitleCSV;
import rosa.website.model.csv.NarrativeSectionsCSV;
import rosa.website.model.select.BookSelectData;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO books/book collections must be cached
public class ArchiveDataServiceImpl extends RemoteServiceServlet implements ArchiveDataService {
    private static final Logger logger = Logger.getLogger("");
    private static final String DEFAULT_LANGUAGE = "en";
    private Store archiveStore;

    /**
     * Use for testing.
     *
     * @param store a Store for accessing the archive.
     */
    ArchiveDataServiceImpl(Store store) {
        this.archiveStore = store;
    }

    public ArchiveDataServiceImpl() {

    }

    @Override
    public void init() {
        logger.info("Initializing ArchiveDataService.");
        Injector injector = Guice.createInjector(new ArchiveCoreModule());

        String path = getServletConfig().getInitParameter("archive-path");
        if (path == null || path.isEmpty()) {
            logger.warning("'archive-path' not specified. Using default value [/mnt]");
            path = "/mnt";
        }

        ByteStreamGroup base = new FSByteStreamGroup(path);
        this.archiveStore = new StoreImpl(injector.getInstance(SerializerSet.class), injector.getInstance(BookChecker.class),
                injector.getInstance(BookCollectionChecker.class), base);
        logger.info("Archive Store set.");
    }

    @Override
    public CSVData loadCSVData(String collection, String lang, CsvType type) throws IOException {
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
        BookCollection col = loadBookCollection(collection);

        if (col == null) {
            logger.severe("No book collection found. [" + collection + "]");
            return null;
        }
        lang = checkLang(lang);

        List<CSVEntry> entries = new ArrayList<>();
        for (String bookName : col.books()) {
            Book book = archiveStore.loadBook(col, bookName, null);

            if (book == null) {
                continue;
            }
            BookMetadata md = book.getBookMetadata(lang);
            BookText rose = null;

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

            boolean hasRose = rose != null;
            int[] illusCount = countPagesWithIllustrations(book.getIllustrationTagging());

            /*
                Roman de la Rose collection counts only those book texts with the identifier 'rose' as only
                those texts contain Roman de la Rose material.
                Pizan and AoR do not care about this, and will count all book texts.
             */
            entries.add(new CSVEntry(
                    bookName, md.getCommonName(), md.getOrigin(), md.getMaterial(),
                    String.valueOf(hasRose ? rose.getNumberOfPages() : md.getNumberOfPages()),
                    String.valueOf(md.getHeight()),
                    String.valueOf(md.getWidth()),
                    String.valueOf(hasRose ? rose.getLeavesPerGathering() : -1),
                    String.valueOf(hasRose ? rose.getLinesPerColumn() : -1),
                    String.valueOf(hasRose ? rose.getNumberOfIllustrations() : -1),
                    String.valueOf(md.getYearStart()),
                    String.valueOf(md.getYearEnd()),
                    String.valueOf(hasRose ? rose.getColumnsPerPage() : -1),
                    String.valueOf(md.getTexts().length),
                    String.valueOf(illusCount[0]),
                    String.valueOf(illusCount[1])
            ));
        }

        Collections.sort(entries, new Comparator<CSVEntry>() {
            @Override
            public int compare(CSVEntry o1, CSVEntry o2) {
                return o1.getValue(CollectionCSV.Column.NAME)
                        .compareToIgnoreCase(o2.getValue(CollectionCSV.Column.NAME));
            }
        });

        return new CollectionCSV(collection, entries);
    }

    @Override
    public BookDataCSV loadCollectionBookData(String collection, String lang) throws IOException {
        // books.csv | books_fr.csv
        BookCollection col = loadBookCollection(collection);

        if (col == null) {
            logger.severe("No book collection found. [" + collection + "]");
            return null;
        }

        lang = checkLang(lang);

        List<CSVEntry> entries = new ArrayList<>();
        for (String bookName : col.books()) {
            entries.add(rowForBookData(loadBook(collection, bookName), lang));
        }

        Collections.sort(entries, new Comparator<CSVEntry>() {
            @Override
            public int compare(CSVEntry o1, CSVEntry o2) {
                return o1.getValue(BookDataCSV.Column.COMMON_NAME)
                        .compareToIgnoreCase(o2.getValue(BookDataCSV.Column.COMMON_NAME));
            }
        });

        return new BookDataCSV(collection, entries);
    }

    @Override
    public BookSelectList loadBookSelectionData(String collection, SelectCategory category, String lang)
            throws IOException{
        BookCollection col = loadBookCollection(collection);

        if (col == null) {
            logger.severe("No book collection found. [" + collection + "]");
            return null;
        }

        lang = checkLang(lang);

        List<BookSelectData> entries = new ArrayList<>();
        for (String bookName : col.books()) {
            Book book = loadBook(collection, bookName);

            entries.add(new BookSelectData(
                    rowForBookData(book, lang),
                    book.getTranscription() != null,
                    book.getIllustrationTagging() != null,
                    book.getAutomaticNarrativeTagging() != null || book.getManualNarrativeTagging() != null,
                    false // No bibliography in current books
            ));
        }
        // Pre sort results?
        return new BookSelectList(category, collection, entries);
    }

    @Override
    public IllustrationTitleCSV loadIllustrationTitles(String collection) throws IOException {
        BookCollection col = loadBookCollection(collection);
        if (col == null) {
            logger.severe("Failed to load book collection.");
            return null;
        }
        IllustrationTitles titles = col.getIllustrationTitles();

        List<CSVEntry> entries = new ArrayList<>();
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

            entries.add(new CSVEntry(
                    String.valueOf(average_position),
                    title,
                    String.valueOf(frequency)
            ));
        }

        // Sort entries by location
        Collections.sort(entries, new Comparator<CSVEntry>() {
            @Override
            public int compare(CSVEntry o1, CSVEntry o2) {
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

        return new IllustrationTitleCSV(titles.getId(), entries);
    }

    @Override
    public BookCollection loadBookCollection(String collection) throws IOException {
        List<String> errors = new ArrayList<>();
        BookCollection col = null;

        try {
            col = archiveStore.loadBookCollection(collection, errors);
            checkErrors(errors);
        } catch (Exception e) {
            // TODO dont catch Exception...
            logger.log(Level.SEVERE, "An error has occurred while loading a collection. [" + collection + "]", e);
        }

        logger.info("Book collection loaded. [" + collection + "]");
        return col;
    }

    @Override
    public Book loadBook(String collection, String book) throws IOException {
        return loadBook(loadBookCollection(collection), book);
    }

    private CharacterNamesCSV loadCharacterNames(String collection) throws IOException {
        logger.info("Loading character_names.csv for collection [" + collection + "]");
        BookCollection col = loadBookCollection(collection);

        List<CSVEntry> entries = new ArrayList<>();

        CharacterNames names = col.getCharacterNames();
        for (String id : names.getAllCharacterIds()) {
            CharacterName name = names.getCharacterName(id);
            entries.add(new CSVEntry(id, name.getNameInLanguage("en"), name.getNameInLanguage("fr")));
        }

        return new CharacterNamesCSV(names.getId(), entries);
    }

    private NarrativeSectionsCSV loadNarrativeSections(String collection) throws IOException {
        logger.info("Loading narrative sections. [" + collection + "]");
        BookCollection col = loadBookCollection(collection);

        List<CSVEntry> entries = new ArrayList<>();
        NarrativeSections sections = col.getNarrativeSections();

        if (sections == null) {
            return null;
        }

        for (NarrativeScene scene : sections.asScenes()) {
            if (scene == null) {
                continue;
            }

            entries.add(new CSVEntry(
                    scene.getId(),
                    scene.getDescription(),
                    scene.getCriticalEditionStart() + "-" + scene.getCriticalEditionEnd()
            ));
        }

        return new NarrativeSectionsCSV(sections.getId(), entries);
    }

    private CSVEntry rowForBookData(Book book, String lang) throws IOException {
        if (book == null) {
            return null;
        }

        BookMetadata md = book.getBookMetadata(lang);
        if (md == null) {
            return null;
        }
        BookText rose = null;

        for (BookText text : md.getTexts()) {
            if (text != null && text.getId() != null && text.getId().equals("rose")) {
                rose = text;
            }
        }

        boolean hasRose = rose != null;

        return new CSVEntry(
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
    }

    private Book loadBook(BookCollection collection, String book) throws IOException {
        List<String> errors = new ArrayList<>();
        Book b = null;

        try {
            b = archiveStore.loadBook(collection, book, errors);
            checkErrors(errors);
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
                Book book = archiveStore.loadBook(collection, name, null);
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
                            positions.put(title, new ArrayList<>(Arrays.asList(this_position)));
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
}
