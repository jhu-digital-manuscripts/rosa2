package rosa.archive.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.core.util.BookImageComparator;
import rosa.archive.core.util.ChecksumUtil;
import rosa.archive.core.util.CropRunnable;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookReferenceSheet;
import rosa.archive.model.BookStructure;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.CropData;
import rosa.archive.model.CropInfo;
import rosa.archive.model.HasId;
import rosa.archive.model.HashAlgorithm;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.ReferenceSheet;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;
import rosa.archive.model.Permission;
import rosa.archive.model.SHA1Checksum;
import rosa.archive.model.Transcription;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.meta.MultilangMetadata;

import com.google.inject.Inject;

/**
 *
 */
public class StoreImpl implements Store, ArchiveConstants {
    private final SerializerSet serializers;
    private final ByteStreamGroup base;
    private final BookCollectionChecker collectionChecker;
    private final BookChecker bookChecker;

    @Inject
    public StoreImpl(SerializerSet serializers, BookChecker bookChecker, BookCollectionChecker collectionChecker,
            ByteStreamGroup base) {
        this.serializers = serializers;
        this.base = base;
        this.collectionChecker = collectionChecker;
        this.bookChecker = bookChecker;
    }

    @Override
    public String[] listBookCollections() throws IOException {
        return base.listByteStreamGroupNames().toArray(new String[] {});
    }

    @Override
    public String[] listBooks(String collectionId) throws IOException {
        ByteStreamGroup collection = base.getByteStreamGroup(collectionId);
        return collection.listByteStreamGroupNames().toArray(new String[] {});
    }

    @Override
    public BookCollection loadBookCollection(String collectionId, List<String> errors) throws IOException {
        errors = nonNullList(errors);
        ByteStreamGroup collectionGroup = base.getByteStreamGroup(collectionId);
        BookCollection collection = new BookCollection();

        collection.setId(collectionId);
        collection.setBooks(listBooks(collectionId));
        collection.setCharacterNames(loadItem(CHARACTER_NAMES, collectionGroup, CharacterNames.class, errors));
        collection.setIllustrationTitles(loadItem(ILLUSTRATION_TITLES, collectionGroup, IllustrationTitles.class,
                errors));
        collection.setNarrativeSections(loadItem(NARRATIVE_SECTIONS, collectionGroup, NarrativeSections.class, errors));
        collection.setChecksum(loadItem(collectionId + SHA1SUM, collectionGroup, SHA1Checksum.class, errors));
        // Add AoR collection data, if applicable
        collection.setPeopleRef(loadItem(PEOPLE, collectionGroup, ReferenceSheet.class, errors));
        collection.setLocationsRef(loadItem(LOCATIONS, collectionGroup, ReferenceSheet.class, errors));
        collection.setBooksRef(loadItem(BOOKS, collectionGroup, BookReferenceSheet.class, errors));

        BookImage missing = new BookImage();
        missing.setId(MISSING_IMAGE);
        missing.setMissing(false);

        int[] missingDimensions = getMissingImageDimensions(collectionId);
        missing.setWidth(missingDimensions[0]);
        missing.setHeight(missingDimensions[1]);

        collection.setMissingImage(missing);

        // Languages from configuration.
        Properties props = new Properties();
        try (InputStream configIn = collectionGroup.getByteStream("config.properties")) {
            props.load(configIn);
        }
        String langs = props.getProperty("languages");
        if (langs != null) {
            collection.setLanguages(langs.split(","));
        }

        return collection;
    }

