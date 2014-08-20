package rosa.archive.core.store;

import com.google.inject.Inject;
import rosa.archive.core.RoseConstants;
import rosa.archive.core.check.Checker;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.serialize.SerializerException;
import rosa.archive.core.serialize.SerializerFactory;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookStructure;
import rosa.archive.model.ChecksumInfo;
import rosa.archive.model.CropInfo;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeTagging;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

    FileStore() {
        // TODO configure TOP
    }


    // TODO use Guice AssistedInject here
    // this will require injecting a FileStoreFactory instead of the FileStore itself
    // but will allow specifying TOP dynamically in code
    // http://stackoverflow.com/questions/8976250/how-to-use-guices-assistedinject
    @Inject
    FileStore(String top, Checker<Object> checker) {
        this.top = Paths.get(top);
        this.checker = checker;
    }

    @Override
    public String[] listBookCollections() {
        // List the names of all directories in top
//        List<String> archives = getSubDirectoryNames(top);
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
//        List<String> books = getSubDirectoryNames(collection);
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

        // Set the list of images.
        try {
            InputStream imagesIn = Files.newInputStream(relativeTo(bookId + RoseConstants.IMAGES, bookPath));

            Serializer<ImageList> imageListSerializer = SerializerFactory.serializer(ImageList.class);
            book.setImages(imageListSerializer.read(imagesIn));

        } catch (SerializerException | IOException e) {
            // TODO log error
            book.setImages(new ImageList());
        }

        // Set the list of cropped images.
        try {
            InputStream in = Files.newInputStream(relativeTo(bookId + RoseConstants.IMAGES_CROP, bookPath));

            Serializer<ImageList> imageListSerializer = SerializerFactory.serializer(ImageList.class);
            book.setCroppedImages(imageListSerializer.read(in));

        } catch (SerializerException | IOException e) {
            // TODO log
            book.setCroppedImages(new ImageList());
        }

        // Set crop info
        try {
            InputStream in = Files.newInputStream(relativeTo(bookId + RoseConstants.CROP, bookPath));

            Serializer<CropInfo> serializer = SerializerFactory.serializer(CropInfo.class);
            book.setCropInfo(serializer.read(in));

        } catch (SerializerException | IOException e) {
            // TODO log
            book.setCropInfo(new CropInfo());
        }

        // Set book metadata
        setFieldFromFile(book, "bookMetadata", relativeTo(bookId + ".description_" + language + ".xml", bookPath),
                new BookMetadata());
        // Set checksum info
        setFieldFromFile(book, "checksumInfo", relativeTo(bookId + RoseConstants.SHA1SUM, bookPath),
                new ChecksumInfo());
        // Set book structure
        setFieldFromFile(book, "bookStructure", relativeTo(bookId + RoseConstants.REDUCED_TAGGING, bookPath),
                new BookStructure());
        // Set illustration tagging
        setFieldFromFile(book, "illustrationTagging", relativeTo(bookId + RoseConstants.IMAGE_TAGGING, bookPath),
                new IllustrationTagging());
        // Set manual narrative tagging
        setFieldFromFile(book, "manualNarrativeTagging", relativeTo(bookId + ".nartag.txt", bookPath),
                new NarrativeTagging());
        // Set automatic narrative tagging
        setFieldFromFile(book, "automaticNarrativeTagging", relativeTo(bookId + ".nartag.csv", bookPath),
                new NarrativeTagging());

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

        return book;
    }

    /**
     * Novelty method for setting fields...
     *
     * @param objToChange
     * @param fieldName
     * @param filepath
     * @param type
     * @param <T>
     */
    protected <T> void setFieldFromFile(Object objToChange, String fieldName, Path filepath, T type) {
        Object value = getValueFromFile(filepath, type);

        Class bookClass = objToChange.getClass();
        String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            @SuppressWarnings("unchecked")
            Method method = bookClass.getMethod(methodName, type.getClass());
            method.invoke(objToChange, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            System.out.println("Could not call method.");
            ex.printStackTrace();
        }
    }

    private <T> Object getValueFromFile(Path filepath, T type) {
        try {
            InputStream in = Files.newInputStream(filepath);
            Serializer<T> serializer = SerializerFactory.serializer(type.getClass());

            return serializer.read(in);

        } catch (IOException | SerializerException e) {
            e.printStackTrace();
            return null;
        }
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
