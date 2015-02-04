package rosa.archive.tool;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.commons.cli.UnrecognizedOptionException;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.model.Book;
import rosa.archive.tool.config.Command;
import rosa.archive.tool.config.Flag;
import rosa.archive.tool.config.ToolConfig;
import rosa.archive.tool.derivative.BookDerivative;
import rosa.archive.tool.derivative.CollectionDerivative;
import rosa.archive.tool.derivative.CropDerivative;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 *
 */
public class ArchiveTool {
    private final ToolConfig config;
    private final Store store;
    private final PrintStream report;

    public ArchiveTool(Store store, ToolConfig config) {
        this(store, config, System.out);
    }
    
    public ArchiveTool(Store store, ToolConfig config, PrintStream report) {
        this.store = store;
        this.config = config;
        this.report = report;
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) throws ParseException, IOException {
        if (args.length < 1) {
            System.out.println("A command must be issued.");
            System.exit(1);
        }

        Injector injector = Guice.createInjector(new ToolModule(), new ArchiveCoreModule());

        ToolConfig config = injector.getInstance(ToolConfig.class);

        CommandLineParser parser = new BasicParser();
        Options options = new Options();

        options.addOption(OptionBuilder.withArgName("property=value")
                .withDescription("set the path of the archive. A default value for this path" +
                        " is set in 'tool-config.properties'")
                .hasArgs(2)
                .withValueSeparator()
                .create("D"));

        // Set specific options TODO need a more generic way of getting "command"
        switch (getCommand(args[0])) {
        case LIST:
            break;
        case CHECK:
            options.addOption(new Option(Flag.CHECK_BITS.shortName(),
                    Flag.CHECK_BITS.longName(), false, "check bit integrity of data in the archive"));
            break;
        case UPDATE:
            options.addOption(Flag.FORCE.shortName(), Flag.FORCE.longName(), false,
                    "force the operation to execute fully, without skipping data");
            break;
        case UPDATE_IMAGE_LIST:
            options.addOption(Flag.FORCE.shortName(), Flag.FORCE.longName(), false,
                    "force the operation to execute fully, overwriting any current image list.");
            break;
        case CROP_IMAGES:
            options.addOption(Flag.FORCE.shortName(), Flag.FORCE.longName(), false,
                    "force the operation to execute fully, overwriting any current cropped images.");
            break;
        default:
            break;
        }

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            System.out.println("Bad option found. [" + e.getOption() + "]");
            System.exit(1);
        } catch (ParseException e) {
            System.out.println("Unable to parse command.");
            System.exit(1);
        }

        // Set archive path if the argument exists in the CLI command issued
        if (cmd.hasOption("D") && cmd.getOptionProperties("D").getProperty("archive.path") != null) {
            config.setArchivePath(
                    cmd.getOptionProperties("D").getProperty("archive.path")
            );
        }

        // Create the tool and run the command
        ByteStreamGroup base = new FSByteStreamGroup(config.getArchivePath());
        Store store = new StoreImpl(injector.getInstance(SerializerSet.class), injector.getInstance(BookChecker.class), injector.getInstance(BookCollectionChecker.class), base);

        ArchiveTool tool = new ArchiveTool(store, config);
        tool.run(cmd);
    }

    private static Command getCommand(String cmd) {
        for (Command c : Command.values()) {
            if (c.display().equals(cmd)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Run the command
     *
     * @param cmd CLI command
     */
    public void run(CommandLine cmd) {
        String command = cmd.getArgs()[0];

        report.println("Archive: " + config.getArchivePath());

        switch (getCommand(command)) {
        case LIST:
            list(cmd);
            break;
        case CHECK:
            check(cmd);
            break;
        case UPDATE:
            update(cmd);
            break;
        case UPDATE_IMAGE_LIST:
            updateImageList(cmd);
            break;
        case CROP_IMAGES:
            cropImages(cmd);
            break;
        default:
            throw new RuntimeException("Unknown command. [" + Arrays.toString(cmd.getArgs()) + "]");
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

    /**
     * List items in the archive according to the command arguments.
     *
     * @param cmd CLI command
     */
    private void list(CommandLine cmd) {
        String[] args = cmd.getArgs();

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
                    Book book = store.loadBook(store.loadBookCollection(args[1], errors), args[2], errors);
                    if (book != null) {
                        for (String item : book.getContent()) {
                            report.println("  " + item);
                        }
                    } else {
                        report.println("Failed to read book. [" + args[1] + ":" + args[2] + "]");
                    }
                } catch (IOException e) {
                    displayError("Error: Unable to load book [" + args[1] + ":" + args[2] + "]", args, e);
                }
                break;
            default:
                displayError("Too many arguments. USAGE: list [-options] <collectionId> <bookId>", args);
                break;
        }
    }

    /**
     * Checks the data consistency and/or bit integrity of items in the archive.
     *
     * @param cmd CLI command
     */
    private void check(CommandLine cmd) {
        String[] args = cmd.getArgs();
        boolean checkBits = cmd.hasOption(Flag.CHECK_BITS.longName()) || cmd.hasOption(Flag.CHECK_BITS.shortName());

        report.println("Checking...");
        switch (args.length) {
            case 1:
                // check everything
                try {
                    String[] collections =  store.listBookCollections();
                    if (collections != null) {
                        for (String collectionName : collections) {
                            if (config.ignore(collectionName)) {
                                continue;
                            }
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
        boolean force = cmd.hasOption(Flag.FORCE.longName()) || cmd.hasOption("f");
        String[] args = cmd.getArgs();

        switch (args.length) {
            case 1:
                // update all checksums in all collections
                report.println("Updating all checksums.");
                try {
                    for (String col : store.listBookCollections()) {
                        if (config.ignore(col)) {
                            continue;
                        }
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
                if (config.ignore(collectionId)) {
                    return;
                }
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
        boolean force = cmd.hasOption(Flag.FORCE.longName()) || cmd.hasOption("f");
        String[] args = cmd.getArgs();

        switch (args.length) {
            case 1:
                try {
                    for (String collection : store.listBookCollections()) {
                        if (config.ignore(collection)) {
                            continue;
                        }
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
        boolean force = cmd.hasOption(Flag.FORCE.longName()) || cmd.hasOption("f");
        String[] args = cmd.getArgs();

        switch (args.length) {
            case 1:
                try {
                    for (String collection : store.listBookCollections()) {
                        if (config.ignore(collection)) {
                            continue;
                        }
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
