package rosa.iiif.presentation.core.html;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.PresentationUris;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class MarginaliaHtmlAdapterTest {
    private MarginaliaHtmlAdapter adapter;

    @Before
    public void setup() {
        IIIFPresentationRequestFormatter requestFormatter = new IIIFPresentationRequestFormatter(
                "scheme",
                "host",
                "prefix",
                11
        );

        adapter = new MarginaliaHtmlAdapter(new PresentationUris(requestFormatter));
    }

    @Test
    public void referenceAtEndTest() {
        final String expected = "This is a <a href=\"id\" target=\"_blank\">test</a> moo";
        final String trans = "This is a test moo";

        String result = adapter.addInternalRefs(trans, Collections.singletonList(newRef()));

        assertEquals(expected, result);
    }

    @Test
    public void referenceAtStartTest() {
        final String transcription = "is a test moo sound";
        final String expected = "is a <a href=\"id\" target=\"_blank\">test</a> moo sound";

        String result = adapter.addInternalRefs(transcription, Collections.singletonList(newRef()));

        assertEquals(expected, result);
    }

    @Test
    public void referenceInMiddleTest() {
        final String transcription = "This is a test moo sound";
        final String expected = "This is a <a href=\"id\" target=\"_blank\">test</a> moo sound";

        String result = adapter.addInternalRefs(transcription, Collections.singletonList(newRef()));

        assertEquals(expected, result);
    }

    @Test
    public void referenceIsTextTest() {
        final String transcription = "is a test moo";
        final String expected = "is a <a href=\"id\" target=\"_blank\">test</a> moo";

        String result = adapter.addInternalRefs(transcription, Collections.singletonList(newRef()));

        assertEquals(expected, result);
    }

    private InternalReference newRef() {
        ReferenceTarget tar =new ReferenceTarget("id", "text", "pre", "post");
        InternalReference r = new InternalReference("text", Collections.singletonList(tar));

        r.setAnchor("test");
        r.setAnchorPrefix("is a ");
        r.setAnchorSuffix(" moo");

        return r;
    }
}
