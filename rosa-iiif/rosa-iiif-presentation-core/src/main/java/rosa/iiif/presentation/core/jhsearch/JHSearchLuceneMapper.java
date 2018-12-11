package rosa.iiif.presentation.core.jhsearch;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.util.TranscriptionSplitter;
import rosa.archive.model.BiblioData;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookDescription;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.ObjectRef;
import rosa.archive.model.ReferenceSheet;
import rosa.archive.model.Transcription;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.Calculation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.GraphNode;
import rosa.archive.model.aor.GraphText;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Table;
import rosa.archive.model.aor.TextEl;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.search.core.BaseLuceneMapper;
import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

/**
 * Index and create queries for data which becomes IIIF Presentation
 * Annotations.
 */
public class JHSearchLuceneMapper extends BaseLuceneMapper {
	private static final Logger logger = Logger.getLogger(JHSearchLuceneMapper.class.toString());
	private static final ArchiveNameParser nameParser = new ArchiveNameParser();

	private final PresentationUris pres_uris;

	public JHSearchLuceneMapper(PresentationUris pres_uris) {
		super(JHSearchField.values());

		this.pres_uris = pres_uris;

		facets_config.setMultiValued(JHSearchCategory.AUTHOR.getFieldName(), true);
	}

	@Override
	public SearchField getIdentifierSearchField() {
		return JHSearchField.OBJECT_ID;
	}

	/**
	 * Create and index Lucene documents for a given book within a book
	 * collection.
	 *
	 * @param col
	 *            BookCollection object
	 * @param book
	 *            Book object
	 * @return list of documents representing the book
	 * @throws IOException if search service is not available
	 */
	public List<Document> createDocuments(BookCollection col, Book book) throws IOException {
	    // Make sure to filter all documents to handle facets
	    
		List<Document> result = new ArrayList<>();

		// Index information associated with canvases

		ImageList images = book.getImages();

		// Create document for each image
		Map<String, String> transcriptionMap = TranscriptionSplitter.split(book.getTranscription());

		if (images.getImages() == null) {
			logger.warning("No image list found. [" + col.getId() + ":" + book.getId() + "]");
		} else {
			for (BookImage image : images.getImages()) {
				Document doc = create_canvas_document(col, book, image);

				String trans = transcriptionMap.get(getStandardPage(image));

				index(col, book, image, doc, trans);
				
				result.add(facets_config.build(doc));
			}
		}

		// Index information associated with manifests

		Document doc = create_manifest_document(col, book);
		index(col, book, doc);
		result.add(facets_config.build(doc));
		
		return result;
	}

	// TODO need better way of getting standard name... refer to how it is done
	// in the transcription splitter
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

