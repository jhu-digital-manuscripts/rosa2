//package rosa.archive.core.check;
//
//import org.junit.Before;
//import org.junit.Test;
//import rosa.archive.model.Book;
//import rosa.archive.model.BookImage;
//import rosa.archive.model.BookMetadata;
//import rosa.archive.model.BookText;
//import rosa.archive.model.ImageList;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertTrue;
//
///**
// * @see rosa.archive.core.check.BookChecker
// */
//public class BookCheckerTest {
//
//    private Checker<Book> bookChecker;
//
//    @Before
//    public void setup() {
//        this.bookChecker = new BookChecker();
//    }
//
//    @Test
//    public void checkBitsTest() {
//        assertFalse(bookChecker.checkBits(createBook()));
//    }
//
//    @Test
//    public void checkContentTest() {
//        assertTrue(bookChecker.checkContent(createBook()));
//        assertFalse(bookChecker.checkContent(createBadBook()));
//    }
//
//    private Book createBook() {
//        Book book = new Book();
//        List<String> content = new ArrayList<>();
//
//        // Metadata
//        BookMetadata metadata = new BookMetadata();
//        metadata.setCommonName("Common Name");
//        metadata.setShelfmark("Shelfmark");
//        metadata.setYearStart(0);
//        metadata.setYearEnd(1);
//        metadata.setCurrentLocation("Baltimore, MD");
//        metadata.setDate("Date");
//        metadata.setDimensions("2x2");
//        metadata.setHeight(2);
//        metadata.setWidth(2);
//        metadata.setMaterial("material");
//        metadata.setNumberOfIllustrations(111);
//        metadata.setNumberOfPages(10);
//        metadata.setOrigin("Origin");
//        metadata.setRepository("Repository");
//        metadata.setType("Type");
//
//        BookText text = new BookText();
//        text.setId("BookTextId");
//        text.setFirstPage("firstPage");
//        text.setLastPage("lastPage");
//        text.setNumberOfIllustrations(101);
//        text.setNumberOfPages(11);
//        text.setColumnsPerPage(2);
//        text.setLeavesPerGathering(4);
//        text.setLinesPerColumn(50);
//        text.setTitle("BookTextTitle");
//
//        metadata.setTexts(new BookText[] {text});
//
//        book.setBookMetadata(metadata);
//
//        // Images
//        ImageList images = new ImageList();
//        ImageList cropped = new ImageList();
//        for (int i = 0; i < 10; i++) {
//            BookImage image = new BookImage();
//            image.setId("BookImage" + i);
//            image.setMissing(false);
//            image.setWidth(100);
//            image.setHeight(100);
//            images.getImages().add(image);
//
//            BookImage crop = new BookImage();
//            crop.setId("CroppedImage" + i);
//            crop.setMissing(true);
//            crop.setHeight(95);
//            crop.setWidth(95);
//            cropped.getImages().add(crop);
//
//            content.add(image.getId());
//        }
//        book.setImages(images);
//        book.setCroppedImages(cropped);
//
//        book.setContent(content.toArray(new String[content.size()]));
//        return book;
//    }
//
//    private Book createBadBook() {
//        Book badBook = new Book();
//        List<String> content = new ArrayList<>();
//
//        // Metadata
//        BookMetadata metadata = new BookMetadata();
//        metadata.setCommonName("Common Name");
//
//        BookText text = new BookText();
//        text.setTitle("BookTextTitle");
//        metadata.setTexts(new BookText[] {text});
//
//        badBook.setBookMetadata(metadata);
//
//        // Images
//        ImageList images = new ImageList();
//        ImageList cropped = new ImageList();
//        for (int i = 0; i < 10; i++) {
//            BookImage image = new BookImage();
//            image.setId("BookImage" + i);
//            images.getImages().add(image);
//
//            BookImage crop = new BookImage();
//            crop.setId("CroppedImage" + i);
//            crop.setMissing(true);
//            cropped.getImages().add(crop);
//
//            content.add(image.getId());
//            // Source of errors (10)
//            content.add(crop.getId());
//        }
//        badBook.setImages(images);
//        badBook.setCroppedImages(cropped);
//
//        badBook.setContent(content.toArray(new String[content.size()]));
//        return badBook;
//    }
//
//}
