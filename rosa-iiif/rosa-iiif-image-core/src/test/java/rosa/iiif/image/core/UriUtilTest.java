package rosa.iiif.image.core;

import static org.junit.Assert.*;

import java.net.URISyntaxException;

import org.junit.Test;

public class UriUtilTest {

    @Test
    public void testEncodePathSegment() throws URISyntaxException {
        assertEquals("", UriUtil.encodePathSegment(""));        
        assertEquals("test", UriUtil.encodePathSegment("test"));        
        assertEquals("m%20oo", UriUtil.encodePathSegment("m oo"));
        assertEquals("h%25", UriUtil.encodePathSegment("h%"));
        assertEquals("%2fhm%2fblah%2f", UriUtil.encodePathSegment("/hm/blah/"));        
        assertEquals("http:%2f%2f%23", UriUtil.encodePathSegment("http://#"));
        assertEquals("urn:sici:1046-8188(199501)13:1%253C69:FTTHBI%253E2.0.TX;2-4", UriUtil.encodePathSegment("urn:sici:1046-8188(199501)13:1%3C69:FTTHBI%3E2.0.TX;2-4"));
    }
    
    @Test
    public void testDecodePathSegment() throws URISyntaxException {
        assertEquals("", UriUtil.decodePathSegment(""));        
        assertEquals("test", UriUtil.decodePathSegment("test"));        
        assertEquals("m oo", UriUtil.decodePathSegment("m%20oo"));
        assertEquals("h%", UriUtil.decodePathSegment("h%25"));
        assertEquals("/hm/blah/", UriUtil.decodePathSegment("%2fhm%2fblah%2f"));        
        assertEquals("http://#", UriUtil.decodePathSegment("http:%2f%2f%23"));
        assertEquals("urn:sici:1046-8188(199501)13:1%3C69:FTTHBI%3E2.0.TX;2-4", UriUtil.decodePathSegment("urn:sici:1046-8188(199501)13:1%253C69:FTTHBI%253E2.0.TX;2-4"));
    }
    
    @Test
    public void testIsValidEncodedPath() {
        assertTrue(UriUtil.isValidEncodedPath("/cow"));
        assertTrue(UriUtil.isValidEncodedPath("/"));
        assertFalse(UriUtil.isValidEncodedPath("[moo]"));
        assertFalse(UriUtil.isValidEncodedPath("moo cow"));
        assertFalse(UriUtil.isValidEncodedPath("gr`r"));
    }
}
