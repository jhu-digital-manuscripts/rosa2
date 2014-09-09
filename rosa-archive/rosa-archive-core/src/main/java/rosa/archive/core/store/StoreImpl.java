package rosa.archive.core.store;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.check.Checker;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class StoreImpl implements Store {

    private final ByteStreamGroup base;
    private final AppConfig config;
    private Map<Class, Serializer> serializerMap;
    private Map<Class, Checker> checkerMap;

    @Inject
    public StoreImpl(Map<Class, Serializer> serializerMap,
                     Map<Class, Checker> checkerMap,
                     AppConfig config,
                     @Assisted ByteStreamGroup base) {
        this.base = base;
        this.config = config;
        this.serializerMap = serializerMap;
        this.checkerMap = checkerMap;
    }

    @Override
    public String[] listBookCollections() {
        return base.listByteStreamGroupNames()
                .toArray(new String[base.numberOfByteStreamGroups()]);
    }

    @Override
    public String[] listBooks(String collectionId) {
        ByteStreamGroup collection = base.getByteStreamGroup(collectionId);
        return collection.listByteStreamGroupNames()
                .toArray(new String[collection.numberOfByteStreamGroups()]);
    }

    @Override
    public BookCollection loadBookCollection(String collectionId) {
        ByteStreamGroup collectionGroup = base.getByteStreamGroup(collectionId);
        BookCollection collection = new BookCollection();

        collection.setId(collectionId);
        collection.setBooks(listBooks(collectionId));
        collection.setCharacterNames(
                loadItem(config.getCHARACTER_NAMES(), collectionGroup, CharacterNames.class));
        collection.setIllustrationTitles(
                loadItem(config.getILLUSTRATION_TITLES(), collectionGroup, IllustrationTitles.class));
        collection.setNarrativeSections(
                loadItem(config.getNARRATIVE_SECTIONS(), collectionGroup, NarrativeSections.class));
        collection.setMissing(loadItem(config.getMISSING_PAGES(), collectionGroup, MissingList.class));

        // Languages from configuration.
        collection.setLanguages(config.languages());

        return collection;
    }

    @Override
    public Book loadBook(String collectionId, String bookId) {
        ByteStreamGroup byteStreams = base.getByteStreamGroup(collectionId);
        if (!byteStreams.hasByteStreamGroup(bookId)) {
            // TODO report missing book
            return null;
        }

        ByteStreamGroup bookStreams = byteStreams.getByteStreamGroup(bookId);
        Book book = new Book();

        book.setId(bookId);
        book.setImages(
                loadItem(bookId + config.getIMAGES(), bookStreams, ImageList.class));
        book.setCroppedImages(
                loadItem(bookId + config.getIMAGES_CROP(), bookStreams, ImageList.class));
        book.setCropInfo(
                loadItem(bookId + config.getCROP(), bookStreams, CropInfo.class));
        book.setBookStructure(
                loadItem(bookId + config.getREDUCED_TAGGING(), bookStreams, BookStructure.class));
        book.setChecksumInfo(
                loadItem(bookId + config.getSHA1SUM(), bookStreams, ChecksumInfo.class));
        book.setIllustrationTagging(
                loadItem(bookId + config.getIMAGE_TAGGING(), bookStreams, IllustrationTagging.class));
        book.setManualNarrativeTagging(
                loadItem(bookId + config.getNARRATIVE_TAGGING_MAN(), bookStreams, NarrativeTagging.class));
        book.setAutomaticNarrativeTagging(
                loadItem(bookId + config.getNARRATIVE_TAGGING(), bookStreams, NarrativeTagging.class));
        book.setTranscription(
                loadItem(bookId + config.getTRANSCRIPTION() + config.getXML(), bookStreams, Transcription.class));

        List<String> content = bookStreams.listByteStreamNames();
        book.setContent(content.toArray(new String[bookStreams.numberOfByteStreams()]));

        // Look for language dependent items
        // TODO validate language found against configured languages
        for (String name : content) {
            String lang = findLanguageCodeInName(name);
            if (name.contains(config.getPERMISSION())) {
                if (StringUtils.isNotBlank(lang)) {
                    Permission perm = loadItem(name, bookStreams, Permission.class);
                    book.addPermission(perm, lang);
                }
            } else if (name.contains(config.getDESCRIPTION())) {
                if (StringUtils.isNoneBlank(lang)) {
                    BookMetadata metadata = loadItem(name, bookStreams, BookMetadata.class);
                    book.addBookMetadata(metadata, lang);
                }
            }
        }

        return book;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean check(Book book, boolean checkBits) {
        return checkerMap.get(Book.class).checkContent(book, base, checkBits);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean check(BookCollection collection, boolean checkBits) {
        return checkerMap.get(BookCollection.class).checkContent(collection, base, checkBits);
    }

    @SuppressWarnings("unchecked")
    protected <T extends HasId> T loadItem(String name, ByteStreamGroup bsg, Class<T> type) {
        List<String> errors = new ArrayList<>();

        try (InputStream in = bsg.getByteStream(name)) {
            Serializer serializer = serializerMap.get(type);

            T obj = (T) serializer.read(in, errors);
            obj.setId(name);

            return obj;

        } catch (IOException e) {
            // TODO
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