    @Override
    public Book loadBook(BookCollection collection, String bookId, List<String> errors) throws IOException {
        errors = nonNullList(errors);
        ByteStreamGroup byteStreams = base.getByteStreamGroup(collection.getId());

        if (!byteStreams.hasByteStreamGroup(bookId)) {
            errors.add("Unable to find book. [" + bookId + "]");
            return null;
        }

        ByteStreamGroup bookStreams = byteStreams.getByteStreamGroup(bookId);
        Book book = new Book();

        book.setId(bookId);
        book.setImages(loadItem(bookId + IMAGES, bookStreams, ImageList.class, errors));
        book.setCroppedImages(loadItem(bookId + IMAGES_CROP, bookStreams, ImageList.class, errors));
        book.setCropInfo(loadItem(bookId + CROP, bookStreams, CropInfo.class, errors));
        book.setBookStructure(loadItem(bookId + REDUCED_TAGGING, bookStreams, BookStructure.class, errors));
        book.setChecksum(loadItem(bookId + SHA1SUM, bookStreams, SHA1Checksum.class, errors));
        book.setIllustrationTagging(loadItem(bookId + IMAGE_TAGGING, bookStreams, IllustrationTagging.class, errors));
        book.setManualNarrativeTagging(loadItem(bookId + NARRATIVE_TAGGING_MAN, bookStreams, NarrativeTagging.class,
                errors));
        book.setAutomaticNarrativeTagging(loadItem(bookId + NARRATIVE_TAGGING, bookStreams, NarrativeTagging.class,
                errors));
        book.setTranscription(loadItem(bookId + TRANSCRIPTION + XML_EXT, bookStreams, Transcription.class, errors));
        book.setMultilangMetadata(loadItem(bookId + ".description.xml", bookStreams, MultilangMetadata.class, errors));

        List<String> content = bookStreams.listByteStreamNames();
        book.setContent(content.toArray(new String[] {}));

        // For all image lists, add in dimensions of missing images
        setMissingDimensions(book.getImages(), collection.getMissingImage());
        setMissingDimensions(book.getCroppedImages(), collection.getMissingImage());

        List<AnnotatedPage> pages = book.getAnnotatedPages();

        // Handle permission and description in all languages
        for (String lang : collection.getAllSupportedLanguages()) {
            String perm_name = bookId + PERMISSION + lang + HTML_EXT;
            book.addPermission(loadItem(perm_name, bookStreams, Permission.class, errors), lang);
            
            String descr_name = bookId + DESCRIPTION + lang + XML_EXT;
            book.addBookMetadata(loadItem(descr_name, bookStreams, BookMetadata.class, errors), lang);
        }

        // Handle AoR annotations
        for (String name : content) {
            if (name.startsWith(bookId + AOR_ANNOTATION ) && name.endsWith(XML_EXT)) {
                pages.add(loadItem(name, bookStreams, AnnotatedPage.class, errors));
            }
        }

        return book;
    }

    private void setMissingDimensions(ImageList images, BookImage missingImage) {
        if (missingImage == null || images == null) {
            return;
        }

        for (BookImage image : images) {
            if (image.isMissing()) {
//                image.setId(missingImage.getId());
                image.setWidth(missingImage.getWidth());
                image.setHeight(missingImage.getHeight());
                image.setMissing(true);
            }
        }
    }

    @Override
    public boolean check(BookCollection collection, Book book, boolean checkBits, List<String> errors,
            List<String> warnings) {
        errors = nonNullList(errors);
        warnings = nonNullList(warnings);
        if (base.hasByteStreamGroup(collection.getId())) {
            ByteStreamGroup collectionGroup = base.getByteStreamGroup(collection.getId());
            if (collectionGroup.hasByteStreamGroup(book.getId())) {
                ByteStreamGroup bookGroup = collectionGroup.getByteStreamGroup(book.getId());
                return bookChecker.checkContent(collection, book, bookGroup, checkBits, errors, warnings);
            }
        }
        errors.add("Unable to find book. [" + book.getId() + "]");
        return false;
    }

    @Override
    public boolean check(BookCollection collection, boolean checkBits, List<String> errors, List<String> warnings) {
        errors = nonNullList(errors);
        warnings = nonNullList(warnings);
        return collectionChecker.checkContent(collection, base.getByteStreamGroup(collection.getId()), checkBits,
                errors, warnings);
    }

