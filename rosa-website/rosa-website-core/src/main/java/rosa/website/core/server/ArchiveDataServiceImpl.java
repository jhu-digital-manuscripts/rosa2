package rosa.website.core.server;

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

import org.apache.commons.lang3.math.NumberUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import rosa.archive.core.serialize.ImageListSerializer;
import rosa.archive.core.util.TranscriptionSplitter;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeScene;
import rosa.archive.model.NarrativeSections;
import rosa.website.core.client.ArchiveDataService;
import rosa.website.core.shared.RosaConfigurationException;
import rosa.website.model.select.BookSelectData;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.DataStatus;
import rosa.website.model.select.SelectCategory;
import rosa.website.model.table.BookDataColumn;
import rosa.website.model.table.Table;
import rosa.website.model.table.Row;
import rosa.website.model.table.Tables;
import rosa.website.model.table.CharacterNamesColumn;
import rosa.website.model.table.CollectionDisplayColumn;
import rosa.website.model.table.IllustrationTitleColumn;
import rosa.website.model.table.NarrativeSectionColumn;
import rosa.website.model.view.BookDescriptionViewModel;
import rosa.website.model.view.FSIViewerModel;

@Singleton
public class ArchiveDataServiceImpl extends ContextRemoteServiceServlet implements ArchiveDataService {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger("");
    private static final String DEFAULT_LANGUAGE = "en";

    private static final int MAX_CACHE_SIZE = 1000;
    private final ConcurrentMap<String, Object> objectCache = new ConcurrentHashMap<>(MAX_CACHE_SIZE);

    private static final ImageListSerializer imageListSerializer = new ImageListSerializer();

    private StoreAccessLayer archiveStore;
    private String collection;

    /** No-arg constructor needed to make GWT RPC work. */
    ArchiveDataServiceImpl() {}

    /**
     * Constructor used by Guice.
     *
     * @param store store access layer
     */
    @Inject
    ArchiveDataServiceImpl(StoreAccessLayer store, @Named("collection.name") String collection) {
        this.archiveStore = store;
        this.collection = collection;
    }

    @Override
    public void init() {
        try {
            if (!archiveStore.hasCollection(collection)) {
                logger.log(Level.SEVERE, "Couldn't find configured collection. (" + collection + ")");
                return;
            }

            logger.info("Initializing data. Collection: (" + collection + ")");
            for (String book : archiveStore.store().listBooks(collection)) {
                logger.info("  - Caching book [" + book + "]");
                archiveStore.book(collection, book);
            }
            logger.info("Initializing done.");
        } catch (IOException e) {
            logger.warning("Failed to initialize.");
        }
    }

    private void assertValidCollection(String collection) throws RosaConfigurationException {
        if (!archiveStore.hasCollection(collection)) {
            throw new RosaConfigurationException("collection", collection);
        }
    }

