package rosa.iiif.presentation.core.transform.impl;

import java.util.List;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.AnnotationListType;
import rosa.iiif.presentation.model.HtmlValue;
import rosa.iiif.presentation.model.annotation.Annotation;

public class AnnotationListTransformer implements TransformerConstants {
    private final AnnotationTransformer annotationTransformer;
    private final PresentationUris pres_uris;
    private final ArchiveNameParser nameParser;
    
    public AnnotationListTransformer(PresentationUris pres_uris,
                                     AnnotationTransformer annotationTransformer) {
        this.pres_uris = pres_uris;
        this.annotationTransformer = annotationTransformer;
        this.nameParser = new ArchiveNameParser();
    }

    public AnnotationList transform(BookCollection collection, Book book, String name) {
    	String[] parts = pres_uris.splitInflectedName(name);
    	String short_image_id = parts[0];
    	String list_type_name = parts[1];
    	
        BookImage pageImage = getPageImage(book, short_image_id);
        
        if (pageImage == null) {
            return null;
        }
        
        AnnotatedPage aPage = book.getAnnotationPage(pageImage.getId());
        AnnotationListType type = AnnotationListType.getType(list_type_name);

        if (type == null) {
        	return null;
        }
        
        if (type == AnnotationListType.ALL) {
            return annotationList(collection, book, pageImage, aPage);
        }
        return annotationList(collection, book, pageImage, aPage, type);
    }

    private BookImage getPageImage(Book book, String short_image_id) {
    	String imageId = nameParser.fullImageIdFromShortId(book.getId(), short_image_id);
    	
        for (BookImage image : book.getImages()) {
            if (image.getId().equals(imageId)) {
                return image;
            }
        }

        return null;
    }

    private AnnotationList annotationList(BookCollection collection, Book book, BookImage image, AnnotatedPage aPage,
                                          AnnotationListType listType) {
        AnnotationList list = new AnnotationList();

        String label = image.getName() + " " +  listType.toString().toLowerCase();
        
        list.setId(pres_uris.getAnnotationListURI(collection.getId(), book.getId(), image, listType));
        list.setType(SC_ANNOTATION_LIST);
        list.setDescription("Annotation list for " + listType.toString().toLowerCase() + " on page "
                + image.getName(), "en");
        list.setLabel(label, "en");
        if (aPage != null && aPage.getReader() != null && !aPage.getReader().isEmpty()) {
            list.getMetadata().put("reader", new HtmlValue(aPage.getReader()));
        }

        List<Annotation> annotations = list.getAnnotations();

        // Illustrations annotations do not need Annotated Page TODO refactor to have less confusing returns
        if (listType == AnnotationListType.ILLUSTRATION) {
            List<Annotation> anns = annotationTransformer.illustrationsForPage(collection, book, image);
            if (anns == null || anns.isEmpty()) {
                return null;
            }

            annotations.addAll(anns);
            return list;
        } else if (listType == AnnotationListType.ROSE_TRANSCRIPTION) {
            // Transcriptions formatted for Rose/Pizan
            List<Annotation> anns = annotationTransformer.roseTranscriptionOnPage(collection, book, image);
            if (anns == null || anns.isEmpty()) {
                return null;
            }

            annotations.addAll(anns);
            return list;
        }

        // Annotated page can be NULL if no transcriptions are present.
        if (aPage == null) {
            return null;
        }
        switch (listType) {
            case MARGINALIA:
                aPage.getMarginalia().forEach(marg ->  annotations.add(annotationTransformer.transform(collection, book, image, marg)) );
                break;
            case SYMBOL:
                aPage.getSymbols().forEach(symb -> annotations.add(annotationTransformer.transform(collection, book, image, symb)) );
                break;
            case GRAPH:
                aPage.getGraphs().forEach(graph -> annotations.add(annotationTransformer.transform(collection, book, image, graph)));
                break;
            case CALCULATION:
                aPage.getCalculations().forEach(c -> annotations.add(annotationTransformer.transform(collection, book, image, c)));
                break;
            case DRAWING:
                aPage.getDrawings().forEach(d -> annotations.add(annotationTransformer.transform(collection, book, image, d)));
                break;
            case TABLE:
                aPage.getTables().forEach(t -> annotations.add(annotationTransformer.transform(collection, book, image, t)));
                break;
            default:
                break;
        }

        return list;
    }

    /**
     * @param collection collection containing the book
     * @param book book that contains this annotation list
     * @param image image associated with this annotation list
     * @param aPage AoR annotations
     * @return top level annotation list for a page, containing all non-image annotations
     */
    private AnnotationList annotationList(BookCollection collection, Book book, BookImage image, AnnotatedPage aPage) {
        AnnotationList list = new AnnotationList();

        for (AnnotationListType type : AnnotationListType.values()) {
            AnnotationList l = annotationList(collection, book, image, aPage, type);

            if (l != null) {
                list.getAnnotations().addAll(l.getAnnotations());
            }
        }

        list.setId(pres_uris.getAnnotationListURI(collection.getId(), book.getId(), image, AnnotationListType.ALL));
        list.setType(SC_ANNOTATION_LIST);
        list.setDescription("Annotation list for everything on page " + image.getName(), "en");
        list.setLabel(image.getName() + " annotations", "en");
        if (aPage != null && aPage.getReader() != null && !aPage.getReader().isEmpty()) {
            list.getMetadata().put("reader", new HtmlValue(aPage.getReader()));
        }

        return list;
    }
}
