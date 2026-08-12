package rosa.archive.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rosa.archive.model.BiblioData;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookDescription;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.ObjectRef;
import rosa.archive.model.ReferenceSheet;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Calculation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.GraphNode;
import rosa.archive.model.aor.GraphNote;
import rosa.archive.model.aor.GraphText;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Table;
import rosa.archive.model.aor.TableCell;
import rosa.archive.model.aor.TextEl;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;
import rosa.archive.model.aor.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpensearchIngestGenerator}.
 */
class OpensearchIngestGeneratorTest {

    private OpensearchIngestGenerator generator;
    private BookCollection collection;
    private Book book;
    private BookImage image;

    @BeforeEach
    void setUp() {
        generator = new OpensearchIngestGenerator();

        collection = new BookCollection();
        collection.setId("testcol");

        book = new Book();
        book.setId("testbook");

        BookMetadata metadata = new BookMetadata();
        Map<String, BiblioData> biblioMap = new HashMap<>();
        BiblioData biblio = new BiblioData();
        biblio.setCommonName("Test Book");
        biblio.setRepository("Test Library");
        biblio.setDateLabel("1520");
        biblio.setOrigin("London");
        biblio.setType("printed");
        biblio.setCurrentLocation("London, UK");
        biblio.setAuthors(new ObjectRef[]{new ObjectRef("Author One", "author1")});
        biblio.setReaders(new ObjectRef[]{new ObjectRef("Gabriel Harvey", "harvey")});
        biblioMap.put("en", biblio);
        metadata.setBiblioDataMap(biblioMap);
        metadata.setNumberOfPages(100);
        metadata.setNumberOfIllustrations(5);
        book.setBookMetadata(metadata);

        image = new BookImage();
        image.setId("testbook.001r.tif");
        image.setName("001r");
        image.setMissing(false);
        image.setLocation(BookImageLocation.BODY_MATTER);
    }

    /** Generate canvas doc with current book/collection/image setup. */
    private ObjectNode generateCanvas() {
        return generator.generateCanvasDocument(collection, book, image, 0, Collections.emptyMap());
    }

    /** Set up an AnnotatedPage for the test image with a reader. */
    private AnnotatedPage setUpAnnotatedPage(String reader) {
        AnnotatedPage ap = new AnnotatedPage();
        ap.setPage(image.getId());
        ap.setReader(reader);
        book.setAnnotatedPages(List.of(ap));
        return ap;
    }

