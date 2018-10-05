package rosa.archive.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.google.inject.Guice;
import com.google.inject.Injector;

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
import rosa.archive.tool.derivative.AbstractDerivative;
import rosa.archive.tool.derivative.BookDerivative;
import rosa.archive.tool.derivative.CollectionDerivative;
import rosa.archive.tool.derivative.CropDerivative;

/**
 *
 */
public class ArchiveTool {
    private final ToolConfig config;
    private final Store store;
    private final PrintStream report;
    private AORTranscriptionChecker aorTranscriptionChecker;
    private AORIdMapper idMapper;
    private ImageListDecorator imageListDecorator;

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

        // Determine command being executed
        
        Command cmd = null;
        
        for (Command c : Command.values()) {
            if (c.display().equals(args[0])) {
                cmd = c;
            }
        }

        if (cmd == null) {
            System.err.println("Invalid command: " + args[0]);
            System.err.print("Commands: ");

            Command[] commands = Command.values();
            
            for (int i = 0; i < commands.length; i++) {
                if (i > 0) {
                    System.err.print("|");
                }
                System.err.print(commands[i].display());
            }
            System.err.println();
            
            System.exit(1);
        }
        
        // Set options for command

        switch (cmd) {
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
        case RENAME_IMAGES:
            options.addOption(Flag.CHANGE_ID.shortName(), Flag.CHANGE_ID.longName(), false,
                    "indicate that the ID portion of the file names needs to be changed in order to match the" +
                            " directory name.");
            options.addOption(Flag.REVERSE.shortName(), Flag.REVERSE.longName(), false,
                    "Rename all images using the reverse relationship in the file map. The file map contains " +
                            "two names delimited by a comma (one,two). If image renaming goes from one -> two, " +
                            "then this option will reverse that relationship, going from two -> one. This will " +
                            "have the effect of returning the images to their original names.");
            break;
        case RENAME_TRANSCRIPTIONS:
            options.addOption(Flag.REVERSE.shortName(), Flag.REVERSE.longName(), false,
                    "Rename all AoR transcription using the reverse relationship in the file map. The file map contains " +
                            "two names delimited by a comma (one,two). If image renaming goes from one -> two, " +
                            "then this option will reverse that relationship, going from two -> one. This will " +
                            "have the effect of returning the images to their original names.");
            break;
        case CHECK_AOR:
            options.addOption(Flag.SPREADSHEET_DIR.shortName(), Flag.SPREADSHEET_DIR.longName(), true,
                    "Specifies the directory in which the reference spreadsheets exist. " +
                            "Books.xlsx, People.xlsx, Locations.xlsx");
            break;
        case GENERATE_TEI:
            // No options
            break;
        case DECORATE_IMAGE_LIST:
            break;
        default:
            break;
        }

        CommandLine cmdline = null;
        
        try {
            cmdline = parser.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            System.err.println("Bad option found. [" + e.getOption() + "]");
            System.exit(1);
        } catch (ParseException e) {
            System.err.println("Unable to parse command.");
            System.exit(1);
        }

        // Set archive path if the argument exists in the CLI command issued
        if (cmdline.hasOption("D") && cmdline.getOptionProperties("D").getProperty("archive.path") != null) {
            config.setArchivePath(
                    cmdline.getOptionProperties("D").getProperty("archive.path")
            );
        }

        // Create the tool and run the command
        ByteStreamGroup base = new FSByteStreamGroup(config.getArchivePath());
        Store store = new StoreImpl(injector.getInstance(SerializerSet.class), injector.getInstance(BookChecker.class),
                injector.getInstance(BookCollectionChecker.class), base, true);

        ArchiveTool tool = new ArchiveTool(store, config);
        tool.aorTranscriptionChecker = injector.getInstance(AORTranscriptionChecker.class);
        tool.idMapper = new AORIdMapper(base, tool.report);
        tool.imageListDecorator = new ImageListDecorator(store, base, tool.report);

