package rosa.archive.aor;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class XmlValidator {
    private static final String DEFAULT_SCHEMA_URL = "http://www.livesandletters.ac.uk/schema/aor_20141118.xsd";
    private static LSResourceResolver resourceResolver = new CachingUrlLSResourceResolver();

    private static List<String> IGNORE = new ArrayList<>();

    static {
        IGNORE.add("XMLSchema");
    }

    /**
     * Run a 'validate' command, validating all XML files in a directory.
     *
     * @param args .
     */
    public static void validate(String[] args) {
        Options options = new Options();

        options.addOption("r", false, "recursive");
        options.addOption("v", false, "verbose");
        options.addOption("S", "schemaUrl", true, "Define the default URL for the schema.");

        CommandLineParser parser = new BasicParser();

        try {
            CommandLine cmd = parser.parse(options, args);
            run(cmd);
        } catch (ParseException e) {
            System.err.println("Failed to parse command.\n" +  e.getMessage());
        }
    }

    private static void run(CommandLine cmd) {
        String[] args = cmd.getArgs();

        boolean recurse = cmd.hasOption("r");
        boolean verbose = cmd.hasOption("v");
        String schemaUrl = cmd.hasOption("schema") ? cmd.getOptionValue("schema") : DEFAULT_SCHEMA_URL;

        SchemaFactory sFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = null;
        try {
            schema = sFactory.newSchema(new URL(schemaUrl));
        } catch (MalformedURLException | SAXException e) {
            System.err.println("Failed to retrieve schema. \n" + e.getMessage());
            System.exit(1);
        }

        for (String path : args) {
            Path p = Paths.get(path);

            if (Files.notExists(p)) {
                continue;
            }

            if (Files.isDirectory(p)) {
                try {
                    handle_directory(p, schema, recurse, verbose);
                } catch (IOException e) {
                    System.err.println("Failed to read directory, skipping. [" + path + "]");
                }
            } else if (Files.isRegularFile(p)) {
                handle_file(p, schema, verbose);
            }
        }
    }

    private static void handle_directory(Path path, Schema schema, boolean recurse, boolean verbose) throws IOException {
        if (path.getFileName().toString().startsWith(".")) {
            return;
        }

        System.out.println("Validating files in directory [" + path.toString() + "]");
        try (DirectoryStream<Path> files = Files.newDirectoryStream(path)) {
            for (Path p : files) {
                if (IGNORE.contains(p.getFileName().toString())) {
                    continue;
                }

                if (Files.isDirectory(p) && recurse) {
                    handle_directory(p, schema, true, verbose);
                } else if (Files.isRegularFile(p)) {
                    handle_file(p, schema, verbose);
                }
            }
        }
    }

    private static void handle_file(final Path path, Schema schema,final boolean verbose) {
        String filename = path.getFileName().toString();
        if (filename.startsWith(".") || !filename.endsWith(".xml")) {
            return;
        }

        Validator validator = schema.newValidator();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) throws SAXException {
                if (!verbose) {
                    System.out.println("  Validating file: [" + path.toString() + "]");
                }
                System.out.println("    [WARNING] " + " ("
                        + e.getLineNumber() + ":" + e.getColumnNumber() + ") - " + e.getMessage());
            }

            @Override
            public void error(SAXParseException e) throws SAXException {
                if (!verbose) {
                    System.out.println("  Validating file: [" + path.toString() + "]");
                }
                System.out.println("    [ERROR] " + " ("
                        + e.getLineNumber() + ":" + e.getColumnNumber() + ") - " + e.getMessage());
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                if (!verbose) {
                    System.out.println("  Validating file: [" + path.toString() + "]");
                }
                System.out.println("    [FATAL ERROR] " + " ("
                        + e.getLineNumber() + ":" + e.getColumnNumber() + ") - " + e.getMessage());
            }
        });
        validator.setResourceResolver(resourceResolver);

        if (verbose) {
            System.out.println("  Validating file: [" + path.toString() + "]");
        }
        try {
            validator.validate(new StreamSource(Files.newInputStream(path)));
        } catch (IOException | SAXException e) {
//            System.out.println("    [EXCEPTION] validating file. ()\n    " + e.getMessage());
        }
    }

}
