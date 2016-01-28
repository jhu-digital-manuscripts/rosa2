package rosa.website.search;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import rosa.archive.core.util.TranscriptionSplitter;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookDescription;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookScene;
import rosa.archive.model.BookStructure;
import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeScene;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;
import rosa.archive.model.redtag.Item;
import rosa.archive.model.redtag.Rubric;
import rosa.archive.model.redtag.StructureColumn;
import rosa.archive.model.redtag.StructurePage;
import rosa.archive.model.redtag.StructurePageSide;
import rosa.search.core.BaseLuceneMapper;
import rosa.search.core.SearchUtil;
import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;
import rosa.website.search.client.model.WebsiteSearchFields;

public class WebsiteLuceneMapper extends BaseLuceneMapper {
    private static final Logger logger = Logger.getLogger(WebsiteLuceneMapper.class.toString());

    private TranscriptionXMLReader transcriptionXMLReader;
    
    public WebsiteLuceneMapper() {
        super(WebsiteSearchFields.values());
        
        this.transcriptionXMLReader = new TranscriptionXMLReader();
    }
    
    @Override
    public SearchField getIdentifierSearchField() {
        return WebsiteSearchFields.ID;
    }
    
    /**
     * Create and index Lucene documents for a given book within a book
     * collection.
     *
     * @param col BookCollection object
     * @param book Book object
     * @return list of documents representing the book
     * @throws IOException
     */
    public List<Document> createDocuments(BookCollection col, Book book)
            throws IOException {
        List<Document> result = new ArrayList<>();

        // Create document for book

        {
            Document doc = new Document();

            index(doc, col, book);

            result.add(doc);
        }

        // Create document for each image
        Map<String, String> transcriptionMap = TranscriptionSplitter.split(book.getTranscription());

        ImageList images = book.getImages();
        if (images.getImages() == null) {
            logger.warning("No image list found. [" + col.getId() + ":" + book.getId() + "]");
        } else {
            for (BookImage image : images.getImages()) {
                Document doc = new Document();

                // Index Rose transcriptions
                index(doc, col, book, image,
                        transcriptionMap != null ? transcriptionMap.get(getStandardPage(image)) : null);

                result.add(doc);
            }
        }

        return result;
    }

    // TODO need better way of getting standard name... refer to how it is done in the transcription splitter
    private String getStandardPage(BookImage image) {
        String start = image.getName();
        if (start.length() == 2) {
            return "00" + start;
        } else if (start.length() == 3) {
            return "0" + start;
        } else {
            return start;
        }
    }

    /**
     * Parse an XML document and return all textual content in a String.
     *
     * @param src source XML
     * @return String of all textual content
     * @throws SAXException
     * @throws IOException
     */
    private static String xml_to_text(InputSource src) throws SAXException, IOException {
        XMLReader r = XMLReaderFactory.createXMLReader();
        final StringBuilder result = new StringBuilder();

        r.setContentHandler(new DefaultHandler() {
            public void characters(char[] text, int offset, int len)
                    throws SAXException {
                result.append(text, offset, len);
            }
        });

        r.parse(src);

        return result.toString();
    }

    private void index(Document doc, BookCollection col, Book book)
            throws IOException {
        addField(doc, WebsiteSearchFields.ID,
                SearchUtil.createId(col.getId(), book.getId()));
        addField(doc, WebsiteSearchFields.COLLECTION_ID, col.getId());
        addField(doc, WebsiteSearchFields.BOOK_ID, book.getId());

        try {
            for (String lc: col.getAllSupportedLanguages()) {

                BookDescription desc = book.getBookDescription(lc);

                if (desc != null) {
                    SearchFieldType type = getSearchFieldTypeForLang(lc);

                    addField(
                            doc,
                            WebsiteSearchFields.DESCRIPTION_TEXT,
                            type,
                            xml_to_text(new InputSource(new StringReader(desc.getXML())))
                    );
                }
            }
        } catch (SAXException e) {
            throw new IOException("Failure to parse xml description of "
                    + book.getId(), e);
        }
    }

