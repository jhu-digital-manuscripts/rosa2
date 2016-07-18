package rosa.archive.aor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import rosa.archive.aor.AnnotationStatsWriter.AnnotationStats;
import rosa.archive.core.ResourceUtil;

/**
 * Do some simple testing of Annotation Stats to make sure they are sane.
 */
public class AnnotationStatsWriterTest {
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private Path base_archive_path;
	private Path folgers_ha2_path;
	private String folgers_ha2_id = "FolgersHa2";
	
	private AnnotationStatsWriter asw = new AnnotationStatsWriter();

	@Before
	public void setup() throws Exception {
		Path temp = tempFolder.newFolder().toPath();
		
		ResourceUtil.copyResource(AnnotationStatsWriterTest.class, "/archive/valid", temp);
		
		base_archive_path = temp.resolve("valid");
		folgers_ha2_path = base_archive_path.resolve(folgers_ha2_id);
	}
	
	private long count_type(List<AnnotationStats> stats, String type) {
		return stats.stream().filter(as -> as.type.equals(type)).count();
	}
	
	private long count_person(List<AnnotationStats> stats, String person) {
		return stats.stream().filter(as -> as.marginalia_people  != null && as.marginalia_people.contains(person)).count();
	}
	
	private long count_lang(List<AnnotationStats> stats, String lang) {
		return stats.stream().filter(as -> as.lang  != null && as.lang.contains(lang)).count();
	}
	
	private long count_place(List<AnnotationStats> stats, String place) {
		return stats.stream().filter(as -> as.marginalia_places  != null && as.marginalia_places.contains(place)).count();
	}
	
	private long count_method(List<AnnotationStats> stats, String method) {
		return stats.stream().filter(as -> as.method  != null && as.method.equals(method)).count();
	}
	
	private long count_name(List<AnnotationStats> stats, String name) {
		return stats.stream().filter(as -> as.name  != null && as.name.equals(name)).count();
	}

	/**
	 * Test results on Folgers 1r transcription.
	 */
	@Test
	public void testGetStatsFolgers1r() throws IOException {
		String xml_id = "FolgersHa2.aor.001r.xml";
		String image_id = "FolgersHa2.001r.tif";
		
		List<AnnotationStats> result = asw.collectStats(folgers_ha2_id, folgers_ha2_path.resolve(xml_id));
		
		assertNotNull(result);
		assertEquals(42, result.size());
		
		assertEquals(1, count_type(result, "symbol"));
		assertEquals(16, count_type(result, "mark"));
		assertEquals(8, count_type(result, "marginalia"));
		assertEquals(17, count_type(result, "underline"));
		
		assertEquals(2, count_person(result, "Geoffrey Chaucer"));
		
		assertEquals(1, count_place(result, "Rome"));
		assertEquals(1, count_place(result, "Athens"));
		
		assertEquals(33, count_method(result, "pen"));
		
		assertEquals(6, count_name(result, "plus_sign"));		
		assertEquals(1, count_name(result, "Sun"));
		
		assertEquals(24, count_lang(result, "IT"));
		assertEquals(6, count_lang(result, "LA"));
		
		result.forEach(as -> assertEquals(folgers_ha2_id, as.book_id));
		result.forEach(as -> assertEquals(image_id, as.image_id));
	}
}
