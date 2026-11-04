package rosa.archive.aor;

import org.junit.jupiter.api.Test;
import rosa.archive.core.FileSystemArchiveStore;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.IdLocations;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AorReferenceResolver}.
 */
class AorReferenceResolverTest {

    private static ImageList images(String book, String... pages) {
        ImageList list = new ImageList();
        for (String page : pages) {
            String id = page.startsWith("+") ? page.substring(1) : book + "." + page + ".tif";
            list.getImages().add(new BookImage(id, 100, 100, false));
        }
        return list;
    }

    private static ImageList withLabel(ImageList list, int index, String label) {
        list.getImages().get(index).setName(label);
        return list;
    }

    private static IdLocations sheet(String... rows) {
        IdLocations l = new IdLocations();
        for (String row : rows) {
            String[] c = row.split(",", -1);
            l.add(c[0], new IdLocations.IdLocation("aor", c[1], c[2], c.length > 3 ? c[3] : ""));
        }
        return l;
    }

    private final Map<String, ImageList> lists = Map.of(
            "BL615d7", withLabel(images("BL615d7", "001r", "001v", "035r", "035v", "+BL615d7.binding.frontcover.tif"), 4, "binding front cover"),
            "RCPD1266", images("RCPD1266", "001r", "037v", "038r"),
            "FolgersHa2", images("FolgersHa2", "005v", "007r"),
            "NYSLWin188", withLabel(images("NYSLWin188", "+NYSLWin188.frontmatter.flyleaf.005r.tif", "001r"), 0, "front matter 5r"));

    private final AorReferenceResolver resolver = new AorReferenceResolver(sheet(
            "BL615d7:615_d_7_f035r,BL615d7,35r",
            "BL615d7:615_d_7_f035r:m0001,BL615d7,35r,BL615d7:615_d_7_f035r:m0001",
            "BL615d7:615_d_7_fblefr,BL615d7,binding front cover",
            "RCPD1266:084,RCPD1266,37v",
            "RCPD1266:002,RCPD1266,RCPD1266.038r.tif",
            "NYSLWin188:00000011,NYSLWin188,front matter 5r",
            "NYSLWin188:00000099,NYSLWin188,no such page",
            "Ghost:1,Ghost,1r",
            "BL615d7:nothing,BL615d7,"),
            lists.keySet(), lists::get);

    private static void assertTarget(Optional<AorReferenceResolver.Target> target, String book, String image, int index) {
        assertTrue(target.isPresent(), "expected a target");
        assertEquals(book, target.get().bookId());
        assertEquals(image, target.get().imageId());
        assertEquals(index, target.get().index());
    }

    @Test
    void resolvesIdsThroughTheSheet() {
        assertTarget(resolver.resolve("BL615d7:615_d_7_f035r"), "BL615d7", "BL615d7.035r.tif", 2);
        assertTarget(resolver.resolve("BL615d7:615_d_7_f035r:m0001"), "BL615d7", "BL615d7.035r.tif", 2);
        // An annotation suffix the sheet does not list is dropped
        assertTarget(resolver.resolve("BL615d7:615_d_7_f035r:u0003"), "BL615d7", "BL615d7.035r.tif", 2);
        // A page given as an image file
        assertTarget(resolver.resolve("RCPD1266:002"), "RCPD1266", "RCPD1266.038r.tif", 2);
        assertEquals("035r", resolver.resolve("BL615d7:615_d_7_f035r").get().canvasName());
    }

    @Test
    void forgivesDotsInTheBookAndPaddingOfTheNumber() {
        // RCPD126.6:84 is RCPD1266:084 in the sheet
        assertTarget(resolver.resolve("RCPD126.6:84"), "RCPD1266", "RCPD1266.037v.tif", 1);
        assertTarget(resolver.resolve("rcpd1266:84"), "RCPD1266", "RCPD1266.037v.tif", 1);
        assertTarget(resolver.resolve(" RCPD1266:0084 "), "RCPD1266", "RCPD1266.037v.tif", 1);
    }

    @Test
    void matchesPagesByLabel() {
        assertTarget(resolver.resolve("NYSLWin188:00000011"), "NYSLWin188", "NYSLWin188.frontmatter.flyleaf.005r.tif", 0);
        assertEquals("frontmatter.flyleaf.005r", resolver.resolve("NYSLWin188:00000011").get().canvasName());
        assertTarget(resolver.resolve("BL615d7:615_d_7_fblefr"), "BL615d7", "BL615d7.binding.frontcover.tif", 4);
    }

