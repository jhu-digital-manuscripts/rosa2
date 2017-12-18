package rosa.archive.core.check;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.core.util.CachingUrlResourceResolver;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookReferenceSheet;
import rosa.archive.model.BookScene;
import rosa.archive.model.BookStructure;
import rosa.archive.model.BookText;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.CropData;
import rosa.archive.model.CropInfo;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;
import rosa.archive.model.Permission;
import rosa.archive.model.ReferenceSheet;
import rosa.archive.model.SHA1Checksum;
import rosa.archive.model.Transcription;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.archive.model.meta.MultilangMetadata;
import rosa.archive.model.redtag.Item;
import rosa.archive.model.redtag.StructureColumn;
import rosa.archive.model.redtag.StructurePage;
import rosa.archive.model.redtag.StructurePageSide;

import com.google.inject.Inject;

/**
 * @see rosa.archive.model.Book
 */
public class BookChecker extends AbstractArchiveChecker {
    private static final String PAGE_PATTERN = "\\w*\\d+(r|v|R|V)";
    private static final String MANUSCRIPT = "manuscript";
    private static final String DESCRIPTION_SCHEMA_LOCATION = "rosa-book-description.xsd";
    private static final LSResourceResolver resourceResolver = new CachingUrlResourceResolver();

    private static Schema aorAnnotationSchema;
    private static Schema teiSchema;
    private static Schema desecriptionSchema;

    /**
     * @param serializers all required serializers
     */
    @Inject
    public BookChecker(SerializerSet serializers) {
        super(serializers);
    }

    /**
     *
     * @param collection book collection
     * @param book book to check
     * @param bsg byte stream group of the book
     * @param checkBits validate checksums for all items in the book?
     * @param errors list of errors found while checking
     * @param warnings list of warnings found while checking
     * @return TRUE if the book validates with no errors, however, warnings may still exist
     */
    public boolean checkContent(
            BookCollection collection, Book book, ByteStreamGroup bsg, boolean checkBits,
            List<String> errors, List<String> warnings) {

        if (book == null) {
            errors.add("Book is missing.");
            return false;
        }

        // Check the following items:
        //   checksumInfo
        check(book.getChecksum(), book, bsg, errors, warnings);
        //   list of images is required
        check(book.getImages(), book, bsg, errors, warnings);
        //   but list of cropped images is not required
        if (book.getCroppedImages() != null) {
            check(book.getCroppedImages(), book, bsg, errors, warnings);
        }
        //   cropInfo
        check(book.getCropInfo(), book, bsg, errors, warnings);

        checkTEIDescriptions(bsg, errors, warnings);
        // Check newer style metadata XML
        check(book.getMultilangMetadata(), bsg, errors, warnings);

        for (String lang : collection.getAllSupportedLanguages()) {
            //   bookMetadata
            check(book.getBookMetadata(lang), book, bsg, errors, warnings);
            //   permissions
            check(book.getPermission(lang), book, bsg, errors, warnings);
        }
        //   compare contents of metadata descriptions in different languages
        check(book, collection.getAllSupportedLanguages(), bsg, errors, warnings);

        //   content
        check(book.getContent(), book.getId(), errors, warnings);
        //   bookStructure
        check(book.getBookStructure(), book, bsg, errors, warnings);
        //   illustrationTagging
        check(book.getIllustrationTagging(), book, bsg, errors, warnings);
        //   manualNarrativeTagging
        check(book.getManualNarrativeTagging(), book, bsg, errors, warnings);
        //   automaticNarrativeTagging
        check(book.getAutomaticNarrativeTagging(), book, bsg, errors, warnings);
        check(book.getAutomaticNarrativeTagging(), book, bsg, errors, warnings);
        //   annotated pages
        check(collection, book, bsg, errors, warnings);
        // Check transcription
        checkTranscription(book.getTranscription(), errors, warnings);

        // Check character_names and illustration_titles
        check(
                book.getIllustrationTagging(), collection.getCharacterNames(),
                collection.getIllustrationTitles(), errors, warnings
        );

        // Check narrative_sections (automatic and manual)
        check(book.getAutomaticNarrativeTagging(), collection.getNarrativeSections(), errors, warnings);
        check(book.getManualNarrativeTagging(), collection.getNarrativeSections(), errors, warnings);

        // check bit integrity
        if (checkBits) {
            checkAllBits(bsg, book, errors, warnings);
        }

        // Return TRUE if all above checks pass
        // Return FALSE if any of the above tests fail
        return errors.isEmpty();
    }

    /**
     * Check to see if an ID is present in an array of content.
     *
     * @param id ID
     * @param contents array of contents to check
     * @return TRUE if the ID is present
     */
    private boolean isInArchive(String id, String[] contents) {
        Arrays.sort(contents);
        return Arrays.binarySearch(contents, id) >= 0;
    }

