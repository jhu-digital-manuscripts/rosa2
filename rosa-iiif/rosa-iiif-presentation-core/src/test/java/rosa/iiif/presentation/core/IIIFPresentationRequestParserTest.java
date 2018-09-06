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
        assertNull(parser.parsePresentationRequest("/name/blah"));
    }
    
    @Test
    public void testParsePresentationRequest() {
        PresentationRequest result;
        PresentationRequest expected;
       
        result = parser.parsePresentationRequest("/rose/Douce195/manifest");
        expected = new PresentationRequest(PresentationRequestType.MANIFEST, "rose", "Douce195");
        assertEquals(expected, result);
       
        result = parser.parsePresentationRequest("/rose/collection");
        expected = new PresentationRequest(PresentationRequestType.COLLECTION, "rose");
        assertEquals(expected, result);
        
        result = parser.parsePresentationRequest("/rose/Douce195/1r/canvas");
        expected = new PresentationRequest(PresentationRequestType.CANVAS, "rose", "Douce195", "1r");
        assertEquals(expected, result);
        
        result = parser.parsePresentationRequest("/rose/Douce195/1r/trans1/annotation");
        expected = new PresentationRequest(PresentationRequestType.ANNOTATION, "rose", "Douce195", "1r", "trans1");
        assertEquals(expected, result);        
        
        result = parser.parsePresentationRequest("/rose/Douce195/1r/transcription/annotations");
        expected = new PresentationRequest(PresentationRequestType.ANNOTATION_LIST, "rose", "Douce195", "1r", "transcription");
        assertEquals(expected, result);        
        
        result = parser.parsePresentationRequest("/rose/jhsearch");
        expected = new PresentationRequest(PresentationRequestType.JHSEARCH, "rose");
        assertEquals(expected, result);        
        
        result = parser.parsePresentationRequest("/rose/Douce195/base/sequence");
        expected = new PresentationRequest(PresentationRequestType.SEQUENCE, "rose", "Douce195", "base");
        assertEquals(expected, result);
        
        result = parser.parsePresentationRequest("/rose/Douce195/top/range");
        expected = new PresentationRequest(PresentationRequestType.RANGE, "rose", "Douce195", "top");
        assertEquals(expected, result);
    }    
}