    @Test
    void matchesFlyleavesPastedownsAndCoversByTheirOldLabels() {
        ImageList list = images("NY", "+NY.binding.frontcover.tif", "+NY.frontmatter.pastedown.tif",
                "+NY.frontmatter.flyleaf.005r.tif", "001r", "+NY.endmatter.flyleaf.002v.tif", "+NY.binding.backcover.tif");
        list.getImages().get(2).setName("title page"); // the label the sheet knew has since changed
        AorReferenceResolver r = new AorReferenceResolver(sheet(
                "NY:1,NY,front matter 5r", "NY:2,NY,front matter pastedown", "NY:3,NY,binding front cover",
                "NY:4,NY,End matter 2v", "NY:5,NY,Binding back cover"),
                List.of("NY"), _ -> list);
        assertTarget(r.resolve("NY:1"), "NY", "NY.frontmatter.flyleaf.005r.tif", 2);
        assertTarget(r.resolve("NY:2"), "NY", "NY.frontmatter.pastedown.tif", 1);
        assertTarget(r.resolve("NY:3"), "NY", "NY.binding.frontcover.tif", 0);
        assertTarget(r.resolve("NY:4"), "NY", "NY.endmatter.flyleaf.002v.tif", 4);
        assertTarget(r.resolve("NY:5"), "NY", "NY.binding.backcover.tif", 5);
        assertEquals("frontmatter.flyleaf.005r", r.resolve("NY:1").get().canvasName());
    }

    @Test
    void fallsBackToAFolioInTheToken() {
        assertTarget(resolver.resolve("FolgersHa2:Ha2.007r"), "FolgersHa2", "FolgersHa2.007r.tif", 1);
        assertTarget(resolver.resolve("FolgersHa2:Ha2.005v.tif"), "FolgersHa2", "FolgersHa2.005v.tif", 0);
        assertTarget(resolver.resolve("BL615d7:c_120_b_(2)_f035v"), "BL615d7", "BL615d7.035v.tif", 3);
    }

    @Test
    void reportsWhyARefDoesNotResolve() {
        assertEquals("empty ref", resolver.resolution("").reason());
        assertEquals("empty ref", resolver.resolution(null).reason());
        assertEquals("not of the form book:page", resolver.resolution("justaword").reason());
        assertEquals("unknown book 'Nope'", resolver.resolution("Nope:1").reason());
        assertEquals("not in id_locations.csv", resolver.resolution("BL615d7:615_d_7_f099r").reason());
        assertTrue(resolver.resolution("NYSLWin188:00000099").reason().startsWith("id_locations.csv page 'no such page' is not an image"));
        assertTrue(resolver.resolution("BL615d7:nothing").reason().startsWith("id_locations.csv has no page"));
        assertTrue(resolver.resolution("Ghost:1").reason().contains("unknown book"));
        assertFalse(resolver.resolution("Ghost:1").resolved());
    }

    @Test
    void worksWithoutASheet() {
        AorReferenceResolver bare = new AorReferenceResolver(null, lists.keySet(), lists::get);
        assertTarget(bare.resolve("FolgersHa2:Ha2.007r"), "FolgersHa2", "FolgersHa2.007r.tif", 1);
        assertEquals("no id_locations.csv", bare.resolution("RCPD1266:84").reason());
    }

    @Test
    void usesImageListsGivenUpFront() {
        List<String> asked = new ArrayList<>();
        AorReferenceResolver r = new AorReferenceResolver(sheet("X:1,X,1r"), List.of("X"), b -> {
            asked.add(b);
            return null;
        });
        r.putImages("X", images("X", "001r"));
        assertTarget(r.resolve("X:1"), "X", "X.001r.tif", 0);
        assertTrue(asked.isEmpty(), "image list was read although it was given");
    }

    @Test
    void canvasNamesFollowTheViewer() {
        assertEquals("037v", new AorReferenceResolver.Target("RCPD1266", "RCPD1266.037v.tif", 83).canvasName());
        assertEquals("misc.insert.07", new AorReferenceResolver.Target("UCLOgden", "UCLOgden.misc.insert.07.tif", 500).canvasName());
        assertEquals("i14", new AorReferenceResolver.Target("X", "missing_image.tif", 14).canvasName());
    }

    @Test
    void resolvesTheFixtureArchive() throws Exception {
        Path archive = Path.of(getClass().getClassLoader().getResource("archive").toURI());
        FileSystemArchiveStore store = new FileSystemArchiveStore(archive);
        BookCollection aor = store.loadCollection("aor");
        assertNotNull(aor.getIdLocations());
        AorReferenceResolver r = new AorReferenceResolver(aor.getIdLocations(), store.listBooks("aor"), book -> {
            try {
                return rosa.archive.core.serialize.ArchiveReaders.readImageList(
                        store.bookFile("aor", book, book + ".images.csv"), new ArrayList<>());
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
        assertTarget(r.resolve("PrincetonU101:00000034"), "PrincetonU101", "PrincetonU101.014v.tif", 33);
        assertEquals("014v", r.resolve("PrincetonU101:00000034").get().canvasName());
        assertTarget(r.resolve("PrincetonU101:00000136"), "PrincetonU101", "PrincetonU101.065v.tif", 135);
    }
}