    /**
     * Index all information relevant for one page in the book.
     *
     * The information indexed here includes:
     * <ul>
     * <li>image ID</li>
     * <li>image short name</li>
     * <li>descriptions of any illustrations that appear on page</li>
     * <li>character names of those characters that appear in the text on page</li>
     * <li>narrative sections</li>
     * <li>transcription text for this page</li>
     * </ul>
     *
     * @param doc Lucene document
     * @param col BookCollection obj
     * @param book Book obj
     * @param image this image
     * @param transcriptionFragment XML fragment
     */
    private void index(Document doc, BookCollection col, Book book, BookImage image,
                       String transcriptionFragment) {
        addField(doc, WebsiteSearchFields.ID,
                SearchUtil.createId(col.getId(), book.getId(), image.getId()));
        addField(doc, WebsiteSearchFields.COLLECTION_ID, col.getId());
        addField(doc, WebsiteSearchFields.BOOK_ID, book.getId());
        addField(doc, WebsiteSearchFields.IMAGE_NAME, image.getName());

        IllustrationTagging imgtag = book.getIllustrationTagging();

        IllustrationTitles titles = col.getIllustrationTitles();
        CharacterNames char_names = col.getCharacterNames();

        if (imgtag != null) {
            StringBuilder char_field = new StringBuilder();
            StringBuilder title_field = new StringBuilder();
            StringBuilder keyword_field = new StringBuilder();

            for (int index: imgtag.findImageIndices(book, image.getId())) {
                Illustration illus = imgtag.getIllustrationData(index);

                for (String char_id: illus.getCharacters()) {
                    CharacterName char_name = char_names
                            .getCharacterName(char_id);

                    if (char_name == null) {
                        char_field.append(char_id);
                        char_field.append(", ");
                    } else {
                        for (String name: char_name.getAllNames()) {
                            char_field.append(name);
                            char_field.append(", ");
                        }
                    }
                }

                for (String title_id: illus.getTitles()) {
                    String title = titles.getTitleById(title_id);

                    if (title != null && !title.isEmpty()) {
                        title_field.append(title);
                        title_field.append(", ");
                    } else {
                        title_field.append(title_id);
                        title_field.append(", ");
                    }
                }

                keyword_field.append(illus.getTextualElement());
                keyword_field.append(", ");
                keyword_field.append(illus.getArchitecture());
                keyword_field.append(", ");
                keyword_field.append(illus.getCostume());
                keyword_field.append(", ");
                keyword_field.append(illus.getOther());
                keyword_field.append(", ");
                keyword_field.append(illus.getObject());
                keyword_field.append(", ");
                keyword_field.append(illus.getLandscape());
                keyword_field.append(", ");
            }

            if (char_field.length() > 0) {
                addField(doc, WebsiteSearchFields.ILLUSTRATION_CHAR,
                        char_field.toString());
            }

            if (title_field.length() > 0) {
                addField(doc, WebsiteSearchFields.ILLUSTRATION_TITLE,
                        title_field.toString());
            }

            if (keyword_field.length() > 0) {
                addField(doc, WebsiteSearchFields.ILLUSTRATION_KEYWORD,
                        keyword_field.toString());
            }
        }

        // Index narrative scenes which start in image

        NarrativeTagging nartag = book.getAutomaticNarrativeTagging();

        if (nartag == null) {
            nartag = book.getManualNarrativeTagging();
        }

        NarrativeSections narsecs = col.getNarrativeSections();

        if (nartag != null) {
            StringBuilder trans_field = new StringBuilder();
            StringBuilder sectionids_field = new StringBuilder();
            StringBuilder sectiondesc_field = new StringBuilder();

            for (BookScene scene: nartag.getScenes()) {
                if (book.guessImageName(scene.getStartPage()).equals(
                        image.getId())) {

                    if (scene.getStartTranscription() != null) {
                        trans_field.append(scene.getStartTranscription());
                        trans_field.append(", ");
                    }

                    sectionids_field.append(scene.getId());
                    sectionids_field.append(" ");

                    int secindex = narsecs.findIndexOfSceneById(scene.getId());

                    if (secindex != -1) {
                        NarrativeScene ns = narsecs.asScenes().get(secindex);
                        sectiondesc_field.append(ns.getDescription());
                        sectiondesc_field.append(", ");
                    }
                }
            }

            if (trans_field.length() > 0) {
                addField(doc, WebsiteSearchFields.TRANSCRIPTION_TEXT,
                        trans_field.toString());
            }

            if (sectionids_field.length() > 0) {
                addField(doc, WebsiteSearchFields.NARRATIVE_SECTION_ID,
                        sectionids_field.toString());
            }

            if (sectiondesc_field.length() > 0) {
                addField(doc, WebsiteSearchFields.NARRATIVE_SECTION_DESCRIPTION,
                        sectionids_field.toString());
            }
        }

        // Index rubrics from reduced tagging

        StructurePageSide side = findReducedTaggingSide(book, image.getId());

        if (side != null) {
            StringBuilder rubrics = new StringBuilder();

            for (StructureColumn c: side.columns()) {
                for (Item item: c.getItems()) {
                    if (item instanceof Rubric) {
                        String rubric = ((Rubric) item).getText();

                        // Remove / used to indicate abbreviations
                        rubric = rubric.replaceAll("/", "");

                        rubrics.append(rubric);
                        rubrics.append(", ");
                    }
                }
            }

            if (rubrics.length() > 0) {
                addField(doc, WebsiteSearchFields.TRANSCRIPTION_RUBRIC,
                        rubrics.toString());
            }
        }

        // Index transcription text that appears on this page
        if (transcriptionFragment != null) {
            try {
                indexTranscriptionFragment(transcriptionFragment, doc);
            } catch (SAXException | IOException e) {
                logger.log(Level.SEVERE, "Failed to parse transcription fragment. ["
                        + image.getName() + "]", e);
            }
        }
    }