    @Override
    public Table loadCSVData(String collection, String lang, Tables type) throws IOException {
        switch (type) {
        case COLLECTION_DISPLAY:
            return loadCollectionDisplayData(collection, lang);
        case BOOK_DATA:
            return loadBookData(collection, lang);        
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
    
    private Table loadCollectionDisplayData(String collection, String lang) throws IOException {
        String key = Table.class + "." + "coldisplay" + collection + "." + lang;

        Object obj = objectCache.get(key);
        
        if (obj != null) {
            return Table.class.cast(obj);
        }

        BookCollection col = loadBookCollection(collection);

        if (col == null) {
            logger.severe("No book collection found. [" + collection + "]");
            return null;
        }
        lang = checkLang(lang);

        List<Row> rows = new ArrayList<>();
        
        for (String bookName : col.books()) {
            if (!archiveStore.hasBook(collection, bookName)) {
                continue;
            }

            Book book = loadBook(collection, bookName);

            if (book == null || book.getId().contains(".ignore")) {
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
                        if ((text.getId() != null && text.getId().equals("rose"))
                                || (text.getTextId() != null && text.getTextId().equals("rose"))) {
                            rose = text;
                            break;
                        }
                    }
                }

                boolean hasRose = rose != null;
                int[] illusCount = countPagesWithIllustrations(book.getIllustrationTagging());

                /*
                * Roman de la Rose collection counts only those book texts with the identifier 'rose' as only
                * those texts contain Roman de la Rose material.
                * Pizan and AoR do not care about this, and will count all book texts.
                */
                
                String date = md.getYearStart() + "-" + md.getYearEnd();
                String size = md.getWidth() + "x" + md.getHeight();

                rows.add(new Row(
                        bookName,
                        date.contains("null") || date.contains("NULL") ? "" : date,
                        String.valueOf(hasRose ? rose.getNumberOfPages() : md.getNumberOfPages()),
                        String.valueOf(hasRose ? rose.getNumberOfIllustrations() : md.getNumberOfIllustrations()),
                        String.valueOf(hasRose ? rose.getColumnsPerPage() : -1),
                        String.valueOf(hasRose ? rose.getLinesPerColumn() : -1),
                        size.contains("null") || size.contains("NULL") ? "" : size,
                        String.valueOf(hasRose ? rose.getLeavesPerGathering() : -1),
                        String.valueOf(illusCount[1])));
            } else {
                // If there is no metadata, add a row with only the book's name
                rows.add(new Row(
                        bookName, "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""
                ));
            }
        }
        
        Collections.sort(rows, (r1, r2) -> r1.getValue(CollectionDisplayColumn.NAME)
                .compareTo(r2.getValue(CollectionDisplayColumn.NAME)));
        
        Table result = new Table(CollectionDisplayColumn.values(), rows);

        updateCache(key, result);
        return result;
    }

    private Table loadBookData(String collection, String lang) throws IOException {
        // books.csv | books_fr.csv
        final String key = Table.class.toString() + "."  + "bookdata" + "." + collection + "." + lang;

        Object obj = objectCache.get(key);
        if (obj != null) {
            return Table.class.cast(obj);
        }
        BookCollection col = loadBookCollection(collection);

        if (col == null) {
            logger.severe("No book collection found. [" + collection + "]");
            return null;
        }

        lang = checkLang(lang);

        List<Row> entries = new ArrayList<>();
        
        for (String bookName : col.books()) {
            if (!archiveStore.hasBook(collection, bookName)) {
                continue;
            }

            Book book = loadBook(collection, bookName);
            
            if (book == null || book.getId().contains(".ignore")) {
                continue;
            }
                    
            entries.add(rowForBookData(book, lang));
        }

        Collections.sort(entries, (r1, r2) -> r2.getValue(BookDataColumn.COMMON_NAME)
                .compareTo(r1.getValue(BookDataColumn.COMMON_NAME)));

        Table result = new Table(BookDataColumn.values(), entries);
        updateCache(key, result);

        return result;
    }
    

    /**
     * {@link #getTranscriptionStatus(Book)}
     *
     * @param collection collection in the archive
     * @param category selection category
     * @param lang language code
     * @return .
     * @throws IOException if archive is unavailable
     */
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
            if (book == null || book.getId().contains(".ignore")) {
                continue;
            }

