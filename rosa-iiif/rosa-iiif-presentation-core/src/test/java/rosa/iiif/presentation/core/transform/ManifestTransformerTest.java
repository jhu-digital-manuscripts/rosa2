package rosa.iiif.presentation.core.transform;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookText;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.Permission;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.meta.BiblioData;
import rosa.archive.model.meta.MultilangMetadata;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.ViewingDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ManifestTransformerTest {
    private static final String[] imageNames = {
            "BOOK.001r.tif", "BOOK.001v.tif", "BOOK.002r.tif", "BOOK.002v.tif",
            "BOOK.003r.tif", "BOOK.003v.tif", "BOOK.004r.tif", "BOOK.004v.tif",
            "BOOK.005r.tif", "BOOK.005v.tif", "BOOK.006r.tif", "BOOK.006v.tif",
            "BOOK.007r.tif", "BOOK.007v.tif", "BOOK.008r.tif", "BOOK.008v.tif"
    };

    private ManifestTransformer transformer;

    @Before
    public void setup() {
        transformer = new ManifestTransformer();
    }

    @Test
    public void test() {
        Manifest manifest = transformer.transform(createBookCollection(), createBook());
        assertNotNull(manifest);

        assertNotNull(manifest.getSequences());
        assertEquals(1, manifest.getSequences().size());
        assertNotNull(manifest.getDefaultSequence());

        // Test sequence
        Sequence seq = manifest.getSequences().get(manifest.getDefaultSequence());
        assertNotNull(seq);
        assertTrue(seq.getStartCanvas() != -1);
        assertEquals(ViewingDirection.LEFT_TO_RIGHT, seq.getViewingDirection());
        assertNotNull(seq.getId());

        // Test canvases
        List<Canvas> canvases = seq.getCanvases();
        assertNotNull(canvases);
        assertTrue(canvases.size() == 16);
    }

    private BookCollection createBookCollection() {
        BookCollection collection = new BookCollection();
        collection.setId("COLLECTION");

        collection.setBooks(new String[] {"BOOK"});
        collection.setLanguages(new String[] {"en", "fr"});
        collection.setCharacterNames(new CharacterNames());
        collection.setIllustrationTitles(new IllustrationTitles());
        collection.setNarrativeSections(new NarrativeSections());

        return collection;
    }

    private Book createBook() {
        Book book = new Book();
        book.setId("BOOK");

        book.setImages(imageList());
        book.setAnnotatedPages(annotatedPages());
        book.setMultilangMetadata(metadata());
        book.addPermission(permission(), "en");
        book.addPermission(permission(), "fr");

        return book;
    }

    private ImageList imageList() {
        ImageList list = new ImageList();
        list.setId("Book.images.csv");

        List<BookImage> images = list.getImages();
        for (String im : imageNames) {
            BookImage image = new BookImage();

            image.setId(im);
            image.setMissing(false);
            image.setWidth(1000);
            image.setHeight(1500);

            images.add(image);
        }

        list.setImages(images);
        return list;
    }

    private List<AnnotatedPage> annotatedPages() {
        List<AnnotatedPage> pages = new ArrayList<>();

        for (String image : imageNames) {
            AnnotatedPage page = new AnnotatedPage();

            page.setId(image.replace(".tif", ".xml"));
            page.setPage(image);

            for (int i = 0; i < 20; i++) {
                page.getUnderlines().add(new Underline("Text line", "pen", "TYPE", "en"));
                page.getUnderlines().add(new Underline("Text line", "pen", "TYPE", "fr"));

                page.getSymbols().add(new Symbol("Text line", "NAME", Location.INTEXT));
                page.getSymbols().add(new Symbol("Text line", "NAME", Location.LEFT_MARGIN));

                page.getMarks().add(new Mark("Text line", "NAME", "METHOD", "en", Location.INTEXT));
                page.getMarks().add(new Mark("Text line", "NAME", "METHOD", "fr", Location.RIGHT_MARGIN));

                page.getNumerals().add(new Numeral("Text line", Location.HEAD));

                page.getErrata().add(new Errata("Copy text", "amended text"));

                Marginalia marg = new Marginalia();
                marg.setTranslation("Translation");
                marg.setReferringText("Text line");
                marg.setHand("Harvey Dent");
                marg.setLocation(Location.TAIL);

                MarginaliaLanguage lang = new MarginaliaLanguage();
                Position pos = new Position();
                pos.getTexts().add("This is marginalia text.");
                lang.getPositions().add(pos);
                marg.getLanguages().add(lang);

                page.getMarginalia().add(marg);
            }
        }

        return pages;
    }

    private MultilangMetadata metadata() {
        MultilangMetadata metadata = new MultilangMetadata();
        metadata.setId("BOOK.description.xml");

        metadata.setNumberOfPages(100);
        metadata.setNumberOfIllustrations(10);
        metadata.setWidth(1000);
        metadata.setHeight(1500);
        metadata.setYearStart(1900);
        metadata.setYearEnd(1920);
        metadata.setDimensionUnits("mm");

        BiblioData en = new BiblioData();
        en.setCurrentLocation("Current Location");
        en.setTitle("BOOK TITLE");
        en.setLanguage("en");
        en.setMaterial("Material");
        en.setDateLabel("Date");
        en.setOrigin("Origin");
        en.setRepository("Repository");
        en.setShelfmark("Shelfmark");
        en.setType("Book");

        BiblioData fr = new BiblioData();
        fr.setCurrentLocation("Current Location");
        fr.setTitle("BOOK TITLE");
        fr.setLanguage("en");
        fr.setMaterial("Material");
        fr.setDateLabel("Date");
        fr.setOrigin("Origin");
        fr.setRepository("Repository");
        fr.setShelfmark("Shelfmark");
        fr.setType("Book");

        Map<String, BiblioData> dataMap = new HashMap<>();
        dataMap.put("en", en);
        dataMap.put("fr", fr);

        metadata.setBiblioDataMap(dataMap);

        BookText text1 = new BookText();
        text1.setColumnsPerPage(2);
        text1.setFirstPage("1");
        text1.setLastPage("100");
        text1.setNumberOfPages(100);
        text1.setNumberOfIllustrations(10);
        text1.setTitle("BOOK TITLE");
        text1.setTextId("text_id");

        metadata.getBookTexts().add(text1);

        return metadata;
    }

    private Permission permission() {
        Permission p = new Permission();
        p.setId("BOOK.permission.html");
        p.setPermission("This is a permission statement.");
        return p;
    }

}