        tool.run(cmdline, cmd);
    }

    private void run(CommandLine cmdline, Command cmd) throws IOException {
        String[] args = cmdline.getArgs();

        if (cmd == Command.RENAME_FILES) {
            if (args.length == 3) {
                AbstractDerivative.renameFiles(new File(args[1]), new File(args[2]));    
            } else {
                System.err.println("Must pass directory and filemap arguments");
            }
            
            return;
        }
        
        switch (args.length) {
        case 1:
            for (String collection : store.listBookCollections()) {
                if (config.ignore(collection)) {
                    continue;
                }
                
                handle_collection(cmdline, cmd);
            }
            break;
        case 2:
            handle_collection(cmdline, cmd);
            break;
        case 3:
            handle_book(cmdline, cmd);
            break;
        default:
            System.err.println("Too many arguments. Usage: <command> [-options] <collectionId> <bookId>");
            System.exit(1);
        }
    }

    private void handle_book(CommandLine cmdline, Command cmd) throws IOException {
        String[] args = cmdline.getArgs();
        BookDerivative deriv = new BookDerivative(args[1], args[2], report, store);

        switch (cmd) {
        case LIST:
            deriv.list();
            break;
        case CHECK:
            deriv.check(has_option(cmdline, Flag.CHECK_BITS));
            break;
        case UPDATE:
            deriv.updateChecksum(has_option(cmdline, Flag.FORCE));
            break;
        case UPDATE_IMAGE_LIST:
            deriv.generateAndWriteImageList(has_option(cmdline, Flag.FORCE));
            break;
        case CROP_IMAGES:
            CropDerivative cDer = new CropDerivative(args[1], args[2], report, store);
            cDer.cropImages(has_option(cmdline, Flag.FORCE));
            break;
        case FILE_MAP:
            deriv.generateFileMap();
            break;
        case VALIDATE_XML:
            deriv.validateXml();
            break;
        case RENAME_IMAGES:
            deriv.renameImages(has_option(cmdline, Flag.CHANGE_ID), has_option(cmdline, Flag.REVERSE));
            break;
        case RENAME_TRANSCRIPTIONS:
            deriv.renameTranscriptions(has_option(cmdline, Flag.REVERSE));
            break;
        case GENERATE_TEI:
            deriv.convertTranscriptionTexts();
            break;
        case CHECK_AOR:
            String sheet_dir = cmdline.getOptionValue(Flag.SPREADSHEET_DIR.longName(), null);
            aorTranscriptionChecker.run(args[1], true, sheet_dir, report);
            break;
        case MIGRATE_TEI_METADATA:
            TEIDescriptionConverter.run(cmdline, config, report);
            break;
        case DECORATE_IMAGE_LIST:
            imageListDecorator.run(args[1], args[2]);
            break;
        default:
            System.out.println("Command not supported on book");
            System.out.println(genericUsage());
            break;
        }
    }
    
    private void handle_collection(CommandLine cmdline, Command cmd) throws IOException {
        String[] args = cmdline.getArgs();
        String[] cols;

        if (args.length == 1) {
            // No collectionID listed, so do it for all collections
            cols = store.listBookCollections();
        } else {
            cols = new String[] {args[1]};
        }        

        for (String col : cols) {
            CollectionDerivative deriv = new CollectionDerivative(col, report, store);

            switch (cmd) {
            case LIST:
                deriv.list();
                break;
            case CHECK:
                deriv.check(has_option(cmdline, Flag.CHECK_BITS));
                break;
            case UPDATE:
                deriv.updateChecksum(has_option(cmdline, Flag.FORCE));
                break;
            case UPDATE_IMAGE_LIST:
                break;
            case CROP_IMAGES:
                CropDerivative cd = new CropDerivative(col, report, store);
                cd.cropImages(has_option(cmdline, Flag.FORCE));
                break;
            case VALIDATE_XML:
                deriv.validateXml();
                break;
            case CHECK_AOR:
                String sheet_dir = cmdline.getOptionValue(Flag.SPREADSHEET_DIR.longName(), null);
                aorTranscriptionChecker.run(args[1], false, sheet_dir, report);
                break;
            case MIGRATE_TEI_METADATA:
                TEIDescriptionConverter.run(cmdline, config, report);
                break;
            case GENERATE_ANNOTATION_MAP:
                idMapper.run(args[1]);
                break;
            case DECORATE_IMAGE_LIST:
                imageListDecorator.run(args[1], null);
                break;
            default:
                System.out.println("Command not supported on collection");
                System.out.println(genericUsage());
                break;
            }
        }
    }

    private String genericUsage() {
        return "Valid commands: [" +
                Arrays.stream(Command.values()).map(Command::display).collect(Collectors.joining(", ")) +
                "]";
    }

    private boolean has_option(CommandLine cmdline, Flag flag) {
        return cmdline.hasOption(flag.longName()) || cmdline.hasOption(flag.shortName());
    }
}
