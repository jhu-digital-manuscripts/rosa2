package rosa.archive.tool;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

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

    private void displayError(String message, String[] args) {
        report.println("Command: " + Arrays.toString(args));
        report.println(message);
    }

    private void run(CommandLine cmd) throws IOException {
        String[] args = cmd.getArgs();

        switch (args.length) {
        case 1:
            for (String collection : store.listBookCollections()) {
                if (config.ignore(collection)) {
                    continue;
                }
                handle_collection(cmd);
            }
            break;
        case 2:
            handle_collection(cmd);
            break;
        case 3:
            handle_book(cmd);
            break;
        default:
            displayError("Too many arguments. Usage: <command> [-options] <collectionId> <bookId>", args);
            break;
        }
    }

    private void handle_book(CommandLine cmd) throws IOException {
        String[] args = cmd.getArgs();
        BookDerivative deriv = new BookDerivative(args[1], args[2], report, store);

        switch (getCommand(args[0])) {
        case LIST:
            deriv.list();
            break;
        case CHECK:
            deriv.check(hasOption(cmd, Flag.CHECK_BITS));
            break;
        case UPDATE:
            deriv.updateChecksum(hasOption(cmd, Flag.FORCE));
            break;
        case UPDATE_IMAGE_LIST:
            deriv.generateAndWriteImageList(hasOption(cmd, Flag.FORCE));
            break;
        case CROP_IMAGES:
            CropDerivative cDer = new CropDerivative(args[1], args[2], report, store);
            cDer.cropImages(hasOption(cmd, Flag.FORCE));
            break;
        case FILE_MAP:
            deriv.generateFileMap();
            break;
        case VALIDATE_XML:
            deriv.validateXml();
            break;
        default:
            displayError("Invalid command found.", args);
            break;
        }
    }

    private void handle_collection(CommandLine cmd) throws IOException {
        String[] args = cmd.getArgs();
        String[] cols = null;

        if (args.length == 1) {
            // No collectionID listed, so do it for all collections
            cols = store.listBookCollections();
        } else {
            cols = new String[] {args[1]};
        }

        for (String col : cols) {
            CollectionDerivative deriv = new CollectionDerivative(col, report, store);

            switch (getCommand(args[0])) {
            case LIST:
                deriv.list();
                break;
            case CHECK:
                deriv.check(hasOption(cmd, Flag.CHECK_BITS));
                break;
            case UPDATE:
                deriv.updateChecksum(hasOption(cmd, Flag.FORCE));
                break;
            case UPDATE_IMAGE_LIST:
                break;
            case CROP_IMAGES:
                CropDerivative cd = new CropDerivative(col, report, store);
                cd.cropImages(hasOption(cmd, Flag.FORCE));
                break;
            case FILE_MAP:
                displayError("Cannot generate file map for collections.", args);
                break;
            case VALIDATE_XML:
                deriv.validateXml();
                break;
            default:
                displayError("Invalid command found.", args);
                break;
            }
        }
    }

    private boolean hasOption(CommandLine cmd, Flag flag) {
        return cmd.hasOption(flag.longName()) || cmd.hasOption(flag.shortName());
    }
}
