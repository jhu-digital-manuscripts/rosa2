package rosa.archive.tool;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.store.Store;
import rosa.archive.core.store.StoreFactory;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.tool.config.ToolConfig;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class ArchiveTool {

    private ToolConfig config;
    private Store store;
    
    private PrintStream report;

    public ArchiveTool(Store store, ToolConfig config) {
        this.store = store;
        this.config = config;

        this.report = System.out;
    }
    
    public ArchiveTool(Store store, ToolConfig config, PrintStream report) {
        this(store, config);
        this.report = report;
    }

    public static void main(String[] args) throws ParseException, IOException {
        if (args.length < 1) {
            System.out.println("A command must be issued.");
            System.exit(1);
        }

        Injector injector = Guice.createInjector(new ToolModule(), new ArchiveCoreModule());

        ToolConfig config = injector.getInstance(ToolConfig.class);
        StoreFactory sFactory = injector.getInstance(StoreFactory.class);

        // Set the valid options for the tool
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("archive.path=value")
                .withDescription("set the path of the archive. A default value for this path" +
                        " can is set in 'tool-config.properties'")
                .hasArgs(2)
                .withValueSeparator()
                .create("D"));
        options.addOption(new Option(config.getFLAG_SHOW_ERRORS(), false, "show all errors"));
        options.addOption(new Option(
                config.getFLAG_CHECK_BITS(), false, "check bit integrity of data in the archive"));

        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse(options, args);

        // Set archive path if the argument exists in the CLI command issued
        ArchiveTool tool;
        if (cmd.hasOption("D")) {
            config.setARCHIVE_PATH(
                    cmd.getOptionProperties("D").getProperty("archive.path")
            );
        }

        // Create the tool and run the command
        ByteStreamGroup base = new FSByteStreamGroup(config.getARCHIVE_PATH());
        Store store = sFactory.create(base);

        tool = new ArchiveTool(store, config);
        tool.run(cmd);
    }

    /**
     * Run the command
     *
     * @param cmd CLI command
     */
    public void run(CommandLine cmd) {
        String command = cmd.getArgs()[0];

        if (command.equals(config.getCMD_LIST())) {
            list(cmd);
        } else if (command.equals(config.getCMD_CHECK())) {
            check(cmd);
        }
    }

    private void displayError(String message, String[] args) {
        report.println("Command: " + Arrays.toString(args));
        report.println(message);
    }

    private void displayError(String message, String[] args, Exception e) {
        displayError(message, args);
        e.printStackTrace(report);
    }

    private void displayError(List<String> errors) {
        report.println("\nErrors: ");
        for (String error : errors) {
            report.println("  " + error);
        }
    }

    /**
     * List items in the archive according to the command arguments.
     *
     * @param cmd CLI command
     */
    private void list(CommandLine cmd) {
        String[] args = cmd.getArgs();
        boolean showErrors = cmd.hasOption(config.getFLAG_SHOW_ERRORS());

        // list
        List<String> errors = new ArrayList<>();
        if (args.length == 1) {
            // list
            report.println("Collections: ");
            try {
                String[] collectionNames = store.listBookCollections();
                for (String name : collectionNames) {
                    report.println("  " + name);
                }
            } catch (IOException e) {
                displayError("Error: Unable to read collection names.", args, e);
            }
        } else if (args.length == 2) {
            // list <collectionId>
            report.println("Books in " + args[1]);
            try {
                String[] books = store.listBooks(args[1]);
                for (String name : books) {
                    report.println("  " + name);
                }
            } catch (IOException e) {
                displayError("Error: Unable to read book names in collection [" + args[1] + "]", args, e);
            }
        } else if (args.length == 3) {
            // list <collectionId> <bookId>
            report.println("Stuff in " + args[1] + ":" + args[2]);
            try {
                Book book = store.loadBook(args[1], args[2], errors);
                for (String item : book.getContent()) {
                    report.println("  " + item);
                }

                if (showErrors && !errors.isEmpty()) {
                    report.println("\nErrors found while processing:");
                    for (String err : errors) {
                        report.println("  " + err);
                    }
                }
            } catch (IOException e) {
                displayError("Error: Unable to load book [" + args[1] + ":" + args[2] + "]", args, e);
            }
        } else {
            displayError("Too many arguments. USAGE: list <collectionId> <bookId>", args);
        }

        if (!showErrors && !errors.isEmpty()) {
            report.println("\nErrors were found while processing the command. Use the -showErrors flag " +
                    "to display the errors.");
        }
    }

    /**
     * Checks the data consistency and/or bit integrity of items in the archive.
     *
     * @param cmd CLI command
     */
    private void check(CommandLine cmd) {
        List<String> errors = new ArrayList<>();

        String[] args = cmd.getArgs();
        boolean checkBits = cmd.hasOption(config.getFLAG_CHECK_BITS());

        report.println("Checking...");

        List<String> loadingErrors = new ArrayList<>();
        if (args.length == 1) {
            // check everything
            try {
                String[] collections =  store.listBookCollections();
                for (String collectionName : collections) {
                    List<String> e = new ArrayList<>();

                    BookCollection collection = store.loadBookCollection(collectionName, loadingErrors);
                    report.println(collectionName);
                    store.check(collection, checkBits, e);

                    if (!e.isEmpty()) {
                        displayError(e);
                        e.clear();
                    }

                    for (String bookName : store.listBooks(collectionName)) {
                        Book book = store.loadBook(collectionName, bookName, loadingErrors);
                        report.println("\n-" + bookName);
                        store.check(collection, book, checkBits, e);

                        if (!e.isEmpty()) {
                            displayError(e);
                            e.clear();
                        }
                    }
                }
            } catch (IOException e) {
                displayError("Error: Unable to check archive.", args, e);
            }
        } else if (args.length == 2) {
            // check collection
            try {
                BookCollection collection = store.loadBookCollection(args[1], loadingErrors);
                store.check(collection, checkBits, errors);
            } catch (IOException e) {
                displayError("Error: Unable to load collection. [" + args[1] + "]", args, e);
            }
        } else if (args.length == 3) {
            // check book
            try {
                BookCollection collection = store.loadBookCollection(args[1], loadingErrors);
                Book book = store.loadBook(args[1], args[2], loadingErrors);
                store.check(collection, book, checkBits, errors);
            } catch (IOException e) {
                displayError("Error: Unable to load book. [" + args[1] + ":" + args[2] + "]", args, e);
            }
        } else {
            displayError("Too many arguments. USAGE: check <collectionId> <bookId>", args);
        }

        report.println("...complete");
        if (!errors.isEmpty()) {
            displayError(errors);
        }
    }

}
