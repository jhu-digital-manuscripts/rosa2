package rosa.archive.core.serialize;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import rosa.archive.model.HTMLAnnotations;

public class HtmlAnnotationsSerializerTest {
	@Test
	public void testSerializerLoad() throws IOException {
		HTMLAnnotationsSerializer serializer = new HTMLAnnotationsSerializer();
		
		try (InputStream is = getClass().getResourceAsStream("/archive/valid/annos.jsonld")) {
			HTMLAnnotations annos = serializer.read(is, null);
			
			assertEquals(1, annos.size());
			
			String anno = annos.getAnnotation("LudwigXV7.003r.tif");
			
			assertEquals("<div>Here is an annotation</div>", anno);
		}
	}
}
