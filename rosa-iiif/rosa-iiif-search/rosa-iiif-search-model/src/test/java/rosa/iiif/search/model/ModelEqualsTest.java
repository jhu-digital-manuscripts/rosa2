package rosa.iiif.search.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

public class ModelEqualsTest {

    @Test
    public void testIIIFSearchRequest() {
        EqualsVerifier.forClass(IIIFSearchRequest.class).allFieldsShouldBeUsed()
                .usingGetClass()
//                .suppress(Warning.NONFINAL_FIELDS, Warning.STRICT_INHERITANCE)
                .verify();
    }

    @Test
    public void testRectangle() {
        EqualsVerifier.forClass(Rectangle.class).allFieldsShouldBeUsed()
                .usingGetClass()
                .verify();
    }

    @Test
    public void testIIIFSearchHit() {
        EqualsVerifier.forClass(IIIFSearchHit.class).allFieldsShouldBeUsed()
                .usingGetClass()
                .verify();
    }

    @Test
    public void testIIIFSearchResult() {
        EqualsVerifier.forClass(IIIFSearchResult.class).allFieldsShouldBeUsed()
                .suppress(Warning.NONFINAL_FIELDS)
                .usingGetClass()
                .verify();
    }

}
