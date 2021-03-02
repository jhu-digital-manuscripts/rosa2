package rosa.iiif.presentation.core.transform.impl;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.serialize.AORAnnotatedPageConstants;
import rosa.archive.core.util.Annotations;
import rosa.archive.core.util.RoseTranscriptionAdapter;
import rosa.archive.core.util.TranscriptionSplitter;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookReferenceSheet;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.HTMLAnnotations;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.aor.Calculation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.Table;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.extras.BookReferenceResourceDb;
import rosa.iiif.presentation.core.extras.ExternalResourceDb;
import rosa.iiif.presentation.core.extras.ISNIResourceDb;
import rosa.iiif.presentation.core.html.AdapterSet;
import rosa.iiif.presentation.core.util.AnnotationLocationUtil;
import rosa.iiif.presentation.model.HtmlValue;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.selector.FragmentSelector;

public class AnnotationTransformer implements TransformerConstants, AORAnnotatedPageConstants {
    private final PresentationUris pres_uris;
    private final ArchiveNameParser nameParser;
    private final AdapterSet htmlAdapters;
    private ISNIResourceDb isni_db;

    private BookReferenceResourceDb[] bookReferenceResourceDbs;

    public AnnotationTransformer(PresentationUris pres_uris,
                                 ArchiveNameParser nameParser, AdapterSet htmlAdapters) {
        this.pres_uris = pres_uris;
        this.nameParser = nameParser;
        this.htmlAdapters = htmlAdapters;
    }

    private void setExternalDbs(BookCollection collection) {
        if (isni_db == null) {
            isni_db = new ISNIResourceDb(collection);
        } else {
            isni_db.setCollection(collection);
        }

        List<BookReferenceResourceDb> bookDbs = new ArrayList<>();
        Arrays.stream(BookReferenceSheet.Link.values()).forEach(link ->
                bookDbs.add(new BookReferenceResourceDb(link, collection.getBooksRef())));
        this.bookReferenceResourceDbs = bookDbs.toArray(new BookReferenceResourceDb[0]);
    }

    public Annotation transform(BookCollection collection, Book book, String name) {    	
    	// TODO This is a huge mess and almost certainly does not lookup the annotation correctly
    	
    	String[] parts = pres_uris.splitInflectedName(name);
    	String anno_id = parts[1];
    	
        // Find annotation in book
        rosa.archive.model.aor.Annotation archiveAnno = Annotations.getArchiveAnnotation(book, anno_id);

        // Transform archive anno -> iiif anno
        return adaptAnnotation(collection, book, archiveAnno, null);
    }

    public Annotation transform(BookCollection collection, Book book, rosa.archive.model.aor.Annotation anno) {
        return adaptAnnotation(collection, book, anno, null);
    }

    public Annotation transform(BookCollection col, Book book, BookImage image, rosa.archive.model.aor.Annotation anno) {
        return adaptAnnotation(col, book, anno, image);
    }

