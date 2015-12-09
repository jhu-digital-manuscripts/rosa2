package rosa.iiif.presentation.core.transform.impl;

import com.google.inject.Inject;
import rosa.archive.core.serialize.AORAnnotatedPageConstants;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.archive.core.util.Annotations;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.selector.FragmentSelector;

import java.util.ArrayList;
import java.util.List;

public class AnnotationTransformer extends BasePresentationTransformer implements Transformer<Annotation>,
        AORAnnotatedPageConstants {

    @Inject
    public AnnotationTransformer(IIIFRequestFormatter presRequestFormatter) {
        super(presRequestFormatter);
    }

    @Override
    public Annotation transform(BookCollection collection, Book book, String name) {
        // Find annotation in book
        rosa.archive.model.aor.Annotation archiveAnno = Annotations.getArchiveAnnotation(book, name);

        // Transform archive anno -> iiif anno
        return adaptAnnotation(collection, book, archiveAnno);
    }

    public Annotation transform(BookCollection collection, Book book, rosa.archive.model.aor.Annotation anno) {
        return adaptAnnotation(collection, book, anno);
    }

    @Override
    public Class<Annotation> getType() {
        return Annotation.class;
    }



    private Annotation adaptAnnotation(BookCollection collection, Book book, rosa.archive.model.aor.Annotation anno) {
        if (anno == null) {
            return null;
        } else if (anno instanceof Marginalia) {
            return adaptMarginalia(collection, book, (Marginalia) anno);
        }

        Annotation a = new Annotation();

        a.setId(urlId(collection.getId(), book.getId(), anno.getId(), PresentationRequestType.ANNOTATION));
        a.setType(IIIFNames.OA_ANNOTATION);
        a.setMotivation(IIIFNames.SC_PAINTING);
        a.setDefaultSource(new AnnotationSource(
                "URI", IIIFNames.DC_TEXT, "text/html",
                anno.toPrettyString(),
                (anno.getLanguage() != null && !anno.getLanguage().isEmpty() ? anno.getLanguage() : "en")
        ));

        a.setDefaultTarget(locationOnCanvas(
                getPageImage(book.getImages(), getAnnotationPage(anno.getId())),
                anno.getLocation())
        );

        for (String lang : collection.getAllSupportedLanguages()) {
            a.setLabel(anno.getId(), lang);
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
     * Marginalia ID structure:
     *
     * @param collection book collection obj
     * @param book book obj
     * @param marg AoR marginalia
     * @return list of annotations
     */
    private Annotation adaptMarginalia(BookCollection collection, Book book, Marginalia marg) {
        String lang = marg.getLanguages() != null && marg.getLanguages().size() > 0
                ? marg.getLanguages().get(0).getLang() : "en";

        Annotation anno = new Annotation();

        anno.setId(urlId(collection.getId(), book.getId(), marg.getId(), PresentationRequestType.ANNOTATION));
        anno.setMotivation(IIIFNames.SC_PAINTING);
        anno.setDefaultSource(new AnnotationSource("URI", IIIFNames.DC_TEXT, "text/html",
                marginaliaToDisplayHtml(marg), lang));

        anno.setDefaultTarget(locationOnCanvas(
                getPageImage(book.getImages(), getAnnotationPage(marg.getId())),
                Location.FULL_PAGE)
        ); // TODO actual position(s)

        return anno;
    }

    private String marginaliaToDisplayHtml(Marginalia marg) {

        String transcription = "";
        List<String> people = new ArrayList<>();
        List<String> books = new ArrayList<>();
        List<String> locs = new ArrayList<>();
        // TODO X-refs
//        List<String> emphasis = new ArrayList<>();

        for (MarginaliaLanguage lang : marg.getLanguages()) {
            for (Position pos : lang.getPositions()) {
                transcription += listToString(pos.getTexts());
                people.addAll(pos.getPeople());
                books.addAll(pos.getBooks());
                locs.addAll(pos.getLocations());

                /*
                    TODO emphasis: perhaps find the emphasized text in transcription and underline it?
                 */
//                for (Underline u : pos.getEmphasis()) {
//                    emphasis.add(u.getReferringText());
//                }
            }
        }

        StringBuilder sb = new StringBuilder("<div>");
        sb.append("<p>");
        sb.append(transcription);
        sb.append("</p>");

        if (marg.getTranslation() != null && !marg.getTranslation().isEmpty()) {
            sb.append("<p class=\"italic\">[");
            sb.append(marg.getTranslation());
            sb.append("]</p>");
        }

        if (!people.isEmpty()) {
            sb.append("<p><span class=\"emphasize\">People:</span> ");
            for (int i = 0; i < people.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                // TODO Will be a link eventually!
                sb.append(people.get(i));
            }
            sb.append("</p>");
        }

        if (!books.isEmpty()) {
            sb.append("<p><span class=\"emphasize\">Books:</span> ");
            for (int i = 0; i < books.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(books.get(i));
            }
            sb.append("</p>");
        }

        if (!locs.isEmpty()) {
            sb.append("<p><span class=\"emphasize\">Locations:</span> ");
            for (int i = 0; i < locs.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(locs.get(i));
            }
            sb.append("</p>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Guess at the location on a canvas based on the limited location information.
     *
     * @param image canvas
     * @param location location on the canvas
     * @return the annotation target
     */
    protected AnnotationTarget locationOnCanvas(BookImage image, Location... location) {
        if (location == null || location.length == 0) {
            return new AnnotationTarget(image.getId(), null);
        }

        double margin_guess = 0.10;

        int x = 0;
        int y = 0;
        int w = image.getWidth();
        int h = image.getHeight();

        AnnotationTarget target = new AnnotationTarget(image.getId());

        // This will overwrite the previous locations...
        for (Location loc : location) {
            switch (loc) {
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
                    // Where Full Page shouldn't need to have a region defined,
                    // Return the full page region because Mirador is misbehaving........................... :)
//                    return new AnnotationTarget(image.getId(), null);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid Location. [" + loc + "]");
            }
        }

        target.setSelector(new FragmentSelector(x, y, w, h));

        return target;
    }

    private String listToString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String str : list) {
            sb.append(str);
            sb.append(' ');
        }
        return sb.toString();
    }

    private BookImage getPageImage(ImageList images, String page) {
        for (BookImage image : images) {
            if (image.getName().equals(page) || image.getId().equals(page)) {
                return image;
            }
        }

        return null;
    }

    private String getAnnotationPage(String name) {
        return split_id(name)[0];
    }

    private String[] split_id(String id) {
        return id.split("_");
    }

    public List<Annotation> illustrationsForPage(BookCollection collection, Book book, BookImage image) {
        String page = image.getName();
        if (book.getIllustrationTagging() == null) {
            return null;
        }

        List<Annotation> anns = new ArrayList<>();
        for (Illustration ill : book.getIllustrationTagging()) {
            String illusPage = ill.getPage();
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

}
