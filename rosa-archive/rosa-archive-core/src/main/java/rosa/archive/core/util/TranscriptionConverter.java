package rosa.archive.core.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

// TODO Validate output as TEI
// TODO The code has become a bit of a mess as the error handling has changed

/**
 * Conversion of text transcriptions to a TEI transcription.
 * Both errors and warnings are logged
 */
public class TranscriptionConverter {
    // TODO schema location must change to the custom TEI schema....
    private static final String TEI_NAMESPACE_URI = "http://www.tei-c.org/ns/1.0";
    private static final String TEI_SCHEMA_URI = "http://www.tei-c.org/release/xml/tei/custom/schema/xsd/tei_ms.xsd";

	private String lastfolio;
	private String lastcol;
	private List<String> warnings;
	private List<String> errors;
	private int nextlinenumber;
	private String foliooverride;

	public TranscriptionConverter() {
		this.warnings = new ArrayList<>();
		this.errors = new ArrayList<>();
	}

	// Set folio to use, overriding folio in text
	public void setFolioOverride(String folio) {
		this.foliooverride = folio;
	}

	public void startConversion(XMLWriter out) throws SAXException {
		lastfolio = null;
		lastcol = null;
		nextlinenumber = 1;
		warnings.clear();
		errors.clear();

		out.startDocument();
        createTEITop(out);

		out.attribute("type", "ms");
		out.startElement("div");
	}

    /**
     * This method will provide proper TEI header content in order for the
     * document to validate. This will result in all transcription data
     * being placed in /TEI/text/body (XPath). All of these tags must be
     * closed when the document is complete.
     *
     * @param out xml writer
     * @throws SAXException
     */
    private void createTEITop(XMLWriter out) throws SAXException {
        out.attribute("xmlns", TEI_NAMESPACE_URI);
        out.attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        out.attribute("xsi:schemaLocation", TEI_NAMESPACE_URI + "    " + TEI_SCHEMA_URI);
        out.startElement("TEI");

        out.startElement("teiHeader");
        out.startElement("fileDesc");

        out.startElement("titleStmt");
        out.startElement("title");
        out.endElement("title");
        out.endElement("titleStmt");

        out.startElement("publicationStmt");
        out.startElement("distributor");
        out.endElement("distributor");
        out.endElement("publicationStmt");

        out.startElement("sourceDesc");
        out.startElement("p");
        out.endElement("p");
        out.endElement("sourceDesc");

        out.endElement("fileDesc");
        out.endElement("teiHeader");

        out.startElement("text");
        out.startElement("body");

    }

	public void convert(File infile, String charset, XMLWriter out) throws IOException, SAXException {
	    ByteBuffer buf = ByteBuffer.wrap(FileUtils.readFileToByteArray(infile));
		String text = Charset.forName(charset).decode(buf).toString();

		convert(text, out);
	}

	public void endConversion(XMLWriter out) throws SAXException {
		out.endElement("div");
        out.endElement("body");
        out.endElement("text");
        out.endElement("TEI");
		out.endDocument();
	}

