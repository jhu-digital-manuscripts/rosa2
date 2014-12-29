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
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.core.store.Store;
import rosa.archive.core.store.StoreImpl;
import rosa.archive.model.Book;
import rosa.archive.tool.config.Command;
import rosa.archive.tool.config.Flag;
import rosa.archive.tool.config.ToolConfig;
import rosa.archive.tool.derivative.BookDerivative;
import rosa.archive.tool.derivative.CollectionDerivative;
import rosa.archive.tool.derivative.CropDerivative;

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

        // Set the valid options for the tool
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("property=value")
                .withDescription("set the path of the archive. A default value for this path" +
                        " is set in 'tool-config.properties'")
                .hasArgs(2)
                .withValueSeparator()
                .create("D"));
        options.addOption(new Option(Flag.SHOW_ERRORS.display(), false, "show all errors"));
        options.addOption(new Option(
                Flag.CHECK_BITS.display(), false, "check bit integrity of data in the archive"));
        options.addOption("f", Flag.FORCE.display(), false, "force the operation to execute fully, without skipping data");

        // Apache CLI to parse input args
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse(options, args);

        // Set archive path if the argument exists in the CLI command issued
        ArchiveTool tool;
        if (cmd.hasOption("D") && cmd.getOptionProperties("D").getProperty("archive.path") != null) {
            config.setArchivePath(
                    cmd.getOptionProperties("D").getProperty("archive.path")
            );
        }

        // Create the tool and run the command
        ByteStreamGroup base = new FSByteStreamGroup(config.getArchivePath());
        Store store = new StoreImpl(injector.getInstance(SerializerSet.class), injector.getInstance(BookChecker.class), injector.getInstance(BookCollectionChecker.class), injector.getInstance(AppConfig.class), base);

        tool = new ArchiveTool(store, config, System.out);
        tool.run(cmd);
    }

    /**
     * Run the command
     *
     * @param cmd CLI command
     */
    public void run(CommandLine cmd) {
        String command = cmd.getArgs()[0];

        if (command.equals(Command.LIST.display())) {
            list(cmd);
        } else if (command.equals(Command.CHECK.display())) {
            check(cmd);
        } else if (command.equals(Command.UPDATE.display())) {
            update(cmd);
        } else if (command.equals(Command.UPDATE_IMAGE_LIST.display())) {
            updateImageList(cmd);
        } else if (command.equals(Command.CROP_IMAGES.display())) {
            cropImages(cmd);
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

    private void displayError(String title, List<String> errors) {
        report.println("\n" + title);
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
        boolean showErrors = cmd.hasOption(Flag.SHOW_ERRORS.display());

        List<String> errors = new ArrayList<>();
        switch (args.length) {
            case 1:
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
                break;
            case 2:
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
                break;
            case 3:
                // list <collectionId> <bookId>
                report.println("Stuff in " + args[1] + ":" + args[2]);
                try {
                    Book book = store.loadBook(args[1], args[2], errors);
                    if (book != null) {
                        for (String item : book.getContent()) {
                            report.println("  " + item);
                        }
                    } else {
                        report.println("Failed to read book. [" + args[1] + ":" + args[2] + "]");
                    }

                    if (showErrors && !errors.isEmpty()) {
                        displayError("Errors: ", errors);
                    }
                } catch (IOException e) {
                    displayError("Error: Unable to load book [" + args[1] + ":" + args[2] + "]", args, e);
                }
                break;
            default:
                displayError("Too many arguments. USAGE: list [-options] <collectionId> <bookId>", args);
                break;
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
        String[] args = cmd.getArgs();
        boolean checkBits = cmd.hasOption(Flag.CHECK_BITS.display());

        report.println("Checking...");
        switch (args.length) {
            case 1:
                // check everything
                try {
                    String[] collections =  store.listBookCollections();
                    if (collections != null) {
                        for (String collectionName : collections) {
                            CollectionDerivative cDeriv = new CollectionDerivative(collectionName, report, store);
                            cDeriv.check(checkBits);
                        }
                    }
                } catch (IOException e) {
                    displayError("Error: Unable to check archive.", args, e);
                }
                break;
            case 2:
                // check collection
                CollectionDerivative cDeriv = new CollectionDerivative(args[1], report, store);
                try {
                    cDeriv.check(checkBits);
                } catch (IOException e) {
                    displayError("Error: Unable to check collection. [" + args[1] + "]", args, e);
                }
                break;
            case 3:
                // check book
                BookDerivative bDeriv = new BookDerivative(args[1], args[2], report, store);
                try {
                    bDeriv.check(checkBits);
                } catch (IOException e) {
                    displayError("Error: Unable to load book. [" + args[1] + ":" + args[2] + "]", args, e);
                }
                break;
            default:
                displayError("Too many arguments. USAGE: check [-options] <collectionId> <bookId>", args);
                break;
        }

        report.println("...complete");
    }

    /**
     * Update checksum values in the archive.
     *
     * @param cmd CLI command
     */
    private void update(CommandLine cmd) {
        boolean force = cmd.hasOption(Flag.FORCE.display()) || cmd.hasOption("f");
        String[] args = cmd.getArgs();

        switch (args.length) {
            case 1:
                // update all checksums in all collections
                report.println("Updating all checksums.");
                try {
                    for (String col : store.listBookCollections()) {
                        CollectionDerivative cDeriv = new CollectionDerivative(col, report, store);
                        cDeriv.updateChecksum(force);
                    }
                } catch (IOException e) {
                    displayError("Unable to update checksums.", args, e);
                }

                break;
            case 2:
                // update checksums for the collection (plus all books?)
                String collectionId = args[1];
                report.println("Updating checksum for collection [" + collectionId + "]");

                CollectionDerivative cDeriv = new CollectionDerivative(collectionId, report, store);
                try {
                    cDeriv.updateChecksum(force);
                } catch (IOException e) {
                    displayError("Failed to update checksums for collection. [" + collectionId + "]", args, e);
                }

                break;
            case 3:
                // update checksums only for the book
                String bookId = args[2];
                report.println("Updating checksums for book [" + args[1] + ":" + bookId + "]");

                BookDerivative bDeriv = new BookDerivative(args[1], bookId, report, store);
                try {
                    bDeriv.updateChecksum(force);
                } catch (IOException e) {
                    displayError("Failed to update checksums. [" + args[1] + ":" + bookId + "]", args, e);
                }

                break;
            default:
                displayError("Too many arguments. USAGE: update [-options] <collectionId> <bookId>", args);
                break;
        }
    }

    /**
     * Update / create image list for books in the archive.
     *
     * @param cmd CLI command
     */
    private void updateImageList(CommandLine cmd) {
        boolean force = cmd.hasOption(Flag.FORCE.display()) || cmd.hasOption("f");
        String[] args = cmd.getArgs();

        switch (args.length) {
            case 1:
                try {
                    for (String collection : store.listBookCollections()) {
                        for (String book : store.listBooks(collection)) {
                            report.println("Updating image list for book. [" + collection + ":" + book + "]");
                            BookDerivative bDeriv = new BookDerivative(collection, book, report, store);
                            bDeriv.generateAndWriteImageList(force);
                        }
                    }
                } catch (IOException e) {
                    displayError("Failed to update image lists.", args, e);
                }
                break;
            case 2:
                try {
                    for (String book : store.listBooks(args[1])) {
                        report.println("Updating image list for book. [" + args[1] + ":" + book + "]");
                        BookDerivative bDeriv = new BookDerivative(args[1], book, report, store);
                        bDeriv.generateAndWriteImageList(force);
                    }
                } catch (IOException e) {
                    displayError("Failed to update image lists. [" + args[1] + "]", args, e);
                }
                break;
            case 3:
                BookDerivative bDeriv = new BookDerivative(args[1], args[2], report, store);
                try {
                    report.println("Updating image list for book. [" + args[1] + ":" + args[2] + "]");
                    bDeriv.generateAndWriteImageList(force);
                } catch (IOException e) {
                    displayError("Failed to update image list. [" + args[1] + ":" + args[2] + "]", args, e);
                }
                break;
            default:
                displayError("Too many arguments. Usage: update-image-list [-options] <collectionId> <bookId>", args);
                break;
        }

        report.println("...complete");
    }

    /**
     * Crop book images in the archive and create *.images.crop.csv
     *
     * @param cmd CLI command
     */
    private void cropImages(CommandLine cmd) {
        boolean force = cmd.hasOption(Flag.FORCE.display()) || cmd.hasOption("f");
        String[] args = cmd.getArgs();

        switch (args.length) {
            case 1:
                try {
                    for (String collection : store.listBookCollections()) {
                        for (String book : store.listBooks(collection)) {
                            report.println("Cropping images for book [" + collection + ":" + book + "]");
                            CropDerivative deriv = new CropDerivative(collection, book, report, store);

                            deriv.cropImages(force);
                        }
                    }
                } catch (IOException e) {
                    displayError("Failed to crop images.", args, e);
                }
                break;
            case 2:
                try {
                    for (String book : store.listBooks(args[1])) {
                        report.println("Cropping images for book [" + args[1] + ":" + book + "]");
                        CropDerivative deriv = new CropDerivative(args[1], book, report, store);

                        deriv.cropImages(force);
                    }
                } catch (IOException e) {
                    displayError("Failed to crop images. [" + args[1] + "]", args, e);
                }
                break;
            case 3:
                report.println("Cropping images for book. [" + args[1] + ":" + args[2] + "]");
                CropDerivative deriv = new CropDerivative(args[1], args[2], report, store);
                try {
                    deriv.cropImages(force);
                } catch (IOException e) {
                    displayError("Failed to crop images. [" + args[1] + ":" + args[2] + "]", args, e);
                }
                break;
            default:
                displayError("Too many arguments. Usage: crop-images [-options] <collectionId> <bookId>", args);
                break;
        }
    }
}