    @Override
    public boolean updateChecksum(String collection, boolean force, List<String> errors) throws IOException {
        errors = nonNullList(errors);
        BookCollection col = loadBookCollection(collection, errors);
        return col != null && updateChecksum(col, force, errors);
    }

    @Override
    public boolean updateChecksum(BookCollection collection, boolean force, List<String> errors) throws IOException {
        errors = nonNullList(errors);

        SHA1Checksum checksums = collection.getChecksum();
        // If SHA1SUM does not exist, create it!
        if (checksums == null) {
            checksums = new SHA1Checksum();
            checksums.setId(collection.getId() + SHA1SUM);
        }

        ByteStreamGroup collectionStreams = base.getByteStreamGroup(collection.getId());

        return updateChecksum(checksums, collectionStreams, force, errors);
    }

    @Override
    public boolean updateChecksum(String collection, String book, boolean force, List<String> errors)
            throws IOException {
        errors = nonNullList(errors);
        BookCollection col = loadBookCollection(collection, errors);
        Book b = loadBook(col, book, errors);
        return col != null && b != null && updateChecksum(col, b, force, errors);
    }

    @Override
    public void generateAndWriteImageList(String collection, String book, boolean force, List<String> errors)
            throws IOException {
        errors = nonNullList(errors);

        if (!base.hasByteStreamGroup(collection)) {
            errors.add("Collection not found in directory. [" + base.id() + "]");
            return;
        } else if (!base.getByteStreamGroup(collection).hasByteStreamGroup(book)) {
            errors.add("Book not found in collection. [" + collection + "]");
            return;
        }

        ByteStreamGroup bookStreams = base.getByteStreamGroup(collection).getByteStreamGroup(book);

        if (!force && bookStreams.hasByteStream(book + IMAGES)) {
            errors.add("[" + book + IMAGES + "] already exists. You can force this operation"
                    + " to update the existing image list.");
            return;
        }

        ImageList list = new ImageList();
        list.setId(book + IMAGES);
        list.setImages(buildImageList(collection, book, true, bookStreams));

        writeItem(list, bookStreams, ImageList.class, errors);
    }

    @Override
    public boolean updateChecksum(BookCollection collection, Book book, boolean force, List<String> errors)
            throws IOException {
        errors = nonNullList(errors);

        SHA1Checksum checksums = book.getChecksum();
        if (checksums == null) {
            checksums = new SHA1Checksum();
            checksums.setId(book.getId() + SHA1SUM);
        }

        ByteStreamGroup colStreams = base.getByteStreamGroup(collection.getId());
        if (colStreams == null || !colStreams.hasByteStreamGroup(book.getId())) {
            return false;
        }
        ByteStreamGroup bookStreams = colStreams.getByteStreamGroup(book.getId());

        return updateChecksum(checksums, bookStreams, force, errors);
    }

    @Override
    public void generateAndWriteCropList(String collection, String book, boolean force, List<String> errors)
            throws IOException {
        errors = nonNullList(errors);

        if (!base.hasByteStreamGroup(collection)) {
            errors.add("Collection not found in directory. [" + base.id() + "]");
            return;
        } else if (!base.getByteStreamGroup(collection).hasByteStreamGroup(book)) {
            errors.add("Book not found in collection. [" + collection + "]");
            return;
        }

        ByteStreamGroup bookStreams = base.getByteStreamGroup(collection).getByteStreamGroup(book);

        if (!bookStreams.hasByteStreamGroup(CROPPED_DIR)) {
            errors.add("No cropped images found. [" + collection + ":" + book + "]");
            return;
        } else if (!force && bookStreams.hasByteStream(book + IMAGES_CROP)) {
            errors.add("[" + book + IMAGES_CROP + "] already exists. You can force this operation"
                    + " to update the existing image list.");
            return;
        }

        ImageList list = new ImageList();
        list.setId(book + IMAGES_CROP);
        list.setImages(buildImageList(collection, book, false, bookStreams.getByteStreamGroup(CROPPED_DIR)));

        writeItem(list, bookStreams, ImageList.class, errors);
    }

