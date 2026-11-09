package rosa.archive.aor;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Checks that AoR transcription files are well-formed XML and, when a schema is given,
 * valid against it.
 *
 * <p>Documents with a DOCTYPE are rejected and nothing is ever fetched from the network:
 * the {@code xsi:noNamespaceSchemaLocation} the files carry is ignored, only the local
 * schema given to {@link #AorXmlValidator(Path, List)} is used.</p>
 */
public final class AorXmlValidator {

    private final Validator validator;

    /**
     * Creates a validator.
     *
     * @param schema a local XML Schema (.xsd) file, or {@code null} to check well-formedness only
     * @param errors receives an error when the schema cannot be read; the validator then only
     *               checks well-formedness
     */
    public AorXmlValidator(Path schema, List<String> errors) {
        Validator v = null;
        if (schema != null) {
            try {
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                // The schema and the documents are local files; never fetch anything
                factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                v = factory.newSchema(schema.toFile()).newValidator();
            } catch (SAXException e) {
                errors.add("Cannot read schema [" + schema + "]: " + e.getMessage());
            }
        }
        this.validator = v;
    }

    /** Whether a schema was loaded and files are validated against it. */
    public boolean hasSchema() {
        return validator != null;
    }

    /**
     * Validates one file. Messages are prefixed with {@code context} and carry the
     * {@code (line:column)} of the problem.
     *
     * @param xmlFile  the file to check
     * @param context  prefix for the messages, such as {@code [book/file]}
     * @param errors   receives the errors
     * @param warnings receives the warnings
     * @return true when the file is well-formed (it may still be invalid against the schema)
     */
    public boolean validate(Path xmlFile, String context, List<String> errors, List<String> warnings) {
        int before = errors.size();
        checkWellFormed(xmlFile, context, errors, warnings);
        boolean wellFormed = errors.size() == before;
        if (wellFormed && validator != null) {
            checkAgainstSchema(xmlFile, context, errors, warnings);
        }
        return wellFormed;
    }

    /**
     * Parses the file with a namespace-aware parser that rejects DOCTYPE declarations,
     * reporting parse problems with their position.
     */
    private static void checkWellFormed(Path xmlFile, String context, List<String> errors, List<String> warnings) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Disable external entities for security
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(handler(context, errors, warnings));

            try (InputStream in = Files.newInputStream(xmlFile)) {
                builder.parse(in);
            }
        } catch (ParserConfigurationException e) {
            errors.add(context + " XML parser configuration error: " + e.getMessage());
        } catch (SAXParseException _) {
            // Already reported by the handler, with its position
        } catch (SAXException e) {
            errors.add(context + " Not well-formed XML: " + e.getMessage());
        } catch (IOException e) {
            errors.add(context + " Failed to read: " + e.getMessage());
        }
    }

    /**
     * Validates the file against the compiled schema. The document's own
     * {@code noNamespaceSchemaLocation} is ignored; only the given schema is used.
     */
    private void checkAgainstSchema(Path xmlFile, String context, List<String> errors, List<String> warnings) {
        validator.setErrorHandler(handler(context, errors, warnings));
        try (InputStream in = Files.newInputStream(xmlFile)) {
            validator.validate(new StreamSource(in));
        } catch (SAXParseException _) {
            // Already reported by the handler, with its position
        } catch (SAXException e) {
            errors.add(context + " Not valid: " + e.getMessage());
        } catch (IOException e) {
            errors.add(context + " Failed to read: " + e.getMessage());
        }
    }

    private static ErrorHandler handler(String context, List<String> errors, List<String> warnings) {
        return new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) {
                warnings.add(message("Warning", e));
            }

            @Override
            public void error(SAXParseException e) {
                errors.add(message("Error", e));
            }

            @Override
            public void fatalError(SAXParseException e) {
                errors.add(message("Fatal", e));
            }

            private String message(String level, SAXParseException e) {
                return context + " " + level + " (" + e.getLineNumber() + ":" + e.getColumnNumber() + "): "
                        + e.getMessage();
            }
        };
    }
}