	public void convert(String text, XMLWriter out) throws SAXException, IOException {
		for (List<Line> b : parse(text)) {
			convert(b, out);
		}
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public List<String> getErrors() {
		return errors;
	}
	
	public List<List<Line>> parse(String text) {
		// Hack for stupid text files
		text = text.replaceAll("\r\n", "\n");
		text = text.replaceAll("\r", "\n");

		String[] textlines = text.split("\n");

		// Find ms
		String ms = null;

		int i = 0;
		for (; i < textlines.length; i++) {
			String s = textlines[i].trim();

			if (s.startsWith("[") && s.endsWith("]")) {
				ms = s.substring(1, s.length() - 1).replaceAll("\\s+", "").toLowerCase();
				i++;
				break;
			}
		}

		if (ms == null) {
			errors.add("Could not find ms decl");
		}

		// Gather lines into blocks. Gather blocks into documents.

		List<List<Line>> doc = new ArrayList<>();
		List<Line> block = null;

		String folio = null;
		String col = null;

		for (; i < textlines.length; i++) {
			String s = textlines[i].trim();

			if (s.length() == 0) {
				if (block != null && block.size() > 0) {
					doc.add(block);
					block = null;
				}
			} else if (s.startsWith("[") && s.endsWith("]")) {
				// parse out ms or folio header

				if (s.substring(1, s.length() - 1).replaceAll("\\s+", "").toLowerCase().equals(ms)) {
					continue;
				}

				String[] parts = s.substring(1, s.length() - 1).split("\\s+");

				if (parts.length != 2) {
					error(i + 1, "Header does not match ms decl and or 'folio col'", s);
				}

				folio = parts[0];

				if (!folio.endsWith("r") && !folio.endsWith("v")) {
					warnings.add("Line " + (i + 1) + "; " + "Odd folio name: " + s);
				}

				try {
					Integer.parseInt(folio.substring(0, folio.length() - 1));
				} catch (NumberFormatException e) {
					warnings.add("Line " + (i + 1) + "; " + "Odd folio name: " + s);
				}

				col = parts[1];

				if (!(col.equals("a") || col.equals("b") || col.equals("c") || col.equals("d"))) {
					error(i + 1, "Invalid column", s);
				}
			} else {
				if (block == null) {
					block = new ArrayList<>();
				}

				if (folio == null) {
					error(i + 1, "Folio or ms declaration missing.", "");
				}

				block.add(new Line(s, i + 1, folio, col));
			}
		}

		if (block != null && block.size() > 0) {
			doc.add(block);
		}
		block = null;

		process(doc);

		return doc;
	}

	public enum LineType {
		POETRY, RUBRIC, CATCHPHRASE, ABSENTLINES, ILLUSTRATION, ANNOTATION
	}

	public class Line {
		String text;
		List<Tag> tags;
		LineType type;
		boolean interpolationStart;
		boolean interpolationEnd;
		boolean interpolated;
		boolean lecoyisletter;
		boolean brokenrhyme;
		String folio;
		String col;

		int linenumber;
		int lecoylinenumber;
		int filelinenumber;

		public Line(String text, int filelinenumber, String folio, String col) {
			this.text = text;
			this.folio = folio;
			this.col = col;
			this.filelinenumber = filelinenumber;
			this.tags = parseTags(text, this, TranscriptionConverter.this);
			this.lecoylinenumber = -1;

			// Interp may start and end on same line
			for (int i = 0; i < tags.size();) {
				Tag t = tags.get(i);

				switch (t.name) {
					case "startInter":
						this.interpolationStart = true;
						strip(t);
						break;
					case "endInter":
						this.interpolationEnd = true;
						strip(t);
						break;
					default:
						i++;
						break;
				}
			}

			determineType();
		}

		private void determineType() {
			this.type = LineType.POETRY;

			if (tags.size() == 1) {
				Tag t = tags.get(0);

				if (t.start == 0 && t.end == text.length()) {
					switch (t.name) {
						case "rubric":
							type = LineType.RUBRIC;
							break;
						case "catchphrase":
							type = LineType.CATCHPHRASE;
							break;
						case "illustration":
							type = LineType.ILLUSTRATION;
							break;
						case "absLine":
							type = LineType.ABSENTLINES;
							break;
						case "ann":
							type = LineType.ANNOTATION;
							break;
						default:
							break;
					}
				}
			}

			// illustration may have multiple tags on same line
			if (tags.size() > 1) {
				boolean allillus = true;

				for (Tag t : tags) {
					if (!t.name.equals("illustration")) {
						allillus = false;
						break;
					}
				}

				if (allillus) {
					type = LineType.ILLUSTRATION;
				}
			}

			// special check for rubric with note
			if (tags.size() == 2) {
				if (tags.get(0).name.equals("rubric") && tags.get(1).name.equals("cn")) {
					type = LineType.RUBRIC;
				}
			}
		}

		// Remove tagged text
		public void strip(Tag tag) {
			text = text.substring(0, tag.start) + text.substring(tag.end);
			text = text.trim();
			tags = parseTags(text, this, TranscriptionConverter.this);

			determineType();
		}

		public String toString() {
			return "[Line " + filelinenumber + "] " + text;
		}

		public Tag findAnchor(String name, String anchorid) {
			for (Tag tag : tags) {
				if (tag.name.equals(name) && tag.anchorid != null && tag.anchorid.equals(anchorid)) {
					return tag;
				}
			}

			return null;
		}
	}

	private void warn(Line line, String msg) {
		warnings.add("Line "
				+ line.filelinenumber
				+ (line.lecoylinenumber == -1 ? "" : "[lecoy "
						+ line.lecoylinenumber + "]") + "; " + msg + ": "
				+ line.text);
	}

	private void error(Line line, String msg) {
		errors.add("Line "
				+ line.filelinenumber
				+ (line.lecoylinenumber == -1 ? "" : "[lecoy "
						+ line.lecoylinenumber + "]") + "; " + msg + ": "
				+ line.text);
	}

	private void error(int line, String msg, String text) {
		errors.add("Line " + line + "; " + msg + ": " + text);
	}

	private void process(List<List<Line>> doc) {
		// Jam together adjacent blocks if they make 4 lines of poetry...

		for (int k = 0; k < doc.size();) {
			List<Line> block1 = doc.get(k);

			if (k == doc.size() - 1) {
				break;
			}

			List<Line> block2 = doc.get(k + 1);

			int n1 = 0;
			for (Line line : block1) {
				if (line.type == LineType.POETRY) {
					n1++;
				}
			}

			int n2 = 0;
			for (Line line : block2) {
				if (line.type == LineType.POETRY) {
					n2++;
				}
			}

			if (n1 + n2 == 4) {
				block1.addAll(block2);
				doc.remove(k + 1);
			}

			k++;
		}

		boolean interpstate = false; // In interpolation

		for (int k = 0; k < doc.size(); k++) {
			List<Line> block = doc.get(k);

			// process anchors
			for (Line line : block) {
				for (Tag tag : line.tags) {
					if (tag.name.equals("cn") || tag.name.equals("corr") || tag.name.equals("order")) {
						String attr = tag.name.equals("order") ? "id" : "n";
						String val = tag.attrs.get(attr);

						if (val == null) {
							error(line, "Need " + attr + " attribute");
						}

						tag.anchorid = val;
					}
				}
			}

			// Strip out lines with a note
			for (int i = 0; i < block.size();) {
				Line line = block.get(i);

				// check to make sure notes have no other tags with them
				if (line.tags.size() > 1) {
					for (Tag tag : line.tags) {
						if (tag.name.endsWith("Note")) {
							error(line, "Note must be on line by itself.");
						}
					}
				}

				if (line.tags.size() != 1) {
					i++;
					continue;
				}

				Tag tag = line.tags.get(0);

				if (!tag.name.endsWith("Note")) {
					i++;
					continue;
				}

				String type = tag.name.replace("Note", "");

				String attr = type.equals("order") ? "idref" : "n";
				String anchorid = tag.attrs.get(attr);

				if (anchorid == null) {
					error(line, "Need " + attr + " attribute");
					break;
				}

				// find anchor by checking current block and all previous blocks
				Tag anchor = null;

				docloop: for (int n = k; n >= 0; n--) {
					for (Line l : doc.get(n)) {
						Tag t = l.findAnchor(type, anchorid);

						if (t != null) {
							anchor = t;
							break docloop;
						}
					}
				}

				if (anchor == null) {
					error(line, "No anchor for note");
					break;
				}

				anchor.text = tag.text;
				anchor.anchorid = null;

				// Don't have to worry about stripping tag, because anchor and
				// not cannot be on same line.
				line.strip(tag);

				if (line.text.trim().length() == 0) {
					block.remove(i);
				} else {
					error(line, "No text allowed around note.");
					break;
				}
			}

			// Check for broken rhymes
			for (Line line : block) {
				line.brokenrhyme = false;

				for (Tag tag : line.tags) {
					if (tag.name.equals("brokenRhyme")) {
						line.brokenrhyme = true;
					}
				}
			}

			// Do logical numbering
			int numpoetry = 0;
			for (Line line : block) {
				if (line.type == LineType.POETRY) {
					line.linenumber = nextlinenumber++;
					numpoetry++;
				}
			}

			// Last quartet may not have all lines
			if (numpoetry != 0 && numpoetry != 4 && k != doc.size() - 1) {
				warn(block.get(0), "Block does not have 4 lines of poetry");
			}

			// Mark interpolated lines
			for (Line line : block) {
				if (line.interpolationStart) {
					line.interpolated = true;
					interpstate = true;
				} else if (line.interpolationEnd) {
					line.interpolated = true;
					interpstate = false;
				} else {
					line.interpolated = interpstate;
				}
			}

			boolean allinterp = true;
			for (Line line : block) {
				if (!line.interpolated) {
					allinterp = false;
					break;
				}
			}

			// Find and strip lecoy number of group
			int lecoyline = -1;

			for (int i = 0; i < block.size(); i++) {
				Line line = block.get(i);

				Pattern p = Pattern.compile("(.*)\\s+(\\d+)");
				Matcher m = p.matcher(line.text);

				if (m.matches()) {
					line.text = m.group(1).trim();
					line.determineType();
					line.lecoylinenumber = Integer.parseInt(m.group(2));
					lecoyline = i;
				} else {
					// Check for lecoy letter if no number
					Pattern p2 = Pattern.compile("(.*)\\s+([a-z])");
					Matcher m2 = p2.matcher(line.text);

					if (m2.matches()) {
						line.text = m2.group(1).trim();
						line.determineType();
						line.lecoylinenumber = m2.group(2).charAt(0);
						line.lecoyisletter = true;
						lecoyline = i;
					}
				}
			}

			if (lecoyline == -1 && numpoetry > 0 && k != doc.size() - 1 && !allinterp) {
				warn(block.get(0), "Line " + "No lecoy number for quartet");
			}

			// Do lecoy numbering
			if (lecoyline != -1) {
				int lecoy = block.get(lecoyline).lecoylinenumber;

				for (int i = lecoyline - 1; i >= 0; i--) {
					Line line = block.get(i);

					if (line.type == LineType.POETRY && !line.interpolated) {
						if (line.lecoylinenumber == -1) {
							line.lecoylinenumber = --lecoy;
						} else {
							lecoy = line.lecoylinenumber;
						}
					}
				}

				lecoy = block.get(lecoyline).lecoylinenumber;

				for (int i = lecoyline + 1; i < block.size(); i++) {
					Line line = block.get(i);

					if (line.type == LineType.POETRY && !line.interpolated) {
						if (line.lecoylinenumber == -1) {
							line.lecoylinenumber = ++lecoy;
						} else {
							lecoy = line.lecoylinenumber;
						}
					}
				}
			}
		}

		// Make sure that every anchor has a note
		for (List<Line> block : doc) {
			// process anchors

			for (Line line : block) {
				for (Tag tag : line.tags) {
					if (tag.name.equals("cn") || tag.name.equals("corr") || tag.name.equals("order")) {

						if (tag.anchorid != null) {
							warn(line, "Anchor without note");
						}
					}
				}
			}
		}
	}

	private void convert(List<Line> block, XMLWriter out) throws IOException, SAXException {
		// Convert the block
		boolean incouplet = false;

		for (Line line : block) {
			if (lastfolio == null || !line.folio.equals(lastfolio) || !line.col.equals(lastcol)) {

				out.attribute("n", foliooverride == null ? line.folio : foliooverride);
				out.emptyElement("pb");

				out.attribute("n", line.col);
				out.emptyElement("cb");

				lastfolio = line.folio;
				lastcol = line.col;
			}

			Tag tag;
			switch (line.type) {
				case POETRY:
					if (incouplet) {
						convertNormalLine(line, out);
						out.endElement("lg");

						incouplet = false;
					} else {
						out.attribute("type", "couplet");
						out.startElement("lg");
						convertNormalLine(line, out);

						incouplet = true;
					}
					break;
				case ABSENTLINES:
					tag = line.tags.get(0);

					out.startElement("l");
					out.startElement("gap");
					out.startElement("desc");
					convertText(tag.text, line, false, out);
					out.endElement("desc");
					out.endElement("gap");
					out.endElement("l");
					break;
				case CATCHPHRASE:
					tag = line.tags.get(0);

					out.attribute("type", "catch");
					out.startElement("fw");
					convertText(tag.text, line, false, out);
					out.endElement("fw");
					break;
				case ILLUSTRATION:
					for (Tag t : line.tags) {
						String val = t.attrs.get("characters");
						val = val == null ? "" : val;

						out.attribute("type", "miniature");

						out.startElement("figure");
						out.startElement("head");
						convertText(t.text, line, false, out);
						out.endElement("head");

						for (String s : val.split(",")) {
							s = s.trim();

							if (s.length() > 0) {
								out.attribute("type", "character");
								out.startElement("note");
								convertText(s, line, false, out);
								out.endElement("note");
							}
						}

                        out.endElement("figure");
					}
					break;
				case RUBRIC:
					tag = line.tags.get(0);

                    out.startElement("l");
                    out.attribute("rend", "rubric");
                    out.startElement("hi");
                    convertText(tag.text, line, false, out);
					out.endElement("hi");

					for (int i = 1; i < line.tags.size(); i++) {
						if (!convertNote(line.tags.get(i), out)) {
							error(line, "Unexpected tag: " + tag.name);
						}
					}

					out.endElement("l");
					break;
				case ANNOTATION:
					tag = line.tags.get(0);

                    out.startElement("l");
                    out.attribute("type", "scribalAnnotation");
                    out.startElement("note");
                    convertText(tag.text, line, false, out);
					out.endElement("note");
					out.endElement("l");
					break;
				default:
					error(line, "Unhandled type: " + line.type);
					break;

			}
		}

		if (incouplet) {
			// warn(block.get(0), "Couplet trouble");
			out.endElement("lg");
			incouplet = false;
		}
	}

	private int findNextAbbrevMark(String text, int offset) {
		for (;;) {
			int i = text.indexOf('/', offset);

			if (i == -1) {
				return -1;
			}

			if (i > 0 && text.charAt(i - 1) == '<') {
				// scribal notation </>
				offset = i + 1;
			} else {
				return i;
			}
		}
	}

	// Handle abbrev and scribal notation
	private void convertInternalText(String text, Line line, XMLWriter out)
			throws SAXException, IOException {
		for (int offset = 0; offset < text.length();) {
			int i = findNextAbbrevMark(text, offset);

			if (i == -1) {
				convertIntenalTextWithScribalNotation(text.substring(offset), line, out);
				break;
			}

			int j = findNextAbbrevMark(text, i + 1);

			if (j == -1) {
				convertIntenalTextWithScribalNotation(text.substring(offset), line, out);
				out.text(text.substring(offset));
				break;
			}

			convertIntenalTextWithScribalNotation(text.substring(offset, i), line, out);
			out.startElement("expan");
			convertIntenalTextWithScribalNotation(text.substring(i + 1, j), line, out);
			out.endElement("expan");
			offset = j + 1;
		}
	}

	private void convertIntenalTextWithScribalNotation(String text, Line line, XMLWriter out)
			throws SAXException, IOException {
		for (int offset = 0; offset < text.length();) {
			int i = text.indexOf('<', offset);

			if (i == -1) {
				out.text(text.substring(offset));
				break;
			}

			int j = text.indexOf('>', i);

			if (j == -1) {
				out.text(text.substring(offset));
				break;
			}

			j++;

			out.text(text.substring(offset, i));

			String name = text.substring(i + 1, j - 1);

			switch (name) {
				default:
					error(line, "Unknown scribal notation: " + text);
					break;
				case ".":
				case "-":
				case "/":
				case ":":
				case ".\'":
				case ".7":
					break;
			}

			out.attribute("type", "scribalPunc");
			out.startElement("note");
			out.text(name);
			out.endElement("note");

			offset = j;
		}
	}

	private void convertText(String text, Line line, boolean initial, XMLWriter out)
			throws SAXException, IOException {
		if (initial) {
			out.attribute("rend", "init");
			out.startElement("hi");
			out.text(text.substring(0, 1));
			out.endElement("hi");

			text = text.substring(1);
		}

		convertInternalText(text, line, out);
	}

	private void convertNormalLine(Line line, XMLWriter out) throws IOException, SAXException {
        /*
         TODO 'rhyme' attribute is not included in any of the community supported TEI schema

         this attribute exists within the full TEI spec within the "verse" module. This module
         does not seem to be included in the major community schema. A TEI schema can be customized
         to include this module.

         For the community supported schema, see http://www.tei-c.org/release/xml/tei/custom/schema/xsd/
         */
//		if (line.brokenrhyme) {
//			out.attribute("rhyme", "X");
//		}
		out.startElement("l");

		int offset = 0;
		boolean initial = false;

		for (int i = 0; i < line.tags.size(); i++) {
			Tag tag = line.tags.get(i);

			String s = line.text.substring(offset, tag.start);

			if (s.length() > 0) {
				convertText(s, line, initial, out);
				initial = false;
			}

			switch (tag.name) {
				case "initial":
					initial = true;
					break;
				case "nota":
					out.attribute("rend", "nota");
					out.startElement("hi");
					out.text("" + (char) 0x261a);
					out.endElement("hi");
					break;
				case "added":
					out.startElement("add");
					convertText(tag.text, line, false, out);
					out.endElement("add");
					break;
				case "expg":
					out.startElement("del");
					convertText(tag.text, line, false, out);
					out.endElement("del");
					break;
				case "ann":
					out.attribute("type", "scribalAnnotation");
					out.startElement("note");
					convertText(tag.text, line, false, out);
					out.endElement("note");
					break;
				case "rubric":
					out.attribute("rend", "rubric");
					out.startElement("hi");
					convertText(tag.text, line, false, out);
					out.endElement("hi");
					break;
				case "brokenRhyme":
					break;
				default:
					if (!convertNote(tag, out)) {
						error(line, "Unexpected tag " + tag.name);
					}
					break;
			}

			offset = tag.end;
		}

		if (offset < line.text.length()) {
			convertText(line.text.substring(offset), line, initial, out);
		}

		if (line.lecoylinenumber != -1) {
			out.attribute("n",
					line.lecoyisletter ? "" + (char) line.lecoylinenumber : "" + line.lecoylinenumber);
			out.attribute("ed", "lecoy");
			out.attribute("unit", "line");
			out.emptyElement("milestone");
		}

		out.endElement("l");
	}

	private boolean convertNote(Tag tag, XMLWriter out) throws SAXException {
		if (tag.anchorid != null) {
			// Note missing, already added warning
			return true;
		}

		switch (tag.name) {
			case "cn":
				out.attribute("type", "critical");
				out.startElement("note");
				out.text(tag.text);
				out.endElement("note");

				return true;
			case "order":
				out.attribute("type", "order");
				out.startElement("note");
				out.text(tag.text);
				out.endElement("note");
				return true;
			case "corr":
				out.attribute("type", "correction");
				out.startElement("note");
				out.text(tag.text);
				out.endElement("note");
				return true;
			default:
				return false;
		}
	}

	private static class Tag {
		int start;
		int end;
		String name;
		String text; // enclosed text or note if anchor
		String anchorid;

		Map<String, String> attrs;

		Tag() {
			this.attrs = new HashMap<>(2);
		}
	}

	// Tags cannot nest and do not span lines.
	// Ignore scribal notation and parse it later
	private static boolean isScribalNotation(String s, int start, int end) {
		int len = end - start;
		return len == 3
				&& !Character.isLetter(s.charAt(start + 1))
				|| (
					len == 4
					&& !Character.isLetter(s.charAt(start + 1))
					&& !Character.isLetter(s.charAt(start + 2))
				   );
	}

	private static List<Tag> parseTags(String s, Line line, TranscriptionConverter conv) {
		List<Tag> tags = new ArrayList<>();

		for (int offset = 0;;) {
			Tag tag = new Tag();

			tag.start = s.indexOf('<', offset);

			if (tag.start == -1) {
				break;
			}

			tag.end = s.indexOf('>', tag.start);

			if (tag.end == -1) {
				conv.error(line, "Malformed tag: " + s);
				return tags;
			}

			tag.end++;

			if (isScribalNotation(s, tag.start, tag.end)) {
				offset = tag.end;
				continue;
			}

			// parse tag name
			int namestart = tag.start + 1;

			if (s.charAt(namestart) == '/') {
				namestart++;
			}

			int i = namestart;

			for (; i < tag.end; i++) {
				if (!Character.isLetterOrDigit(s.charAt(i))) {
					tag.name = s.substring(namestart, i);
					break;
				}
			}

			if (tag.name == null) {
				conv.error(line, "Malformed tag: " + s);
				return tags;
			}

			// parse attrs

			int attrend = tag.end - 1;
			boolean single = false;

			if (s.charAt(attrend - 1) == '/') {
				single = true;
				attrend--;
			}

			if (i < attrend) {
				String[] attrs = s.substring(i, attrend).split("=");

				if ((attrs.length & 1) > 0) {
					conv.error(line, "Malformed tag attrs: " + s);
					return tags;
				}

				for (int j = 0; j < attrs.length;) {
					String name = attrs[j++].trim();
					String val = attrs[j++].trim();

					if (!val.startsWith("\"") || !val.endsWith("\"")) {
						conv.error(line, "Malformed tag attrs: " + s);
						return tags;
					}

					val = val.substring(1, val.length() - 1).trim();

					tag.attrs.put(name, val);
				}
			}

			offset = tag.end;
			int textstart = tag.end;

			while (!single) {
				// Look for close tag while skipping scribal notation
				int closestart = s.indexOf('<', offset);

				if (closestart == -1) {
					conv.error(line, "Tag not closed: " + s);
					return tags;
				}

				tag.end = s.indexOf('>', closestart);

				if (tag.end == -1) {
					conv.error(line, "Malformed tag: " + s);
					return tags;
				}

				tag.end++;

				namestart = tag.start + 1;

				if (isScribalNotation(s, closestart, tag.end)) {
					offset = tag.end;
					continue;
				}

				if (s.charAt(closestart + 1) != '/') {
					conv.error(line, "Expecting close tag: " + s);
					return tags;
				}

				if (!s.subSequence(closestart + 2, tag.end - 1)
						.equals(tag.name)) {
					conv.error(line, "Nested tags: " + s);
					return tags;
				}

				tag.text = s.substring(textstart, closestart).trim();
				offset = tag.end;
				break;
			}

			tags.add(tag);
		}

		return tags;
	}
}