    @Override
    public void cropImages(String collection, String book, boolean force, List<String> errors) throws IOException {
        errors = nonNullList(errors);

        if (!base.hasByteStreamGroup(collection)) {
            errors.add("Collection not found in directory. [" + base.id() + "]");
            return;
        } else if (!base.getByteStreamGroup(collection).hasByteStreamGroup(book)) {
            errors.add("Book not found in collection. [" + collection + "]");
            return;
        }

        // Load the book
        ByteStreamGroup bookStreams = base.getByteStreamGroup(collection).getByteStreamGroup(book);
        BookCollection col = loadBookCollection(collection, errors);       
        Book b = loadBook(col, book, errors);

        if (!force && b.getCroppedImages() != null && bookStreams.hasByteStreamGroup(CROPPED_DIR)) {
            errors.add("Cropped images already exist for this book. [" + collection + ":" + book
                    + "]. Force overwrite with '-force'");
            return;
        }

        // Create the cropped/ directory
        ByteStreamGroup cropGroup = bookStreams.newByteStreamGroup(CROPPED_DIR);

        CropInfo cropInfo = b.getCropInfo();
        ImageList images = b.getImages();

        // Crop images using ImageMagick, 4 at a time
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for (BookImage image : images) {
            if (image.isMissing()) {
                continue;
            }

            CropData cropping = cropInfo.getCropDataForPage(image.getId());
            if (cropping == null) {
                errors.add("Image missing from cropping information, copying old file. [" + image.getId() + "]");
                bookStreams.copyByteStream(image.getId(), cropGroup);
                continue;
            }

            Runnable cropper = new CropRunnable(bookStreams.id(), image, cropping, CROPPED_DIR, errors);
            executorService.execute(cropper);
        }
        executorService.shutdown();

        try {
            executorService.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            errors.add("Cropping was interrupted!\n" + stacktrace(e));
        }
    }

    /**
     * Inspect a list of images and add images that are missing.
     * 
     * Some images seem to be required, but can sometimes be missing: front/back
     * cover, front/back pastedown
     * 
     * @param book
     *            book ID
     * @param images
     *            list of images in the book
     */
    private void addMissingImages(String book, List<BookImage> images, int[] missingDimensions) {
        if (images == null || missingDimensions == null || missingDimensions.length != 2) {
            return;
        }

        if (images.size() < 2) {
            String[] requiredPages = { book + IMG_FRONTCOVER, book + IMG_FRONTPASTEDOWN,
                    book + IMG_FRONT_FLYLEAF + "001r.tif", book + IMG_FRONT_FLYLEAF + "001v.tif",
                    book + IMG_ENDPASTEDOWN, book + IMG_BACKCOVER };

            for (String name : requiredPages) {
                BookImage image = new BookImage();
                image.setId(name);
                image.setWidth(missingDimensions[0]);
                image.setHeight(missingDimensions[1]);
                image.setMissing(true);

                if (!images.contains(image)) {
                    images.add(image);
                }
            }
            return;
        }
        // front/back covers can be missing
        BookImage img = images.get(0);
        if (!img.getId().contains(IMG_FRONTCOVER)) {
            images.add(0, new BookImage(book + IMG_FRONTCOVER, missingDimensions[0], missingDimensions[1], true));
        }
        img = images.get(1);
        if (!img.getId().contains(IMG_FRONTPASTEDOWN)) {
            images.add(1, new BookImage(book + IMG_FRONTPASTEDOWN, missingDimensions[0], missingDimensions[1], true));
        }
        // front/back pastedown can be missing
        img = images.get(images.size() - 2);
        if (!img.getId().contains(IMG_ENDPASTEDOWN)) {
            images.add(images.size(), new BookImage(book + IMG_ENDPASTEDOWN, missingDimensions[0],
                    missingDimensions[1], true));
        }
        img = images.get(images.size() - 1);
        if (!img.getId().contains(IMG_BACKCOVER)) {
            images.add(images.size(), new BookImage(book + IMG_BACKCOVER, missingDimensions[0], missingDimensions[1],
                    true));
        }

        // If images were skipped over, add those
        addSkippedImages(images, missingDimensions);

        // Flyleaves, if present, must end in 'v' and have at minimum 1r & 1v
        int frontFlyleafIndex = lastIndexOfPrefix(book + IMG_FRONT_FLYLEAF, images);
        if (frontFlyleafIndex != -1) {
            img = images.get(frontFlyleafIndex);
            if (img.getId().endsWith("r.tif")) {
                images.add(frontFlyleafIndex + 1, new BookImage(img.getId().substring(0, img.getId().length() - 5)
                        + "v.tif", missingDimensions[0], missingDimensions[1], true));
            }
        }
        int endFlyleafIndex = lastIndexOfPrefix(book + IMG_END_FLYLEAF, images);
        if (endFlyleafIndex != -1) {
            img = images.get(endFlyleafIndex);
            if (img.getId().endsWith("r.tif")) {
                images.add(endFlyleafIndex + 1, new BookImage(img.getId().substring(0, img.getId().length() - 5)
                        + "v.tif", missingDimensions[0], missingDimensions[1], true));
            }
        }
    }

