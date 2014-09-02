package rosa.archive.core.store;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.RoseConstants;
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
    private Map<Class, Serializer> serializerMap;

    @Inject
    public DefaultStore(Map<Class, Serializer> serializerMap, @Assisted ByteStreamGroup base) {
        this.base = base;
        this.serializerMap = serializerMap;
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

        collection.setBooks(listBooks(collectionId));
        collection.setCharacterNames(
                loadItem(RoseConstants.CHARACTER_NAMES, collectionGroup, CharacterNames.class));
        collection.setIllustrationTitles(
                loadItem(RoseConstants.ILLUSTRATION_TITLES, collectionGroup, IllustrationTitles.class));
        collection.setNarrativeSections(
                loadItem(RoseConstants.NARRATIVE_SECTIONS, collectionGroup, NarrativeSections.class));
        collection.setMissing(loadItem(RoseConstants.MISSING_FOLIOS, collectionGroup, MissingList.class));

        // Guess what languages are supported by inspecting the Character Names object...
//        collection.getCharacterNames().getAllCharacterIds()
        // TODO make languages configurable!

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
                loadItem(bookId + RoseConstants.IMAGES, bookStreams, ImageList.class));
        book.setCroppedImages(
                loadItem(bookId + RoseConstants.IMAGES_CROP, bookStreams, ImageList.class));
        book.setCropInfo(
                loadItem(bookId + RoseConstants.CROP, bookStreams, CropInfo.class));
        book.setBookMetadata(
                loadItem(bookId + RoseConstants.DESCRIPTION, bookStreams, BookMetadata.class));
        book.setBookStructure(
                loadItem(bookId + RoseConstants.REDUCED_TAGGING, bookStreams, BookStructure.class));
        book.setChecksumInfo(
                loadItem(bookId + RoseConstants.SHA1SUM, bookStreams, ChecksumInfo.class));
        book.setIllustrationTagging(
                loadItem(bookId + RoseConstants.IMAGE_TAGGING, bookStreams, IllustrationTagging.class));
        book.setManualNarrativeTagging(
                loadItem(bookId + RoseConstants.MANUAL_NARRATIVE_TAGGING, bookStreams, NarrativeTagging.class));
        book.setAutomaticNarrativeTagging(
                loadItem(bookId + RoseConstants.AUTOMATIC_NARRATIVE_TAGGING, bookStreams, NarrativeTagging.class));
        book.setTranscription(
                loadItem(bookId + RoseConstants.TRANSCRIPTION, bookStreams, Transcription.class));

        List<String> content = bookStreams.listByteStreamNames();
        book.setContent(content.toArray(new String[bookStreams.numberOfByteStreams()]));

        // Look for permissions
        for (String name : content) {
            if (name.contains(RoseConstants.PERMISSION)) {
                String lang = findLanguageCodeInName(name);
                if (StringUtils.isNotBlank(lang)) {
                    Permission perm = loadItem(
                            bookId + RoseConstants.PERMISSION + lang + RoseConstants.XML,
                            bookStreams,
                            Permission.class
                    );
                    book.addPermission(perm, lang);
                }
            }
        }

        return book;
    }

    @SuppressWarnings("unchecked")
    protected <T> T loadItem(String name, ByteStreamGroup bsg, Class<T> type) {
        List<String> errors = new ArrayList<>();

        try {
            InputStream in = bsg.getByteStream(name);
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

    @Override
    public boolean checkBitIntegrity(BookCollection collection) {
        return false;
    }

    @Override
    public boolean checkBitIntegrity(Book book) {
        return false;
    }

    @Override
    public boolean checkContentConsistency(BookCollection collection) {
        return false;
    }

    @Override
    public boolean checkContentConsistency(Book book) {
        return false;
    }
}
