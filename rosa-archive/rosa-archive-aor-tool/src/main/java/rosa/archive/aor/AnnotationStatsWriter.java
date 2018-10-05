package rosa.archive.aor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import rosa.archive.core.serialize.ImageListSerializer;
import rosa.archive.core.util.CSV;
import rosa.archive.model.BookImage;
import rosa.archive.model.HasId;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.Calculation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.PhysicalLink;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Table;
import rosa.archive.model.aor.TableRow;
import rosa.archive.model.aor.Underline;

// TODO Rewrite to use archive infrastructure

public class AnnotationStatsWriter {
	private static final String LIST_DELIMITER = "|";
	class AnnotationStats {
		String reader;

		String text;
		String translation;
		String type;
		String signature;
		String image_id;
		int image_index;
		String book_id;
		String name;
		String location;
		String method;
		String lang;
		String hand;
		String symbols;

		/** Referenced people */
		String people;
		/** Referenced places */
		String places;
		/** Referenced books */
		String books;
		String internalRefs;
	}

	protected List<AnnotationStats> collectStats(String book_id, Path xml_path, ImageList images) throws IOException {
		AnnotatedPage ap = Util.readAorPage(xml_path.toString());

		List<AnnotationStats> result = new ArrayList<>();

		if (ap == null) {
			return result;
		}

		collect_stats(ap, book_id, images, result);

		return result;
	}

	private void collect_stats(AnnotatedPage ap, String book_id, ImageList images, List<AnnotationStats> result) {
		String image_id = ap.getPage();
		Optional<BookImage> image = images.getImages().stream().filter(i -> i.getId().equals(image_id)).findAny();
		
		if (!image.isPresent()) {
			throw new IllegalStateException("Could not find image " + image_id);
		}
		
		int image_index = images.getImages().indexOf(image.get());
		
		ap.getAnnotations().forEach(a -> {
			result.add(collect_stats(ap, a, book_id, image_index));
		});
	}

	private AnnotationStats collect_stats(AnnotatedPage ap, Annotation a, String book_id, int image_index) {
		AnnotationStats result = new AnnotationStats();

		result.reader = ap.getReader();
		result.book_id = book_id;
		result.image_index = image_index;
		result.image_id = ap.getPage();
		result.signature = ap.getSignature();
		result.text = a.getReferencedText();
		result.type = a.getClass().getSimpleName().toLowerCase();

		if (a.getLocation() != null) {
			result.location = a.getLocation().name().toLowerCase();
		}

		if (a.getLanguage() != null) {
			result.lang = a.getLanguage();
		}

		if (a instanceof Marginalia) {
			collect_marginalia(Marginalia.class.cast(a), result);
		} else if (a instanceof Graph) {
			collect_graph(Graph.class.cast(a), result);
		} else if (a instanceof Table) {
			collect_table(Table.class.cast(a), result);
		} else if (a instanceof Calculation) {
			collect_calculation(Calculation.class.cast(a), result);
		} else if (a instanceof Underline) {
			Underline u = Underline.class.cast(a);

			result.method = u.getMethod();
		} else if (a instanceof Symbol) {
			Symbol s = Symbol.class.cast(a);

			result.name = s.getName();
			result.symbols = s.getName();
		} else if (a instanceof Numeral) {
			Numeral n = Numeral.class.cast(a);

			result.name = n.getNumeral();
		} else if (a instanceof Mark) {
			Mark m = Mark.class.cast(a);

			result.name = m.getName();
			result.method = m.getMethod();
		} else if (a instanceof Drawing) {
			collect_drawing(Drawing.class.cast(a), result);
		} else if (a instanceof Errata) {
			String amend = Errata.class.cast(a).getAmendedText();

			if (amend != null) {
				result.text = result.text + " -> " + amend;
			}
		} else if (a instanceof PhysicalLink) {

		}

		return result;
	}

	private void collect_marginalia(Marginalia m, AnnotationStats result) {
		result.translation = m.getTranslation();
		result.hand = m.getHand();

		if (m.getOtherReader() != null && !m.getOtherReader().isEmpty()) {
			result.reader = m.getOtherReader();
		}

		Set<String> locs = new HashSet<>();
		List<String> texts = new ArrayList<>();
		Set<String> langs = new HashSet<>();
		Set<String> books = new HashSet<>();
		Set<String> people = new HashSet<>();
		Set<String> places = new HashSet<>();
		Set<String> refs = new HashSet<>();
		Set<String> symb = new HashSet<>();

		if (result.lang != null) {
			langs.add(result.lang);
		}

		if (result.location != null) {
			locs.add(result.location);
		}

		m.getLanguages().forEach(ml -> {
			String lang = ml.getLang();

			if (lang != null) {
				langs.add(lang);
			}

			ml.getPositions().forEach(p -> {
				texts.addAll(p.getTexts());

				if (p.getPlace() != null) {
					locs.add(p.getPlace().name().toLowerCase());
				}

				places.addAll(p.getLocations());
				people.addAll(p.getPeople());
				books.addAll(p.getBooks());
				symb.addAll(p.getSymbols());
				refs.addAll( // Concat all target reference IDs of all internal references
						p.getInternalRefs().stream().map(ref ->
							ref.getTargets().stream()
									.map(ReferenceTarget::getTargetId)
									.collect(Collectors.joining(", "))
						).collect(Collectors.toList())
				);
			});
		});

		result.location = join(locs, LIST_DELIMITER);
		result.text = join(texts, " ");
		result.lang = join(langs, LIST_DELIMITER);

		result.books = join(books, LIST_DELIMITER);
		result.places = join(places, LIST_DELIMITER);
		result.people = join(people, LIST_DELIMITER);
		result.symbols = join(symb, LIST_DELIMITER);
		result.internalRefs = join(refs, LIST_DELIMITER);
	}