    // Find the page side from the reduced tagging corresponding to an image id
    private StructurePageSide findReducedTaggingSide(Book book, String image_id) {
        BookStructure struct = book.getBookStructure();

        if (struct == null) {
            return null;
        }

        for (StructurePage page: struct.pages()) {
            String test = book.guessImageName(page.getId() + "r");

            if (test != null && image_id.equals(test)) {
                return page.getRecto();
            }

            test = book.guessImageName(page.getId() + "v");

            if (test != null && image_id.equals(test)) {
                return page.getVerso();
            }
        }

        return null;
    }

    private void indexTranscriptionFragment(String transcription, Document doc) throws SAXException, IOException {
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();

        transcriptionXMLReader.clear();
        xmlReader.setContentHandler(transcriptionXMLReader);

        xmlReader.parse(new InputSource(new StringReader(transcription)));

        addField(doc, WebsiteSearchFields.TRANSCRIPTION_TEXT, transcriptionXMLReader.getPoetry());

        if (transcriptionXMLReader.hasCatchphrase()) {
            addField(doc, WebsiteSearchFields.TRANSCRIPTION_TEXT, transcriptionXMLReader.getCatchphrase());
        }
        if (transcriptionXMLReader.hasRubric()) {
            addField(doc, WebsiteSearchFields.TRANSCRIPTION_RUBRIC, transcriptionXMLReader.getRubric());
        }
        if (transcriptionXMLReader.hasIllus()) {
            addField(doc, WebsiteSearchFields.TRANSCRIPTION_TEXT, transcriptionXMLReader.getIllustration());
        }

        addField(doc, WebsiteSearchFields.TRANSCRIPTION_LECOY, transcriptionXMLReader.getLecoy());
        addField(doc, WebsiteSearchFields.TRANSCRIPTION_TEXT, transcriptionXMLReader.getLine());

        if (transcriptionXMLReader.hasNote()) {
            addField(doc, WebsiteSearchFields.TRANSCRIPTION_NOTE, transcriptionXMLReader.getNote());
        }
    }
}
