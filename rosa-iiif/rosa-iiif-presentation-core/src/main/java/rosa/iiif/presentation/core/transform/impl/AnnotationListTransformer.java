package rosa.iiif.presentation.core.transform.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Marginalia;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.AnnotationListType;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.annotation.Annotation;

import java.util.List;

public class AnnotationListTransformer extends BasePresentationTransformer implements Transformer<AnnotationList> {

    private AnnotationTransformer annotationTransformer;

    @Inject
    public AnnotationListTransformer(@Named("formatter.presentation") IIIFPresentationRequestFormatter presRequestFormatter,
                                     AnnotationTransformer annotationTransformer) {
        super(presRequestFormatter);
        this.annotationTransformer = annotationTransformer;
    }

    public AnnotationList transform(BookCollection collection, Book book, String name) {
        String page = get_annotation_list_page(name);
        String listType = get_annotation_list_id(name);

        // TODO need better way of finding a page
        BookImage pageImage = getPageImage(book.getImages(), page);
        if (pageImage == null) {
            return null;
        }

        AnnotatedPage aPage = book.getAnnotationPage(pageImage.getId());
        AnnotationListType type = AnnotationListType.getType(listType);

        if (type == AnnotationListType.ALL) {
            return annotationList(collection, book, pageImage, aPage);
        }
        return annotationList(collection, book, pageImage, aPage, type);
    }

    @Override
    public Class<AnnotationList> getType() {
        return AnnotationList.class;
    }

    private String[] split_id(String id) {
        String[] parts = id.split("\\.");

        if (parts.length != 2) {
            return null;
        }

        return parts;
    }

    private String get_annotation_list_page(String name) {
        String[] parts = split_id(name);

        if (parts == null) {
            return null;
        }

        return parts[0];
    }

    private String get_annotation_list_id(String name) {
        String[] parts = split_id(name);

        if (parts == null) {
            return null;
        }

        return parts[1];
    }

    private BookImage getPageImage(ImageList images, String page) {
        for (BookImage image : images) {
            if (image.getName().equals(page)) {
                return image;
            }
        }

        return null;
    }

    private AnnotationList annotationList(BookCollection collection, Book book, BookImage image, AnnotatedPage aPage,
                                          AnnotationListType listType) {
        AnnotationList list = new AnnotationList();

        String label = annotationListName(image.getName(), listType.toString().toLowerCase());
        list.setId(urlId(collection.getId(), book.getId(), annotationListName(image.getName(),
                listType.toString().toLowerCase()), PresentationRequestType.ANNOTATION_LIST));
        list.setType(SC_ANNOTATION_LIST);
        list.setDescription("Annotation list for " + listType.toString().toLowerCase() + " on page "
                + image.getName(), "en");
        list.setLabel(label, "en");
        list.setWithin(urlId(collection.getId(), book.getId(), listType.toString().toLowerCase(),
                PresentationRequestType.LAYER));

        List<Annotation> annotations = list.getAnnotations();

        // Illustrations annotations do not need Annotated Page TODO refactor to have less confusing returns
        if (listType == AnnotationListType.ILLUSTRATION) {
            List<Annotation> anns = annotationTransformer.illustrationsForPage(collection, book, image);
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
                for (Marginalia marg : aPage.getMarginalia()) {
                    annotations.add(annotationTransformer.transform(collection, book, marg));
                }
                break;
            case SYMBOL:
                for (rosa.archive.model.aor.Annotation ann : aPage.getSymbols()) {
                    annotations.add(annotationTransformer.transform(collection, book, ann));
                }
                break;
//            case MARK:
//                for (rosa.archive.model.aor.Annotation ann : aPage.getMarks()) {
//                    annotations.add(adaptAnnotation(collection, book.getId(), ann, image));
//                }
//                break;
//            // Underlines will not become annotations.
//            case UNDERLINE:
//                for (rosa.archive.model.aor.Annotation ann : aPage.getUnderlines()) {
//                    annotations.add(adaptAnnotation(collection, book.getId(), ann, image));
//                }
//                break;
//            case NUMBERAL:
//                for (rosa.archive.model.aor.Annotation ann : aPage.getNumerals()) {
//                    annotations.add(adaptAnnotation(collection, book.getId(), ann, image));
//                }
//                break;
//            case ERRATA:
//                for (rosa.archive.model.aor.Annotation ann : aPage.getErrata()) {
//                    annotations.add(adaptAnnotation(collection, book.getId(), ann, image));
//                }
//                break;
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
        String type = AnnotationListType.ALL.toString().toLowerCase();
        String name = annotationListName(image.getName(), type);

        list.setId(urlId(collection.getId(), book.getId(), name, PresentationRequestType.ANNOTATION_LIST));
        list.setType(SC_ANNOTATION_LIST);
        list.setDescription("Annotation list for " + type + " on page " + image.getName(), "en");
        list.setLabel(name, "en");
        list.setWithin(urlId(collection.getId(), book.getId(), "all", PresentationRequestType.LAYER));

        return list;
    }

    private String annotationListName(String page, String listType) {
        return page + (listType == null ? "" : "." + listType);
    }

}