	private void collect_drawing(Drawing d, AnnotationStats result) {
		result.translation = d.getTranslation();
		result.method = d.getMethod();

		Set<String> hands = new HashSet<>();
		Set<String> texts = new HashSet<>();

		d.getTexts().forEach(text -> {
			hands.add(text.getHand());
			texts.add(text.getText());
		});

		result.hand = join(hands, LIST_DELIMITER);
		result.text = join(texts, " ");

		result.books = join(d.getBooks(), LIST_DELIMITER);
		result.people = join(d.getPeople(), LIST_DELIMITER);
		result.places = join(d.getLocations(), LIST_DELIMITER);
		result.internalRefs = stringify(d.getInternalRefs());
	}

	private void collect_calculation(Calculation c, AnnotationStats result) {
		result.name = c.getType();
		result.method = c.getMethod();
		result.text = c.getContent();
	}

	private void collect_graph(Graph g, AnnotationStats result) {
		Set<String> texts = new HashSet<>();
		Set<String> books = new HashSet<>();
		Set<String> people = new HashSet<>();
		Set<String> places = new HashSet<>();
		Set<String> symbols = new HashSet<>();
		Set<String> translations = new HashSet<>();
		Set<String> hands = new HashSet<>();
		Set<String> langs = new HashSet<>();

		g.getGraphTexts().forEach(gt -> {
			books.addAll(gt.getBooks());
			people.addAll(gt.getPeople());
			places.addAll(gt.getLocations());
			symbols.addAll(gt.getSymbols());
			translations.addAll(gt.getTranslations());

			gt.getNotes().forEach(n -> {
				if (n.content != null) {
					texts.add(n.content);
				}
				hands.add(n.hand);
				langs.add(n.language);
			});
		});

		result.method = g.getMethod();
		result.internalRefs = stringify(g.getInternalRefs());

		result.text = join(texts, " ");
		result.books = join(books, LIST_DELIMITER);
		result.people = join(people, LIST_DELIMITER);
		result.places = join(places, LIST_DELIMITER);
		result.symbols = join(symbols, LIST_DELIMITER);
		result.translation = join(translations, LIST_DELIMITER);
		result.hand = join(hands, LIST_DELIMITER);
		result.lang = join(langs, LIST_DELIMITER);
	}

	private void collect_table(Table t, AnnotationStats result) {
		result.translation = t.getTranslation();

		Set<String> hands = new HashSet<>();
		Set<String> texts = t.getRows().stream().map(TableRow::getHeaderLabel).collect(Collectors.toSet());
		Set<String> langs = new HashSet<>();

		t.getCells().forEach(c -> {
			if (c.content != null) {
				texts.add(c.content);
			}
		});

		t.getTexts().forEach(text -> {
			texts.add(text.getText());
			langs.add(text.getLanguage());
			hands.add(text.getHand());
		});

		result.hand = join(hands, LIST_DELIMITER);
		result.lang = join(langs, LIST_DELIMITER);
		result.text = join(texts, " ");

		result.people = join(t.getPeople(), LIST_DELIMITER);
		result.book_id = join(t.getBooks(), LIST_DELIMITER);
		result.location = join(t.getLocations(), LIST_DELIMITER);
		result.symbols = join(t.getSymbols(), LIST_DELIMITER);
		result.internalRefs = stringify(t.getInternalRefs());
	}

	private static String join(Collection<String> values, String sep) {
		if (values.size() == 0) {
			return "";
		}

		Iterator<String> iter = values.iterator();
		StringBuilder result = new StringBuilder(iter.next());

		while (iter.hasNext()) {
			result.append(sep);
			result.append(iter.next());
		}

		return result.toString();
	}

	private String stringify(List<InternalReference> refs) {
		return refs.stream().map(ref ->
				ref.getTargets().stream()
						.map(ReferenceTarget::getTargetId)
						.collect(Collectors.joining(LIST_DELIMITER))
		).collect(Collectors.joining(LIST_DELIMITER));
	}

	private void write_stats(AnnotationStats stats, PrintWriter out) {
		cell(stats.type, out);
		cell(stats.name, out);
		cell(stats.lang, out);
		cell(stats.location, out);
		cell(stats.reader, out);
		cell(stats.method, out);
		cell(stats.hand, out);
		cell(stats.signature, out);
		cell(stats.image_id, out);
		cell(String.valueOf(stats.image_index), out);
		cell(stats.book_id, out);
		cell(stats.books, out);
		cell(stats.people, out);
		cell(stats.places, out);
		cell(stats.symbols, out);
		cell(stats.internalRefs, out);
		cell(stats.text, out);
		cell(stats.translation, out);

		out.println();
	}

	private void cell(String value, PrintWriter out) {
		if (value != null) {
			// Compress whitespace
			value = value.replaceAll("\\s+", " ");

			out.print(CSV.escape(value));
		}

		out.print(",");
	}

	public void writeStatsHeader(PrintWriter out) {
		out.println(
				"type, name, languages, location, reader, method, hand, signature, image_id, image_index, book_id, books, people, places, symbols, internal_references, text, translation");
	}
	
	public void writeStats(Path book_path, PrintWriter out) throws IOException {
		String book_id = book_path.getFileName().toString();

		ImageListSerializer ils = new ImageListSerializer();
		Path images_file = book_path.resolve(book_id + ".images.csv");
		ImageList images;
		
		try (InputStream is = Files.newInputStream(images_file)) {
			images = ils.read(is, null);
		}
		
		for (Path xml_path : Files.newDirectoryStream(book_path, "*aor*.xml")) {
			collectStats(book_id, xml_path, images).forEach(s -> write_stats(s, out));
			out.flush();
		}
	}
}