    private List<String> jsonArrayToList(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode n : arrayNode) {
                result.add(n.asText());
            }
        }
        return result;
    }

    @Nested
    @DisplayName("stripTranscriberMarks")
    class StripTranscriberMarksTest {
        @Test
        void removesSquareBrackets() {
            assertEquals("hello world", OpensearchIngestGenerator.stripTranscriberMarks("[hello] world"));
        }

        @Test
        void returnsNullForNull() {
            assertNull(OpensearchIngestGenerator.stripTranscriberMarks(null));
        }

        @Test
        void returnsUnchangedWhenNoBrackets() {
            assertEquals("simple text", OpensearchIngestGenerator.stripTranscriberMarks("simple text"));
        }
    }

    @Nested
    @DisplayName("Canvas Document - Structural")
    class CanvasStructuralTest {
        @Test
        void usesImageNameByDefault() {
            ObjectNode doc = generateCanvas();
            assertEquals("001r", doc.get("label").asText());
        }

        @Test
        void prefersPaginationFromAnnotatedPage() {
            AnnotatedPage ap = new AnnotatedPage();
            ap.setPage(image.getId());
            ap.setPagination("p. 42");
            book.setAnnotatedPages(List.of(ap));

            ObjectNode doc = generateCanvas();
            assertEquals("p. 42", doc.get("label").asText());
        }

        @Test
        void usesSignatureWhenNoPagination() {
            AnnotatedPage ap = new AnnotatedPage();
            ap.setPage(image.getId());
            ap.setSignature("A1");
            book.setAnnotatedPages(List.of(ap));

            ObjectNode doc = generateCanvas();
            assertEquals("A1", doc.get("label").asText());
        }

        @Test
        void collectionIdIsArray() {
            ObjectNode doc = generateCanvas();
            assertTrue(doc.get("collection_id").isArray());
            assertEquals("testcol", doc.get("collection_id").get(0).asText());
        }

        @Test
        void iiifImageIdIsPopulated() {
            ObjectNode doc = generateCanvas();
            assertEquals("testcol/testbook/testbook.001r", doc.get("iiif_image_id").asText());
        }
    }

    @Nested
    @DisplayName("Manifest Document")
    class ManifestDocumentTest {
        @Test
        void includesLabel() {
            ObjectNode doc = generator.generateManifestDocument(collection, book);
            assertEquals("Test Book", doc.get("label").asText());
        }

        @Test
        void titleIncludesBookTextTitles() {
            BookText bt = new BookText();
            bt.setTitle("De Rhetorica");
            book.getBookMetadata().setBookTexts(List.of(bt));

            ObjectNode doc = generator.generateManifestDocument(collection, book);
            String title = doc.get("title").asText();
            assertTrue(title.contains("Test Book"));
            assertTrue(title.contains("De Rhetorica"));
        }

        @Test
        void includesBookTextAuthors() {
            BookText bt = new BookText();
            bt.addAuthor("Cicero");
            book.getBookMetadata().setBookTexts(List.of(bt));

            ObjectNode doc = generator.generateManifestDocument(collection, book);
            JsonNode authors = doc.get("authors");
            assertNotNull(authors);
            boolean found = false;
            for (JsonNode a : authors) {
                if (a.asText().equals("Cicero")) { found = true; break; }
            }
            assertTrue(found);
        }

        @Test
        void usesNumIllustrations() {
            ObjectNode doc = generator.generateManifestDocument(collection, book);
            assertEquals(5, doc.get("num_illustrations").asInt());
            assertFalse(doc.has("number_of_illustrations"));
        }

        @Test
        void collectionIdIsArray() {
            ObjectNode doc = generator.generateManifestDocument(collection, book);
            assertTrue(doc.get("collection_id").isArray());
            assertEquals("testcol", doc.get("collection_id").get(0).asText());
        }

        @Test
        void logoDefaultsToCollectionJpg() {
            ObjectNode doc = generator.generateManifestDocument(collection, book);
            assertEquals("testcol.jpg", doc.get("logo").asText());
        }

        @Test
        void logoForAorUsesFirstReader() {
            collection.setId("aor");
            ObjectNode doc = generator.generateManifestDocument(collection, book);
            assertEquals("gabriel_harvey.jpg", doc.get("logo").asText());
        }

        @Test
        void thumbnailSelectsNonMissingBodyMatterPages() {
            ImageList imageList = new ImageList();
            BookImage frontMatter = new BookImage("front.tif", 100, 100, false);
            frontMatter.setLocation(BookImageLocation.FRONT_MATTER);
            BookImage missing = new BookImage("miss.tif", 100, 100, true);
            missing.setLocation(BookImageLocation.BODY_MATTER);
            BookImage body1 = new BookImage("body1.tif", 100, 100, false);
            body1.setLocation(BookImageLocation.BODY_MATTER);
            BookImage body2 = new BookImage("body2.tif", 100, 100, false);
            body2.setLocation(BookImageLocation.BODY_MATTER);
            imageList.setImages(List.of(frontMatter, missing, body1, body2));
            book.setImages(imageList);

            ObjectNode doc = generator.generateManifestDocument(collection, book);
            JsonNode thumbnail = doc.get("thumbnail");
            assertNotNull(thumbnail);
            assertTrue(thumbnail.isArray());
            assertEquals(2, thumbnail.size());
            // Each entry is an object with iiif_image_id and page_num
            assertEquals("testcol/testbook/body1", thumbnail.get(0).get("iiif_image_id").asText());
            assertEquals(2, thumbnail.get(0).get("page_num").asInt());
            assertEquals("testcol/testbook/body2", thumbnail.get(1).get("iiif_image_id").asText());
            assertEquals(3, thumbnail.get(1).get("page_num").asInt());
        }

        @Test
        void noTitlesField() {
            ObjectNode doc = generator.generateManifestDocument(collection, book);
            assertFalse(doc.has("titles"));
        }

        @Test
        void descriptionIsEmptyByDefault() {
            ObjectNode doc = generator.generateManifestDocument(collection, book);
            assertEquals("", doc.get("description").asText());
        }

        @Test
        void descriptionUsesEnglishBookDescription() {
            BookDescription desc = new BookDescription();
            desc.addNote("IDENTIFICATION", "MS Test 123, Test Library");
            desc.addNote("MATERIAL", "Parchment in good condition");
            book.addDescription(desc, "en");

            ObjectNode doc = generator.generateManifestDocument(collection, book);
            String description = doc.get("description").asText();
            assertFalse(description.isEmpty());
            assertTrue(description.contains("MS Test 123"));
            assertTrue(description.contains("Parchment"));
        }

        @Test
        void descriptionIgnoresFrenchIfEnglishAvailable() {
            BookDescription enDesc = new BookDescription();
            enDesc.addNote("IDENTIFICATION", "English description");
            book.addDescription(enDesc, "en");

            BookDescription frDesc = new BookDescription();
            frDesc.addNote("IDENTIFICATION", "French description");
            book.addDescription(frDesc, "fr");

            ObjectNode doc = generator.generateManifestDocument(collection, book);
            String description = doc.get("description").asText();
            assertTrue(description.contains("English description"));
            assertFalse(description.contains("French description"));
        }

        @Test
        void descriptionIsEmptyWhenOnlyFrenchAvailable() {
            BookDescription frDesc = new BookDescription();
            frDesc.addNote("IDENTIFICATION", "French description");
            book.addDescription(frDesc, "fr");

            ObjectNode doc = generator.generateManifestDocument(collection, book);
            assertEquals("", doc.get("description").asText());
        }
    }

    @Nested
    @DisplayName("Mark Annotation")
    class MarkAnnotationTest {
        @Test
        void storesMarkNameInKeywordSubField() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            ap.setMarks(List.of(new Mark("mark1", "[hello]", "plus_sign", "pen", "la", Location.HEAD)));

            ObjectNode doc = generateCanvas();
            assertEquals("plus_sign", doc.get("mark.keyword").asText());
        }

        @Test
        void stripsTranscriberMarksFromReferencedText() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            ap.setMarks(List.of(new Mark("mark1", "[important] text", "dash", "pen", "en", Location.HEAD)));

            ObjectNode doc = generateCanvas();
            assertEquals("important text", doc.get("mark.en").asText());
        }

        @Test
        void methodAddedToKeywordArray() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            ap.setMarks(List.of(new Mark("mark1", "text", "dash", "pen", "en", Location.HEAD)));

            ObjectNode doc = generateCanvas();
            List<String> methods = jsonArrayToList(doc.get("method"));
            assertTrue(methods.contains("pen"));
        }
    }

    @Nested
    @DisplayName("Numeral Annotation")
    class NumeralAnnotationTest {
        @Test
        void indexesBothReferencedTextAndNumeralValue() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            ap.setNumerals(List.of(new Numeral("num1", "[page] reference", "42", "en", Location.HEAD)));

            ObjectNode doc = generateCanvas();
            String text = doc.get("numeral.en").asText();
            assertTrue(text.contains("page reference"));
            assertTrue(text.contains("42"));
        }
    }

    @Nested
    @DisplayName("Underline Annotation")
    class UnderlineAnnotationTest {
        @Test
        void stripsTranscriberMarks() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            ap.setUnderlines(List.of(new Underline("ul1", "[underlined] text", "pen", null, "la", Location.HEAD)));

            ObjectNode doc = generateCanvas();
            assertEquals("underlined text", doc.get("underline.la").asText());
        }
    }

    @Nested
    @DisplayName("Errata Annotation")
    class ErrataAnnotationTest {
        @Test
        void indexesBothReferencedAndAmendedText() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            ap.setErrata(List.of(new Errata("err1", "la", "[wrong] word", "correct word")));

            ObjectNode doc = generateCanvas();
            String text = doc.get("errata.la").asText();
            assertTrue(text.contains("wrong word"));
            assertTrue(text.contains("correct word"));
        }
    }

    @Nested
    @DisplayName("Symbol Annotation")
    class SymbolAnnotationTest {
        @Test
        void indexesSymbolNameAndReferencedText() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            ap.setSymbols(List.of(new Symbol("sym1", "[noted] passage", "SS", "la", Location.HEAD)));

            ObjectNode doc = generateCanvas();
            assertEquals("noted passage", doc.get("symbol.la").asText());
            assertEquals("SS", doc.get("symbol.keyword").asText());
            List<String> symbols = jsonArrayToList(doc.get("symbols"));
            assertTrue(symbols.contains("SS"));
        }
    }

    @Nested
    @DisplayName("Drawing Annotation")
    class DrawingAnnotationTest {
        @Test
        void indexesReferencedTextAndHand() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Drawing drawing = new Drawing("draw1", "[figure] here", Location.HEAD, "diagram", "pen", "en");
            TextEl textEl = new TextEl("Harvey", "en", "anchor text", "drawn text");
            drawing.setTexts(List.of(textEl));
            ap.setDrawings(List.of(drawing));

            ObjectNode doc = generateCanvas();
            assertTrue(doc.has("drawing.en"));
            List<String> hands = jsonArrayToList(doc.get("hand"));
            assertTrue(hands.contains("Harvey"));
        }

        @Test
        void indexesPeopleWithAlternates() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Drawing drawing = new Drawing("draw1", null, Location.HEAD, "diagram", null, "en");
            drawing.setPeople(List.of("Aristotle"));
            ap.setDrawings(List.of(drawing));

            ReferenceSheet peopleRef = new ReferenceSheet();
            peopleRef.addValues("Aristotle", "Aristotle", "Aristoteles", "The Philosopher");
            collection.setPeopleRef(peopleRef);

            ObjectNode doc = generateCanvas();
            List<String> people = jsonArrayToList(doc.get("people"));
            assertTrue(people.contains("Aristotle"));
            assertTrue(people.contains("Aristoteles"));
            assertTrue(people.contains("The Philosopher"));
        }

        @Test
        void indexesDrawingTypeInKeyword() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Drawing drawing = new Drawing("draw1", null, Location.HEAD, "manicule", null, "en");
            ap.setDrawings(List.of(drawing));

            ObjectNode doc = generateCanvas();
            assertEquals("manicule", doc.get("drawing.keyword").asText());
        }
    }

    @Nested
    @DisplayName("Calculation Annotation")
    class CalculationAnnotationTest {
        @Test
        void indexesTypeMethodDataAndContent() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Calculation calc = new Calculation("calc1", "arithmetic", 0, Location.HEAD, "pen", null);
            calc.addData("3 + 4");
            calc.addData("= 7");
            calc.setContent("simple addition");
            ap.setCalculations(List.of(calc));

            ObjectNode doc = generateCanvas();
            assertEquals("arithmetic", doc.get("calculation.keyword").asText());
            List<String> methods = jsonArrayToList(doc.get("method"));
            assertTrue(methods.contains("pen"));
            String text = doc.get("calculation.en").asText();
            assertTrue(text.contains("3 + 4"));
            assertTrue(text.contains("= 7"));
            assertTrue(text.contains("simple addition"));
        }
    }

    @Nested
    @DisplayName("Graph Annotation")
    class GraphAnnotationTest {
        @Test
        void indexesNodeTextAndPerson() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Graph graph = new Graph("graph1", "genealogy", 0, Location.HEAD, "pen");
            graph.setLanguage("en");
            GraphNode node1 = new GraphNode("n1", "Aristotle", "Greek philosopher", "content here");
            GraphNode node2 = new GraphNode("n2", "Plato", "Teacher of Aristotle", null);
            graph.addNode(node1);
            graph.addNode(node2);
            ap.setGraphs(List.of(graph));

            ObjectNode doc = generateCanvas();
            assertTrue(doc.has("graph.en"));
            String text = doc.get("graph.en").asText();
            assertTrue(text.contains("Greek philosopher"));
            assertTrue(text.contains("content here"));
            List<String> people = jsonArrayToList(doc.get("people"));
            assertTrue(people.contains("Aristotle"));
            assertTrue(people.contains("Plato"));
        }

        @Test
        void indexesGraphTextHandAndTranslations() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Graph graph = new Graph("graph1", "table", 0, Location.HEAD, null);
            graph.setLanguage("la");
            GraphText gt = new GraphText();
            gt.addNote(new GraphNote("note1", "Harvey", "la", null, "anchor", "content"));
            gt.addTranslation("This is the English translation");
            graph.addGraphText(gt);
            ap.setGraphs(List.of(graph));

            ObjectNode doc = generateCanvas();
            List<String> hands = jsonArrayToList(doc.get("hand"));
            assertTrue(hands.contains("Harvey"));
            assertTrue(doc.has("graph.en"));
            assertTrue(doc.get("graph.en").asText().contains("This is the English translation"));
        }
    }

    @Nested
    @DisplayName("Table Annotation")
    class TableAnnotationTest {
        @Test
        void indexesCellDataAndTexts() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Table table = new Table("table1", Location.HEAD);
            table.setLanguage("la");
            table.setTranslation("English translation of table");
            table.setAggregatedInfo("Summary info");
            TextEl textEl = new TextEl("Harvey", "la", "anchor", "written text");
            table.setTexts(List.of(textEl));
            TableCell cell = new TableCell(0, 0, "cellAnchor", "cellData", "cellContent");
            table.setCells(List.of(cell));
            table.setPeople(List.of("Cicero"));
            ap.setTables(List.of(table));

            ObjectNode doc = generateCanvas();

            assertTrue(doc.has("table.la"));
            assertTrue(doc.has("table.en"));
            String enText = doc.get("table.en").asText();
            assertTrue(enText.contains("cellData"));
            assertTrue(enText.contains("cellAnchor"));
            assertTrue(enText.contains("cellContent"));
            assertTrue(enText.contains("Summary info"));
            assertTrue(doc.has("translation.en"));
            assertTrue(doc.get("translation.en").asText().contains("English translation of table"));
            assertTrue(doc.has("people"));
            List<String> hands = jsonArrayToList(doc.get("hand"));
            assertTrue(hands.contains("Harvey"));
        }
    }

    @Nested
    @DisplayName("Marginalia Annotation")
    class MarginaliaAnnotationTest {
        @Test
        void indexesAnchorTextToAnchorTextField() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Marginalia marg = createSimpleMarginalia("la", null);
            marg.setReferencedText("some anchor text");
            ap.setMarginalia(List.of(marg));

            ObjectNode doc = generateCanvas();
            // anchor_text should go to anchor_text field, NOT marginalia
            assertTrue(doc.has("anchor_text.en") || doc.has("anchor_text.la") || doc.has("anchor_text.it"),
                    "anchor_text should be in anchor_text field: " + doc);
            assertFalse(doc.has("marginalia.la") && doc.get("marginalia.la").asText().contains("some anchor text"),
                    "anchor_text should NOT be in marginalia field");
            assertFalse(doc.has("marginalia.en") && doc.get("marginalia.en").asText().contains("some anchor text"),
                    "anchor_text should NOT be in marginalia.en field");
        }

        @Test
        void indexesMarginaliaTextFromPositionToCorrectLanguage() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");

            // Create a marginalia with EN language section containing text
            Marginalia marg = new Marginalia();
            marg.setId("marg1");
            MarginaliaLanguage ml = new MarginaliaLanguage();
            ml.setLang("en");
            Position pos = new Position();
            pos.setTexts(List.of("A boy at helme."));
            ml.setPositions(List.of(pos));
            marg.setLanguages(List.of(ml));
            ap.setMarginalia(List.of(marg));

            ObjectNode doc = generateCanvas();
            assertTrue(doc.has("marginalia.en"), "EN text should go to marginalia.en: " + doc);
            assertTrue(doc.get("marginalia.en").asText().contains("A boy at helme."),
                    "marginalia.en should contain the EN text");
        }

        @Test
        void multiLanguageMarginaliaRoutedToCorrectFields() {
            // Simulates BL615d7.067v.xml structure with 4 marginalia in different languages
            AnnotatedPage ap = setUpAnnotatedPage("John Dee");

            // Add underlines with Italian language (as in the real page) to establish defaultLang
            Underline ul = new Underline(null, "Punta Santa ben tre leghe", "pen", "straight", "IT", null);
            ap.setUnderlines(List.of(ul));

            // Marginalia 1: LA → "Nota." + translation "Note."
            Marginalia marg1 = new Marginalia();
            marg1.setId("marg1");
            MarginaliaLanguage ml1 = new MarginaliaLanguage();
            ml1.setLang("la");
            Position pos1 = new Position();
            pos1.setTexts(List.of("Nota."));
            ml1.setPositions(List.of(pos1));
            marg1.setLanguages(List.of(ml1));
            marg1.setTranslation("Note.");

            // Marginalia 2: EN → "A boy at helme." with anchor_text
            Marginalia marg2 = new Marginalia();
            marg2.setId("marg2");
            marg2.setReferencedText("lasciando il temone in gouerno di vn garzone");
            MarginaliaLanguage ml2 = new MarginaliaLanguage();
            ml2.setLang("en");
            Position pos2 = new Position();
            pos2.setTexts(List.of("A boy at helme."));
            ml2.setPositions(List.of(pos2));
            marg2.setLanguages(List.of(ml2));

            // Marginalia 3: IT → "Il Patron della nave." with anchor_text + translation
            Marginalia marg3 = new Marginalia();
            marg3.setId("marg3");
            marg3.setReferencedText("il patron della naue");
            MarginaliaLanguage ml3 = new MarginaliaLanguage();
            ml3.setLang("it");
            Position pos3 = new Position();
            pos3.setTexts(List.of("Il Patron della nave."));
            ml3.setPositions(List.of(pos3));
            marg3.setLanguages(List.of(ml3));
            marg3.setTranslation("The patron of the ship.");

            // Marginalia 4: EN → "Note: mistaking"
            Marginalia marg4 = new Marginalia();
            marg4.setId("marg4");
            MarginaliaLanguage ml4 = new MarginaliaLanguage();
            ml4.setLang("en");
            Position pos4 = new Position();
            pos4.setTexts(List.of("Note: mistaking"));
            ml4.setPositions(List.of(pos4));
            marg4.setLanguages(List.of(ml4));

            ap.setMarginalia(List.of(marg1, marg2, marg3, marg4));

            ObjectNode doc = generateCanvas();

            // LA marginalia text should be in marginalia.la
            assertTrue(doc.has("marginalia.la"), "Should have marginalia.la: " + doc);
            assertTrue(doc.get("marginalia.la").asText().contains("Nota."),
                    "marginalia.la should contain 'Nota.'");

            // EN marginalia text + translations should be in marginalia.en
            assertTrue(doc.has("marginalia.en"), "Should have marginalia.en: " + doc);
            String enText = doc.get("marginalia.en").asText();
            assertTrue(enText.contains("A boy at helme."),
                    "marginalia.en should contain EN text 'A boy at helme.': " + enText);
            assertTrue(enText.contains("Note: mistaking"),
                    "marginalia.en should contain EN text 'Note: mistaking': " + enText);
            assertTrue(enText.contains("Note."),
                    "marginalia.en should contain translation 'Note.': " + enText);
            assertTrue(enText.contains("The patron of the ship."),
                    "marginalia.en should contain translation 'The patron of the ship.': " + enText);

            // IT marginalia text should be in marginalia.it
            assertTrue(doc.has("marginalia.it"), "Should have marginalia.it: " + doc);
            assertTrue(doc.get("marginalia.it").asText().contains("Il Patron della nave."),
                    "marginalia.it should contain 'Il Patron della nave.'");

            // Anchor text should be in anchor_text.it (Italian, the language of the printed text)
            assertTrue(doc.has("anchor_text.it"),
                    "Anchor text should be in anchor_text.it (the book text language): " + doc);
            String anchorIt = doc.get("anchor_text.it").asText();
            assertTrue(anchorIt.contains("lasciando il temone"),
                    "anchor_text.it should contain anchor text: " + anchorIt);
            assertTrue(anchorIt.contains("il patron della naue"),
                    "anchor_text.it should contain anchor text: " + anchorIt);
            assertFalse(enText.contains("lasciando il temone"),
                    "marginalia.en should NOT contain anchor text: " + enText);
            assertFalse(enText.contains("il patron della naue"),
                    "marginalia.en should NOT contain anchor text: " + enText);
        }

        @Test
        void indexesReferencedText() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Marginalia marg = createSimpleMarginalia("la", "[referenced] text");
            marg.setReferencedText("[referenced] text");
            ap.setMarginalia(List.of(marg));

            ObjectNode doc = generateCanvas();
            // Referenced text (anchor_text) now goes to anchor_text field
            assertTrue(doc.has("anchor_text.en") || doc.has("anchor_text.la"),
                    "anchor text should be in anchor_text field: " + doc);
        }

        @Test
        void indexesOtherReader() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Marginalia marg = createSimpleMarginalia("la", "text");
            marg.setOtherReader("John Dee");
            ap.setMarginalia(List.of(marg));

            ObjectNode doc = generateCanvas();
            List<String> annotators = jsonArrayToList(doc.get("annotator"));
            assertTrue(annotators.contains("John Dee"));
            assertTrue(annotators.contains("Harvey"));
        }

        @Test
        void indexesXRefTextField() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Marginalia marg = new Marginalia();
            marg.setId("marg1");
            marg.setLanguage("la");
            MarginaliaLanguage ml = new MarginaliaLanguage();
            ml.setLang("la");
            Position pos = new Position();
            XRef xref = new XRef("Cicero", "De Oratore", "see chapter 3", "la");
            pos.setXRefs(List.of(xref));
            ml.setPositions(List.of(pos));
            marg.setLanguages(List.of(ml));
            ap.setMarginalia(List.of(marg));

            ObjectNode doc = generateCanvas();
            assertTrue(doc.has("cross_reference.en") || doc.has("cross_reference.la"));
            String enRef = doc.has("cross_reference.en") ? doc.get("cross_reference.en").asText() : "";
            assertTrue(enRef.contains("Cicero") || enRef.contains("De Oratore"));
        }

        @Test
        void indexesPeopleWithReferenceSheetAlternates() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Marginalia marg = new Marginalia();
            marg.setId("marg1");
            marg.setLanguage("la");
            MarginaliaLanguage ml = new MarginaliaLanguage();
            ml.setLang("la");
            Position pos = new Position();
            pos.setPeople(List.of("Aristotle"));
            ml.setPositions(List.of(pos));
            marg.setLanguages(List.of(ml));
            ap.setMarginalia(List.of(marg));

            ReferenceSheet peopleRef = new ReferenceSheet();
            peopleRef.addValues("Aristotle", "Aristotle", "Aristoteles");
            collection.setPeopleRef(peopleRef);

            ObjectNode doc = generateCanvas();
            List<String> people = jsonArrayToList(doc.get("people"));
            assertTrue(people.contains("Aristotle"));
            assertTrue(people.contains("Aristoteles"));
        }

        @Test
        void indexesMarginaliaLanguage() {
            AnnotatedPage ap = setUpAnnotatedPage("Harvey");
            Marginalia marg = new Marginalia();
            marg.setId("marg1");
            marg.setLanguage("la");
            MarginaliaLanguage ml = new MarginaliaLanguage();
            ml.setLang("la");
            ml.setPositions(List.of(new Position()));
            marg.setLanguages(List.of(ml));
            ap.setMarginalia(List.of(marg));

            ObjectNode doc = generateCanvas();
            List<String> margLangs = jsonArrayToList(doc.get("marginalia_language"));
            assertTrue(margLangs.contains("la"));
        }

        private Marginalia createSimpleMarginalia(String lang, String referencedText) {
            Marginalia marg = new Marginalia();
            marg.setId("marg1");
            marg.setLanguage(lang);
            marg.setReferencedText(referencedText);
            MarginaliaLanguage ml = new MarginaliaLanguage();
            ml.setLang(lang);
            ml.setPositions(List.of(new Position()));
            marg.setLanguages(List.of(ml));
            return marg;
        }
    }

    @Nested
    @DisplayName("Transcription")
    class TranscriptionTest {
        @Test
        void indexesTranscriptionText() {
            String xmlFragment = "<div><lg><l>Li jolis temps de mai</l></lg></div>";
            Map<String, String> transcriptionPages = Map.of("001r", xmlFragment);

            ObjectNode doc = generator.generateCanvasDocument(collection, book, image, 0, transcriptionPages);
            assertTrue(doc.has("transcription.ofr") || doc.has("transcription.en"),
                    "transcription should have some content");
        }
    }

    @Nested
    @DisplayName("Illustration - mixed numeric/literal references")
    class IllustrationMixedRefsTest {
        @Test
        void numericCharacterIdIsResolved() {
            CharacterNames charNames = new CharacterNames();
            CharacterName cn = new CharacterName();
            cn.setId("27");
            cn.addName("Dancers Group", "en");
            charNames.addCharacterName(cn);
            collection.setCharacterNames(charNames);
            collection.setIllustrationTitles(new IllustrationTitles());

            IllustrationTagging tagging = new IllustrationTagging();
            Illustration illus = new Illustration();
            illus.setId("illus1");
            illus.setPage(image.getId());
            illus.setTitles(new String[]{});
            illus.setCharacters(new String[]{"27"});
            tagging.addIllustrationData(illus);
            book.setIllustrationTagging(tagging);

            // Need to tell the tagging which image this illustration is on
            ObjectNode doc = generateCanvasWithIllustration(illus);

            List<String> charNameList = jsonArrayToList(doc.get("char_name"));
            assertTrue(charNameList.contains("Dancers Group"),
                    "Numeric ID '27' should be resolved: " + charNameList);
        }

        @Test
        void nonNumericCharacterTreatedAsLiteral() {
            collection.setCharacterNames(new CharacterNames());
            collection.setIllustrationTitles(new IllustrationTitles());

            Illustration illus = new Illustration();
            illus.setId("illus1");
            illus.setPage(image.getId());
            illus.setTitles(new String[]{});
            illus.setCharacters(new String[]{"dancers", "musician"});

            ObjectNode doc = generateCanvasWithIllustration(illus);

            List<String> charNameList = jsonArrayToList(doc.get("char_name"));
            assertTrue(charNameList.contains("dancers"),
                    "Non-numeric 'dancers' should be treated as literal: " + charNameList);
            assertTrue(charNameList.contains("musician"),
                    "Non-numeric 'musician' should be treated as literal: " + charNameList);
        }

        @Test
        void mixedNumericAndLiteralCharacters() {
            CharacterNames charNames = new CharacterNames();
            CharacterName cn = new CharacterName();
            cn.setId("27");
            cn.addName("Dancers Group", "en");
            charNames.addCharacterName(cn);
            collection.setCharacterNames(charNames);
            collection.setIllustrationTitles(new IllustrationTitles());

            Illustration illus = new Illustration();
            illus.setId("illus1");
            illus.setPage(image.getId());
            illus.setTitles(new String[]{});
            illus.setCharacters(new String[]{"27", "dancers", "musician"});

            ObjectNode doc = generateCanvasWithIllustration(illus);

            List<String> charNameList = jsonArrayToList(doc.get("char_name"));
            assertTrue(charNameList.contains("Dancers Group"),
                    "Numeric ID '27' should be resolved: " + charNameList);
            assertTrue(charNameList.contains("dancers"),
                    "Non-numeric 'dancers' should be literal: " + charNameList);
            assertTrue(charNameList.contains("musician"),
                    "Non-numeric 'musician' should be literal: " + charNameList);
        }

        @Test
        void nonNumericTitleTreatedAsLiteral() {
            IllustrationTitles titles = new IllustrationTitles();
            titles.setData(Map.of("3", "L'Amans Sleeps"));
            collection.setIllustrationTitles(titles);
            collection.setCharacterNames(new CharacterNames());

            Illustration illus = new Illustration();
            illus.setId("illus1");
            illus.setPage(image.getId());
            illus.setTitles(new String[]{"3", "some literal title"});
            illus.setCharacters(new String[]{});

            ObjectNode doc = generateCanvasWithIllustration(illus);

            String text = doc.get("illustration.en").asText();
            assertTrue(text.contains("L'Amans Sleeps"), "Numeric title ID should be resolved");
            assertTrue(text.contains("some literal title"), "Non-numeric title should be literal");
        }

        /**
         * Helper: generates a canvas document with a single illustration directly.
         * Sets up the book's image list and illustration tagging so that
         * IllustrationTagging.findImageIndices matches the test image.
         */
        private ObjectNode generateCanvasWithIllustration(Illustration illus) {
            // Set page to folio value that guessImage will resolve to testbook.001r.tif
            illus.setPage("1r");

            // Set up image list so guessImage can match
            ImageList imageList = new ImageList();
            imageList.setImages(List.of(image));
            book.setImages(imageList);

            IllustrationTagging tagging = new IllustrationTagging();
            tagging.addIllustrationData(illus);
            book.setIllustrationTagging(tagging);

            return generator.generateCanvasDocument(collection, book, image, 0, Collections.emptyMap());
        }
    }
}