    private boolean isKnownName(String name) {
        return name.endsWith(XML_EXT)
                || name.endsWith(TXT_EXT)
                || name.endsWith(CSV_EXT)
                || name.endsWith(TIF_EXT)
                || name.contains(SHA1SUM)
                || name.contains(PERMISSION)
                || name.contains(NARRATIVE_TAGGING)
                || name.contains(NARRATIVE_TAGGING_MAN)
                || name.contains(IMAGE_TAGGING)
                || name.contains(BNF_FILEMAP)
                || name.contains(BNF_MD5SUM);
    }

    /**
     * Check the bit integrity of all items in the book archive. Checksum
     * values are calculated for each item in the book archive and compared
     * to known checksum values that are stored in the archive.
     *
     * @param bsg object wrapping all InputStreams for the archive
     * @param book book archive
     * @param errors list of errors
     * @param warnings list of warnings
     * @return list of errors found while performing check
     */
    protected List<String> checkAllBits(ByteStreamGroup bsg, Book book, List<String> errors, List<String> warnings) {
        if (book.getChecksum() == null) {
            return Arrays.asList(
                    ("Book [" + book.getId() + "] has no stored SHA1SUM. "
                            + "Cannot check bit integrity.")
            );
        }
        return checkStreams(bsg, book.getChecksum().getId(), errors, warnings);
    }

