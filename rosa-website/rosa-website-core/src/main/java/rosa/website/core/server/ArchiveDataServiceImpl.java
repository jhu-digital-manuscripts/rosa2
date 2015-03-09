package rosa.website.core.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;
import org.apache.commons.lang3.math.NumberUtils;
import rosa.archive.core.Store;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.website.core.client.ArchiveDataService;
import rosa.website.model.csv.BookDataCSV;
import rosa.website.model.csv.CSVEntry;
import rosa.website.model.csv.CollectionCSV;
import rosa.website.model.csv.IllustrationTitleCSV;

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

public class ArchiveDataServiceImpl extends RemoteServiceServlet implements ArchiveDataService {
    private static final Logger logger = Logger.getLogger("");
    private final Store archiveStore;

    @Inject
    public ArchiveDataServiceImpl(Store store) {
        // TODO can i inject a store directly? or do i have to instantiate it manually?
        this.archiveStore = store;
    }

    @Override
    public void init() {
        // Servlet initialization if necessary
    }

    @Override
    public CollectionCSV loadCollectionData(String collection, String lang) throws IOException {
        // collection_data.csv | collection_data_fr.csv
        BookCollection col = archiveStore.loadBookCollection(collection, null);

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

        List<CSVEntry> entries = new ArrayList<>();
        for (String bookName : col.books()) {
            Book book = archiveStore.loadBook(col, bookName, null);
            if (book == null) {
                continue;
            }

            BookMetadata md = book.getBookMetadata(lang);
            BookText rose = null;

            for (BookText text : md.getTexts()) {
                if (text != null && text.getId() != null && text.getId().equals("rose")) {
                    rose = text;
                }
            }

            boolean hasRose = rose != null;

            entries.add(new CSVEntry(
                    bookName,
                    md.getRepository(),
                    md.getShelfmark(),
                    md.getCommonName(),
                    md.getCurrentLocation(),
                    md.getDate(),
                    md.getOrigin(),
                    md.getType(),
                    String.valueOf(hasRose ? rose.getNumberOfIllustrations() : md.getNumberOfIllustrations()),
                    String.valueOf(hasRose ? rose.getNumberOfPages() : md.getNumberOfPages())
            ));
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
    public IllustrationTitleCSV loadIllustrationTitles(String collection) throws IOException {
        BookCollection col = loadBookCollection(collection);
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
        return archiveStore.loadBookCollection(collection, null);
    }

    @Override
    public Book loadBook(String collection, String book) throws IOException {
        return archiveStore.loadBook(
                archiveStore.loadBookCollection(collection, null),
                book,
                null
        );
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
}