    private Annotation adaptAnnotation(BookCollection collection, Book book, rosa.archive.model.aor.Annotation anno, BookImage image) {
        setExternalDbs(collection);
        if (anno == null) {
            return null;
        }

        String language = anno.getLanguage() != null && !anno.getLanguage().isEmpty() ? anno.getLanguage() : "en";
        String locationIcon = AnnotationLocationUtil.locationToHtml(anno.getLocation());

        Annotation a = new Annotation();

        a.setId(pres_uris.getAnnotationURI(collection.getId(), book.getId(), image, anno.getId()));
        a.setType(IIIFNames.OA_ANNOTATION);
        a.setMotivation(IIIFNames.OA_COMMENTING);
        a.getMetadata().put("type", new HtmlValue(anno.getClass().getSimpleName()));

        String text = locationIcon + " " + anno.toPrettyString();
        if (anno instanceof Marginalia) {
            text = htmlAdapters.get(Marginalia.class).adapt(collection, book, image, (Marginalia) anno,
                    Stream.concat(Stream.of(isni_db), Stream.of(bookReferenceResourceDbs)).toArray(ExternalResourceDb[]::new));
        } else if (anno instanceof Drawing) {
            text = htmlAdapters.get(Drawing.class).adapt(collection, book, image, (Drawing) anno, bookReferenceResourceDbs);
        } else if (anno instanceof Table) {
            text = htmlAdapters.get(Table.class).adapt(collection, book, image, (Table) anno, bookReferenceResourceDbs);
        } else if (anno instanceof Graph) {
            text = htmlAdapters.get(Graph.class).adapt(collection, book, image, (Graph) anno, bookReferenceResourceDbs);
        } else if (anno instanceof Calculation) {
            text = htmlAdapters.get(Calculation.class).adapt(collection, book, image, (Calculation) anno, bookReferenceResourceDbs);
        }

        a.setDefaultSource(new AnnotationSource(null, IIIFNames.DC_TEXT, "text/html", text, language));

        if (image == null) {
            image = getPageImage(book, getAnnotationPage(anno.getId()));
        }
        AnnotationTarget target = locationOnCanvas(image, Location.FULL_PAGE);
        target.setUri(pres_uris.getCanvasURI(
                collection.getId(),
                book.getId(),
                image
        ));

        a.setDefaultTarget(target);

        for (String lang : collection.getAllSupportedLanguages()) {
            a.setLabel(anno.getId(), lang);
        }

        return a;
    }

    /**
     * Create an simple element that may have attributes and may have simple string content.
     * This element cannot have nested elements.
     *
     * @param writer xml stream writer
     * @param element element name
     * @param content simple String content
     * @param attrs array of attributes for the new element, always attribute label followed by attribute value
     *              IF PRESENT, attrs MUST have even number of elements
     */
    private void addSimpleElement(XMLStreamWriter writer, String element, String content, String ... attrs)
            throws XMLStreamException {
        writer.writeStartElement(element);
        if (attrs != null && attrs.length % 2 == 0) {
            for (int i = 0; i < attrs.length - 1;) {
                writer.writeAttribute(attrs[i++], attrs[i++]);
            }
        }
        if (isNotEmpty(content)) {
            writer.writeCharacters(content);
        }
        writer.writeEndElement();
    }

