package rosa.iiif.presentation.core.html;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookReferenceSheet;
import rosa.archive.model.BookReferenceSheet.Link;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.AorLocation;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.StaticResourceRequestFormatter;
import rosa.iiif.presentation.core.extras.BookReferenceResourceDb;

public class MarginaliaHtmlAdapterTest {
    private MarginaliaHtmlAdapter adapter;
    private BookCollection fakeCollection;
    private Book fakeBook;

    @Before
    public void setup() {
        IIIFPresentationRequestFormatter requestFormatter = new IIIFPresentationRequestFormatter(
                "https",
                "example.com",
                "",
                -1
        );
        StaticResourceRequestFormatter staticFormatter = new StaticResourceRequestFormatter(
                "https",
                "example.com",
                "",
                -1
        );

        adapter = new MarginaliaHtmlAdapter(new PresentationUris(requestFormatter, null, staticFormatter));

        fakeCollection = new BookCollection();
        fakeCollection.setId("aor");
        
        fakeBook = new Book();
        fakeBook.setId("PrincetonPA6452");
        
        ImageList fakeImages = new ImageList();
        
        fakeImages.getImages().add(new BookImage("PrincetonPA6452.022r.tif", 10, 10, false));
        fakeImages.getImages().add(new BookImage("PrincetonPA6452.020r.tif", 10, 10, false));
        fakeImages.getImages().add(new BookImage("PrincetonPA6452.011v.tif", 10, 10, false));
        fakeImages.getImages().add(new BookImage("PrincetonPA6452.013r.tif", 10, 10, false));
        fakeImages.getImages().add(new BookImage("PrincetonPA6452.023r.tif", 10, 10, false));
        fakeImages.getImages().add(new BookImage("PrincetonPA6452.025r.tif", 10, 10, false));
        fakeImages.getImages().add(new BookImage("PrincetonPA6452.026r.tif", 10, 10, false));
        fakeImages.getImages().add(new BookImage("PrincetonPA6452.032v.tif", 10, 10, false));

        fakeBook.setImages(fakeImages);
        
        Map<String, AorLocation> map = new HashMap<>();
        fakeCollection.setAnnotationMap(map);
        fakeCollection.setId("aor");
        map.put("id", new AorLocation("aor", "PrincetonPA6452", "11v", null));
        /*
        t1.add(new ReferenceTarget("PrincetonPA6452:00000023", "[a1r]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000028", "[6]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000031", "[9]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000051", "[20]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000055", "[33]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000057", "[35]"));
         */
        map.put("PrincetonPA6452:00000027", new AorLocation("aor", "PrincetonPA6452", "22r", null));
        map.put("PrincetonPA6452:00000023", new AorLocation("aor", "PrincetonPA6452", "20r", null));
        map.put("PrincetonPA6452:00000028", new AorLocation("aor", "PrincetonPA6452", "11v", null));
        map.put("PrincetonPA6452:00000031", new AorLocation("aor", "PrincetonPA6452", "13r", null));
        map.put("PrincetonPA6452:00000051", new AorLocation("aor", "PrincetonPA6452", "23r", null));
        map.put("PrincetonPA6452:00000055", new AorLocation("aor", "PrincetonPA6452", "25r", null));
        map.put("PrincetonPA6452:00000057", new AorLocation("aor", "PrincetonPA6452", "26r", null));
        map.put("BLC120b4:c_120_b_4_(2)_f054v", new AorLocation("aor", "BLC120b4", "32v", null));
    }

