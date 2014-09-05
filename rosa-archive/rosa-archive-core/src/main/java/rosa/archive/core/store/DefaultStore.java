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
public class DefaultStore implements Store {

    private final ByteStreamGroup base;
    private final AppConfig config;
    private Map<Class, Serializer> serializerMap;
    private Map<Class, Checker> checkerMap;

    @Inject
    public DefaultStore(Map<Class, Serializer> serializerMap,
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
        collection.getCharacterNames().setId(config.getCHARACTER_NAMES());
        collection.setIllustrationTitles(
                loadItem(config.getILLUSTRATION_TITLES(), collectionGroup, IllustrationTitles.class));
        collection.getIllustrationTitles().setId(config.getILLUSTRATION_TITLES());
        collection.setNarrativeSections(
                loadItem(config.getNARRATIVE_SECTIONS(), collectionGroup, NarrativeSections.class));
        collection.getNarrativeSections().setId(config.getNARRATIVE_SECTIONS());
        collection.setMissing(loadItem(config.getMISSING_PAGES(), collectionGroup, MissingList.class));
        collection.getMissing().setId(config.getMISSING_PAGES());

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
        if (book.getImages() != null) {
            book.getImages().setId(bookId + config.getIMAGES());
        }
        book.setCroppedImages(
                loadItem(bookId + config.getIMAGES_CROP(), bookStreams, ImageList.class));
        if (book.getCroppedImages() != null) {
            book.getCroppedImages().setId(bookId + config.getIMAGES_CROP());
        }
        book.setCropInfo(
                loadItem(bookId + config.getCROP(), bookStreams, CropInfo.class));
        if (book.getCropInfo() != null) {
            book.getCropInfo().setId(bookId + config.getCROP());
        }
        book.setBookStructure(
                loadItem(bookId + config.getREDUCED_TAGGING(), bookStreams, BookStructure.class));
        if (book.getBookStructure() != null) {
            book.getBookStructure().setId(bookId + config.getREDUCED_TAGGING());
        }
        book.setChecksumInfo(
                loadItem(bookId + config.getSHA1SUM(), bookStreams, ChecksumInfo.class));
        if (book.getChecksumInfo() != null) {
            book.getChecksumInfo().setId(bookId + config.getSHA1SUM());
        }
        book.setIllustrationTagging(
                loadItem(bookId + config.getIMAGE_TAGGING(), bookStreams, IllustrationTagging.class));
        if (book.getIllustrationTagging() != null) {
            book.getIllustrationTagging().setId(bookId + config.getIMAGE_TAGGING());
        }
        book.setManualNarrativeTagging(
                loadItem(bookId + config.getNARRATIVE_TAGGING_MAN(), bookStreams, NarrativeTagging.class));
        if (book.getManualNarrativeTagging() != null) {
            book.getManualNarrativeTagging().setId(bookId + config.getNARRATIVE_TAGGING_MAN());
        }
        book.setAutomaticNarrativeTagging(
                loadItem(bookId + config.getNARRATIVE_TAGGING(), bookStreams, NarrativeTagging.class));
        if (book.getAutomaticNarrativeTagging() != null) {
            book.getAutomaticNarrativeTagging().setId(bookId + config.getNARRATIVE_TAGGING());
        }
        book.setTranscription(
                loadItem(bookId + config.getTRANSCRIPTION() + config.getXML(), bookStreams, Transcription.class));
        if (book.getTranscription() != null) {
            book.getTranscription().setId(bookId + config.getTRANSCRIPTION() + config.getXML());
        }

        List<String> content = bookStreams.listByteStreamNames();
        book.setContent(content.toArray(new String[bookStreams.numberOfByteStreams()]));

        // Look for language dependent items
        // TODO validate language found against configured languages
        for (String name : content) {
            String lang = findLanguageCodeInName(name);
            if (name.contains(config.getPERMISSION())) {
                if (StringUtils.isNotBlank(lang)) {
                    Permission perm = loadItem(name, bookStreams, Permission.class);
                    if (perm != null) {
                        perm.setId(name);
                    }

                    book.addPermission(perm, lang);
                }
            } else if (name.contains(config.getDESCRIPTION())) {
                if (StringUtils.isNoneBlank(lang)) {
                    BookMetadata metadata = loadItem(name, bookStreams, BookMetadata.class);
                    if (metadata != null) {
                        metadata.setId(name);
                    }

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
    protected <T> T loadItem(String name, ByteStreamGroup bsg, Class<T> type) {
        List<String> errors = new ArrayList<>();

        try (InputStream in = bsg.getByteStream(name)) {
            Serializer serializer = serializerMap.get(type);

            return (T) serializer.read(in, errors);

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
