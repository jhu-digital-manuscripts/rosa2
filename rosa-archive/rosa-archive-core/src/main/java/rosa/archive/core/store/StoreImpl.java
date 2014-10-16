package rosa.archive.core.store;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.*;
import rosa.archive.model.BookMetadata;

import java.io.IOException;
import java.io.InputStream;
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
    public boolean check(BookCollection collection, Book book, boolean checkBits, List<String> errors) {
        if (base.hasByteStreamGroup(collection.getId())) {
            ByteStreamGroup collectionGroup = base.getByteStreamGroup(collection.getId());
            if (collectionGroup.hasByteStreamGroup(book.getId())) {
                ByteStreamGroup bookGroup = collectionGroup.getByteStreamGroup(book.getId());
                return bookChecker.checkContent(
                        collection,
                        book,
                        bookGroup,
                        checkBits,
                        errors
                );
            }
        }
        errors.add("Unable to find book. [" + book.getId() + "]");
        return false;
    }

    @Override
    public boolean check(BookCollection collection, boolean checkBits, List<String> errors) {
        return collectionChecker.checkContent(
                collection,
                base.getByteStreamGroup(collection.getId()),
                checkBits,
                errors
        );
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