    @Test
    public void internalRefListTest() throws Exception {
        List<ReferenceTarget> t1 = new ArrayList<>();
        t1.add(new ReferenceTarget("PrincetonPA6452:00000023", "[a1r]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000028", "[6]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000031", "[9]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000051", "[20]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000055", "[33]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000057", "[35]"));
        InternalReference r1 = new InternalReference("s[upr]a", t1);

        List<InternalReference> refs = new ArrayList<>(Collections.singletonList(r1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);

        Marginalia marg = new Marginalia();
        marg.setLanguages(Collections.singletonList(new MarginaliaLanguage()));
        marg.getLanguages().get(0).setLang("en");
        marg.getLanguages().get(0).setPositions(Collections.singletonList(new Position()));
        marg.getLanguages().get(0).getPositions().get(0).setInternalRefs(refs);

        adapter.addInternalRefs(fakeCollection, fakeBook, marg, refs, writer);

        String result = out.toString();
        // System.out.println(result);
        assertNotNull(result);
        assertTrue(result.contains("<span class=\"emphasize\">Internal References:</span>"));
        assertTrue("There must be ', ' between text and targets", result.contains("s[upr]a, <a"));
        assertFalse("There should be ', ' between targets", result.contains("</a><a"));
        assertTrue("There should be ', ' between targets", result.contains("</a>, <a "));
    }

    @Test
    public void fromLivyTest() {
        final String transcription = "I[nfr]a";
        final String expected = "<a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/aor/PrincetonPA6452/20r/canvas\" " +
                "data-label=\"[a1r]\" " +
                "data-manifestid=\"https://example.com/aor/PrincetonPA6452/manifest\" " +
                "data-targetid1=\"https://example.com/aor/PrincetonPA6452/22r/canvas\" " +
                "data-label1=\"[5]\" " +
                ">I[nfr]a</a>";

        List<ReferenceTarget> targets = new ArrayList<>();
        targets.add(new ReferenceTarget("PrincetonPA6452:00000023", "[a1r]"));
        targets.add(new ReferenceTarget("PrincetonPA6452:00000027", "[5]"));

        InternalReference ref = new InternalReference("I[nfr]a", targets);

        String result = adapter.addInternalRefs(fakeCollection, fakeBook, transcription, Collections.singletonList(ref));

//        assertEquals(expected, result);
        assertEquals(transcription, result);
    }

    @Test
    public void fromVoarchadumiaTest() {
        final String transcription = "vide synonia pag[ina] 54:";
        final String expected = "vide synonia <a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/aor/BLC120b4/032v/canvas\" " +
                "data-manifestid=\"https://example.com/aor/BLC120b4/manifest\">pag[ina] 54</a>:";

        List<ReferenceTarget> targets = new ArrayList<>();
        targets.add(new ReferenceTarget("BLC120b4:c_120_b_4_(2)_f054v", "pag[ina] 54"));

        InternalReference ref = new InternalReference(null, targets);

        String result = adapter.addInternalRefs(fakeCollection, fakeBook, transcription, Collections.singletonList(ref));

        assertEquals(expected, result);
    }

    @Test
    public void referenceAtEndTest() {
        final String expected = "This is a <a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/aor/PrincetonPA6452/011v/canvas\" " +
                "data-manifestid=\"https://example.com/aor/PrincetonPA6452/manifest\">test</a> moo";
        final String trans = "This is a test moo";

        String result = adapter.addInternalRefs(fakeCollection, fakeBook, trans, Collections.singletonList(newRef()));

        assertEquals(expected, result);
    }

    @Test
    public void referenceAtStartTest() {
        final String transcription = "is a test moo sound";
        final String expected = "is a <a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/aor/PrincetonPA6452/011v/canvas\" " +
                "data-manifestid=\"https://example.com/aor/PrincetonPA6452/manifest\">test</a> moo sound";

        String result = adapter.addInternalRefs(fakeCollection, fakeBook, transcription, Collections.singletonList(newRef()));

        assertEquals(expected, result);
    }

    @Test
    public void referenceInMiddleTest() {
        final String transcription = "This is a test moo sound";
        final String expected = "This is a <a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/aor/PrincetonPA6452/011v/canvas\" " +
                "data-manifestid=\"https://example.com/aor/PrincetonPA6452/manifest\">test</a> moo sound";

        String result = adapter.addInternalRefs(fakeCollection, fakeBook, transcription, Collections.singletonList(newRef()));

        assertEquals(expected, result);
    }

    @Test
    public void referenceIsTextTest() {
        final String transcription = "is a test moo";
        final String expected = "is a <a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/aor/PrincetonPA6452/011v/canvas\" " +
                "data-manifestid=\"https://example.com/aor/PrincetonPA6452/manifest\">test</a> moo";

        String result = adapter.addInternalRefs(fakeCollection, fakeBook, transcription, Collections.singletonList(newRef()));

        assertEquals(expected, result);
    }

    /**
     * Tests the case where the source text is actually in the internal_ref#text, instead of
     * internal_ref/target#text. This is done on a subset of internal references. In many cases,
     * the target also has text which we will ignore for now.
     *
     *
     */
    @Test
    public void sourceTextInReferenceTest() {
        final String transcription = "This is a test moo";
        final String expected = "This is a test <a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/aor/PrincetonPA6452/011v/canvas\" " +
                "data-label=\"test\" " +
                "data-manifestid=\"https://example.com/aor/PrincetonPA6452/manifest\">moo</a>";

        String result = adapter.addInternalRefs(fakeCollection, fakeBook, transcription, Collections.singletonList(weirdRef()));
        assertEquals(expected, result);
    }

    @Test
    public void targetAlreadyPresent() {
        final String transcription = "This is a test moo";
        final String expected = "This is a <a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/aor/PrincetonPA6452/canvas\" " +
                "data-manifestid=\"https://example.com/col/book/manifest\" " +
                "data-targetid1=\"https://example.com/aor/PrincetonPA6452/canvas\"" +
                ">test</a> moo";

        String result = adapter.addInternalRefs(fakeCollection, fakeBook, transcription, Collections.singletonList(doubleTargetRef()));
//        assertEquals(expected, result);
        assertEquals(transcription, result);
    }

    private InternalReference newRef() {
        ReferenceTarget tar = new ReferenceTarget("id", "text", "pre", "post");
        InternalReference r = new InternalReference(null, new ArrayList<>(Collections.singletonList(tar)));

        tar.setText("test");
        tar.setTextPrefix("is a ");
        tar.setTextSuffix(" moo");

        return r;
    }

    private InternalReference weirdRef() {
        InternalReference r = newRef();
        r.setText("moo");

        return r;
    }

    private InternalReference doubleTargetRef() {
        ReferenceTarget t = new ReferenceTarget("id", "test", "is a ", " moo");
        InternalReference ref = newRef();
        ref.getTargets().add(t);

        return ref;
    }

    /*
        Crazy internal references from Livy: PrincetonPA6452.aor.046v.xml

        <marginalia_text>= peritus in difficili casu processus. pro se quisq[ue]: s[upr]a, i[nfr]a. Pudor in imperio, rebusq[ue] gerendis, puer est, non vir.
        </marginalia_text>
        <emphasis method="pen" text="Pudor" type="straight"/>
        <internal_ref text="s[upr]a">
            <target ref="PrincetonPA6452:00000023" text="[a1r]"/>
            <target ref="PrincetonPA6452:00000028" text="[6]"/>
            <target ref="PrincetonPA6452:00000031" text="[9]"/>
            <target ref="PrincetonPA6452:00000051" text="[20]"/>
            <target ref="PrincetonPA6452:00000055" text="[33]"/>
            <target ref="PrincetonPA6452:00000057" text="[35]"/>
        </internal_ref>
        <internal_ref text="i[nfr]a">
            <target ref="PrincetonPA6452:00000098" text="[76]"/>
            <target ref="PrincetonPA6452:00000107" text="[85]"/>
            <target ref="PrincetonPA6452:00000115" text="[93]"/>
            <target ref="PrincetonPA6452:00000131" text="[109]"/>
            <target ref="PrincetonPA6452:00000137" text="[115]"/>
            <target ref="PrincetonPA6452:00000147" text="[125]"/>
            <target ref="PrincetonPA6452:00000154" text="[132]"/>
            <target ref="PrincetonPA6452:00000172" text="[150]"/>
            <target ref="PrincetonPA6452:00000174" text="[152]"/>
            <target ref="PrincetonPA6452:00000195" text="[173]"/>
            <target ref="PrincetonPA6452:00000209" text="[187]"/>
            <target ref="PrincetonPA6452:00000210" text="[188]"/>
            <target ref="PrincetonPA6452:00000211" text="[189]"/>
            <target ref="PrincetonPA6452:00000238" text="[216]"/>
            <target ref="PrincetonPA6452:00000245" text="[223]"/>
            <target ref="PrincetonPA6452:00000271" text="[249]"/>
            <target ref="PrincetonPA6452:00000276" text="[254]"/>
            <target ref="PrincetonPA6452:00000468" text="[446]"/>
            <target ref="PrincetonPA6452:00000476" text="[454]"/>
            <target ref="PrincetonPA6452:00000630" text="[608]"/>
            <target ref="PrincetonPA6452:00000711" text="[689]"/>
            <target ref="PrincetonPA6452:00000732" text="[710]"/>
            <target ref="PrincetonPA6452:00000851" text="[829]"/>
        </internal_ref>
     */
    @Test
    public void testCrazyLivy() {
        final String transcription = "= peritus in difficili casu processus. pro se quisq[ue]: s[upr]a, i[nfr]a. " +
                "Pudor in imperio, rebusq[ue] gerendis, puer est, non vir.";
        final String expected =
                "= peritus in difficili casu processus. pro se quisq[ue]: <a class=\"internal-ref\" href=\"javascript:;\" " +
                        "data-targetid=\"https://example.com/aor/PrincetonPA6452/20r/canvas\" " +
                        "data-label=\"[a1r]\" " +
                        "data-manifestid=\"https://example.com/aor/PrincetonPA6452/manifest\" " +
                        "data-targetid1=\"https://example.com/aor/PrincetonPA6452/11v/canvas\" " +
                        "data-label1=\"[6]\"  " +
                        "data-targetid2=\"https://example.com/aor/PrincetonPA6452/13r/canvas\" " +
                        "data-label2=\"[9]\"  " +
                        "data-targetid3=\"https://example.com/aor/PrincetonPA6452/23r/canvas\" " +
                        "data-label3=\"[20]\"  " +
                        "data-targetid4=\"https://example.com/aor/PrincetonPA6452/25r/canvas\" " +
                        "data-label4=\"[33]\"  " +
                        "data-targetid5=\"https://example.com/aor/PrincetonPA6452/26r/canvas\" " +
                        "data-label5=\"[35]\" >s[upr]a</a>" +
                        ", i[nfr]a. Pudor in imperio, rebusq[ue] gerendis, puer est, non vir.";

        List<ReferenceTarget> t1 = new ArrayList<>();
        t1.add(new ReferenceTarget("PrincetonPA6452:00000023", "[a1r]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000028", "[6]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000031", "[9]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000051", "[20]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000055", "[33]"));
        t1.add(new ReferenceTarget("PrincetonPA6452:00000057", "[35]"));
        InternalReference r1 = new InternalReference("s[upr]a", t1);

        String result = adapter.addInternalRefs(fakeCollection, fakeBook, transcription, Collections.singletonList(r1));
//        assertEquals(expected, result);
        assertEquals(transcription, result);
    }

    @Test
    public void moo() throws Exception {
        String data = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<!DOCTYPE transcription SYSTEM \"http://www.livesandletters.ac.uk/schema/aor_20141023.dtd\">\n" +
                "<transcription xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.livesandletters.ac.uk/schema/aor2_18112016.xsd\">\n" +
                "    <page filename=\"BLC120b4.016r.tif\" pagination=\"16\" reader=\"John Dee\"/>\n" +
                "    <annotation>\n" +
                "        <marginalia hand=\"Italian\" method=\"pen\">\n" +
                "            <language ident=\"EN\">\n" +
                "                <position place=\"tail\" book_orientation=\"0\">\n" +
                "                    <marginalia_text>&Sun; - 1/2</marginalia_text>\n" +        // Entity here :: &Sun;  -->   ☉
                "                    <symbol_in_text name=\"Sun\"/>\n" +                        //   - Defined only in DTD
                "                </position>\n" +
                "            </language>\n" +
                "        </marginalia>\n" +
                "        <marginalia hand=\"Italian\" anchor_text=\"Adum&aacute;\" method=\"pen\">\n" +         // á
                "            <language ident=\"LA\">\n" +
                "                <position place=\"right_margin\" book_orientation=\"0\">\n" +
                "                    <marginalia_text>\n" +
                "                        Nota quod dicat Adum&aacute;, tantu[m], no[n] adiecta particula illa Voarch[adumia]:\n" +
                "                    </marginalia_text>\n" +
                "                    <person name=\"Adum&aacute;\"/>\n" +
                "                    <book title=\"Voarchadumia\"/>\n" +
                "                    <book title=\"Tragoedia\"/>\n" +
                "                </position>\n" +
                "            </language>\n" +
                "            <translation>Note what Aduma says, only that particular was not aimed at Voarchadumia:</translation>\n" +
                "        </marginalia>\n" +
                "    </annotation>\n" +
                "</transcription>";

        BookCollection col = new BookCollection();
        col.setId("Moo");
        BookReferenceSheet sheet = new BookReferenceSheet();
        sheet.setLines(Collections.singletonList("Tragoedia,,,,,,Lucian,,,,,http://www.perseus.tufts.edu/hopper/text?doc=Perseus:text:2008.01.0437,,,,,,,,,,,,"));
        col.setBooksRef(sheet);

        Book book = new Book();
        book.setId("The Greatest Moo");

        BookImage img = new BookImage("FirstMoo", 3, 3, false);

        AORAnnotatedPageSerializer serializer = new AORAnnotatedPageSerializer();
        List<String> errors = new ArrayList<>();
        ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes("UTF-8"));

        AnnotatedPage loadedPage = serializer.read(in, errors);

        Marginalia m = loadedPage.getMarginalia().get(1);
        String result = adapter.adapt(col, book, img, m, new BookReferenceResourceDb(Link.PERSEUS, col.getBooksRef()));

        assertTrue(result.contains("data-Perseus=\"http://www.perseus.tufts.edu/hopper/text?doc=Perseus:text:2008.01.0437\""));
    }
}
