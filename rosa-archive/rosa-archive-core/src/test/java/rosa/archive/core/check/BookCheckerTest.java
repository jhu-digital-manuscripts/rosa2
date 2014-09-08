package rosa.archive.core.check;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import rosa.archive.core.AbstractFileSystemTest;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.GuiceJUnitRunner;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.config.AppConfig;
import rosa.archive.model.Book;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookScene;
import rosa.archive.model.BookStructure;
import rosa.archive.model.BookText;
import rosa.archive.model.ChecksumData;
import rosa.archive.model.ChecksumInfo;
import rosa.archive.model.CropData;
import rosa.archive.model.CropInfo;
import rosa.archive.model.HashAlgorithm;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeTagging;
import rosa.archive.model.StructurePage;
import rosa.archive.model.StructurePageSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
* @see rosa.archive.core.check.BookChecker
*/
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class BookCheckerTest extends AbstractFileSystemTest {
    @Before
    public void setup() {
        super.setup();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkContentTest() {
        BookChecker bChecker = new BookChecker(new AppConfig());

        // TODO
        //assertTrue(bChecker.checkContent(createBook(), base, true));
        //assertFalse(bChecker.checkContent(createBadBook(), base, true));
        //assertTrue(bChecker.checkContent(createBook(), base, false));
        //assertFalse(bChecker.checkContent(createBadBook(), base, false));
    }

    private Book createBook() {
        Book book = new Book();
        book.setId("BookId");
        List<String> content = new ArrayList<>();

        // Metadata
        BookMetadata metadata = new BookMetadata();
        metadata.setId("BookId.description_en.csv");
        metadata.setCommonName("Common Name");
        metadata.setShelfmark("Shelfmark");
        metadata.setYearStart(0);
        metadata.setYearEnd(1);
        metadata.setCurrentLocation("Baltimore, MD");
        metadata.setDate("Date");
        metadata.setDimensions("2x2");
        metadata.setHeight(2);
        metadata.setWidth(2);
        metadata.setMaterial("material");
        metadata.setNumberOfIllustrations(111);
        metadata.setNumberOfPages(10);
        metadata.setOrigin("Origin");
        metadata.setRepository("Repository");
        metadata.setType("Type");

        BookText text = new BookText();
        text.setId("BookTextId");
        text.setFirstPage("firstPage");
        text.setLastPage("lastPage");
        text.setNumberOfIllustrations(101);
        text.setNumberOfPages(11);
        text.setColumnsPerPage(2);
        text.setLeavesPerGathering(4);
        text.setLinesPerColumn(50);
        text.setTitle("BookTextTitle");

        metadata.setTexts(new BookText[] {text});

        content.add(metadata.getId());
        book.addBookMetadata(metadata, "en");
        book.addBookMetadata(metadata, "fr");

        // Images
        ImageList images = new ImageList();
        ImageList cropped = new ImageList();
        images.setId("BookId.images.csv");
        cropped.setId("BookId.images.crop.csv");
        for (int i = 0; i < 10; i++) {
            BookImage image = new BookImage();
            image.setId("BookId.00" + i + "v.tif");
            image.setMissing(false);
            image.setWidth(100);
            image.setHeight(100);
            images.getImages().add(image);
            BookImage image1 = new BookImage();
            image1.setId("BookId.00" + i + "r.tif");
            image1.setMissing(false);
            image1.setWidth(100);
            image1.setHeight(100);
            images.getImages().add(image1);

            // cropped images marked as missing
            BookImage crop = new BookImage();
            crop.setId("CroppedImage.00" + i + "r.tif");
            crop.setMissing(true);
            crop.setHeight(95);
            crop.setWidth(95);
            cropped.getImages().add(crop);

            content.add(image.getId());
            content.add(image1.getId());
        }
        content.add(images.getId());
        content.add(cropped.getId());
        book.setImages(images);
        book.setCroppedImages(cropped);

        // Crop info
        CropInfo cropInfo = new CropInfo();
        cropInfo.setId("BookId.crop.txt");
        for (int i = 0; i < 10; i++) {
            CropData data = new CropData();
            data.setId("BookId.00" + i + "v.tif");
            data.setLeft(.01);
            data.setRight(.02);
            data.setTop(0.03);
            data.setBottom(.04);

            cropInfo.addCropData(data);
        }
        content.add(cropInfo.getId());
        book.setCropInfo(cropInfo);

        // reduced tagging
        BookStructure structure = new BookStructure();
        structure.setId("BookId.redtag.csv");
        List<StructurePage> pages = structure.pages();
        for (int i = 1; i < 10; i++) {
            StructurePage page = new StructurePage();
            page.setId(String.valueOf(i));
            page.setName(String.valueOf(i));
            page.setRecto(new StructurePageSide(page.getId() + "r", 10));
            page.setVerso(new StructurePageSide(page.getId() + "v", 10));

            pages.add(page);
        }
        content.add(structure.getId());
        book.setBookStructure(structure);

        // Illustration tagging
        IllustrationTagging ilTag = new IllustrationTagging();
        ilTag.setId("BookId.imagetag.csv");
        for (int i = 0; i < 15; i++) {
            Illustration ill = new Illustration();
            ill.setId(String.valueOf(i));
            ill.setPage("1v");
            ill.setInitials("Initials");
            ill.setCharacters(new String[] {"1", "2"});
            ill.setTitles(new String[] {"1", "2"});

            ilTag.addIllustrationData(ill);
        }
        content.add(ilTag.getId());
        book.setIllustrationTagging(ilTag);

        // Manual narrative tagging
        NarrativeTagging manNarTag = new NarrativeTagging();
        NarrativeTagging autNarTag = new NarrativeTagging();
        manNarTag.setId("BookId.nartag.txt");
        autNarTag.setId("BookId.nartag.csv");
        List<BookScene> manScenes = manNarTag.getScenes();
        List<BookScene> autScenes = autNarTag.getScenes();
        for (int i = 0; i < 10; i++) {
            BookScene scene = new BookScene();
            scene.setId("scene" + i);
            scene.setStartPage("1r");
            scene.setEndPage("3r");
            manScenes.add(scene);

            BookScene auto = new BookScene();
            auto.setId("auto-scene" + i);
            auto.setStartPage("2v");
            auto.setEndPage("4v");
            autScenes.add(auto);
        }
        content.add(manNarTag.getId());
        content.add(autNarTag.getId());
        book.setManualNarrativeTagging(manNarTag);
        book.setAutomaticNarrativeTagging(autNarTag);

        // Checksum info
        ChecksumInfo checksums = new ChecksumInfo();
        checksums.setId("BookId.SHA1SUM");
        for (String filename : content) {
            ChecksumData data = new ChecksumData();
            data.setId(filename);
            data.setAlgorithm(HashAlgorithm.SHA1);
            data.setHash("12341234abaa");

            checksums.addChecksum(data);
        }
        content.add(checksums.getId());
        book.setChecksumInfo(checksums);

        book.setContent(content.toArray(new String[content.size()]));
        return book;
    }

    private Book createBadBook() {
        Book badBook = new Book();
        List<String> content = new ArrayList<>();

        // Metadata
        BookMetadata metadata = new BookMetadata();
        metadata.setCommonName("Common Name");

        BookText text = new BookText();
        text.setTitle("BookTextTitle");
        metadata.setTexts(new BookText[] {text});

        badBook.addBookMetadata(metadata, "asdf");

        // Images
        ImageList images = new ImageList();
        ImageList cropped = new ImageList();
        for (int i = 0; i < 10; i++) {
            BookImage image = new BookImage();
            image.setId("BookImage" + i);
            images.getImages().add(image);

            BookImage crop = new BookImage();
            crop.setId("CroppedImage" + i);
            crop.setMissing(true);
            cropped.getImages().add(crop);

            content.add(image.getId());
            // Source of errors (10)
            content.add(crop.getId());
        }
        badBook.setImages(images);
        badBook.setCroppedImages(cropped);

        badBook.setContent(content.toArray(new String[content.size()]));
        return badBook;
    }

}
