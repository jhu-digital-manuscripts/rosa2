package rosa.iiif.presentation.core;

import static org.junit.Assert.*;

import org.junit.Test;

import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;

public class IIIFPresentationRequestParserTest {
    private IIIFPresentationRequestParser parser = new IIIFPresentationRequestParser();
    
    @Test
    public void testParseMalformedPresentationRequest() {
        assertNull(parser.parsePresentationRequest("lkasjdf"));
        assertNull(parser.parsePresentationRequest(""));
        assertNull(parser.parsePresentationRequest("lkj;lka../a;/z'"));
        assertNull(parser.parsePresentationRequest("%2F/;"));
        assertNull(parser.parsePresentationRequest("/rosecollection.Douce195/manifest/jhsearch"));
    }
    
    @Test
    public void testParsePresentationRequest() {
        PresentationRequest result;
        PresentationRequest expected;
       
        result = parser.parsePresentationRequest("/rosecollection.Douce195/manifest");
        expected = new PresentationRequest("rosecollection.Douce195", null, PresentationRequestType.MANIFEST);       
        assertEquals(expected, result);
       
        result = parser.parsePresentationRequest("/collection/rosecollection");
        expected = new PresentationRequest(null, "rosecollection", PresentationRequestType.COLLECTION);       
        assertEquals(expected, result);
        
        result = parser.parsePresentationRequest("/moo/canvas/blah");
        expected = new PresentationRequest("moo", "blah", PresentationRequestType.CANVAS);       
        assertEquals(expected, result);
        
        result = parser.parsePresentationRequest("/moo/sequence/blah");
        expected = new PresentationRequest("moo", "blah", PresentationRequestType.SEQUENCE);       
        assertEquals(expected, result);        
    }    
}