    /**
     * Guess at the location on a canvas based on the limited location information.
     *
     * @param image canvas
     * @param location location on the canvas
     * @return the annotation target
     */
    private AnnotationTarget locationOnCanvas(BookImage image, Location... location) {
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

    private BookImage getPageImage(Book book, String short_image_id) {
    	String imageId = nameParser.fullImageIdFromShortId(book.getId(), short_image_id);
    	
        for (BookImage image : book.getImages()) {
            if (image.getId().equals(imageId)) {
                return image;
            }
        }

        return null;
    }

    private String getAnnotationPage(String name) {
        if (name.contains("_")) {
            name = name.split("_")[0];
        }
        return nameParser.shortName(split_id(name)[0]);
    }

    private String[] split_id(String id) {
        return id.split("_");
    }

    // TODO need better way of getting standard name... refer to how it is done in the transcription splitter
    // Ripped from WebsiteLuceneMapper#getStandardPage(String)
    private String getStandardPage(BookImage image) {
        String start = image.getName();
        if (start.length() == 2) {
            return "00" + start;
        } else if (start.length() == 3) {
            return "0" + start;
        } else {
            return start;
        }
    }

    // TODO stub for rose transcription -> annotation transform
    List<Annotation> roseTranscriptionOnPage(BookCollection collection, Book book, BookImage image) {
        if (image == null) {
            return null;
        }
        String page = getStandardPage(image);

        // TODO only want to do this once, not once PER PAGE
        Map<String, String> transcriptionMap = TranscriptionSplitter.split(book.getTranscription());

        String transcription = transcriptionMap.get(page);

        if (transcription == null || transcription.isEmpty()) {
            return null;
        }

        RoseTranscriptionAdapter adapter = new RoseTranscriptionAdapter();
        transcription = adapter.toHtml(transcription, page);

        if (isNotEmpty(transcription)) {
            Annotation ann = new Annotation();

            ann.setLabel("Transcription for page " + page, "en");
            ann.setId(pres_uris.getAnnotationURI(collection.getId(), book.getId(), image, "transcription"));
            ann.setMotivation(OA_COMMENTING);
            ann.setType(OA_ANNOTATION);

            ann.setDefaultTarget(locationOnCanvas(image, Location.INTEXT));
            ann.setDefaultSource(new AnnotationSource(null, IIIFNames.DC_TEXT, "text/html", transcription, "en"));

            return Collections.singletonList(ann);
        } else {
            return Collections.emptyList();
        }
    }

    List<Annotation> illustrationsForPage(BookCollection collection, Book book, BookImage image) {
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
            String anno_name = "illustration_" + ill.getId();

            Annotation ann = new Annotation();
            ann.setLabel("Illustration(s) on " + page, "en");
            ann.setId(pres_uris.getAnnotationURI(collection.getId(), book.getId(), image, anno_name));
            ann.setMotivation(OA_COMMENTING);
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

            String content;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                XMLStreamWriter xml = XMLOutputFactory.newInstance().createXMLStreamWriter(output);

                addSimpleElement(xml, "p", "Illustration", "class", "annotation-title");

                if (isNotEmpty(ill.getTitles())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Titles:", "class", "bold");
                    xml.writeCharacters(" " + sb_titles.toString());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getCharacters())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Characters:", "class", "bold");
                    xml.writeCharacters(" " + sb_names.toString());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getTextualElement())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Textual Elements:", "class", "bold");
                    xml.writeCharacters(" " + ill.getTextualElement());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getCostume())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Costume:", "class", "bold");
                    xml.writeCharacters(" " + ill.getCostume());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getInitials())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Initials:", "class", "bold");
                    xml.writeCharacters(" " + ill.getInitials());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getObject())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Object:", "class", "bold");
                    xml.writeCharacters(" " + ill.getObject());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getLandscape())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Landscape:", "class", "bold");
                    xml.writeCharacters(" " + ill.getLandscape());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getArchitecture())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Architecture:", "class", "bold");
                    xml.writeCharacters(" " + ill.getArchitecture());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getOther())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Other:", "class", "bold");
                    xml.writeCharacters(" " + ill.getOther());
                    xml.writeEndElement();
                }

                content = output.toString("UTF-8");
            } catch (XMLStreamException | UnsupportedEncodingException e) {
                content = "";
            }

            ann.setDefaultSource(new AnnotationSource(null, IIIFNames.DC_TEXT, "text/html", content, "en"));
            ann.setDefaultTarget(locationOnCanvas(image, Location.INTEXT));
            anns.add(ann);
        }

        return anns;
    }

    private boolean isNotEmpty(String[] str) {
        return str != null && str.length > 0;
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

	public List<Annotation> htmlAnnotationsForPage(BookCollection col, Book book, BookImage image) {
		HTMLAnnotations annos = col.getHTMLAnnotations();
		
		if (annos == null) {
			return null;
		}
		
		String content = annos.getAnnotation(image.getId());
		
		if (content == null) {
			return null;
		}
		
        Annotation ann = new Annotation();
        ann.setLabel("Annotation on " + image.getName(), "en");
        ann.setId(pres_uris.getAnnotationURI(col.getId(), book.getId(), image, "htmlanno1"));
        ann.setMotivation(OA_COMMENTING);
        ann.setType(OA_ANNOTATION);
        ann.setDefaultSource(new AnnotationSource(null, IIIFNames.DC_TEXT, "text/html", content, "en"));
        ann.setDefaultTarget(locationOnCanvas(image, Location.INTEXT));
        
        List<Annotation> result = new ArrayList<>();
        result.add(ann);
        return result;
	}

}