    /**
     * Add any images that were skipped. The manuscripts contain several
     * separate sequences of images (frontmatter, endmatter, and the main
     * sequence). In each of these sequences, it is possible for images to be
     * missing. This method adds a placeholder to the image list with the
     * correct name for those missing images.
     * 
     * @param images
     *            list of all BookImages
     * @param missingDimensions
     *            dimensions of the missing image
     */
    private void addSkippedImages(List<BookImage> images, int[] missingDimensions) {
        Collections.sort(images, BookImageComparator.instance());
        // Java8 only!
        // images.sort(BookImageComparator.instance());

        List<BookImage> missing = new ArrayList<>();
        for (int i = 1; i < images.size(); i++) {
            BookImage i1 = images.get(i - 1);
            BookImage i2 = images.get(i);

            String[] p1 = i1.getId().split("\\.");
            String[] p2 = i2.getId().split("\\.");

            if (p1.length != p2.length) {
                // assuming prefixes with folios change length
                continue;
            }

            String f1 = findFolio(i1.getId());
            String f2 = findFolio(i2.getId());

            if (StringUtils.isNotBlank(f1) && StringUtils.isNotBlank(f2)) {
                String prefix1 = i1.getId().substring(0, i1.getId().length() - (f1 + TIF_EXT).length());
                String prefix2 = i2.getId().substring(0, i2.getId().length() - (f2 + TIF_EXT).length());

                if (!prefix1.equals(prefix2)) {
                    continue;
                }

                int seq1 = Integer.parseInt(f1.substring(0, f1.length() - 1));
                int seq2 = Integer.parseInt(f2.substring(0, f2.length() - 1));

                char rv1 = f1.charAt(f1.length() - 1);
                char rv2 = f2.charAt(f2.length() - 1);

                // if seq1 == seq2 AND rv1 == 'r' AND rv2 == 'v'
                // nothing missing.
                // if seq1 == seq2 AND (rv1 == 'v' OR rv2 == 'r')
                // something is wrong!
                // if seq1 > seq2
                // list not sorted correctly
                if (seq1 < seq2) {
                    int numMissing = (seq2 - seq1 - 1) * 2;

                    if (rv1 == 'r') {
                        numMissing++;
                    }
                    if (rv2 == 'v') {
                        numMissing++;
                    }

                    char next_rv = rv1;
                    int next_seq = seq1;
                    for (int j = 0; j < numMissing; j++) {
                        if (next_rv == 'v') {
                            next_rv = 'r';
                            next_seq++;
                        } else {
                            next_rv = 'v';
                        }

                        BookImage missingImage = new BookImage(prefix1 + String.format("%03d", next_seq) + next_rv
                                + TIF_EXT, missingDimensions[0], missingDimensions[1], true);
                        missing.add(missingImage);
                    }
                }
            }
        }

        images.addAll(missing);
        Collections.sort(images, BookImageComparator.instance());
        // images.sort(BookImageComparator.instance());
    }

