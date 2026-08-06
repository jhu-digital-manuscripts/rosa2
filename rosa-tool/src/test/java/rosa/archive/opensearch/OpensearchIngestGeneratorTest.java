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
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookReferenceSheet;
import rosa.archive.model.BookText;
import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.HTMLAnnotations;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTitles;
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
import rosa.archive.model.aor.Location;
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

import java.util.ArrayList;
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
        biblioMap.put("en", biblio);
        metadata.setBiblioDataMap(biblioMap);
        metadata.setNumberOfPages(100);
        metadata.setNumberOfIllustrations(5);
        book.setBookMetadata(metadata);

        image = new BookImage();
        image.setId("testbook.001r.tif");
        image.setName("001r");
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
    @DisplayName("Canvas Document")
    class CanvasDocumentTest {
        @Test
        void usesImageNameByDefault() {
            ObjectNode doc = generator.generateCanvasDocument(collection, book, image, 1);
            assertEquals("001r", doc.get("label").asText());
        }

        @Test
        void prefersPaginationFromAnnotatedPage() {
            AnnotatedPage ap = new AnnotatedPage();
            ap.setPage("testbook.001r.tif");
            ap.setPagination("p. 42");
            book.setAnnotatedPages(List.of(ap));

            ObjectNode doc = generator.generateCanvasDocument(collection, book, image, 1);
            assertEquals("p. 42", doc.get("label").asText());
        }

        @Test
        void usesSignatureWhenNoPagination() {
            AnnotatedPage ap = new AnnotatedPage();
            ap.setPage("testbook.001r.tif");
            ap.setSignature("A1");
            book.setAnnotatedPages(List.of(ap));

            ObjectNode doc = generator.generateCanvasDocument(collection, book, image, 1);
            assertEquals("A1", doc.get("label").asText());
        }
    }

    @Nested
    @DisplayName("Manifest Document")
    class ManifestDocumentTest {
        @Test
        void includesBookTextTitles() {
            BookText bt = new BookText();
            bt.setTitle("De Rhetorica");
            book.getBookMetadata().setBookTexts(List.of(bt));

            ObjectNode doc = generator.generateManifestDocument(collection, book);

            JsonNode titles = doc.get("titles");
            assertNotNull(titles);
            assertTrue(titles.isArray());
            boolean found = false;
            for (JsonNode t : titles) {
                if (t.asText().equals("De Rhetorica")) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "BookText title should appear in titles array");
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
                if (a.asText().equals("Cicero")) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "BookText author should appear in authors array");
        }

        @Test
        void descriptionIsEmpty() {
            ObjectNode doc = generator.generateManifestDocument(collection, book);
            assertEquals("", doc.get("description").asText());
        }
    }

    @Nested
    @DisplayName("Mark Annotation")
    class MarkAnnotationTest {
        @Test
        void storesMarkNameInMarkNameField() {
            Mark mark = new Mark("mark1", "[hello]", "plus_sign", "pen", "la", Location.HEAD);

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, mark, "Harvey");

            assertEquals("mark", doc.get("type").asText());
            assertEquals("plus_sign", doc.get("mark_name").asText());
            assertFalse(doc.has("hand") && doc.get("hand").asText().equals("plus_sign"),
                    "mark name should NOT be in hand field");
        }

        @Test
        void stripsTranscriberMarksFromReferencedText() {
            Mark mark = new Mark("mark1", "[important] text", "dash", "pen", "en", Location.HEAD);

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, mark, "Harvey");

            assertEquals("important text", doc.get("text.en").asText());
        }
    }

    @Nested
    @DisplayName("Numeral Annotation")
    class NumeralAnnotationTest {
        @Test
        void indexesBothReferencedTextAndNumeralValue() {
            Numeral numeral = new Numeral("num1", "[page] reference", "42", "en", Location.HEAD);

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, numeral, "Harvey");

            assertEquals("numeral", doc.get("type").asText());
            String text = doc.get("text.en").asText();
            assertTrue(text.contains("page reference"), "should contain stripped referenced text");
            assertTrue(text.contains("42"), "should contain numeral value");
        }
    }

    @Nested
    @DisplayName("Underline Annotation")
    class UnderlineAnnotationTest {
        @Test
        void stripsTranscriberMarks() {
            Underline underline = new Underline("ul1", "[underlined] text", "pen", null, "la", Location.HEAD);

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, underline, "Harvey");

            assertEquals("underline", doc.get("type").asText());
            assertEquals("underlined text", doc.get("text.la").asText());
        }
    }

    @Nested
    @DisplayName("Errata Annotation")
    class ErrataAnnotationTest {
        @Test
        void indexesBothReferencedAndAmendedText() {
            Errata errata = new Errata("err1", "la", "[wrong] word", "correct word");

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, errata, "Harvey");

            assertEquals("errata", doc.get("type").asText());
            String text = doc.get("text.la").asText();
            assertTrue(text.contains("wrong word"));
            assertTrue(text.contains("correct word"));
        }
    }

    @Nested
    @DisplayName("Symbol Annotation")
    class SymbolAnnotationTest {
        @Test
        void indexesSymbolNameAndReferencedText() {
            Symbol symbol = new Symbol("sym1", "[noted] passage", "SS", "la", Location.HEAD);

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, symbol, "Harvey");

            assertEquals("symbol", doc.get("type").asText());
            assertEquals("noted passage", doc.get("text.la").asText());
            assertTrue(doc.has("symbols"));
            assertEquals("SS", doc.get("symbols").get(0).asText());
        }
    }

    @Nested
    @DisplayName("Drawing Annotation")
    class DrawingAnnotationTest {
        @Test
        void indexesReferencedTextAndHand() {
            Drawing drawing = new Drawing("draw1", "[figure] here", Location.HEAD, "diagram", "pen", "en");

            TextEl textEl = new TextEl("Harvey", "en", "anchor text", "drawn text");
            drawing.setTexts(List.of(textEl));

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, drawing, "Harvey");

            assertEquals("drawing", doc.get("type").asText());
            assertEquals("Harvey", doc.get("hand").asText());
            // Text should contain both textEl content and referenced text
            assertTrue(doc.has("text.en"));
        }

        @Test
        void indexesPeopleWithAlternates() {
            Drawing drawing = new Drawing("draw1", null, Location.HEAD, "diagram", null, "en");
            drawing.setPeople(List.of("Aristotle"));

            ReferenceSheet peopleRef = new ReferenceSheet();
            peopleRef.addValues("Aristotle", "Aristotle", "Aristoteles", "The Philosopher");
            collection.setPeopleRef(peopleRef);

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, drawing, "Harvey");

            JsonNode people = doc.get("people");
            assertNotNull(people);
            List<String> values = new ArrayList<>();
            for (JsonNode v : people) {
                values.add(v.asText());
            }
            assertTrue(values.contains("Aristotle"));
            assertTrue(values.contains("Aristoteles"));
            assertTrue(values.contains("The Philosopher"));
        }
    }

    @Nested
    @DisplayName("Calculation Annotation")
    class CalculationAnnotationTest {
        @Test
        void indexesTypeMethodDataAndContent() {
            Calculation calc = new Calculation("calc1", "arithmetic", 0, Location.HEAD, "pen", null);
            calc.addData("3 + 4");
            calc.addData("= 7");
            calc.setContent("simple addition");

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, calc, "Harvey");

            assertEquals("calculation", doc.get("type").asText());
            assertEquals("pen", doc.get("method").asText());
            String text = doc.get("text.en").asText();
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
            Graph graph = new Graph("graph1", "genealogy", 0, Location.HEAD, "pen");
            graph.setLanguage("en");

            GraphNode node1 = new GraphNode("n1", "Aristotle", "Greek philosopher", "content here");
            GraphNode node2 = new GraphNode("n2", "Plato", "Teacher of Aristotle", null);
            graph.addNode(node1);
            graph.addNode(node2);

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, graph, "Harvey");

            assertEquals("graph", doc.get("type").asText());
            assertTrue(doc.has("text.en"));
            String text = doc.get("text.en").asText();
            assertTrue(text.contains("Greek philosopher"));
            assertTrue(text.contains("content here"));

            JsonNode people = doc.get("people");
            assertNotNull(people);
            List<String> names = new ArrayList<>();
            for (JsonNode p : people) names.add(p.asText());
            assertTrue(names.contains("Aristotle"));
            assertTrue(names.contains("Plato"));
        }

        @Test
        void indexesGraphTextHandAndTranslations() {
            Graph graph = new Graph("graph1", "table", 0, Location.HEAD, null);
            graph.setLanguage("la");

            GraphText gt = new GraphText();
            gt.addNote(new GraphNote("note1", "Harvey", "la", null, "anchor", "content"));
            gt.addTranslation("This is the English translation");
            graph.addGraphText(gt);

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, graph, "Harvey");

            assertEquals("Harvey", doc.get("hand").asText());
            assertTrue(doc.has("text.en"));
            assertTrue(doc.get("text.en").asText().contains("This is the English translation"));
        }
    }

    @Nested
    @DisplayName("Table Annotation")
    class TableAnnotationTest {
        @Test
        void indexesCellDataAndTexts() {
            Table table = new Table("table1", Location.HEAD);
            table.setLanguage("la");
            table.setTranslation("English translation of table");
            table.setAggregatedInfo("Summary info");

            TextEl textEl = new TextEl("Harvey", "la", "anchor", "written text");
            table.setTexts(List.of(textEl));

            TableCell cell = new TableCell(0, 0, "cellAnchor", "cellData", "cellContent");
            table.setCells(List.of(cell));

            table.setPeople(List.of("Cicero"));

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, table, "Harvey");

            assertEquals("table", doc.get("type").asText());
            assertEquals("Harvey", doc.get("hand").asText());

            // Check text.la has TextEl content
            assertTrue(doc.has("text.la"));

            // Check text.en has cell data and aggregated info
            assertTrue(doc.has("text.en"));
            String enText = doc.get("text.en").asText();
            assertTrue(enText.contains("cellData"));
            assertTrue(enText.contains("cellAnchor"));
            assertTrue(enText.contains("cellContent"));
            assertTrue(enText.contains("Summary info"));

            // Check translation
            assertTrue(doc.has("translation.en"));
            assertTrue(doc.get("translation.en").asText().contains("English translation of table"));

            // Check people
            assertTrue(doc.has("people"));
        }
    }

    @Nested
    @DisplayName("Marginalia Annotation")
    class MarginaliaAnnotationTest {
        @Test
        void indexesReferencedText() {
            Marginalia marg = createSimpleMarginalia("la", "[referenced] text");

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, marg, "Harvey");

            assertTrue(doc.has("text.la"));
            assertTrue(doc.get("text.la").asText().contains("referenced text"));
        }

        @Test
        void indexesOtherReader() {
            Marginalia marg = createSimpleMarginalia("la", "text");
            marg.setOtherReader("John Dee");

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, marg, "Harvey");

            assertEquals("John Dee", doc.get("annotator").asText());
        }

        @Test
        void routesTranslationToEnglish() {
            Marginalia marg = createSimpleMarginalia("la", "text");
            marg.setTranslation("This is the English translation");

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, marg, "Harvey");

            assertTrue(doc.has("translation.en"));
            assertEquals("This is the English translation", doc.get("translation.en").asText());
            assertFalse(doc.has("translation.la"),
                    "Translation should NOT be routed to Latin");
        }

        @Test
        void indexesXRefTextField() {
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

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, marg, "Harvey");

            assertTrue(doc.has("cross_reference.en") || doc.has("cross_reference.la"));
            // Person and title go to en, text goes to la
            String enRef = doc.has("cross_reference.en") ? doc.get("cross_reference.en").asText() : "";
            assertTrue(enRef.contains("Cicero") || enRef.contains("De Oratore"));
        }

        @Test
        void indexesPeopleWithReferenceSheetAlternates() {
            Marginalia marg = new Marginalia();
            marg.setId("marg1");
            marg.setLanguage("la");

            MarginaliaLanguage ml = new MarginaliaLanguage();
            ml.setLang("la");
            Position pos = new Position();
            pos.setPeople(List.of("Aristotle"));
            ml.setPositions(List.of(pos));
            marg.setLanguages(List.of(ml));

            ReferenceSheet peopleRef = new ReferenceSheet();
            peopleRef.addValues("Aristotle", "Aristotle", "Aristoteles");
            collection.setPeopleRef(peopleRef);

            ObjectNode doc = generator.generateAnnotationDocument(collection, book, image, marg, "Harvey");

            JsonNode people = doc.get("people");
            assertNotNull(people);
            List<String> names = new ArrayList<>();
            for (JsonNode n : people) names.add(n.asText());
            assertTrue(names.contains("Aristotle"));
            assertTrue(names.contains("Aristoteles"));
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
    @DisplayName("Illustration Document")
    class IllustrationDocumentTest {
        @Test
        void resolvesIllustrationTitles() {
            IllustrationTitles titles = new IllustrationTitles();
            titles.setData(Map.of("ROSE_GARDEN", "Garden of the Rose"));
            collection.setIllustrationTitles(titles);

            Illustration illus = new Illustration();
            illus.setId("illus1");
            illus.setTitles(new String[]{"ROSE_GARDEN"});
            illus.setCharacters(new String[]{});

            ObjectNode doc = generator.generateIllustrationDocument(collection, book, image, illus);

            assertTrue(doc.has("text.en"));
            assertTrue(doc.get("text.en").asText().contains("Garden of the Rose"));
        }

        @Test
        void resolvesCharacterNames() {
            CharacterNames charNames = new CharacterNames();
            CharacterName cn = new CharacterName();
            cn.setId("DANGER");
            cn.addName("Danger", "en");
            cn.addName("Dangier", "fr");
            charNames.addCharacterName(cn);
            collection.setCharacterNames(charNames);

            Illustration illus = new Illustration();
            illus.setId("illus1");
            illus.setTitles(new String[]{});
            illus.setCharacters(new String[]{"DANGER"});

            ObjectNode doc = generator.generateIllustrationDocument(collection, book, image, illus);

            // Characters should appear in people array
            assertTrue(doc.has("people"));
            JsonNode people = doc.get("people");
            List<String> names = new ArrayList<>();
            for (JsonNode n : people) names.add(n.asText());
            assertTrue(names.contains("Danger") || names.contains("Dangier"));
        }

        @Test
        void indexesDetailFields() {
            Illustration illus = new Illustration();
            illus.setId("illus1");
            illus.setTitles(new String[]{});
            illus.setCharacters(new String[]{});
            illus.setTextualElement("text element");
            illus.setArchitecture("castle");
            illus.setCostume("robes");
            illus.setObject("sword");
            illus.setLandscape("garden");
            illus.setOther("decorative border");

            ObjectNode doc = generator.generateIllustrationDocument(collection, book, image, illus);

            String text = doc.get("text.en").asText();
            assertTrue(text.contains("text element"));
            assertTrue(text.contains("castle"));
            assertTrue(text.contains("robes"));
            assertTrue(text.contains("sword"));
            assertTrue(text.contains("garden"));
            assertTrue(text.contains("decorative border"));
        }

        @Test
        void includesHtmlAnnotations() {
            HTMLAnnotations htmlAnnotations = new HTMLAnnotations();
            htmlAnnotations.setAnnotation("testbook.001r.tif", "<p>An <b>important</b> illustration</p>");
            collection.setHTMLAnnotations(htmlAnnotations);

            Illustration illus = new Illustration();
            illus.setId("illus1");
            illus.setTitles(new String[]{});
            illus.setCharacters(new String[]{});

            ObjectNode doc = generator.generateIllustrationDocument(collection, book, image, illus);

            assertTrue(doc.has("text.en"));
            String text = doc.get("text.en").asText();
            assertTrue(text.contains("important"));
            assertTrue(text.contains("illustration"));
            assertFalse(text.contains("<p>"), "HTML tags should be stripped");
        }

        @Test
        @DisplayName("Resolves numeric title IDs and character IDs as used in real imagetag.csv")
        void resolvesNumericIdsFromCsvData() {
            // Set up illustration_titles matching real CSV format: id -> title
            IllustrationTitles titles = new IllustrationTitles();
            titles.setData(Map.of(
                    "1", "Portrait of Author (Guillaume de Lorris)",
                    "3", "L'Amans Sleeps",
                    "7", "L'Amans Discovers the Garden of Deduiz"
            ));
            collection.setIllustrationTitles(titles);

            // Set up character_names matching real CSV format: id -> multi-language names
            CharacterNames charNames = new CharacterNames();
            CharacterName guillaumeDeLorris = new CharacterName();
            guillaumeDeLorris.setId("49");
            guillaumeDeLorris.addName("Guillaume de Lorris", "en");
            guillaumeDeLorris.addName("Guillaume de Lorriz, Guillaumes", "fr");
            charNames.addCharacterName(guillaumeDeLorris);

            CharacterName amans = new CharacterName();
            amans.setId("2");
            amans.addName("L'Amans", "en");
            amans.addName("Amans, Amant", "fr");
            charNames.addCharacterName(amans);

            collection.setCharacterNames(charNames);

            // Create illustration matching row 1 of Douce195.imagetag.csv:
            // title ID = "1", character ID = "49"
            Illustration illus = new Illustration();
            illus.setId("1");
            illus.setTitles(new String[]{"1"});
            illus.setCharacters(new String[]{"49"});
            illus.setCostume("Blue sleeveless outer garment and hat");

            ObjectNode doc = generator.generateIllustrationDocument(collection, book, image, illus);

            assertTrue(doc.has("text.en"), "Should have text.en field");
            String text = doc.get("text.en").asText();

            // Title ID "1" should resolve to the full title, not just "1"
            assertTrue(text.contains("Portrait of Author (Guillaume de Lorris)"),
                    "Title ID '1' should be resolved to 'Portrait of Author (Guillaume de Lorris)' but got: " + text);
            assertFalse(text.startsWith("1,"),
                    "Raw title ID '1' should not appear unresolved at the start of text");

            // Character ID "49" should resolve to the character's names
            assertTrue(text.contains("Guillaume de Lorris"),
                    "Character ID '49' should be resolved to 'Guillaume de Lorris' but got: " + text);

            // People array should contain resolved names, not raw IDs
            assertTrue(doc.has("people"), "Should have people array");
            JsonNode people = doc.get("people");
            List<String> peopleNames = new ArrayList<>();
            for (JsonNode n : people) peopleNames.add(n.asText());
            assertTrue(peopleNames.stream().anyMatch(n -> n.contains("Guillaume de Lorris")),
                    "people array should contain resolved character name, got: " + peopleNames);
            assertFalse(peopleNames.contains("49"),
                    "people array should NOT contain raw character ID '49'");

            // Detail fields should still appear
            assertTrue(text.contains("Blue sleeveless outer garment and hat"));
        }

        @Test
        @DisplayName("Resolves multiple title IDs and character IDs in a single illustration")
        void resolvesMultipleIdsPerIllustration() {
            IllustrationTitles titles = new IllustrationTitles();
            titles.setData(Map.of(
                    "10", "Portrait of Vilanie",
                    "11", "Portrait of Covoitise"
            ));
            collection.setIllustrationTitles(titles);

            CharacterNames charNames = new CharacterNames();
            CharacterName vilanie = new CharacterName();
            vilanie.setId("111");
            vilanie.addName("Vilanie", "en");
            charNames.addCharacterName(vilanie);

            CharacterName covoitise = new CharacterName();
            covoitise.setId("21");
            covoitise.addName("Covoitise", "en");
            covoitise.addName("Convoitise", "fr");
            charNames.addCharacterName(covoitise);

            collection.setCharacterNames(charNames);

            // Illustration with two titles and two characters
            Illustration illus = new Illustration();
            illus.setId("multi");
            illus.setTitles(new String[]{"10", "11"});
            illus.setCharacters(new String[]{"111", "21"});

            ObjectNode doc = generator.generateIllustrationDocument(collection, book, image, illus);

            String text = doc.get("text.en").asText();
            assertTrue(text.contains("Portrait of Vilanie"), "Should resolve title '10'");
            assertTrue(text.contains("Portrait of Covoitise"), "Should resolve title '11'");
            assertTrue(text.contains("Vilanie"), "Should resolve character '111'");

            JsonNode people = doc.get("people");
            List<String> names = new ArrayList<>();
            for (JsonNode n : people) names.add(n.asText());
            assertTrue(names.stream().anyMatch(n -> n.contains("Vilanie")));
            assertTrue(names.stream().anyMatch(n -> n.contains("Covoitise") || n.contains("Convoitise")));
        }

        @Test
        @DisplayName("Throws error when title ID cannot be resolved")
        void throwsOnUnresolvableTitleId() {
            IllustrationTitles titles = new IllustrationTitles();
            titles.setData(Map.of());
            collection.setIllustrationTitles(titles);
            collection.setCharacterNames(new CharacterNames());

            Illustration illus = new Illustration();
            illus.setId("1");
            illus.setTitles(new String[]{"999"});
            illus.setCharacters(new String[]{});

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    generator.generateIllustrationDocument(collection, book, image, illus));
            assertTrue(ex.getMessage().contains("999"), "Error should mention the unresolvable ID");
        }

        @Test
        @DisplayName("Throws error when character ID cannot be resolved")
        void throwsOnUnresolvableCharacterId() {
            IllustrationTitles titles = new IllustrationTitles();
            titles.setData(Map.of());
            collection.setIllustrationTitles(titles);
            collection.setCharacterNames(new CharacterNames());

            Illustration illus = new Illustration();
            illus.setId("1");
            illus.setTitles(new String[]{});
            illus.setCharacters(new String[]{"UNKNOWN_CHAR"});

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    generator.generateIllustrationDocument(collection, book, image, illus));
            assertTrue(ex.getMessage().contains("UNKNOWN_CHAR"), "Error should mention the unresolvable ID");
        }

        @Test
        @DisplayName("Throws error when illustration_titles.csv is not loaded")
        void throwsWhenIllustrationTitlesNotLoaded() {
            // No IllustrationTitles set on collection (null)
            collection.setCharacterNames(new CharacterNames());

            Illustration illus = new Illustration();
            illus.setId("1");
            illus.setTitles(new String[]{"1"});
            illus.setCharacters(new String[]{});

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    generator.generateIllustrationDocument(collection, book, image, illus));
            assertTrue(ex.getMessage().contains("illustration_titles.csv"),
                    "Error should mention missing illustration_titles.csv");
        }
    }
}
