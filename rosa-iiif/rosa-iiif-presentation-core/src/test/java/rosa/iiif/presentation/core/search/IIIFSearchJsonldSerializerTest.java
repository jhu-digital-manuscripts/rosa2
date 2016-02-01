package rosa.iiif.presentation.core.search;

import org.junit.Before;
import org.junit.Test;
import rosa.iiif.presentation.core.search.IIIFSearchJsonldSerializer;
import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.TextValue;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.search.IIIFSearchHit;
import rosa.iiif.presentation.model.search.IIIFSearchResult;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class IIIFSearchJsonldSerializerTest {
    private IIIFSearchJsonldSerializer serializer;

    @Before
    public void setup() {
        serializer = new IIIFSearchJsonldSerializer();
    }

    @Test
    public void test() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        serializer.write(mockResults(), os);

        String results = os.toString("UTF-8");
        assertNotNull("Transformed results were NULL.", results);
        assertFalse("Transformed results were blank.", results.isEmpty());
    }

    private IIIFSearchResult mockResults() {
        IIIFSearchResult result = new IIIFSearchResult();

        List<IIIFSearchHit> hits = new ArrayList<>();
        List<Annotation> anns = result.getAnnotations();
        for (int i = 0; i < 10; i++) {
            Annotation a = new Annotation();
            a.setId("Annotation_" + i);
            a.setLabel("Annotation " + i, "en");

            AnnotationTarget target = new AnnotationTarget("Target_" + i);
            target.setParentRef(new Reference("ParentRef_" + i, new TextValue("val", "en"), "oa:annotation"));
            a.setDefaultTarget(target);

            a.setDefaultSource(new AnnotationSource("SourceURI", "text blah", "text/plain", "Text To Display", "en"));
            a.setMotivation("sc:Painting");

            anns.add(a);

            hits.add(new IIIFSearchHit(new String[] {"Annotation_"+i}, "Display", "Text To ", ""));
        }
        result.setHits(hits.toArray(new IIIFSearchHit[hits.size()]));

        result.setStartIndex(0);
        result.setId("SearchResults_1");
        result.setFirst("First");
        result.setLast("Last");
        result.setNext("Next");
        result.setPrev("Prev");
        result.setTotal(10);
        result.setIgnored(new String[] {"date", "user", "box"});

        return result;
    }
}
