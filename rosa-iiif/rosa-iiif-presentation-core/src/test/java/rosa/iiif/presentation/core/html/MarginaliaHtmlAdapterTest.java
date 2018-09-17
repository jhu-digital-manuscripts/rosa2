package rosa.iiif.presentation.core.html;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookCollection;
import rosa.archive.model.aor.AorLocation;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.PresentationUris;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MarginaliaHtmlAdapterTest {
    private MarginaliaHtmlAdapter adapter;
    private BookCollection fakeCollection;

    @Before
    public void setup() {
        IIIFPresentationRequestFormatter requestFormatter = new IIIFPresentationRequestFormatter(
                "https",
                "example.com",
                "",
                -1
        );

        adapter = new MarginaliaHtmlAdapter(new PresentationUris(requestFormatter, null));

        fakeCollection = new BookCollection();

        Map<String, AorLocation> map = new HashMap<>();
        fakeCollection.setAnnotationMap(map);
        map.put("id", new AorLocation("col", "book", "page", null));
        map.put("PrincetonPA6452:00000027", new AorLocation("aor", "Princeton6452", "22r", null));
        map.put("PrincetonPA6452:00000023", new AorLocation("aor", "Princeton6452", "20r", null));
        map.put("BLC120b4:c_120_b_4_(2)_f054v", new AorLocation("aor", "BLC120b4", "32v", null));
    }

    @Test
    public void fromLivyTest() {
        final String transcription = "I[nfr]a";
        final String expected = "<a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/aor/Princeton6452/20r/canvas\" " +
                "data-manifestid=\"https://example.com/aor/Princeton6452/manifest\">I[nfr]a</a>";

        List<ReferenceTarget> targets = new ArrayList<>();
        targets.add(new ReferenceTarget("PrincetonPA6452:00000023", "[a1r]"));
        targets.add(new ReferenceTarget("PrincetonPA6452:00000027", "[5]"));

        InternalReference ref = new InternalReference("I[nfr]a", targets);

        String result = adapter.addInternalRefs(fakeCollection, transcription, Collections.singletonList(ref));

        assertEquals(expected, result);
    }

    @Test
    public void fromVoarchadumiaTest() {
        final String transcription = "vide synonia pag[ina] 54:";
        final String expected = "vide synonia <a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/aor/BLC120b4/32v/canvas\" " +
                "data-manifestid=\"https://example.com/aor/BLC120b4/manifest\">pag[ina] 54</a>:";

        List<ReferenceTarget> targets = new ArrayList<>();
        targets.add(new ReferenceTarget("BLC120b4:c_120_b_4_(2)_f054v", "pag[ina] 54"));

        InternalReference ref = new InternalReference(null, targets);

        String result = adapter.addInternalRefs(fakeCollection, transcription, Collections.singletonList(ref));

        assertEquals(expected, result);
    }

    @Test
    public void referenceAtEndTest() {
        final String expected = "This is a <a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/col/book/page/canvas\" " +
                "data-manifestid=\"https://example.com/col/book/manifest\">test</a> moo";
        final String trans = "This is a test moo";

        String result = adapter.addInternalRefs(fakeCollection, trans, Collections.singletonList(newRef()));

        assertEquals(expected, result);
    }

    @Test
    public void referenceAtStartTest() {
        final String transcription = "is a test moo sound";
        final String expected = "is a <a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/col/book/page/canvas\" " +
                "data-manifestid=\"https://example.com/col/book/manifest\">test</a> moo sound";

        String result = adapter.addInternalRefs(fakeCollection, transcription, Collections.singletonList(newRef()));

        assertEquals(expected, result);
    }

    @Test
    public void referenceInMiddleTest() {
        final String transcription = "This is a test moo sound";
        final String expected = "This is a <a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/col/book/page/canvas\" " +
                "data-manifestid=\"https://example.com/col/book/manifest\">test</a> moo sound";

        String result = adapter.addInternalRefs(fakeCollection, transcription, Collections.singletonList(newRef()));

        assertEquals(expected, result);
    }

    @Test
    public void referenceIsTextTest() {
        final String transcription = "is a test moo";
        final String expected = "is a <a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"https://example.com/col/book/page/canvas\" " +
                "data-manifestid=\"https://example.com/col/book/manifest\">test</a> moo";

        String result = adapter.addInternalRefs(fakeCollection, transcription, Collections.singletonList(newRef()));

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
                "data-targetid=\"https://example.com/col/book/page/canvas\" " +
                "data-manifestid=\"https://example.com/col/book/manifest\">moo</a>";

        String result = adapter.addInternalRefs(fakeCollection, transcription, Collections.singletonList(weirdRef()));
        assertEquals(expected, result);
    }

    private InternalReference newRef() {
        ReferenceTarget tar = new ReferenceTarget("id", "text", "pre", "post");
        InternalReference r = new InternalReference(null, Collections.singletonList(tar));

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
}