            entries.add(new BookSelectData(
                    rowForBookData(book, lang),
                    book.getTranscription() != null,
                    book.getIllustrationTagging() != null,
                    book.getAutomaticNarrativeTagging() != null || book.getManualNarrativeTagging() != null,
                    false, // No bibliography in current books
                    getTranscriptionStatus(book)
            ));
        }
        // Pre sort results?
        BookSelectList result = new BookSelectList(category, collection, entries);
        updateCache(key, result);

        return result;
    }

    private Table loadIllustrationTitles(String collection) throws IOException {
        String key = Table.class +"." + "illustitles" + "." + collection;

        Object obj = objectCache.get(key);
        
        if (obj != null) {
            return Table.class.cast(obj); 
        }

        BookCollection col = loadBookCollection(collection);
        if (col == null) {
            logger.severe("Failed to load book collection.");
            return null;
        }
        IllustrationTitles titles = col.getIllustrationTitles();

        List<Row> entries = new ArrayList<>();
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

            if (average_position != -1) {
                entries.add(new Row(
                        String.valueOf(average_position),
                        title,
                        String.valueOf(frequency)
                ));
            }
        }

        // Sort entries by location
        Collections.sort(entries, new Comparator<Row>() {
            @Override
            public int compare(Row o1, Row o2) {
                if (o1 == null && o2 != null) { // first is NULL
                    return -1;
                } else if (o1 != null && o2 == null) { // second is NULL
                    return 1;
                } else if (o1 == null) { // both are NULL
                    return 0;
                }

                String o1_val = o1.getValue(IllustrationTitleColumn.LOCATION);
                String o2_val = o2.getValue(IllustrationTitleColumn.LOCATION);

                try {
                    int o1_loc = Integer.parseInt(o1_val);
                    int o2_loc = Integer.parseInt(o2_val);

                    return o1_loc - o2_loc;
                } catch (NumberFormatException e) {
                    return o1.getValue(IllustrationTitleColumn.LOCATION)
                            .compareTo(o2.getValue(IllustrationTitleColumn.LOCATION));
                }
            }
        });

        Table result = new Table(IllustrationTitleColumn.values(), entries);
        updateCache(key, result);

        return result;
    }

    @Override
    public String loadPermissionStatement(String collection, String book, String lang) throws IOException {
        Book b = loadBook(collection, book);
        if (b == null) {
            return null;
        }

        return b.getPermission(lang).getPermission();
    }

    @Override
    public String loadImageListAsString(String collection, String book) throws IOException {
        Book b = loadBook(collection, book);

        if (b == null || b.getImages() == null || b.getImages().getImages() == null
                || b.getImages().getImages().isEmpty()) {
            throw new RosaConfigurationException("Image list", "book", book);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        imageListSerializer.write(b.getImages(), out);

        return out.toString();
    }

    @Override
    public ImageList loadImageList(String collection, String book) throws IOException {
        Book b = loadBook(collection, book);

        if (b == null || b.getImages() == null || b.getImages().getImages() == null
                || b.getImages().getImages().isEmpty()) {
            throw new RosaConfigurationException("Image list", "book", book);
        }

        return b.getImages();
    }

    @Override
    public FSIViewerModel loadFSIViewerModel(String collection, String book, String language) throws IOException {
        logger.info("Loading FSI model.");
        String key = FSIViewerModel.class + "." + collection + "." + book + "." + language;

        Object obj = objectCache.get(key);
        if (obj != null) {
            return (FSIViewerModel) obj;
        }

        BookCollection col = loadBookCollection(collection);
        if (col == null) {
            return null;
        }

        Book b = loadBook(collection, book);
        if (b == null) {
            return null;
        }

        // Come up with human readable book title
        String bookTitle;
        BookMetadata metadata = b.getBookMetadata(language);
        if (metadata != null) {
            bookTitle = metadata.getRepository() + ", " + metadata.getCommonName();
        } else {
            bookTitle = b.getId();
        }

        IllustrationTitles allTitles = col.getIllustrationTitles();
        CharacterNames allNames = col.getCharacterNames();
        IllustrationTagging illustrationTagging = b.getIllustrationTagging();

        if (illustrationTagging != null && allTitles != null && allNames != null) {
            for (Illustration ill : illustrationTagging) {

                // Replace all character IDs with character names in appropriate language
                String[] chars = ill.getCharacters();
                for (int i = 0; i < chars.length; i++) {
                    String name = allNames.getNameInLanguage(chars[i], language);
                    if (name != null && !name.isEmpty()) {
                        chars[i] = name;
                    }
                }

                // Replace all illustration title IDs with illustration title
                String[] titles = ill.getTitles();
                for (int i = 0; i < titles.length; i++) {
                    String title = allTitles.getTitleById(titles[i]);
                    if (title != null && !title.isEmpty()) {
                        titles[i] = title;
                    }
                }
            }
        }

        FSIViewerModel model = FSIViewerModel.Builder.newBuilder()
                .title(bookTitle)
                .permission(b.getPermission(language))
                .images(b.getImages())
                .transcriptions(TranscriptionSplitter.split(b.getTranscription()))
                .illustrationTagging(b.getIllustrationTagging())
                .narrativeTagging(b.getManualNarrativeTagging() == null ?
                        b.getAutomaticNarrativeTagging() : b.getManualNarrativeTagging())
                .narrativeSections(col.getNarrativeSections())
                .build();
        updateCache(key, model);

        return model;
    }

    @Override
    public BookDescriptionViewModel loadBookDescriptionModel(String collection, String book, String language)
            throws IOException {
        logger.info("Loading FSI model.");
        String key = BookDescriptionViewModel.class + "." + collection + "." + book + "." + language;

        Object obj = objectCache.get(key);
        if (obj != null) {
            return (BookDescriptionViewModel) obj;
        }

        Book b = loadBook(collection, book);
        if (b == null) {
            return null;
        }

        BookDescriptionViewModel model = new BookDescriptionViewModel(
                b.getBookDescription(language),
                b.getBookMetadata(language),
                b.getImages(),
                getTranscriptionStatus(b),
                getIllusDescStatus(b)
        );
        updateCache(key, model);

        return model;
    }

    private BookCollection loadBookCollection(String collection) throws IOException {
        assertValidCollection(collection);
        BookCollection col = null;

        try {
            col = archiveStore.collection(collection);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error has occurred while loading a collection. [" + collection + "]", e);
        }

        return col;
    }

    private Book loadBook(String collection, String book) throws IOException {
        assertValidCollection(collection);
        if (!archiveStore.hasBook(collection, book)) {
            throw new RosaConfigurationException("book", book);
        }

        try {
            return archiveStore.book(collection, book);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred while loading a book. [" + collection + ":" + book + "]", e);
            return null;
        }
    }

    private Table loadCharacterNames(String collection) throws IOException {
        logger.info("Loading character_names.csv for collection [" + collection + "]");
        String key = Table.class + "." +  "charnames" + "." + collection;

        Object obj = objectCache.get(key);
        
        if (obj != null) {
            return Table.class.cast(obj);
        }

        BookCollection col = loadBookCollection(collection);
        
        if (col == null) {
            logger.severe("Failed to load book collection. (" + collection + ")");
            return null;
        }

        List<Row> entries = new ArrayList<>();

        CharacterNames names = col.getCharacterNames();
        for (String id : names.getAllCharacterIds()) {
            CharacterName name = names.getCharacterName(id);

            String siteName = name.getNameInLanguage("site name");
            if (siteName == null || siteName.isEmpty()) {
                siteName = id;
            }

            entries.add(new Row(siteName, name.getNameInLanguage("en"), name.getNameInLanguage("fr")));
        }

        Collections.sort(entries, new Comparator<Row>() {
            @Override
            public int compare(Row o1, Row o2) {
                if (o1 == null && o2 != null) { // first is NULL
                    return -1;
                } else if (o1 != null && o2 == null) { // second is NULL
                    return 1;
                } else if (o1 == null) { // both are NULL
                    return 0;
                }

                return o1.getValue(CharacterNamesColumn.NAME)
                        .compareToIgnoreCase(o2.getValue(CharacterNamesColumn.NAME));
            }
        });

        Table csv = new Table(CharacterNamesColumn.values(), entries);
        updateCache(key, csv);

        return csv;
    }

    private Table loadNarrativeSections(String collection) throws IOException {
        logger.info("Loading narrative sections. [" + collection + "]");
        String key = Table.class + "." + "narsecs" +  "." + collection;

        Object obj = objectCache.get(key);
        if (obj != null) {
            return Table.class.cast(obj);
        }

        BookCollection col = loadBookCollection(collection);

        List<Row> entries = new ArrayList<>();
        NarrativeSections sections = col.getNarrativeSections();

        if (sections == null) {
            return null;
        }

        for (NarrativeScene scene : sections.asScenes()) {
            if (scene == null) {
                continue;
            }

            entries.add(new Row(
                    scene.getId(),
                    scene.getDescription(),
                    scene.getCriticalEditionStart() + "-" + scene.getCriticalEditionEnd()
            ));
        }

        Table csv = new Table(NarrativeSectionColumn.values(), entries);
        updateCache(key, csv);

        return csv;
    }

    private Row rowForBookData(Book book, String lang) throws IOException {
        if (book == null) {
            return null;
        }

        if (book.getBookMetadata(lang) != null) {
            BookMetadata md = book.getBookMetadata(lang);
            BookText rose = null;

            for (BookText text : md.getTexts()) {
                if (text != null &&
                        ((text.getId() != null && text.getId().equals("rose"))
                        || (text.getTextId() != null && text.getTextId().equals("rose")))) {
                    rose = text;
                }
            }

            boolean hasRose = rose != null;

            return new Row(
                    book.getId(),
                    md.getRepository(),
                    md.getShelfmark(),
                    md.getCommonName(),
                    md.getCurrentLocation(),
                    md.getDate(),
                    md.getOrigin(),
                    md.getType(),
                    String.valueOf(hasRose ?
                            nonNegative(rose.getNumberOfIllustrations()) : nonNegative(md.getNumberOfIllustrations())),
                    String.valueOf(hasRose ?
                            nonNegative(rose.getNumberOfPages()) : nonNegative(md.getNumberOfPages()))
            );
        } else {
            return new Row(book.getId(), "", "", "", "", "", "", "", "", "");
        }

    }

    private int nonNegative(int num) {
        return num < 0 ? 0 : num;
    }

    /**
     * Find the number of times each illustration in the collection is used through
     * all of the books in the collection.
     *
     * @param positions map associating illustration titles with all positions (page index) it has in books
     * @param frequency map associating illustration titles with the frequency it appears in books
     * @param collection the collection of books
     */
    private void getIllustrationFrequencyAndPosition(Map<String, List<Integer>> positions,
                                                     Map<String, Integer> frequency, BookCollection collection) {
        IllustrationTitles titles = collection.getIllustrationTitles();

        for (String name : collection.books()) {
            try {
                Book book = loadBook(collection.getId(), name);
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
     * @param book book loaded from archive
     * @return does this book have transcriptions for every page?
     */
    private DataStatus getTranscriptionStatus(Book book) {
        if (book.getTranscription() == null || book.getTranscription().getXML().isEmpty()
                || book.getImages() == null) {
            return DataStatus.NONE;
        }

        boolean hasAtLeastOne = false;
        for (BookImage image : book.getImages()) {
            if (image.getLocation() == BookImageLocation.BODY_MATTER) {
                // If page is not found among transcriptions
                if (!hasAtLeastOne && book.getTranscription().getXML().contains(image.getName())) {
                    // If there exists transcription for this page, mark that at least
                    // one transcription exists
                    hasAtLeastOne = true;
                } else if (hasAtLeastOne && !book.getTranscription().getXML().contains(image.getName())) {
                    // If at least one page has a transcription, but none was found
                    // for this page
                    return DataStatus.PARTIAL;
                }
            }
        }

        if (hasAtLeastOne) {
            return DataStatus.FULL;
        } else {
            return DataStatus.NONE;
        }
    }

    /**
     * Check the whether illustration descriptions are available for
     * some, all, or no illustrations in a book.
     *
     * Illustration tagging is currently the only way to determine that
     * an illustration is present in a book. It must be assumed that
     * they are complete, as there is no mechanism for telling otherwise.
     *
     * @param book book object
     * @return what is the status of illustration description coverage
     */
    private DataStatus getIllusDescStatus(Book book) {
        if (book == null || book.getIllustrationTagging() == null
                || book.getImages() == null) {
            return DataStatus.NONE;
        }

        return DataStatus.FULL;
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