	private void index(BookDescription desc, Document doc) throws IOException {
		try {
			addField(doc, JHSearchField.DESCRIPTION, SearchFieldType.ENGLISH, xml_to_text(desc.getXML()));
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Parse an XML document and return all textual content in a String.
	 *
	 * @param src
	 *            source XML
	 * @return String of all textual content
	 * @throws SAXException if XMl is malformed
	 * @throws IOException if search service is not available
	 */
	private static String xml_to_text(InputSource src) throws SAXException, IOException {
		XMLReader r = XMLReaderFactory.createXMLReader();
		final StringBuilder result = new StringBuilder();

		r.setContentHandler(new DefaultHandler() {
			public void characters(char[] text, int offset, int len) throws SAXException {
				result.append(text, offset, len);
			}
		});

		r.parse(src);

		return result.toString();
	}

	private String xml_to_text(String xml) throws SAXException, IOException {
		return xml_to_text(new InputSource(new StringReader(xml)));
	}

	private void index(BookCollection col, Book book, Document doc) throws IOException {
		BookDescription desc = book.getBookDescription("en");

		if (desc != null) {
			index(desc, doc);
		}
	
		index_book_facets(book, doc);
	}
	
	private void index_book_facets(Book book, Document doc) {
        BookMetadata md = book.getBookMetadata();	    
        BiblioData bd = book.getBiblioData("en");

        String facet_loc = bd.getCurrentLocation();
        String facet_repo = bd.getRepository();
        String facet_date = bd.getDateLabel();
        int numPages = md.getNumberOfPages();
        int numIlls = md.getNumberOfIllustrations();
        String facet_common_name = bd.getCommonName();
        String facet_origin = bd.getOrigin();
        String facet_type = bd.getType();

        addFacet(doc, JHSearchCategory.COMMON_NAME, facet_common_name);
        addFacet(doc, JHSearchCategory.NUM_PAGES, quantize(numPages, 100));
        addFacet(doc, JHSearchCategory.LOCATION, facet_loc);
        addFacet(doc, JHSearchCategory.REPOSITORY, facet_repo);
        addFacet(doc, JHSearchCategory.DATE, facet_date);
        addFacet(doc, JHSearchCategory.ORIGIN, facet_origin);
        addFacet(doc, JHSearchCategory.NUM_ILLUS, quantize(numIlls, 10));
        addFacet(doc, JHSearchCategory.TYPE, facet_type);

        for (String author : Arrays.stream(bd.getAuthors()).map(ObjectRef::getName).collect(Collectors.toList())) {
            addFacet(doc, JHSearchCategory.AUTHOR, author);
        }
        
        Transcription tr = book.getTranscription();
        // TODO how to tell if we have full VS partial transcription?
        boolean hasTranscription = tr != null && tr.getXML() != null && !tr.getXML().isEmpty();
        addFacet(doc, JHSearchCategory.TRANSCRIPTION, String.valueOf(hasTranscription));

        for (BookText text : book.getBookMetadata().getBookTexts()) {
            text.getAuthors().forEach(author -> addFacet(doc, JHSearchCategory.AUTHOR, author));
        }
	}
	
	private String quantize(int n, int bin_size) {
	    if (n < 0) {
	        return "Unknown";
	    }
	    
	    int bin = n / bin_size;
	    
	    int start = bin * bin_size;
	    int end = start + (bin_size - 1);
	    
	    return start + "-" + end;
	}

	private void index(BookCollection col, Book book, BookImage image, Document doc, String trans) {
		AnnotatedPage page = book.getAnnotationPage(image.getId());

		if (page != null) {
			index(col, book, image, doc, page);
		}

		IllustrationTagging tag = book.getIllustrationTagging();

		if (tag != null) {
			index(col, book, image, doc, tag);
		}

		// Index transcription text that appears on this page
		if (trans != null) {
			try {
				indexTranscriptionFragment(trans, doc);
			} catch (SAXException | IOException e) {
				logger.log(Level.SEVERE, "Failed to parse transcription fragment. [" + image.getName() + "]", e);
			}
		}

	}

	private void index(BookCollection col, Book book, BookImage image, Document doc, IllustrationTagging imgtag) {
		IllustrationTitles titles = col.getIllustrationTitles();
		CharacterNames char_names = col.getCharacterNames();

		StringBuilder text = new StringBuilder();

		for (int index : imgtag.findImageIndices(book, image.getId())) {
			Illustration illus = imgtag.getIllustrationData(index);

			StringBuilder chars = new StringBuilder();
			for (String char_id : illus.getCharacters()) {
				rosa.archive.model.CharacterName char_name = char_names.getCharacterName(char_id);

				if (char_name == null) {
                    chars.append(char_id).append(", ");
				} else {
                    char_name.getAllNames().forEach(name -> chars.append(name).append(", "));
				}
			}
            addField(doc, JHSearchField.PEOPLE, SearchFieldType.ENGLISH, chars.toString());
			addField(doc, JHSearchField.CHAR_NAME, chars.toString());
            text.append(chars);

            StringBuilder t = new StringBuilder();
			for (String title_id : illus.getTitles()) {
				String title = titles.getTitleById(title_id);

				if (title != null && !title.isEmpty()) {
                    t.append(title).append(", ");
				} else {
                    t.append(title_id).append(", ");
				}
			}
//			addField(doc, JHSearchField.TITLE, SearchFieldType.ENGLISH, t.toString());
            text.append(t);

			text.append(illus.getTextualElement());
			text.append(", ");
			text.append(illus.getArchitecture());
			text.append(", ");
			text.append(illus.getCostume());
			text.append(", ");
			text.append(illus.getOther());
			text.append(", ");
			text.append(illus.getObject());
			text.append(", ");
			text.append(illus.getLandscape());
			text.append(", ");
		}

		if (text.length() > 0) {
			addField(doc, JHSearchField.ILLUSTRATION, SearchFieldType.ENGLISH, text.toString());
		}
	}

	private void indexTranscriptionFragment(String transcription, Document doc) throws SAXException, IOException {
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();

		TranscriptionXMLReader trxml = new TranscriptionXMLReader();
		xmlReader.setContentHandler(trxml);
		xmlReader.parse(new InputSource(new StringReader(transcription)));

		addField(doc, JHSearchField.TRANSCRIPTION, SearchFieldType.OLD_FRENCH, trxml.getPoetry());
        addField(doc, JHSearchField.TEXT, SearchFieldType.OLD_FRENCH, trxml.getPoetry());

		if (trxml.hasCatchphrase()) {
			addField(doc, JHSearchField.TRANSCRIPTION, SearchFieldType.OLD_FRENCH, trxml.getCatchphrase());
            addField(doc, JHSearchField.TEXT, SearchFieldType.OLD_FRENCH, trxml.getCatchphrase());
		}

		if (trxml.hasRubric()) {
            addField(doc, JHSearchField.TRANSCRIPTION, SearchFieldType.OLD_FRENCH, trxml.getRubric());
            addField(doc, JHSearchField.TEXT, SearchFieldType.OLD_FRENCH, trxml.getRubric());
		}

		if (trxml.hasIllus()) {
			addField(doc, JHSearchField.TRANSCRIPTION, SearchFieldType.ENGLISH, trxml.getIllustration());
            addField(doc, JHSearchField.TEXT, SearchFieldType.ENGLISH, trxml.getIllustration());
		}

		addField(doc, JHSearchField.TRANSCRIPTION, SearchFieldType.ENGLISH, trxml.getLecoy());
		addField(doc, JHSearchField.TRANSCRIPTION, SearchFieldType.ENGLISH, trxml.getLine());
        addField(doc, JHSearchField.TEXT, SearchFieldType.ENGLISH, trxml.getLecoy());
        addField(doc, JHSearchField.TEXT, SearchFieldType.ENGLISH, trxml.getLine());

		if (trxml.hasNote()) {
			addField(doc, JHSearchField.TRANSCRIPTION, SearchFieldType.ENGLISH, trxml.getNote());
            addField(doc, JHSearchField.TEXT, SearchFieldType.ENGLISH, trxml.getNote());
		}
	}

	/**
	 * Index all information about the AoR transcriptions.
	 *
	 * <ul>
	 * <li>image ID</li>
	 * <li>image short name</li>
	 * <li>reader</li>
	 * <li>pagination</li>
	 * <li>signature</li>
	 * <li>underlined text</li>
	 * <li>symbols</li>
	 * <li>marks</li>
	 * <li>marginalia translation</li>
	 * <li>marginalia transcription</li>
	 * <li>marginalia references to books, both internal to corpus and external
	 * </li>
	 * <li>marginalia references to people</li>
	 * <li>marginalia references to locations</li>
	 * <li>errata</li>
	 * <li>drawing</li>
	 * <li>numerals</li>
	 * </ul>
	 *
	 *
	 * @param col
	 *            BookCollection obj
	 * @param book
	 *            Book obj
	 * @param image
	 *            this image
	 * @param page
	 *            transcriptions of AoR annotations on this page
	 */
	private void index(BookCollection col, Book book, BookImage image, Document doc, AnnotatedPage page) {
		page.getSymbols().forEach(a -> index(col, book, image, a, doc));
		page.getMarks().forEach(a -> index(col, book, image, a, doc));
		page.getDrawings().forEach(a -> index(col, book, image, a, doc));
		page.getErrata().forEach(a -> index(col, book, image, a, doc));
		page.getNumerals().forEach(a -> index(col, book, image, a, doc));
		page.getMarginalia().forEach(a -> index(col, book, image, a, doc));
		page.getUnderlines().forEach(a -> index(col, book, image, a, doc));
		page.getCalculations().forEach(a -> index(col, book, image, a, doc));
		page.getGraphs().forEach(a -> index(col, book, image, a, doc));
		page.getTables().forEach(a -> index(col, book, image, a, doc));

		// Index languages used and breaking out marginalia specifically

		Set<String> langs = new HashSet<>();
		Set<String> marg_langs = new HashSet<>();

		page.getAnnotations().forEach(a -> {
			String lang = a.getLanguage();

			if (lang != null) {
				langs.add(lang.toLowerCase());
			}

			// Handle additional languages in Marginalia

			if (a instanceof Marginalia) {
				for (MarginaliaLanguage ml : Marginalia.class.cast(a).getLanguages()) {
					lang = ml.getLang();

					if (lang != null) {
						langs.add(lang.toLowerCase());
						marg_langs.add(lang.toLowerCase());
					}
				}
			}
		});

		// Index languages with all annotations
		langs.forEach(lc -> addField(doc, JHSearchField.LANGUAGE, lc));
		// Index languages within marginalia only
		marg_langs.forEach(lc -> addField(doc, JHSearchField.MARGINALIA_LANGUAGE, lc));

		// Index method used
		Set<String> methods = new HashSet<>();

		page.getUnderlines().forEach(a -> {
			String method = a.getMethod();

			if (method != null) {
				methods.add(method.toLowerCase());
			}
		});

		page.getMarks().forEach(a -> {
			String method = a.getMethod();

			if (method != null) {
				methods.add(method.toLowerCase());
			}
		});

		methods.forEach(method -> addField(doc, JHSearchField.METHOD, method));
		if (page.getReader() != null && !page.getReader().isEmpty()) {
			addField(doc, JHSearchField.ANNOTATOR, page.getReader());
		}
	}

	// Create document to index a canvas
	private Document create_canvas_document(BookCollection col, Book book, BookImage image) {
		Document doc = new Document();

		String collection_id = pres_uris.getCollectionURI(col.getId());
		String manifest_id =  pres_uris.getManifestURI(col.getId(), book.getId());
		String canvas_id = pres_uris.getCanvasURI(col.getId(), book.getId(), nameParser.shortName(image.getId()));

		addField(doc, JHSearchField.COLLECTION_ID, collection_id);

		addField(doc, JHSearchField.OBJECT_ID, canvas_id);
		addField(doc, JHSearchField.OBJECT_TYPE, IIIFNames.SC_CANVAS);

		String label = image.getName();
		AnnotatedPage transcript = book.getAnnotationPage(image.getId());
		if (transcript != null) {
			if (transcript.getPagination() != null && !transcript.getPagination().isEmpty()) {
				label = transcript.getPagination();
			} else if (transcript.getSignature() != null && !transcript.getSignature().isEmpty()) {
				label = transcript.getSignature();
			}
		}
		addField(doc, JHSearchField.OBJECT_LABEL, label);

		addField(doc, JHSearchField.MANIFEST_ID, manifest_id);
		addField(doc, JHSearchField.MANIFEST_LABEL, book.getBiblioData("en").getCommonName());

		return doc;
	}

	// Create document to index a manifest
	private Document create_manifest_document(BookCollection col, Book book) {
		Document doc = new Document();

		String collection_id = pres_uris.getCollectionURI(col.getId());
		String manifest_id = pres_uris.getManifestURI(col.getId(), book.getId());

        BiblioData bd = book.getBiblioData("en");
        String manifest_label = bd.getCommonName();

        addField(doc, JHSearchField.COLLECTION_ID, collection_id);

		addField(doc, JHSearchField.OBJECT_ID, manifest_id);
		addField(doc, JHSearchField.OBJECT_TYPE, IIIFNames.SC_MANIFEST);
		addField(doc, JHSearchField.OBJECT_LABEL, manifest_label);

		addField(doc, JHSearchField.MANIFEST_ID, manifest_id);
		addField(doc, JHSearchField.MANIFEST_LABEL, manifest_label);

        addField(doc, JHSearchField.TITLE, SearchFieldType.ENGLISH, manifest_label);
        addField(doc, JHSearchField.REPO, SearchFieldType.ENGLISH, bd.getRepository());

        addField(doc, JHSearchField.PLACE, bd.getCurrentLocation());

        BookMetadata md = book.getBookMetadata();
        
        if (md.getBookTexts() != null) {
            for (BookText text : md.getBookTexts()) {
                addField(doc, JHSearchField.TITLE, SearchFieldType.ENGLISH, text.getTitle());
            }
        }

        BookMetadata mm = book.getBookMetadata();
        if (mm != null && mm.getBiblioDataMap() != null) {
            BiblioData data = mm.getBiblioDataMap().get("en");
            if (data.getAuthors() != null) {
                for (String author : Arrays.stream(data.getAuthors()).map(ObjectRef::getName).collect(Collectors.toList())) {
                    addField(doc, JHSearchField.PEOPLE, author);
                }
            }
        }

		return doc;
	}

	private void index(BookCollection col, Book book, BookImage image, Symbol symbol, Document doc) {
		addField(doc, JHSearchField.SYMBOL, SearchFieldType.STRING, symbol.getName());
		addField(doc, JHSearchField.SYMBOL, get_type_for_lang(symbol), stripTranscribersMarks(symbol.getReferencedText()));
        addField(doc, JHSearchField.TEXT, get_type_for_lang(symbol), stripTranscribersMarks(symbol.getReferencedText()));
	}

	private void index(BookCollection col, Book book, BookImage image, Drawing drawing, Document doc) {
		addField(doc, JHSearchField.DRAWING, SearchFieldType.STRING, drawing.getType());
		addField(doc, JHSearchField.HAND, String.join(", ",
				drawing.getTexts().stream().map(TextEl::getHand).distinct().collect(Collectors.toList())));

		addField(doc, JHSearchField.METHOD, drawing.getMethod());

		// All text: ./text[text] and ./text[anchor_text],
		for (TextEl txt : drawing.getTexts()) {
			SearchFieldType sft = get_type_for_lang(txt.getLanguage());
			String toIndex = txt.getText() + " " + txt.getAnchor_text();
			addSomeText(doc, JHSearchField.DRAWING, sft, toIndex);
		}
		addSomeText(doc, JHSearchField.DRAWING, get_type_for_lang(drawing), drawing.getReferencedText());
		addSomeText(doc, JHSearchField.DRAWING, SearchFieldType.ENGLISH, drawing.getTranslation());

        addRefList(drawing.getPeople(), col.getPeopleRef(), JHSearchField.PEOPLE, doc);
        addRefList(drawing.getBooks(), col.getBooksRef(), JHSearchField.BOOK, doc);
        addRefList(drawing.getLocations(), col.getLocationsRef(), JHSearchField.PLACE, doc);
		addField(doc, JHSearchField.SYMBOL, SearchFieldType.STRING, String.join(", ", drawing.getSymbols()));
	}

	private void index(BookCollection col, Book book, BookImage image, Errata errata, Document doc) {
		SearchFieldType type = get_type_for_lang(errata);

		addField(doc, JHSearchField.ERRATA, type, errata.getAmendedText());
		addField(doc, JHSearchField.ERRATA, type, stripTranscribersMarks(errata.getReferencedText()));
        addField(doc, JHSearchField.TEXT, type, stripTranscribersMarks(errata.getReferencedText()));
	}

	private void index(BookCollection col, Book book, BookImage image, Mark mark, Document doc) {
		addField(doc, JHSearchField.MARK, SearchFieldType.STRING, mark.getName());
		addField(doc, JHSearchField.MARK, get_type_for_lang(mark), stripTranscribersMarks(mark.getReferencedText()));
        addField(doc, JHSearchField.TEXT, get_type_for_lang(mark), stripTranscribersMarks(mark.getReferencedText()));
	}

	private void index(BookCollection col, Book book, BookImage image, Numeral numeral, Document doc) {
		SearchFieldType type = get_type_for_lang(numeral);

		addField(doc, JHSearchField.NUMERAL, type, stripTranscribersMarks(numeral.getReferencedText()));
        addField(doc, JHSearchField.TEXT, type, stripTranscribersMarks(numeral.getReferencedText()));
		addField(doc, JHSearchField.NUMERAL, type, numeral.getNumeral());
	}

	private void index(BookCollection col, Book book, BookImage image, Underline underline, Document doc) {
		addField(doc, JHSearchField.UNDERLINE, get_type_for_lang(underline),
                stripTranscribersMarks(underline.getReferencedText()));
        addField(doc, JHSearchField.TEXT, get_type_for_lang(underline), stripTranscribersMarks(underline.getReferencedText()));
	}

	private void index(BookCollection col, Book book, BookImage image, Calculation calc, Document doc) {
	    addField(doc, JHSearchField.CALCULATION, SearchFieldType.STRING, calc.getType());
	    addField(doc, JHSearchField.METHOD, SearchFieldType.STRING, calc.getMethod());
	    addField(doc, JHSearchField.CALCULATION, SearchFieldType.ENGLISH, String.join(", ", calc.getData()));
	    addSomeText(doc, JHSearchField.CALCULATION, SearchFieldType.ENGLISH, calc.getContent());
    }

    private SearchFieldType get_type_for_lang(Annotation a) {
        return get_type_for_lang(a.getLanguage());
    }
    
    private SearchFieldType get_type_for_lang(String lc) {
        SearchFieldType type = null;
        
        if (lc != null) {
            type = getSearchFieldTypeForLang(lc);
        }

        if (type == null) {
            return SearchFieldType.ENGLISH;
        }

        return type;
    }

	/**
	 * Add a list of values to the index. If a reference sheet exists, also add any
	 * relevant alternative values.
	 *
	 * @param values list of values to index
	 * @param reference reference sheet that may contain alternate values
	 * @param field search field to index data
	 * @param doc Lucene document
	 */
	private void addRefList(List<String> values, ReferenceSheet reference, JHSearchField field, Document doc) {
		values.forEach(v -> {
			addField(doc, field, v);

			if (reference != null && reference.hasAlternates(v)) {
				reference.getAlternates(v).forEach(alt -> addField(doc, field, SearchFieldType.ENGLISH, alt));
			}
		});
	}

	private void addSomeText(Document doc, JHSearchField field, SearchFieldType lang, String text) {
		addField(doc, field, lang, text);
		addField(doc, JHSearchField.TEXT, lang, text);
	}

	private void index(BookCollection col, Book book, BookImage image, Marginalia marg, Document doc) {
		addField(doc, JHSearchField.MARGINALIA, get_type_for_lang(marg), stripTranscribersMarks(marg.getReferencedText()));
        addField(doc, JHSearchField.TEXT, get_type_for_lang(marg), stripTranscribersMarks(marg.getReferencedText()));

		StringBuilder transcription = new StringBuilder();
		StringBuilder emphasis = new StringBuilder();

		SearchFieldType marg_lang_type = SearchFieldType.ENGLISH;

		ReferenceSheet peopleRefs = col.getPeopleRef();
		ReferenceSheet bookRefs = col.getBooksRef();
		ReferenceSheet locationRefs = col.getLocationsRef();

		for (MarginaliaLanguage lang : marg.getLanguages()) {
			marg_lang_type =  get_type_for_lang(lang.getLang());

			for (Position pos : lang.getPositions()) {
				transcription.append(String.join(", ", pos.getTexts()));

				// Index <person> tags + variants
				addRefList(pos.getPeople(), peopleRefs, JHSearchField.PEOPLE, doc);
				// Index <book> tags + variants
				addRefList(pos.getBooks(), bookRefs, JHSearchField.BOOK, doc);
				// Index <location> tags + variants
				addRefList(pos.getLocations(), locationRefs, JHSearchField.PLACE, doc);

				pos.getEmphasis().forEach(underline -> emphasis.append(stripTranscribersMarks(underline.getReferencedText())).append(" "));

				for (XRef xref : pos.getxRefs()) {
                    addField(doc, JHSearchField.CROSS_REFERENCE, SearchFieldType.ENGLISH, xref.getPerson());
                    addField(doc, JHSearchField.CROSS_REFERENCE, SearchFieldType.ENGLISH, xref.getTitle());
                    
                    if (xref.getText() != null && xref.getLanguage() != null) {
                        SearchFieldType type = get_type_for_lang(xref.getLanguage());
                        addField(doc, JHSearchField.CROSS_REFERENCE, type, stripTranscribersMarks(xref.getText()));
                    }
				}

				pos.getSymbols().forEach(s -> addField(doc, JHSearchField.SYMBOL, SearchFieldType.STRING, s));
			}
		}
		
		// Remove [] used for transcribers marks
		addSomeText(doc, JHSearchField.MARGINALIA, marg_lang_type, stripTranscribersMarks(transcription.toString()));
		addSomeText(doc, JHSearchField.MARGINALIA, SearchFieldType.ENGLISH, marg.getTranslation());
		addField(doc, JHSearchField.EMPHASIS, marg_lang_type, emphasis.toString());
		addField(doc, JHSearchField.HAND, marg.getHand());
		addField(doc, JHSearchField.ANNOTATOR, marg.getOtherReader());
	}

	private void index(BookCollection col, Book book, BookImage image, Graph graph, Document doc) {
	    addField(doc, JHSearchField.GRAPH, SearchFieldType.STRING, graph.getType());

	    for (GraphText text : graph.getGraphTexts()) {
	        addField(doc, JHSearchField.HAND, String.join(", ",
                    text.getNotes().stream().map(note -> note.hand).distinct().collect(Collectors.toList())));
	        addRefList(text.getPeople(), col.getPeopleRef(), JHSearchField.PEOPLE, doc);
	        addRefList(text.getBooks(), col.getBooksRef(), JHSearchField.BOOK, doc);
	        addRefList(text.getLocations(), col.getLocationsRef(), JHSearchField.PLACE, doc);
	        addField(doc, JHSearchField.SYMBOL, SearchFieldType.STRING, String.join(", ", text.getSymbols()));
	        addSomeText(doc, JHSearchField.GRAPH, SearchFieldType.ENGLISH, String.join(", ", text.getTranslations()));
        }

	    addSomeText(doc, JHSearchField.GRAPH, get_type_for_lang(graph), String.join(", ",
				graph.getNodes().stream().map(node -> node.getText() + " " + node.getContent()).collect(Collectors.toList())));
        addField(doc, JHSearchField.PEOPLE, SearchFieldType.ENGLISH, String.join(", ",
                graph.getNodes().stream().map(GraphNode::getPerson).collect(Collectors.toList())));
    }

    private void index(BookCollection col, Book book, BookImage image, Table table, Document doc) {
	    addField(doc, JHSearchField.TABLE, SearchFieldType.STRING, table.getType());
        addField(doc, JHSearchField.HAND,
                String.join(", ", table.getTexts().stream().map(TextEl::getHand).distinct().collect(Collectors.toList())));

        addSomeText(doc, JHSearchField.TABLE, get_type_for_lang(table), String.join(", ",
				table.getTexts().stream().map(txt -> txt.getAnchor_text() + " " + txt.getText()).collect(Collectors.toList())));
        addSomeText(doc, JHSearchField.TABLE, SearchFieldType.ENGLISH, table.getTranslation());
        addField(doc, JHSearchField.TABLE, table.getAggregatedInfo());

        addRefList(table.getPeople(), col.getPeopleRef(), JHSearchField.PEOPLE, doc);
        addRefList(table.getBooks(), col.getBooksRef(), JHSearchField.BOOK, doc);
        addRefList(table.getLocations(), col.getLocationsRef(), JHSearchField.PLACE, doc);

        addField(doc, JHSearchField.SYMBOL, SearchFieldType.STRING, String.join(", ", table.getSymbols()));

        table.getCells().stream()
                .map(cell -> cell.getAnchorData() + " " + cell.getAnchorText() + " " + cell.getContent())
                .forEach(stuff -> {
                    addField(doc, JHSearchField.TABLE, stuff);
                    addField(doc, JHSearchField.TEXT, stuff);
                });
    }

	protected static String stripTranscribersMarks(String s) {
		return s.replace("[", "").replace("]", "");
	}

}
