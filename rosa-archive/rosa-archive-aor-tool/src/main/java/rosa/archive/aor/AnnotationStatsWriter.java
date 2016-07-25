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

import rosa.archive.core.serialize.ImageListSerializer;
import rosa.archive.core.util.CSV;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;

// TODO Rewrite to use archive infrastructure

public class AnnotationStatsWriter {
	protected class AnnotationStats {

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
		String marginalia_people;
		String marginalia_places;
		String marginalia_books;
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
			Marginalia m = Marginalia.class.cast(a);

			result.translation = m.getTranslation();

			Set<String> locs = new HashSet<>();
			List<String> texts = new ArrayList<>();
			Set<String> langs = new HashSet<>();
			Set<String> books = new HashSet<>();
			Set<String> people = new HashSet<>();
			Set<String> places = new HashSet<>();

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
				});
			});

			result.location = join(locs, "|");
			result.text = join(texts, " ");
			result.lang = join(langs, "|");

			result.marginalia_books = join(books, "|");
			result.marginalia_places = join(places, "|");
			result.marginalia_people = join(people, "|");

		} else if (a instanceof Underline) {
			Underline u = Underline.class.cast(a);

			result.method = u.getMethod();
		} else if (a instanceof Symbol) {
			Symbol s = Symbol.class.cast(a);

			result.name = s.getName();
		} else if (a instanceof Numeral) {
			Numeral n = Numeral.class.cast(a);

			result.name = n.getNumeral();
		} else if (a instanceof Mark) {
			Mark m = Mark.class.cast(a);

			result.name = m.getName();
			result.method = m.getMethod();
		} else if (a instanceof Drawing) {
			result.name = Drawing.class.cast(a).getName();
		} else if (a instanceof Errata) {
			String amend = Errata.class.cast(a).getAmendedText();

			if (amend != null) {
				result.text = result.text + " -> " + amend;
			}
		}

		return result;
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

	private void write_stats(AnnotationStats stats, PrintWriter out) {
		cell(stats.type, out);
		cell(stats.name, out);
		cell(stats.lang, out);
		cell(stats.location, out);
		cell(stats.method, out);
		cell(stats.signature, out);
		cell(stats.image_id, out);
		cell(String.valueOf(stats.image_index), out);
		cell(stats.book_id, out);
		cell(stats.marginalia_books, out);
		cell(stats.marginalia_people, out);
		cell(stats.marginalia_places, out);
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
				"type, name, languages, locations, method, signature, image_id, image_index, book_id, marginalia_books, marginalia_people, marginalia_places, text, translation");		
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