    /**
     * Find the folio designation from an image file name
     * 
     * @param filename
     *            .
     * @return folio number + side
     */
    public static String findFolio(String filename) {
        Pattern p = Pattern.compile("(\\d+)(r|v)");
        Matcher m = p.matcher(filename);

        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            return String.format("%03d", n) + m.group(2);
        } else {
            return null;
        }
    }

    /**
     * Take all images from
     * 
     * @param bookStreams
     *            and build a list of {@link rosa.archive.model.BookImage}s.
     * 
     * @param collection
     *            collection name
     * @param book
     *            book name
     * @param bookStreams
     *            byte stream group containing the images
     * @return list of BookImages
     * @throws IOException
     */
    private List<BookImage> buildImageList(String collection, String book, boolean addMissing,
            ByteStreamGroup bookStreams) throws IOException {
        List<BookImage> images = new ArrayList<>();
        for (String file : bookStreams.listByteStreamNames()) {
            if (!file.startsWith(".") && file.endsWith(TIF_EXT)) {

                String filepath = Paths.get(bookStreams.id()).resolve(file).toString();
                int[] dimensions = getImageDimensionsHack(filepath);

                BookImage img = new BookImage();
                img.setId(file);
                img.setWidth(dimensions[0]);
                img.setHeight(dimensions[1]);
                img.setMissing(false);

                images.add(img);
            }
        }
        Collections.sort(images, BookImageComparator.instance());
        // images.sort(BookImageComparator.instance());

        if (addMissing) {
            int[] missingDimensions = getMissingImageDimensions(collection);
            addMissingImages(book, images, missingDimensions);
        }

        return images;
    }

    private int[] getMissingImageDimensions(String collection) throws IOException {
        return base.getByteStreamGroup(collection).hasByteStream(MISSING_IMAGE) ? getImageDimensionsHack(Paths
                .get(base.getByteStreamGroup(collection).id()).resolve(MISSING_IMAGE).toString())
                : new int[] { 0, 0 };
    }

    /**
     * @param prefix
     *            defines the sequence
     * @param images
     *            list of all images
     * @return index of last item or -1 if prefix does not appear in the list
     */
    private int lastIndexOfPrefix(String prefix, List<BookImage> images) {
        int last = -1;

        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getId().startsWith(prefix)) {
                last = i;
            }
        }
        return last;
    }

    /**
     * Code adapted from original rosa archive tool, now uses Apache Commons IO
     * instead of custom byte array wrapper class.
     * 
     * @param path
     *            file path of image
     * @return array: [width, height]
     * @throws IOException
     */
    private static int[] getImageDimensionsHack(String path) throws IOException {

        String[] cmd = new String[] { "identify", "-ping", "-format", "%w %h ", path + "[0]" };
        Process p = Runtime.getRuntime().exec(cmd);

        try {
            if (p.waitFor() != 0) {
                byte[] byteArray = IOUtils.toByteArray(p.getErrorStream());
                String err = new String(byteArray, "UTF-8");

                throw new IOException("Failed to run on " + path + ": " + err);
            }

            byte[] buff = IOUtils.toByteArray(p.getInputStream());
            String result = new String(buff, "UTF-8");

            String[] s = result.trim().split("\\s+");
            if (s.length != 2) {
                throw new IOException("Invalid result " + result + " on " + path);
            }

            return new int[] { Integer.parseInt(s[0]), Integer.parseInt(s[1].trim()) };
        } catch (NumberFormatException e) {
            throw new IOException("Invalid result.");
        } catch (InterruptedException e) {
            throw new IOException(e);
        } finally {
            p.destroy();
        }
    }

    /**
     * Update all checksum values for an archive.
     * 
     * <p>
     * A {@link rosa.archive.core.ByteStreamGroup} with top level byte streams
     * representing the level in the archive to be checked is used. Each byte
     * stream is checked for the last time its contents were modified. If this
     * date falls after (more recent) the last modified date of the checksum
     * file, then the checksum value for that byte stream is updated and saved.
     * A new checksum entry is calculated and saved for those streams that are
     * not already present in the checksum data.
     * </p>
     * <p>
     * All entries can be forced to update, regardless of last modified dates by
     * using the
     * 
     * @param force
     *            flag.
     *            </p>
     *            <p>
     *            If a checksum entry is initially present for a byte stream
     *            that no is not present in this
     * @param bsg
     *            , it is assumed to no longer exist in the archive. In this
     *            case, the entry is removed from the checksum data.
     *            </p>
     * 
     * @param checksums
     *            container holding checksum information
     * @param errors
     *            list of errors found while calculating checksums
     * @return if checksums were updated and written successfully
     * @throws IOException
     */
    protected boolean updateChecksum(SHA1Checksum checksums, ByteStreamGroup bsg, boolean force, List<String> errors)
            throws IOException {
        boolean success = true;

        long checksumLastMod = bsg.getLastModified(checksums.getId());
        Map<String, String> checksumMap = new HashMap<>();

        for (String streamName : bsg.listByteStreamNames()) {
            // Do not record checksum for the checksum file!
            if (streamName.equals(checksums.getId())) {
                continue;
            }

            long lastMod = bsg.getLastModified(streamName);
            String checksum = checksums.checksums().get(streamName);

            if (force || lastMod >= checksumLastMod || checksum == null) {
                // Write checksum if it is out of date or doesn't exist or it is
                // forced.
                try (InputStream in = bsg.getByteStream(streamName)) {

                    String checksumValue = ChecksumUtil.calculateChecksum(in, HashAlgorithm.SHA1);
                    checksumMap.put(streamName, checksumValue);

                } catch (NoSuchAlgorithmException e) {
                    errors.add("Failed to generate checksum. [" + bsg.name() + ":" + streamName + "]");
                    success = false;
                }
            } else if (lastMod < checksumLastMod) {
                // Keep if the item already has a checksum value that is
                // up-to-date AND it still exists in the archive.
                checksumMap.put(streamName, checksum);
            }
        }
        // Replace old checksum map. This serves to remove all checksum entries
        // that exist for
        // files that are no longer in the archive.
        checksums.checksums().clear();
        checksums.checksums().putAll(checksumMap);

        // Write out checksums only if nothing has failed yet.
        return success && writeItem(checksums, bsg, SHA1Checksum.class, errors);
    }

    protected <T extends HasId> boolean writeItem(T item, ByteStreamGroup bsg, Class<T> type, List<String> errors) {
        // No item to write
        if (item == null) {
            errors.add("Cannot write an object that does not exist! [type=" + type.toString() + "]");
            return false;
        }

        try (OutputStream out = bsg.getOutputStream(item.getId())) {
            serializers.getSerializer(type).write(item, out);
            return true;
        } catch (IOException e) {
            errors.add("Failed to write [" + item.getId() + "]\n" + stacktrace(e));
            return false;
        }
    }

    protected <T extends HasId> T loadItem(String name, ByteStreamGroup bsg, Class<T> type, List<String> errors) {
        // The file does not exist
        if (!bsg.hasByteStream(name)) {
            return null;
        }

        try (InputStream in = bsg.getByteStream(name)) {
            T obj = serializers.getSerializer(type).read(in, errors);
            obj.setId(name);

            return obj;

        } catch (IOException e) {
            errors.add("Failed to read item in archive. [" + name + "]\n" + stacktrace(e));
            return null;
        }
    }

    private String stacktrace(Exception e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(out));

        return out.toString();
    }

    /**
     * @param in input list
     * @return a new list if input is NULL, the input list otherwise
     */
    private List<String> nonNullList(List<String> in) {
        if (in == null) {
            return new ArrayList<>();
        }
        return in;
    }
}
