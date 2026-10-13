package rosa.archive.core.serialize;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.GraphNote;
import rosa.archive.model.aor.GraphText;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Table;
import rosa.archive.model.aor.TextEl;
import rosa.archive.model.aor.XRef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ArchiveReaders#readAnnotatedPage(Path, List)}.
 */
class ArchiveReadersAnnotatedPageTest {

    private static final String PAGE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <transcription>
                <page filename="Test.001r.tif" reader="John Dee"/>
                <annotation>
                    <marginalia hand="Italian" method="typewriter">
                        <language ident="LA">
                            <position place="head">
                                <marginalia_text>Sol et Luna</marginalia_text>
                                <symbol_in_text name="Sun"/>
                                <symbol_in_text name="Moon"/>
                                <marginalia_symbol name="Mars"/>
                                <X-ref book_title="Adages" language="LA" person="Erasmus" text="Omnium horarum homo"/>
                                <emphasis method="pen" text="et Luna" type="straight"/>
                            </position>
                        </language>
                    </marginalia>
                    <table type="table" place="tail">
                        <symbol_in_text name="Florilegium"/>
                        <person name="Livy"/>
                        <text hand="Italian" language="LA">Summa annorum</text>
                        <tr><th label="Anni">Years</th></tr>
                        <tr><td anchor_text="CCC">300</td></tr>
                    </table>
                    <numeral place="head" language="LA" text="tres">3</numeral>
                    <drawing method="pen" place="head" type="manicule" book_orientation="0">
                        <person name="Cicero"/>
                        <translation>Look here</translation>
                    </drawing>
                    <graph method="pen" place="head" type="family_tree" book_orientation="0">
                        <node id="n1" person="Adam" anchor_text="Adamus">first man</node>
                        <graph_text>
                            <note hand="Italian" language="IT" anchor_text="radix">la radice</note>
                            <translation>the root</translation>
                        </graph_text>
                    </graph>
                </annotation>
            </transcription>
            """;

    private AnnotatedPage read(Path dir) throws IOException {
        Path file = dir.resolve("Test.aor.001r.xml");
        Files.writeString(file, PAGE.strip());
        List<String> errors = new ArrayList<>();
        AnnotatedPage page = ArchiveReaders.readAnnotatedPage(file, errors);
        assertTrue(errors.isEmpty(), errors.toString());
        assertNotNull(page);
        return page;
    }

    @Test
    void readsMarginaliaMethod(@TempDir Path dir) throws IOException {
        Marginalia marg = read(dir).getMarginalia().getFirst();
        assertEquals("typewriter", marg.getMethod());
    }

    @Test
    void readsSymbolsNamedInTable(@TempDir Path dir) throws IOException {
        Table table = read(dir).getTables().getFirst();
        assertEquals(List.of("Florilegium"), table.getSymbols());
    }

    @Test
    void readsMarginaliaSymbolXRefAndEmphasis(@TempDir Path dir) throws IOException {
        Position pos = read(dir).getMarginalia().getFirst()
                .getLanguages().getFirst().getPositions().getFirst();
        assertEquals(List.of("Sun", "Moon", "Mars"), pos.getSymbols());
        assertEquals(List.of(new XRef("Erasmus", "Adages", "Omnium horarum homo", "LA")), pos.getXRefs());
        assertEquals("et Luna", pos.getEmphasis().getFirst().getReferencedText());
    }

    @Test
    void readsTableTextsPeopleHeadersAndCells(@TempDir Path dir) throws IOException {
        Table table = read(dir).getTables().getFirst();
        assertEquals(List.of("Livy"), table.getPeople());
        assertEquals(List.of(new TextEl("Italian", "LA", "", "Summa annorum")), table.getTexts());
        assertEquals("Anni", table.getColHeaders().getFirst().label());
        assertEquals("Years", table.getColHeaders().getFirst().content());
        assertEquals("CCC", table.getCells().getFirst().anchorText());
    }

    @Test
    void readsNumeralLanguage(@TempDir Path dir) throws IOException {
        assertEquals("LA", read(dir).getNumerals().getFirst().getLanguage());
    }

    @Test
    void readsDrawingPeopleAndTranslation(@TempDir Path dir) throws IOException {
        Drawing drawing = read(dir).getDrawings().getFirst();
        assertEquals(List.of("Cicero"), drawing.getPeople());
        assertEquals("Look here", drawing.getTranslation());
    }

    @Test
    void readsGraphNodeAnchorNotesAndTranslations(@TempDir Path dir) throws IOException {
        Graph graph = read(dir).getGraphs().getFirst();
        assertEquals("Adamus", graph.getNodes().getFirst().text());
        GraphText text = graph.getGraphTexts().getFirst();
        GraphNote note = text.getNotes().getFirst();
        assertEquals("Italian", note.hand());
        assertEquals("IT", note.language());
        assertEquals("radix", note.anchorText());
        assertEquals("la radice", note.content());
        assertEquals(List.of("the root"), text.getTranslations());
    }
}
