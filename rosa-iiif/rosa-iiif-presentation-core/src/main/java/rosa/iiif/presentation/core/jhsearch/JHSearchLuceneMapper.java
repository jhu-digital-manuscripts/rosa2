package rosa.iiif.presentation.core.jhsearch;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

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
import rosa.archive.model.ReferenceSheet;
import rosa.archive.model.Transcription;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
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

	private final PresentationUris pres_uris;

	public JHSearchLuceneMapper(IIIFPresentationRequestFormatter formatter) {
		super(JHSearchField.values());

		this.pres_uris = new PresentationUris(formatter);		        
		
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

        for (String author : bd.getAuthors()) {
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

			if (a != null) {
				methods.add(method.toLowerCase());
			}
		});

		page.getMarks().forEach(a -> {
			String method = a.getMethod();

			if (a != null) {
				methods.add(method.toLowerCase());
			}
		});

		methods.forEach(method -> addField(doc, JHSearchField.METHOD, method));
	}

	// Create document to index a canvas
	private Document create_canvas_document(BookCollection col, Book book, BookImage image) {
		Document doc = new Document();

		String collection_id = pres_uris.getCollectionURI(col.getId());
		String manifest_id =  pres_uris.getManifestURI(col.getId(), book.getId());
		String canvas_id = pres_uris.getCanvasURI(col.getId(), book.getId(), image.getName());

		addField(doc, JHSearchField.COLLECTION_ID, collection_id);

		addField(doc, JHSearchField.OBJECT_ID, canvas_id);
		addField(doc, JHSearchField.OBJECT_TYPE, IIIFNames.SC_CANVAS);
		addField(doc, JHSearchField.OBJECT_LABEL, image.getName());

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
                for (String author : data.getAuthors()) {
                    addField(doc, JHSearchField.PEOPLE, author);
                }
            }
        }

		return doc;
	}

	private void index(BookCollection col, Book book, BookImage image, Symbol symbol, Document doc) {
		addField(doc, JHSearchField.SYMBOL, SearchFieldType.STRING, symbol.getName());
		addField(doc, JHSearchField.SYMBOL, get_lang(symbol), stripTranscribersMarks(symbol.getReferencedText()));
        addField(doc, JHSearchField.TEXT, get_lang(symbol), stripTranscribersMarks(symbol.getReferencedText()));
	}

	private void index(BookCollection col, Book book, BookImage image, Drawing drawing, Document doc) {
		addField(doc, JHSearchField.DRAWING, SearchFieldType.STRING, drawing.getName());
		addField(doc, JHSearchField.DRAWING, get_lang(drawing), stripTranscribersMarks(drawing.getReferencedText()));
        addField(doc, JHSearchField.TEXT, get_lang(drawing), stripTranscribersMarks(drawing.getReferencedText()));
	}

	private void index(BookCollection col, Book book, BookImage image, Errata errata, Document doc) {
		SearchFieldType type = get_lang(errata);

		addField(doc, JHSearchField.ERRATA, type, errata.getAmendedText());
		addField(doc, JHSearchField.ERRATA, type, stripTranscribersMarks(errata.getReferencedText()));
        addField(doc, JHSearchField.TEXT, type, stripTranscribersMarks(errata.getReferencedText()));
	}

	private void index(BookCollection col, Book book, BookImage image, Mark mark, Document doc) {
		addField(doc, JHSearchField.MARK, SearchFieldType.STRING, mark.getName());
		addField(doc, JHSearchField.MARK, get_lang(mark), stripTranscribersMarks(mark.getReferencedText()));
        addField(doc, JHSearchField.TEXT, get_lang(mark), stripTranscribersMarks(mark.getReferencedText()));
	}

	private void index(BookCollection col, Book book, BookImage image, Numeral numeral, Document doc) {
		SearchFieldType type = get_lang(numeral);

		addField(doc, JHSearchField.NUMERAL, type, stripTranscribersMarks(numeral.getReferencedText()));
        addField(doc, JHSearchField.TEXT, type, stripTranscribersMarks(numeral.getReferencedText()));
		addField(doc, JHSearchField.NUMERAL, type, numeral.getNumeral());
	}

	private void index(BookCollection col, Book book, BookImage image, Underline underline, Document doc) {
		addField(doc, JHSearchField.UNDERLINE, get_lang(underline),
                stripTranscribersMarks(underline.getReferencedText()));
        addField(doc, JHSearchField.TEXT, get_lang(underline), stripTranscribersMarks(underline.getReferencedText()));
	}

	private SearchFieldType get_lang(Annotation a) {
		SearchFieldType type = null;

		if (a.getLanguage() != null) {
			type = getSearchFieldTypeForLang(a.getLanguage());
		}

		if (type == null) {
			// TODO Check book metadata for lang
			return SearchFieldType.ENGLISH;
		}

		return type;
	}

	private void index(BookCollection col, Book book, BookImage image, Marginalia marg, Document doc) {
		addField(doc, JHSearchField.MARGINALIA, get_lang(marg), stripTranscribersMarks(marg.getReferencedText()));
        addField(doc, JHSearchField.TEXT, get_lang(marg), stripTranscribersMarks(marg.getReferencedText()));

		StringBuilder transcription = new StringBuilder();
		StringBuilder emphasis = new StringBuilder();

		SearchFieldType marg_lang_type = SearchFieldType.ENGLISH;

		ReferenceSheet peopleRefs = col.getPeopleRef();
		ReferenceSheet bookRefs = col.getBooksRef();
		ReferenceSheet locationRefs = col.getLocationsRef();

		for (MarginaliaLanguage lang : marg.getLanguages()) {
			marg_lang_type = getSearchFieldTypeForLang(lang.getLang());

			for (Position pos : lang.getPositions()) {
				transcription.append(to_string(pos.getTexts()));

				// Index <person> tags + variants

				pos.getPeople().forEach(person -> {
					addField(doc, JHSearchField.PEOPLE, SearchFieldType.ENGLISH, person);

					if (peopleRefs != null && peopleRefs.hasAlternates(person)) {
						peopleRefs.getAlternates(person)
								.forEach(alt -> addField(doc, JHSearchField.PEOPLE, SearchFieldType.ENGLISH, alt));
					}
				});

				// Index <book> tags + variants

				pos.getBooks().forEach(bookRef -> {
					addField(doc, JHSearchField.BOOK, SearchFieldType.ENGLISH, bookRef);

					if (bookRefs != null && bookRefs.hasAlternates(bookRef)) {
						bookRefs.getAlternates(bookRef)
								.forEach(alt -> addField(doc, JHSearchField.BOOK, SearchFieldType.ENGLISH, alt));
					}
				});

				// Index <location> tags + variants

				pos.getLocations().forEach(location -> {
					addField(doc, JHSearchField.PLACE, SearchFieldType.ENGLISH, location);

					if (locationRefs != null && locationRefs.hasAlternates(location)) {
						locationRefs.getAlternates(location)
								.forEach(alt -> addField(doc, JHSearchField.PLACE, SearchFieldType.ENGLISH, alt));
					}
				});

				pos.getEmphasis().forEach(underline -> emphasis.append(stripTranscribersMarks(underline.getReferencedText())).append(" "));

				for (XRef xref : pos.getxRefs()) {
                    addField(doc, JHSearchField.CROSS_REFERENCE, SearchFieldType.ENGLISH, xref.getPerson());
                    addField(doc, JHSearchField.CROSS_REFERENCE, SearchFieldType.ENGLISH, xref.getTitle());
                    
                    if (xref.getText() != null && xref.getLanguage() != null) {
                        SearchFieldType type = getSearchFieldTypeForLang(xref.getLanguage());
                        String text = stripTranscribersMarks(xref.getText());
                        
                        // TODO Do some checks of X-Ref lang and some logging
                        if (type != null) {
                            addField(doc, JHSearchField.CROSS_REFERENCE, type, text);
                        }
                    }
				}
			}
		}
		
		// Remove [] used for transcribers marks

		addField(doc, JHSearchField.MARGINALIA, marg_lang_type, stripTranscribersMarks(transcription.toString()));
		addField(doc, JHSearchField.MARGINALIA, SearchFieldType.ENGLISH, marg.getTranslation());
        addField(doc, JHSearchField.TEXT, SearchFieldType.ENGLISH, marg.getTranslation());
		addField(doc, JHSearchField.EMPHASIS, marg_lang_type, emphasis.toString());
	}

	protected static String stripTranscribersMarks(String s) {
		return s.replace("[", "").replace("]", "");
	}

	private String to_string(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for (String str : list) {
			sb.append(str.trim());
			sb.append(' ');
		}
		return sb.toString();
	}
}
