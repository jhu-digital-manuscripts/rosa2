package rosa.archive.core.store;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.util.ChecksumUtil;
import rosa.archive.model.*;
import rosa.archive.model.BookMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class StoreImpl implements Store {

    private final ByteStreamGroup base;
    private final AppConfig config;
    private Map<Class, Serializer> serializerMap;
    private BookCollectionChecker collectionChecker;
    private BookChecker bookChecker;

    @Inject
    public StoreImpl(Map<Class, Serializer> serializerMap,
                     BookChecker bookChecker,
                     BookCollectionChecker collectionChecker,
                     AppConfig config,
                     @Assisted ByteStreamGroup base) {
        this.base = base;
        this.config = config;
        this.serializerMap = serializerMap;
        this.collectionChecker = collectionChecker;
        this.bookChecker = bookChecker;
    }

    @Override
    public String[] listBookCollections() throws IOException {
        return base.listByteStreamGroupNames()
                .toArray(new String[base.numberOfByteStreamGroups()]);
    }

    @Override
    public String[] listBooks(String collectionId) throws IOException{
        ByteStreamGroup collection = base.getByteStreamGroup(collectionId);
        return collection.listByteStreamGroupNames()
                .toArray(new String[collection.numberOfByteStreamGroups()]);
    }

    @Override
    public BookCollection loadBookCollection(String collectionId, List<String> errors) throws IOException {
        ByteStreamGroup collectionGroup = base.getByteStreamGroup(collectionId);
        BookCollection collection = new BookCollection();

        collection.setId(collectionId);
        collection.setBooks(listBooks(collectionId));
        collection.setCharacterNames(
                loadItem(config.getCHARACTER_NAMES(), collectionGroup, CharacterNames.class, errors));
        collection.setIllustrationTitles(
                loadItem(config.getILLUSTRATION_TITLES(), collectionGroup, IllustrationTitles.class, errors));
        collection.setNarrativeSections(
                loadItem(config.getNARRATIVE_SECTIONS(), collectionGroup, NarrativeSections.class, errors));
        collection.setChecksums(
                loadItem(collectionId + config.getSHA1SUM(), collectionGroup, SHA1Checksum.class, errors)
        );

        // Languages from configuration.
        collection.setLanguages(config.languages());

        return collection;
    }

    @Override
    public Book loadBook(String collectionId, String bookId, List<String> errors) throws IOException {
        ByteStreamGroup byteStreams = base.getByteStreamGroup(collectionId);
        if (!byteStreams.hasByteStreamGroup(bookId)) {
            errors.add("Unable to find book. [" + bookId + "]");
            return null;
        }

        ByteStreamGroup bookStreams = byteStreams.getByteStreamGroup(bookId);
        Book book = new Book();

        book.setId(bookId);
        book.setImages(
                loadItem(bookId + config.getIMAGES(), bookStreams, ImageList.class, errors));
        book.setCroppedImages(
                loadItem(bookId + config.getIMAGES_CROP(), bookStreams, ImageList.class, errors));
        book.setCropInfo(
                loadItem(bookId + config.getCROP(), bookStreams, CropInfo.class, errors));
        book.setBookStructure(
                loadItem(bookId + config.getREDUCED_TAGGING(), bookStreams, BookStructure.class, errors));
        book.setSHA1Checksum(
                loadItem(bookId + config.getSHA1SUM(), bookStreams, SHA1Checksum.class, errors));
        book.setIllustrationTagging(
                loadItem(bookId + config.getIMAGE_TAGGING(), bookStreams, IllustrationTagging.class, errors));
        book.setManualNarrativeTagging(
                loadItem(bookId + config.getNARRATIVE_TAGGING_MAN(), bookStreams, NarrativeTagging.class, errors));
        book.setAutomaticNarrativeTagging(
                loadItem(bookId + config.getNARRATIVE_TAGGING(), bookStreams, NarrativeTagging.class, errors));
        book.setTranscription(
                loadItem(bookId + config.getTRANSCRIPTION() + config.getXML(), bookStreams, Transcription.class, errors));

        List<String> content = bookStreams.listByteStreamNames();
        book.setContent(content.toArray(new String[bookStreams.numberOfByteStreams()]));

        // Look for language dependent items
        for (String name : content) {
            String lang = findLanguageCodeInName(name);
            if (StringUtils.isNotBlank(lang)) {
                if (name.contains(config.getPERMISSION())) {
                    Permission perm = loadItem(name, bookStreams, Permission.class, errors);
                    book.addPermission(perm, lang);
                } else if (name.contains(config.getDESCRIPTION())) {
                    BookMetadata metadata = loadItem(name, bookStreams, BookMetadata.class, errors);
                    book.addBookMetadata(metadata, lang);
                }
            }
        }

        return book;
    }

    @Override
    public boolean check(BookCollection collection, Book book, boolean checkBits,
                         List<String> errors, List<String> warnings) {
        if (base.hasByteStreamGroup(collection.getId())) {
            ByteStreamGroup collectionGroup = base.getByteStreamGroup(collection.getId());
            if (collectionGroup.hasByteStreamGroup(book.getId())) {
                ByteStreamGroup bookGroup = collectionGroup.getByteStreamGroup(book.getId());
                return bookChecker.checkContent(
                        collection,
                        book,
                        bookGroup,
                        checkBits,
                        errors,
                        warnings
                );
            }
        }
        errors.add("Unable to find book. [" + book.getId() + "]");
        return false;
    }

    @Override
    public boolean check(BookCollection collection, boolean checkBits, List<String> errors, List<String> warnings) {
        return collectionChecker.checkContent(
                collection,
                base.getByteStreamGroup(collection.getId()),
                checkBits,
                errors,
                warnings
        );
    }

    @Override
    public boolean updateChecksum(String collection, boolean force, List<String> errors) throws IOException {
        BookCollection col = loadBookCollection(collection, errors);
        return col != null && updateChecksum(col, force, errors);
    }

    @Override
    public boolean updateChecksum(BookCollection collection, boolean force, List<String> errors) throws IOException {

        SHA1Checksum checksums = collection.getChecksums();
        // If SHA1SUM does not exist, create it!
        if (checksums == null) {
            checksums = new SHA1Checksum();
            checksums.setId(collection.getId() + config.getSHA1SUM());
        }

        ByteStreamGroup collectionStreams = base.getByteStreamGroup(collection.getId());

        return updateChecksum(checksums, collectionStreams, force, errors);
    }

    @Override
    public boolean updateChecksum(String collection, String book, boolean force, List<String> errors) throws IOException {
        BookCollection col = loadBookCollection(collection, errors);
        Book b = loadBook(collection, book, errors);
        return col != null && b != null && updateChecksum(col, b, force, errors);
    }

    @Override
    public boolean updateChecksum(BookCollection collection, Book book, boolean force, List<String> errors) throws IOException {

        SHA1Checksum checksums = book.getSHA1Checksum();
        if (checksums == null) {
            checksums = new SHA1Checksum();
            checksums.setId(book.getId() + config.getSHA1SUM());
        }

        ByteStreamGroup colStreams = base.getByteStreamGroup(collection.getId());
        if (colStreams == null || !colStreams.hasByteStreamGroup(book.getId())) {
            return false;
        }
        ByteStreamGroup bookStreams = colStreams.getByteStreamGroup(book.getId());

        return updateChecksum(checksums, bookStreams, force, errors);
    }

    /**
     * Update all checksum values for an archive.
     *
     * <p>
     *     A {@link rosa.archive.core.ByteStreamGroup} with top level byte streams
     *     representing the level in the archive to be checked is used. Each byte
     *     stream is checked for the last time its contents were modified. If this
     *     date falls after (more recent) the last modified date of the checksum
     *     file, then the checksum value for that byte stream is updated and saved.
     *     A new checksum entry is calculated and saved for those streams that are
     *     not already present in the checksum data.
     * </p>
     * <p>
     *     All entries can be forced to update, regardless of last modified dates
     *     by using the {@param force} flag.
     * </p>
     * <p>
     *     If a checksum entry is initially present for a byte stream that no is not
     *     present in this {@param bsg}, it is assumed to no longer exist in the
     *     archive. In this case, the entry is removed from the checksum data.
     * </p>
     *
     * @param checksums container holding checksum information
     * @param bsg byte stream group
     * @param force overwrite all checksum values?
     * @param errors list of errors found while calculating checksums
     * @return if checksums were updated and written successfully
     * @throws IOException
     */
    protected boolean updateChecksum(SHA1Checksum checksums, ByteStreamGroup bsg, boolean force,
                                     List<String> errors) throws IOException {
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
                // Write checksum if it is out of date or doesn't exist or it is forced.
                try (InputStream in = bsg.getByteStream(streamName)) {

                    String checksumValue = ChecksumUtil.calculateChecksum(in, HashAlgorithm.SHA1);
                    checksumMap.put(streamName, checksumValue);

                } catch (NoSuchAlgorithmException e) {
                    errors.add("Failed to generate checksum. [" + bsg.name() + ":" + streamName + "]");
                    success = false;
                }
            } else if (lastMod < checksumLastMod) {
                // Keep if the item already has a checksum value that is up-to-date AND it still exists in the archive.
                checksumMap.put(streamName, checksum);
            }
        }
        // Replace old checksum map. This serves to remove all checksum entries that exist for
        // files that are no longer in the archive.
        checksums.checksums().clear();
        checksums.checksums().putAll(checksumMap);

        // Write out checksums only if nothing has failed yet.
        return success && writeItem(checksums, bsg, SHA1Checksum.class, errors);
    }

    @SuppressWarnings("unchecked")
    protected <T extends HasId> boolean  writeItem(T item, ByteStreamGroup bsg, Class<T> type, List<String> errors) {
        // No item to write
        if (item == null) {
            errors.add("Cannot write an object that does not exist! [type=" + type.toString() + "]");
            return false;
        }

        try (OutputStream out = bsg.getOutputStream(item.getId())) {

            Serializer serializer = serializerMap.get(type);
            serializer.write(item, out);

            return true;
        } catch (IOException e) {
            errors.add("Failed to write [" + item.getId() + "]");
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends HasId> T loadItem(String name, ByteStreamGroup bsg, Class<T> type, List<String> errors) {
        // The file does not exist
        if (!bsg.hasByteStream(name)) {
            return null;
        }

        try (InputStream in = bsg.getByteStream(name)) {
            Serializer serializer = serializerMap.get(type);

            T obj = (T) serializer.read(in, errors);
            obj.setId(name);

            return obj;

        } catch (IOException e) {
            errors.add("Failed to read item in archive. [" + name + "]");
            return null;
        }
    }

    protected String findLanguageCodeInName(String name) {

        String[] parts = name.split("_");
        for (String part : parts) {
            if (part.matches("(\\w){2,3}(?:(\\.[\\w]+)|$)")) {
                return part.split("\\.")[0];
            }
        }

        return "";
    }


}