    /**
     * Logical check on the contents of the Book archive. This method checks to
     * make sure that the content names are of the expected form.
     *
     * Code taken from Rosa1 project: rosa.tool.deriv.BaseDerivative#checkFilenames(..)
     *
     * @param content item to check
     * @param bookId containing book
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(String[] content, String bookId, List<String> errors, List<String> warnings) {

        for (String name : content) {
            if (!name.startsWith(bookId + ".") && !name.contains("filemap")) {
                errors.add("File does not start with manuscript ID. [" + name + "]");
            }
            if (name.contains(" ")) {
                errors.add("File name contains forbidden character. [" + name + "]");
            } else if (!isKnownName(name)) {
                // Code taken from Rosa1 project, BaseDerivative#checkFilenames(archive)
                errors.add("Unknown file. [" + name + "]");
            }
        }
    }

    private void check(MultilangMetadata metadata, ByteStreamGroup bsg, final List<String> errors,
                       final List<String> warnings) {
        if (metadata == null) {
            return;
        }
        final String file = metadata.getId();
        if (desecriptionSchema == null) {
            SchemaFactory schemaFactory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            schemaFactory.setErrorHandler(null);

            try (InputStream in = getClass().getClassLoader().getResourceAsStream(DESCRIPTION_SCHEMA_LOCATION)) {
                desecriptionSchema = schemaFactory.newSchema(new StreamSource(in));
            } catch (IOException | SAXException e) {
                errors.add("Failed to load description schema (rosa-book-description.xsd)\n" + stacktrace(e));
            }
        }

        Validator validator = desecriptionSchema.newValidator();
        validator.setResourceResolver(resourceResolver);

        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) throws SAXException {
                warnings.add("[Warn: " + file + "] (" + e.getLineNumber() + ":"
                        + e.getColumnNumber() + "): " + e.getMessage());
            }

            @Override
            public void error(SAXParseException e) throws SAXException {
                errors.add("[Error: " + file + "] (" + e.getLineNumber() + ":"
                        + e.getColumnNumber() + "): " + e.getMessage());
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                errors.add("[Fatal Error: " + file + "] (" + e.getLineNumber() + ":"
                        + e.getColumnNumber() + "): " + e.getMessage());
            }
        });

        try {
            validator.validate(new StreamSource(bsg.getByteStream(file)));
        } catch (IOException | SAXException e) {
            errors.add("Failed to validate file (" + file + ")");
        }
    }

    /**
     * Check data consistency of a {@link rosa.archive.model.BookMetadata} object according
     * to the rules:
     *
     * <ul>
     *     <li>The metadata must exist as a data model object.</li>
     *     <li>The associated stored metadata file must exist in the archive and be readable.</li>
     *     <li>The metadata fields must not be NULL.</li>
     *     <li>The {@code yearStart} field must fall logically before the {@code yearEnd}.</li>
     * </ul>
     *
     * @param metadata data to check
     * @param parent parent container
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(BookMetadata metadata, Book parent, ByteStreamGroup bsg,
                               List<String> errors, List<String> warnings) {
        if (metadata == null) {
            errors.add("Metadata is missing.");
            return;
        }

        if (!isInArchive(metadata.getId(), parent.getContent())) {
            errors.add("Metadata not present in parent book. [" + metadata.getId() + "]");
        }

        if (StringUtils.isBlank(metadata.getDate())) {
            errors.add("Metadata date not set. [" + metadata.getId() + "]");
        }
        if (metadata.getYearStart() == -1) {
            errors.add("Metadata start year not set. [" + metadata.getId() + "]");
        }
        if (metadata.getYearEnd() == -1) {
            errors.add("Metadata end year not set. [" + metadata.getId() + "]");
        }
        if (metadata.getYearEnd() < metadata.getYearStart()) {
            errors.add("Date range ends before it begins! " +
                    "Check the description <date notBefore=" + metadata.getYearStart()
                    + " notAfter=" + metadata.getYearEnd() + "> element. ["
                    + metadata.getId() + "]");
        }
        if (StringUtils.isBlank(metadata.getCurrentLocation())) {
            errors.add("Metadata current location not set. [" + metadata.getId() + "]");
        }
        if (StringUtils.isBlank(metadata.getRepository())) {
            errors.add("Metadata repository not set. [" + metadata.getId() + "]");
        }
        if (StringUtils.isBlank(metadata.getOrigin())) {
            warnings.add("Metadata origin not set. [" + metadata.getId() + "]");
        }
        if (metadata.getNumberOfIllustrations() == -1) {
            warnings.add("Metadata number of illustrations not set. [" + metadata.getId() + "]");
        }
        if (metadata.getNumberOfPages() == -1) {
            warnings.add("Metadata number of pages/folios not set. [" + metadata.getId() + "]");
        }
        if (StringUtils.isBlank(metadata.getType())) {
            warnings.add("Metadata type not set. [" + metadata.getId() + "]");
        }
        if (StringUtils.isBlank(metadata.getCommonName())) {
            errors.add("Metadata common name not set. [" + metadata.getId() + "]");
        }
        if (StringUtils.isBlank(metadata.getMaterial())) {
            warnings.add("Metadata material not set. [" + metadata.getId() + "]");
        }
        if (metadata.getTexts() == null) {
            errors.add("Metadata texts not set. [" + metadata.getId() + "]");
        } else if (metadata.getTexts().length < 1) {
            errors.add("Metadata texts, must have at least one!");
        } else {
            for (BookText text : metadata.getTexts()) {
                if (StringUtils.isBlank(text.getFirstPage())) {
                    warnings.add("Metadata text first page not set. [" + metadata.getId() + ":" + text.getId() + "]");
                } else if (!text.getFirstPage().matches(PAGE_PATTERN) && metadata.getType().equalsIgnoreCase(MANUSCRIPT)) {
                    errors.add("Page has bad format. [" + metadata.getId() + ":" + text.getFirstPage() + "]");
                }
                if (StringUtils.isBlank(text.getLastPage())) {
                    warnings.add("Metadata text last page not set. [" + metadata.getId() + ":" + text.getId() + "]");
                } else if (!text.getLastPage().matches(PAGE_PATTERN) && metadata.getType().equalsIgnoreCase(MANUSCRIPT)) {
                    errors.add("Page has bad format. [" + metadata.getId() + ":" + text.getLastPage() + "]");
                }
                if (StringUtils.isBlank(text.getTitle())) {
                    warnings.add("Metadata text title not set. [" + metadata.getId() + ":" + text.getId() + "]");
                }
                if (text.getNumberOfIllustrations() == -1) {
                    warnings.add("Metadata number of illustrations not set. [" + metadata.getId() + ":" + text.getId() + "]");
                } else if (text.getNumberOfIllustrations() > metadata.getNumberOfIllustrations()) {
                    errors.add("Number of illustrations in text [" + text.getNumberOfIllustrations()
                            + "] is greater than the number if illustrations in the manuscript ["
                            + metadata.getNumberOfIllustrations() + "] {" + metadata.getId() + ":" + text.getId() + "}");
                }
                if (text.getNumberOfPages() == -1) {
                    warnings.add("Metadata number of pages not set. [" + metadata.getId() + ":" + text.getId() + "]");
                } else if (text.getNumberOfPages() > metadata.getNumberOfPages()) {
                    errors.add("Number of pages in text [" + text.getNumberOfPages() + "] exceeds number " +
                            "of pages in the manuscript [" + metadata.getNumberOfPages() + "] {"
                            + metadata.getId() + ":" + text.getId() + "}");
                }
                if (text.getColumnsPerPage() == -1) {
                    warnings.add("Metadata columns per page not set. [" + metadata.getId() + ":" + text.getId() + "]");
                }
                if (text.getLinesPerColumn() == -1) {
                    warnings.add("Metadata lines per column not set. [" + metadata.getId() + ":" + text.getId() + "]");
                }
            }
        }

        attemptToRead(metadata, bsg, errors, warnings);
    }

    private void check(Book parent, String[] languages, ByteStreamGroup bsg, List<String> errors, List<String> warnings) {
        List<BookMetadata> metadatas = new ArrayList<>();

        for (String lang : languages) {
            if (parent.getBookMetadata(lang) != null) {
                metadatas.add(parent.getBookMetadata(lang));
            }
        }

        if (metadatas.size() == 0) {
            errors.add("Failed to get metadata descriptions. [" + parent.getId() + "]");
            return;
        }
        BookMetadata reference = metadatas.get(0);
        for (int i = 0; i < metadatas.size(); i++) {
            BookMetadata test = metadatas.get(i);
            String fileLabel = " [" + reference.getId() + "/" + test.getId() + "] ";

            if (test.getYearStart() != reference.getYearStart()) {
                errors.add("Metadata year start different." + fileLabel
                        + "(" + reference.getYearStart() + "/" + test.getYearStart() + ")");
            }
            if (test.getYearEnd() != reference.getYearEnd()) {
                errors.add("Metadata year end different." + fileLabel
                        + "(" + reference.getYearEnd() + "/" + test.getYearEnd() + ")");
            }
            if (test.getWidth() != reference.getWidth()) {
                errors.add("Metadata width different." + fileLabel
                        + "(" + reference.getWidth() + "/" + test.getWidth() + ")");
            }
            if (test.getHeight() != reference.getHeight()) {
                errors.add("Metadata height different." + fileLabel
                        + "(" + reference.getHeight() + "/" + test.getHeight() + ")");
            }
            if (test.getNumberOfIllustrations() != reference.getNumberOfIllustrations()) {
                errors.add("Metadata number of illustrations different." + fileLabel
                        + "(" + reference.getNumberOfIllustrations() + "/" + test.getNumberOfIllustrations() + ")");
            }
            if (test.getNumberOfPages() != reference.getNumberOfPages()) {
                errors.add("Metadata number of pages different." + fileLabel
                        + "(" + reference.getNumberOfPages() + "/" + test.getNumberOfPages() + ")");
            }

            if (test.getTexts().length != reference.getTexts().length) {
                errors.add("Number of texts different." + fileLabel
                        + "(" + reference.getTexts().length + "/" + test.getTexts().length + ")");
            } else {
                for (int j = 0; j < reference.getTexts().length; j++) {
                    BookText refText = reference.getTexts()[j];
                    BookText testText = test.getTexts()[j];
                    String textLabel = " [" + refText.getTextId() + "/" + testText.getTextId() + "] ";

                    if (testText.getLinesPerColumn() != refText.getLinesPerColumn()) {
                        errors.add("Book text lines per column different" + textLabel
                                + "(" + refText.getLinesPerColumn() + "/" + testText.getLinesPerColumn() + ")");
                    }
                    if (testText.getColumnsPerPage() != refText.getColumnsPerPage()) {
                        errors.add("Book text columns per page different." + textLabel
                                + "(" + refText.getColumnsPerPage() + "/" + testText.getColumnsPerPage() + ")");
                    }
                    if (testText.getLeavesPerGathering() != refText.getLeavesPerGathering()) {
                        errors.add("Book text leaves per gathering different." + textLabel
                                + "(" + refText.getLeavesPerGathering() + "/" + testText.getLeavesPerGathering() + ")");
                    }
                    if (testText.getNumberOfIllustrations() != refText.getNumberOfIllustrations()) {
                        errors.add("Book text number of illustrations different." + textLabel
                                + "(" + refText.getNumberOfIllustrations() + "/" + testText.getNumberOfIllustrations() + ")");
                    }
                    if (testText.getNumberOfPages() != refText.getNumberOfPages()) {
                        errors.add("Book text number of pages different." + textLabel
                                + "(" + refText.getNumberOfPages() + "/" + testText.getNumberOfPages() + ")");
                    }
                }
            }
        }
    }

    /**
     * Permission statement on use of content is required.
     *
     * @param permission item to check
     * @param parent containing Book
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(Permission permission, Book parent, ByteStreamGroup bsg,
                               List<String> errors, List<String> warnings) {
        if (permission == null) {
            errors.add("Permission statement missing.");
            return;
        }

        if (StringUtils.isBlank(permission.getId())) {
            errors.add("Permission ID not set. [" + permission.getId() + "]");
        }
        if (StringUtils.isBlank(permission.getPermission())) {
            errors.add("Permission statement not set. [" + permission.getId() + "]");
        }
        if (!isInArchive(permission.getId(), parent.getContent())) {
            errors.add("Permission not in archive. [" + permission.getId() + "]");
        }

        attemptToRead(permission, bsg, errors, warnings);
    }

    /**
     * Check data consistency of an {@link rosa.archive.model.ImageList} according
     * to the following rules:
     *
     * <ul>
     *     <li>The image list must exist.</li>
     *     <li>Each image in the list must be initialized with non-null field values.</li>
     *     <li>Any image marked as missing must not appear in the Book's list of contents.</li>
     *     <li>Any image marked as not missing must appear in the Book's list of contents.</li>
     * </ul>
     *
     * @param images {@link rosa.archive.model.ImageList}
     * @param parent {@link rosa.archive.model.Book} that contains this image list
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(ImageList images, Book parent, ByteStreamGroup bsg,
                               List<String> errors, List<String> warnings) {
        if (images == null || images.getImages() == null) {
            errors.add("Image list is missing.");
            return;
        }

        for (BookImage image : images.getImages()) {
            // Make sure the width, height, ID fields were set
            if (StringUtils.isBlank(image.getId())) {
                errors.add("Image ID not set. [" + image + "]");
            }
            if (image.getWidth() == -1) {
                errors.add("Image width not set. [" + image.getId() + "]");
            }
            if (image.getHeight() == -1) {
                errors.add("Image height not set. [" + image.getId() + "]");
            }

            // Relies on the fact that the array is sorted.
            boolean inContent = isInArchive(image.getId(), parent.getContent());
            if (image.isMissing() && inContent) {
                errors.add("Image [" + image.getId() + "] marked as missing is present in archive. ["
                        + images.getId() + "]");
            } else if (!image.isMissing() && !inContent) {
                errors.add("Image [" + image.getId() + "] marked as present in archive is missing. ["
                        + images.getId() + "]");
            }
        }

        attemptToRead(images, bsg, errors, warnings);
    }

    /**
     * Check the following:
     *
     * <ul>
     *     <li>Crop information is not required.</li>
     *     <li>Crop information must exist in parent book content.</li>
     *     <li>For each data row, the ID must exist in the parent book content.</li>
     *     <li>For each data row, the values left, right, top, bottom must be between 0 and 1.</li>
     * </ul>
     *
     * @param cropInfo cropping data
     * @param parent parent book
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(CropInfo cropInfo, Book parent, ByteStreamGroup bsg,
                               List<String> errors, List<String> warnings) {
        if (cropInfo == null) {
            return;
        }

        if (!isInArchive(parent.getId() + CROP, parent.getContent())) {
            errors.add("Crop information missing from parent book content. [" + parent.getId() + "]");
        }

        for (CropData crop : cropInfo) {
            if (StringUtils.isBlank(crop.getId())) {
                errors.add("Crop ID missing. [" + crop + "]");
            } else if (!isInArchive(crop.getId(), parent.getContent())) {
                errors.add("Cropping information for item [" + crop.getId()
                        + "] missing from parent Book archive. [" + parent.getId() + "]");
            }

            if (crop.getLeft() < 0.0 || crop.getRight() < 0.0
                    || crop.getTop() < 0.0 || crop.getBottom() < 0.0) {
                errors.add("Crop cannot have negative numbers. [" + crop + "]");
            }
            if (crop.getLeft() > 1.0 || crop.getRight() > 1.0 || crop.getTop() > 1.0 || crop.getBottom() > 1.0) {
                errors.add("Crop cannot have numbers greater than 1.0 [" + crop + "]");
            }
        }

        attemptToRead(cropInfo, bsg, errors, warnings);
    }

    /**
     * <ul>
     *     <li>The item containing the checksum information must exist in the archive.</li>
     *     <li>For each hash in the list, there must be an ID to identify the item in the
     *         archive associated with the hash.</li>
     *     <li>For each hash in the list, the hashing algorithm used must be identified</li>
     *     <li>For each hash in the list, the hash value must be defined.</li>
     * </ul>
     *
     * @param info item to check
     * @param parent containing Book
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(SHA1Checksum info, Book parent, ByteStreamGroup bsg,
                               List<String> errors, List<String> warnings) {
        if (info == null) {
            String message = "SHA1SUM missing";
            if (isInArchive(parent.getId() + BNF_MD5SUM, parent.getContent())) {
                message += "; bnf.MD5SUM is present.";
                warnings.add(message);
            } else {
                message += "; no checksum information found.";
                errors.add(message);
            }
            return;
        }

        if (!isInArchive(info.getId(), parent.getContent())) {
            errors.add("Checksum information not present in archive. [" + info.getId() + "]");
        }

        for (String dataId : info.getAllIds()) {
            String hash = info.checksums().get(dataId);

            if (!isInArchive(dataId, parent.getContent())
                    && !dataId.contains("~") && !dataId.contains("#")) {
                errors.add("Checksum data found for [" + dataId + "]. Item does not exist in archive ["
                        + parent.getId() + "]");
            }

            if (StringUtils.isBlank(hash)) {
                errors.add("Hash value missing. [" + dataId + "]");
            } else if (!StringUtils.isAlphanumeric(hash)) {
                errors.add("Malformed hash value. [" + dataId + " -> " + hash + "]");
            }
        }

        attemptToRead(info, bsg, errors, warnings);
    }

    /**
     * <ul>
     *     <li>The {@link rosa.archive.model.BookStructure} is not required.</li>
     *     <li>The associated item in the archive must exist (Reduced Tagging).</li>
     *     <li>For each page in the structure, there must be an ID, name and front and back sides.</li>
     * </ul>
     *
     * @param structure item to check
     * @param parent parent container
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(BookStructure structure, Book parent, ByteStreamGroup bsg,
                               List<String> errors, List<String> warnings) {
        if (structure == null) {
            return;
        }

        if (!isInArchive(structure.getId(), parent.getContent())) {
            errors.add("Reduced tagging missing from archive. [" + structure.getId() + "]");
        }

        for (StructurePage page : structure) {
            if (StringUtils.isBlank(page.getId())) {
                errors.add("Page ID missing. [" + page.getId() + "]");
            }
            if (page.getVerso() != null) {
                check(page.getVerso(), errors, warnings);
                if (guessImageName(page.getId() + "v", parent) == null) {
                    errors.add("Could not find image associated with verso. ["
                            + parent.getId() + ":" + page.getVerso().getParentPage() + "]");
                }
            }
            if (page.getRecto() != null) {
                check(page.getRecto(), errors, warnings);
                if (guessImageName(page.getId() + "r", parent) == null) {
                    errors.add("Could not find image associated with recto. ["
                            + parent.getId() + ":" + page.getRecto().getParentPage() + "]");
                }
            }
        }

        attemptToRead(structure, bsg, errors, warnings);
    }

    /**
     * <ul>
     *     <li>The side of the page must exist.</li>
     *     <li>The side must have 0 or more columns. Each column must be checked.</li>
     *     <li>The side must have 0 or more items spanning the side.
     *         Each item spanning the side must be checked.</li>
     * </ul>
     *
     * @param side side of a page to check
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(StructurePageSide side, List<String> errors, List<String> warnings) {
        if (side == null) {
            return;
        }

        if (side.columns() != null) {
            for (StructureColumn column : side.columns()) {
                check(column, errors, warnings);
            }
        }
        if (side.spanning() != null) {
            for (Item item : side.spanning()) {
                check(item, errors, warnings);
            }
        }
    }

    /**
     * <ul>
     *     <li>The column must exist.</li>
     *     <li>The column must exist on a page.</li>
     *     <li>The column must have a single letter designation.</li>
     *     <li>Other information describing the column must be present,
     *         or be initialized to a reasonable default.</li>
     * </ul>
     *
     * @param column column of stuff on a page
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(StructureColumn column, List<String> errors, List<String> warnings) {
        if (column == null) {
            return;
        }

        if (StringUtils.isBlank(column.getParentSide())) {
            errors.add("Parent side of column missing. [" + column + "]");
        }
        if (column.getColumnLetter() == '\u0000') {
            errors.add("Column letter designation missing. [" + column + "]");
        }
        if (column.getTotalLines() < 0) {
            warnings.add("Total lines of column not defined. [" + column.getParentSide()
                    + " " + column.getColumnLetter() + "]");
        }
        if (column.getItems() != null) {
            for (Item item : column.getItems()) {
                check(item, errors, warnings);
            }
        }
    }

    /**
     * <ul>
     *     <li>The item must exist.</li>
     *     <li>The item must take up 0 or more lines.</li>
     * </ul>
     *
     * @param item item to check
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(Item item, List<String> errors, List<String> warnings) {
        if (item == null) {
            return;
        }

        if (item.getLines() < 0) {
            warnings.add("Lines is negative. [" + item + "]");
        }
    }

    /**
     * Not required.
     *
     * @param tagging item to check
     * @param parent containing Book
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(IllustrationTagging tagging, Book parent, ByteStreamGroup bsg,
                               List<String> errors, List<String> warnings) {
        if (tagging == null) {
            return;
        }

        if (!isInArchive(tagging.getId(), parent.getContent())) {
            errors.add("Image tagging missing from archive. [" + parent.getId() + "]");
        }

        for (Illustration illustration : tagging) {
            if (StringUtils.isBlank(illustration.getId())) {
                errors.add("Illustration ID missing. [" + illustration.getId() + "]");
            }
            if (StringUtils.isBlank(illustration.getPage())) {
                errors.add("Illustration page missing. [" + illustration.getId() + "]");
            } else if (!illustration.getPage().matches(PAGE_PATTERN) &&
                    !illustration.getPage().matches("\\w")) {
                warnings.add("Illustration page may be formatted incorrectly. [" + illustration.getPage() + "]");
            }
            if (illustration.getTitles() == null) {
                errors.add("Illustration title missing. [" + illustration.getId() + "]");
            } else {
                for (String id : illustration.getTitles()) {
                    if (StringUtils.isNotBlank(id) && !StringUtils.isNumeric(id)
                            && containsDigit(id)) {
                        warnings.add("Illustration ID may be incorrect. "
                                + "Illustration [" + illustration.getId() + "], "
                                + "[" + id + "]");
                    }
                    // Link to illustration_titles.csv in collection checked below
                }
            }
            if (illustration.getCharacters() == null) {
                errors.add("Illustration characters missing. [" + illustration.getId() + "]");
            } else {
                for (String id : illustration.getCharacters()) {
                    if (StringUtils.isNotBlank(id) && !StringUtils.isNumeric(id)
                            && containsDigit(id)) {
                        warnings.add("Illustration character ID may be incorrect. "
                                + "Illustration [" + illustration.getId() + "], "
                                + "character ID [" + id + "]");
                    }
                    // Link to character_names.csv in collection checked in the collection checker
                }
            }
        }

        attemptToRead(tagging, bsg, errors,warnings);
    }

    /**
     * Not required.
     *
     * @param tagging item to check
     * @param parent containing Book
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(NarrativeTagging tagging, Book parent, ByteStreamGroup bsg,
                               List<String> errors, List<String> warnings) {
        if (tagging == null) {
            return;
        }

        if (!isInArchive(tagging.getId(), parent.getContent())) {
            errors.add("Narrative tagging not found in archive. ["
                    + parent.getId() + ":" + tagging.getId() + "]");
        }

        for (BookScene scene : tagging) {
            // Link to narrative_sections.csv checked in collection checker
            if (guessImageName(scene.getStartPage(), parent) == null) {
                errors.add("Could not find start page of scene. ["
                        + scene.getId() + " : " + scene.getStartPage() + "]");
            }
            if (guessImageName(scene.getEndPage(), parent) == null) {
                errors.add("Could not find end page of scene. ["
                        + scene.getId() + " : " + scene.getEndPage() + "]");
            }
        }

        attemptToRead(tagging, bsg, errors, warnings);
    }

    /**
     * Check validity of AoR transcription data.
     * <ul>
     *     <li>XML transcriptions must validate against the schema</li>
     *     <li>XML transcription must be in the archive</li>
     *     <li>Page that the transcription refers to must be in archive</li>
     * </ul>
     *
     * @param collection parent collection
     * @param parent parent book
     * @param bsg byte stream group that holds these pages
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(BookCollection collection, Book parent, ByteStreamGroup bsg,
                       final List<String> errors, final List<String> warnings) {
        List<AnnotatedPage> pages = parent.getAnnotatedPages();
        if (pages == null || pages.isEmpty()) {
            return;
        }

        pages.forEach(page -> {
            if (!isInArchive(page.getId(), parent.getContent())) {
                errors.add("Annotated page not found in archive. [" + parent.getId() + ":" + page.getId() + "]");
            }

            if (!isInArchive(page.getPage(), parent.getContent())) {
                errors.add("Annotated page refers to a page not in archive. [" + page.getPage() + "]");
            }

            if (!bsg.hasByteStream(page.getId())) {
                errors.add("Cannot find file. [" + page.getId() + "]");
            } else {
                validateAgainstSchema(page.getId(), bsg, errors, warnings);
            }
        });

        AORChecker.ResultSet results = AORChecker.checkAORTranscriptions(collection, parent);
        errors.addAll(results.errors);
        warnings.addAll(results.warnings);
    }

    /**
     * Validate a particular XML item against a known schema.
     *
     * @param file name of item to validate
     * @param bsg byte stream group containing this item
     * @param errors list of errors
     * @param warnings list of warnings
     */
    public void validateAgainstSchema(final String file, ByteStreamGroup bsg, final List<String> errors,
                                      final List<String> warnings) {
        try {
            if (aorAnnotationSchema == null) {
                URL schemaUrl = new URL(annotationSchemaUrl);
                SchemaFactory schemaFactory =
                        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                aorAnnotationSchema = schemaFactory.newSchema(schemaUrl);
            }

            Validator validator = aorAnnotationSchema.newValidator();
            validator.setResourceResolver(resourceResolver);

            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) throws SAXException {
                    warnings.add("[Warn: " + file + "] (" + e.getLineNumber() + ":"
                            + e.getColumnNumber() + "): " + e.getMessage());
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    errors.add("[Error: " + file + "] (" + e.getLineNumber() + ":"
                            + e.getColumnNumber() + "): " + e.getMessage());
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    errors.add("[Fatal Error: " + file + "] (" + e.getLineNumber() + ":"
                            + e.getColumnNumber() + "): " + e.getMessage());
                }
            });

            Source source = new StreamSource(bsg.getByteStream(file));
            validator.validate(source);

        } catch (SAXException | IOException e) {
            errors.add("[" + file + "] failed to validate.\n" + stacktrace(e));
        }
    }

    /**
     * Validate
     *
     * @param transcription .
     * @param errors list of errors
     * @param warnings list of warnings
     */
    public void checkTranscription(Transcription transcription, final List<String> errors,
                                   final List<String> warnings) {

        if (transcription == null) {
            return;
        }

        validateTEI(
                transcription.getId(),
                new StreamSource(new StringReader(transcription.getXML())),
                errors,
                warnings
        );
    }

    public void checkTEIDescriptions(ByteStreamGroup bookGroup, final List<String> errors, final List<String> warnings) {
        try {
            for (String file : bookGroup.listByteStreamNames()) {
                if (file.contains("description_")) {
                    validateTEI(file, new StreamSource(bookGroup.getByteStream(file)), errors, warnings);
                }
            }
        } catch (IOException e) {
            errors.add("Failed to check TEI descriptions.");
        }
    }

    private void validateTEI(final String id, Source xmlSource, final List<String> errors, final List<String> warnings) {
        // Validate as TEI
        try {
            if (teiSchema == null) {
                URL schemaUrl = new URL(TEI_SCHEMA_URL);
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                teiSchema = schemaFactory.newSchema(schemaUrl);
            }

            Validator validator = teiSchema.newValidator();
            validator.setResourceResolver(resourceResolver);

            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) throws SAXException {
                    warnings.add("[Warn: " + id + "] (" + e.getLineNumber() + ":"
                            + e.getColumnNumber() + "): " + e.getMessage());
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    errors.add("[Error: " + id + "] (" + e.getLineNumber() + ":"
                            + e.getColumnNumber() + "): " + e.getMessage());
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    errors.add("[Fatal Error: " + id + "] (" + e.getLineNumber() + ":"
                            + e.getColumnNumber() + "): " + e.getMessage());
                }
            });

            validator.validate(xmlSource);

        } catch (SAXException | IOException e) {
            errors.add("[" + id + "] failed to validate.\n" + stacktrace(e));
        }
    }

    /**
     * Guess the name of the image associated with a name. TODO take out when image names are abstracted
     *
     * @param name name of item to check
     * @param book book archive
     * @return the name of the image or NULL if no image is found
     */
    private String guessImageName(String name, Book book) {
        name = name.trim();

        if (name.matches("\\d+")) {
            name += "r";
        }

        if (name.matches("\\d[rRvV]")) {
            name = "00" + name;
        } else if (name.matches("\\d\\d[rRvV]")) {
            name = "0" + name;
        }

        if (!name.endsWith(".tif")) {
            name += ".tif";
        }

        if (!name.startsWith(book.getId())) {
            name = book.getId() + "." + name;
        }

        for (String itemName : book.getContent()) {
            if (itemName.equalsIgnoreCase(name)) {
                return itemName;
            }
        }
        
        return null;
    }

    /**
     *
     * @param tagging illustration tagging
     * @param names character names
     * @param titles illustration titles
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(IllustrationTagging tagging, CharacterNames names, IllustrationTitles titles,
                               List<String> errors, List<String> warnings) {
        if (tagging == null) {
            return;
        }

        // Checking CharacterNames and IllustrationTitles, referenced in image tagging
        for (Illustration ill : tagging) {
            // Check character_names references in imagetag
            if (names != null) {
                List<String> characters = Arrays.asList(ill.getCharacters());
                for (String character : characters) {
                        if (StringUtils.isNumeric(character) && !names.hasCharacter(character)) {
                            errors.add("Character ID [" + character + "] in illustration [" +
                                    ill.getId() + "] missing.");
                        }
                }
            }

            // Check illustration_titles references in imagetag
            if (titles != null) {
                List<String> t = Arrays.asList(ill.getTitles());
                for (String title : t) {
                    if (StringUtils.isNumeric(title) && !titles.hasTitle(title)) {
                        errors.add("Title [" + title + "] in illustration [" + ill.getId() + "] missing.");
                    }
                }
            }
        }
    }

    /**
     * Make sure the IDs in a book's narrative tagging exist in the collections narrative sections.
     *
     * @param sections narrative sections to check
     * @param tagging image tagging to check against
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(NarrativeTagging tagging, NarrativeSections sections,
                               List<String> errors, List<String> warnings) {
        if (tagging == null || sections == null) {
            return;
        }

        for (BookScene scene : tagging) {
            if (sections.findIndexOfSceneById(scene.getId()) < 0) {
                errors.add("Narrative tagging scene [" + scene.getId()
                        + "] not found in narrative_sections.");
            }
        }
    }

    /**
     * @param s a string
     * @return does the string contain any numbers?
     */
    public final boolean containsDigit(String s){
        boolean containsDigit = false;

        if(s != null && !s.isEmpty()){
            for(char c : s.toCharArray()){
                if(containsDigit = Character.isDigit(c)){
                    break;
                }
            }
        }

        return containsDigit;
    }
}
