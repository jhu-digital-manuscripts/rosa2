package rosa.iiif.presentation.core.transform.impl;

import com.google.inject.Inject;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.AnnotationListType;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.selector.FragmentSelector;

import java.util.ArrayList;
import java.util.List;

public class AnnotationListTransformer extends BasePresentationTransformer implements Transformer<AnnotationList> {
    private static int annotation_counter = 0;

    @Inject
    public AnnotationListTransformer(IIIFRequestFormatter presRequestFormatter,
                                     ArchiveNameParser nameParser) {
        super(presRequestFormatter, nameParser);
    }

    public AnnotationList transform(BookCollection collection, Book book, String name) {
        String page = get_annotation_list_page(name);
        String listType = get_annotation_list_id(name);

        // TODO need better way of finding a page
        BookImage pageImage = getPageImage(book.getImages(), page);
        if (pageImage == null) {
            return null;
        }

        AnnotatedPage aPage = book.getAnnotationPage(page);
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
            if (image.getPage().equals(page)) {
                return image;
            }
        }

        return null;
    }

    private AnnotationList annotationList(BookCollection collection, Book book, BookImage image, AnnotatedPage aPage,
                                          AnnotationListType listType) {
        AnnotationList list = new AnnotationList();

        String label = annotationListName(image.getPage(), listType.toString().toLowerCase());
        list.setId(urlId(collection.getId(), book.getId(), annotationListName(image.getPage(),
                listType.toString().toLowerCase()), PresentationRequestType.ANNOTATION_LIST));
        list.setType(SC_ANNOTATION_LIST);
        list.setDescription("Annotation list for " + listType.toString().toLowerCase() + " on page "
                + image.getPage(), "en");
        list.setLabel(label, "en");

        List<Annotation> annotations = list.getAnnotations();

        // Illustrations annotations do not need Annotated Page TODO refactor to have less confusing returns
        if (listType == AnnotationListType.ILLUSTRATION) {
            List<Annotation> anns = illustrationsForPage(collection, book, image);
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
                    annotations.addAll(adaptMarginalia(collection, book.getId(), marg, image));
                }
                break;
            case MARK:
                for (rosa.archive.model.aor.Annotation ann : aPage.getMarks()) {
                    annotations.add(adaptAnnotation(collection, book.getId(), ann, image));
                }
                break;
            case SYMBOL:
                for (rosa.archive.model.aor.Annotation ann : aPage.getSymbols()) {
                    annotations.add(adaptAnnotation(collection, book.getId(), ann, image));
                }
                break;
            case UNDERLINE:
                for (rosa.archive.model.aor.Annotation ann : aPage.getUnderlines()) {
                    annotations.add(adaptAnnotation(collection, book.getId(), ann, image));
                }
                break;
            case NUMBERAL:
                for (rosa.archive.model.aor.Annotation ann : aPage.getNumerals()) {
                    annotations.add(adaptAnnotation(collection, book.getId(), ann, image));
                }
                break;
            case ERRATA:
                for (rosa.archive.model.aor.Annotation ann : aPage.getErrata()) {
                    annotations.add(adaptAnnotation(collection, book.getId(), ann, image));
                }
                break;
            default:
                break;
        }

        return list;
    }

    private List<Annotation> illustrationsForPage(BookCollection collection, Book book, BookImage image) {
        String page = image.getPage();
        if (book.getIllustrationTagging() == null) {
            return null;
        }

        List<Annotation> anns = new ArrayList<>();
        for (Illustration ill : book.getIllustrationTagging()) {
            String illusPage = nameParser.page(ill.getPage());
            if (!illusPage.equals(page)) {
                continue;
            }
            String anno_name = page + ".illustration_" + ill.getId();

            Annotation ann = new Annotation();
            ann.setLabel("Illustration(s) on " + page, "en");
            ann.setId(urlId(collection.getId(), book.getId(), anno_name, PresentationRequestType.ANNOTATION));
            ann.setMotivation(SC_PAINTING);
            ann.setType(OA_ANNOTATION);

            CharacterNames names = collection.getCharacterNames();
            IllustrationTitles titles = collection.getIllustrationTitles();

            // Resolve character name IDs (should be done in archive layer)
            StringBuilder sb_names = new StringBuilder();
            for (String name_id : ill.getCharacters()) {
                String name = names.getNameInLanguage(name_id, "en");

                sb_names.append(name == null ? name_id : name);
                if (!sb_names.toString().isEmpty()) {
                    sb_names.append(", ");
                } else {
                    sb_names.append(' ');
                }
            }

            // Resolve illustration title IDs (should be done in archive layer)
            StringBuilder sb_titles = new StringBuilder();
            for (String title_id : ill.getTitles()) {
                String title = titles.getTitleById(title_id);

                sb_titles.append(title == null ? title_id : title);
                if (!sb_titles.toString().isEmpty()) {
                    sb_titles.append(", ");
                } else {
                    sb_titles.append(' ');
                }
            }

            String text = "<p><b>Illustration</b><br/>" +
                    (ill.getTitles() == null || ill.getTitles().length == 0 ?
                            "" : "  <i>titles</i>: " + sb_titles.toString()) +
                    (ill.getTextualElement() == null || ill.getTextualElement().isEmpty() ?
                            "" : "  <i>textual elements</i>: '" + ill.getTextualElement() + "'<br/>") +
                    (ill.getCostume() == null || ill.getCostume().isEmpty() ?
                            "" : "  <i>costume</i>: '" + ill.getCostume() + "'<br/>") +
                    (ill.getInitials() == null || ill.getInitials().isEmpty() ?
                            "" : "  <i>initials</i>: '" + ill.getInitials() + "'<br/>") +
                    (ill.getObject() == null || ill.getObject().isEmpty() ?
                            "" : "  <i>object</i>: '" + ill.getObject() + "'<br/>") +
                    (ill.getLandscape() == null || ill.getLandscape().isEmpty() ?
                            "" : "  <i>landscape</i>: '" + ill.getLandscape() + "'<br/>") +
                    (ill.getArchitecture() == null || ill.getArchitecture().isEmpty() ?
                            "" : "  <i>architecture</i>: '" + ill.getArchitecture() + "'<br/>") +
                    (ill.getOther() == null || ill.getOther().isEmpty() ?
                            "" : "  <i>other</i>: '" + ill.getObject() + "'<br/>") +
                    (ill.getCharacters() == null || ill.getCharacters().length == 0 ?
                            "" : "  <i>characters</i>: " + sb_names.toString()) +
                    "</p>";

            ann.setDefaultSource(new AnnotationSource("ID", IIIFNames.DC_TEXT, "text/html", text, "en"));
            ann.setDefaultTarget(locationOnCanvas(image, Location.INTEXT));

            anns.add(ann);
        }

        return anns;
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
        String name = annotationListName(image.getPage(), type);

        list.setId(urlId(collection.getId(), book.getId(), name, PresentationRequestType.ANNOTATION_LIST));
        list.setType(SC_ANNOTATION_LIST);
        list.setDescription("Annotation list for " + type + " on page " + image.getPage(), "en");
        list.setLabel(name, "en");

        return list;
    }

    private String annotationListName(String page, String listType) {
        return page + (listType == null ? "" : "." + listType);
    }

    /**
     * Transform an archive annotation into a Presentation annotation.
     *
     * @param anno an archive annotation
     * @param image the canvas
     * @return a IIIF presentation API annotation
     */
    private Annotation adaptAnnotation(BookCollection collection, String book, rosa.archive.model.aor.Annotation anno,
                                       BookImage image) {
        Annotation a = new Annotation();
        String annoName = image.getPage() + "_" + annotation_counter++;

        a.setId(urlId(collection.getId(), book, annoName, PresentationRequestType.ANNOTATION));
        a.setType(IIIFNames.OA_ANNOTATION);
        a.setMotivation(IIIFNames.SC_PAINTING);
        a.setDefaultSource(new AnnotationSource(
                "URI", IIIFNames.DC_TEXT, "text/html", anno.toPrettyString(), "en"
        )); // TODO ask about this, we might not need to make these resolvable

        a.setDefaultTarget(locationOnCanvas(image, anno.getLocation()));

        for (String lang : collection.getAllSupportedLanguages()) {
            a.setLabel(annoName, lang);
        }

        return a;
    }

    /**
     * Transform marginalia data into a list of annotations that are associated
     * with a canvas.
     *
     * Marginalia is split into potentially several languages, each of which are
     * split into potentially several locations. Currently, each piece is treated
     * as a new and separate IIIF annotation. TODO these pieces must be linked somehow
     *
     * @param marg AoR marginalia
     * @param image the canvas
     * @return list of annotations
     */
    private List<Annotation> adaptMarginalia(BookCollection collection, String book, Marginalia marg, BookImage image) {
        List<Annotation> annotations = new ArrayList<>();
        for (MarginaliaLanguage lang : marg.getLanguages()) {
            for (Position pos : lang.getPositions()) {
                Annotation anno = new Annotation();
                String label = image.getPage() + "_" + annotation_counter++;

                anno.setId(urlId(collection.getId(), book, label, PresentationRequestType.ANNOTATION)); // TODO name
                anno.setMotivation(IIIFNames.SC_PAINTING);
                anno.setDefaultSource(new AnnotationSource(
                        "URI", IIIFNames.DC_TEXT, "text/html",
                        pos.getTexts().toString(), lang.getLang()
                )); // TODO ask about this, we might not need to make these resolvable
                anno.setDefaultTarget(locationOnCanvas(image, pos.getPlace()));

                annotations.add(anno);
            }
        }

        return annotations;
    }

    /**
     * Guess at the location on a canvas based on the limited location information.
     *
     * @param image canvas
     * @param location location on the canvas
     * @return the annotation target
     */
    protected AnnotationTarget locationOnCanvas(BookImage image, Location location) {
        double margin_guess = 0.10;

        int x = 0;
        int y = 0;
        int w = image.getWidth();
        int h = image.getHeight();

        AnnotationTarget target = new AnnotationTarget(image.getId());

        switch (location) {
            case HEAD:
                h = (int) (image.getHeight() * margin_guess);
                break;
            case TAIL:
                y = (int) (image.getHeight() * (1 - margin_guess));
                h = (int) (image.getHeight() * margin_guess);
                break;
            case LEFT_MARGIN:
                w = (int) (image.getWidth() * margin_guess);
                break;
            case RIGHT_MARGIN:
                x = (int) (image.getWidth() * (1 - margin_guess));
                w = (int) (image.getWidth() * margin_guess);
                break;
            case INTEXT:
                x = (int) (image.getWidth() * margin_guess);
                y = (int) (image.getHeight() * margin_guess);
                w = (int) (image.getWidth() * (1 - 2 * margin_guess));
                h = (int) (image.getHeight() * (1 - 2 * margin_guess));
                break;
            case FULL_PAGE:
                return new AnnotationTarget(image.getId(), null);
            default:
                throw new IllegalArgumentException("Invalid Location. [" + location + "]");
        }

        target.setSelector(new FragmentSelector(x, y, w, h));

        return target;
    }

}
