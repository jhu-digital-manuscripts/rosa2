package rosa.archive.core.store;

import com.google.inject.Inject;
import rosa.archive.core.RoseConstants;
import rosa.archive.core.check.Checker;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookStructure;
import rosa.archive.model.ChecksumInfo;
import rosa.archive.model.CropInfo;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeTagging;
import rosa.archive.model.Permission;
import rosa.archive.model.Transcription;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link rosa.archive.core.store.Store} based on the structure of the
 * Roman de la Rose Digital Library archive.
 *
 * The RRDL archive has a set file hierarchy with the top level directory containing
 * manuscript or book collections as directories. Each collection directory contains metadata
 * files and directories representing each individual book or manuscript.
 */
public class FileStore implements Store {
    private Path top;

    private Checker<Object> checker;
    private Map<Class, Serializer> serializers;

    FileStore() {
        // TODO configure TOP
    }


    // TODO use Guice AssistedInject here
    // this will require injecting a FileStoreFactory instead of the FileStore itself
    // but will allow specifying TOP dynamically in code
    // http://stackoverflow.com/questions/8976250/how-to-use-guices-assistedinject
    @Inject
    FileStore(String top, Checker<Object> checker, Map<Class, Serializer> serializers) {
        this.top = Paths.get(top);
        this.checker = checker;
        this.serializers = serializers;
    }

    @Override
    public String[] listBookCollections() {
        // List the names of all directories in top
        List<String> archives = getItemNamesFromDirectory(top, new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isDirectory(entry);
            }
        });
        return archives.toArray(new String[archives.size()]);
    }

    @Override
    public String[] listBooks(String collectionId) {
        Path collection = relativeTo(collectionId, top);

        // Make sure this Path is a directory
        if (!Files.isDirectory(collection)) {
            throw new RuntimeException("[" + collectionId + "] is not a directory!");
        }

        // List the names of all directories in the collection directory
        List<String> books = getItemNamesFromDirectory(collection, new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isDirectory(entry);
            }
        });
        return books.toArray(new String[books.size()]);
    }

    @Override
    public BookCollection loadBookCollection(String collectionId) {
        return null;
    }

    @Override
    public Book loadBook(String collectionId, String bookId) {
        String language = "en";

        Path collectionPath = relativeTo(collectionId, top);
        Path bookPath = relativeTo(bookId, collectionPath);

        if (!Files.isDirectory(bookPath)) {
            // TODO log
            return null;
        }

        Book book = new Book();
        book.setId(bookId);

        book.setImages(
                loadFromFile(relativeTo(bookId + RoseConstants.IMAGES, bookPath), ImageList.class));
        book.setCroppedImages(
                loadFromFile(relativeTo(bookId + RoseConstants.IMAGES_CROP, bookPath), ImageList.class));
        book.setCropInfo(
                loadFromFile(relativeTo(bookId + RoseConstants.CROP, bookPath), CropInfo.class));
        book.setBookMetadata(
                loadFromFile(relativeTo(
                        bookId + RoseConstants.DESCRIPTION + language + RoseConstants.XML, bookPath), BookMetadata.class));
        book.setBookStructure(
                loadFromFile(relativeTo(bookId + RoseConstants.REDUCED_TAGGING, bookPath), BookStructure.class));
        book.setChecksumInfo(
                loadFromFile(relativeTo(bookId + RoseConstants.SHA1SUM, bookPath), ChecksumInfo.class));
        book.setIllustrationTagging(
                loadFromFile(relativeTo(bookId + RoseConstants.IMAGE_TAGGING, bookPath), IllustrationTagging.class));
        book.setManualNarrativeTagging(
                loadFromFile(relativeTo(
                        bookId + RoseConstants.MANUAL_NARRATIVE_TAGGING, bookPath), NarrativeTagging.class));
        book.setAutomaticNarrativeTagging(
                loadFromFile(relativeTo(
                        bookId + RoseConstants.AUTOMATIC_NARRATIVE_TAGGING, bookPath), NarrativeTagging.class));
        book.setTranscription(
                loadFromFile(relativeTo(bookId + RoseConstants.TRANSCRIPTION, bookPath), Transcription.class));

        book.setContent(getItemNamesFromDirectory(
                bookPath,
                new Filter<Path>() {
                    @Override
                    public boolean accept(Path entry) throws IOException {
                        // grab everything
                        return true;
                    }
                })
                .toArray(new String[0])
        );

        // Find all permissions files, each should be for a different language.
        List<String> permNames = getItemNamesFromDirectory(
                bookPath,
                new Filter<Path>() {
                    @Override
                    public boolean accept(Path entry) throws IOException {
                        String filename = entry.getFileName().toString();
                        return filename.contains(RoseConstants.PERMISSION);
                    }
                }
        );
        for (String permission : permNames) {
            // Parse language code out of file name
            String lang = findLanguageCodeInName(permission);
            // Load permission statement from file as single String
            Permission perm = loadFromFile(relativeTo(bookId + RoseConstants.TRANSCRIPTION, bookPath), Permission.class);
            // Dump in Book obj
            book.addPermission(perm, lang);
        }



        return book;
    }

    // TODO needs to be tested!
    protected String findLanguageCodeInName(String name) {

        String[] parts = name.split("_");
        for (String part : parts) {
            if (part.matches("(\\w){2,3}(?:(\\.[\\w]+)|$)")) {
                return part.split("\\.")[0];
            }
        }

        return "";
    }

    /**
     * Load data from a file in the archive without throwing an exception if it fails.
     *
     * @param file file to read
     * @param type type token
     * @param <T> return type
     * @return data from file
     */
    @SuppressWarnings("unchecked")
    private <T> T loadFromFile(Path file, Class<T> type) {

        try {
            InputStream in = Files.newInputStream(file);
            Serializer serializer = serializers.get(type);

            return (T) serializer.read(in);

        } catch (IOException e) {
            // TODO log
        }

        return null;
    }

    @Override
    public boolean checkBitIntegrity(BookCollection collection) {
        return checker.checkBits(collection);
    }

    @Override
    public boolean checkBitIntegrity(Book book) {
        return checker.checkBits(book);
    }

    @Override
    public boolean checkContentConsistency(BookCollection collection) {
        return checker.checkContent(collection);
    }

    @Override
    public boolean checkContentConsistency(Book book) {
        return checker.checkContent(book);
    }

    private Path relativeTo(String id, Path parent) {
        return parent.resolve(id);
    }

    /**
     * Search through a directory non-recursively specified by {@param directory} and get
     * a list of all items that are accepted by {@param filter}.
     *
     * @param directory directory to search
     * @param filter defines what items to list. This parameter can be NULL, in which case,
     *               it defaults to accepting all items.
     * @return list of items
     */
    private List<String> getItemNamesFromDirectory(Path directory, Filter<Path> filter) {
        List<String> items = new ArrayList<>();

        if (filter == null) {
            // NULL filter > accept all
            filter = new Filter<Path>() {
                @Override
                public boolean accept(Path entry) throws IOException {
                    return true;
                }
            };
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(directory, filter)) {
            for (Path item : ds) {
                items.add(item.getFileName().toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to get items from Path [" + directory + "]", e);
        }

        return items;
    }

}
