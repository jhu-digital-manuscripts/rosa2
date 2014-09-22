package rosa.archive.tool;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.lang3.ArrayUtils;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.store.Store;
import rosa.archive.core.store.StoreFactory;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.tool.config.ToolConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class ArchiveTool {

    private ToolConfig config;
    private Store store;

    public ArchiveTool(Store store, ToolConfig config) {
        this.store = store;
        this.config = config;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("A command must be issued.");
            System.exit(1);
        }

        Injector injector = Guice.createInjector(new ToolModule(), new ArchiveCoreModule());

        ToolConfig config = injector.getInstance(ToolConfig.class);
        StoreFactory sFactory = injector.getInstance(StoreFactory.class);

        ByteStreamGroup base = new FSByteStreamGroup(config.getARCHIVE_PATH());
        Store store = sFactory.create(base);

        ArchiveTool tool = new ArchiveTool(store, config);
        tool.run(args);
    }

    public void run(String[] args) {
        String command = args[0];

        // Commands:
        if (command.equals(config.getCMD_LIST())) {
            list(args);
        } else if (command.equals(config.getCMD_CHECK())) {
            // check
            check(args);
        } else {
            displayError("Unknown command.", args);
        }
    }

    private void displayError(String message, String[] args) {
        System.err.println("Command: " + Arrays.toString(args));
        System.err.println(message);
    }

    /**
     * List items in the archive according to the command arguments.
     *
     * USAGE: java -jar &lt;jar file&gt; list &lt;collectionId (optional)&gt; &lt;bookId (optional)&gt;
     * <ul>
     *     <li>collectionId: if this is specified by itself (bookId is missing), then list all books
     *         in the collection, as specified by this ID.</li>
     *     <li>bookId: if this is specified, collectionId must also be specified. List all items in the book.</li>
     *     <li>If no IDs are present, then the collections will be listed.</li>
     *     <li>-showErrors : errors will be displayed.</li>
     * </ul>
     *
     * Example: {@code java -jar tool.jar rose LudwigXV7}
     *
     * This will list all items in the archive in the 'LudwigXV7' book in the 'rose' archive.
     *
     * @param args command plus arguments
     */
    private void list(String[] args) {

        int[] flagsPositions = findFlags(args);
        boolean showErrors = false;
        for (int i : flagsPositions) {
            if (args[i].equals(config.getFLAG_SHOW_ERRORS())) {
                showErrors = true;
                List<String> argsList = new ArrayList<>();
                argsList.addAll(Arrays.asList(args));
                argsList.remove(i);
                args = argsList.toArray(new String[argsList.size()]);
            } else {
                displayError("Unsupported flag found: [" + args[i] + "]", args);
                System.exit(1);
            }
        }

        // list
        List<String> errors = new ArrayList<>();
        if (args.length == 1) {
            // list
            System.out.println("Collections: ");
            try {
                String[] collectionNames = store.listBookCollections();
                for (String name : collectionNames) {
                    System.out.println("  " + name);
                }
            } catch (IOException e) {
                displayError("Error: Unable to read collection names.", args);
            }
        } else if (args.length == 2) {
            // list <collectionId>
            System.out.println("Books in " + args[1]);
            try {
                String[] books = store.listBooks(args[1]);
                for (String name : books) {
                    System.out.println("  " + name);
                }
            } catch (IOException e) {
                displayError("Error: Unable to read book names in collection [" + args[1] + "]", args);
            }
        } else if (args.length == 3) {
            // list <collectionId> <bookId>
            System.out.println("Stuff in " + args[1] + ":" + args[2]);
            try {
                Book book = store.loadBook(args[1], args[2], errors);
                for (String item : book.getContent()) {
                    System.out.println("  " + item);
                }

                if (showErrors) {
                    System.out.println("\nErrors found while processing:");
                    for (String err : errors) {
                        System.out.println("  " + err);
                    }
                }
            } catch (IOException e) {
                displayError("Error: Unable to load book [" + args[1] + ":" + args[2] + "]", args);
            }
        } else {
            displayError("Too many arguments. USAGE: list <collectionId> <bookId>", args);
        }

        if (showErrors) {

        } else {
            System.out.println("\nErrors were found while processing the command. Use the -showErrors flag " +
                    "to display the errors.");
        }
    }

    /**
     * Checks the data consistency and/or bit integrity of items in the archive.
     *
     * USAGE: java -jar &lt;jar file&gt; check [-checkBits] &lt;collectionId&gt; &lt;bookId&gt;
     * <ul>
     *     <li>If collectionID and bookId are missing, every item in the archive will be checked.</li>
     *     <li>collectionId : all items in this collection will be checked. All books in the collection
     *         are also checked.</li>
     *     <li>bookId : check all items in this book.</li>
     *     <li>-checkBits : tells the command to check the bit integrity of items</li>
     *     <li>If any other flag besides '-checkBits' is found, the command will terminate.</li>
     * </ul>
     *
     * Example: {@code java -jar tool.jar rose LudwigXV7}
     *
     * @param args command
     */
    private void check(String[] args) {
        List<String> errors = new ArrayList<>();

        // Look for -checkBits flag
        int[] flagPositions = findFlags(args);
        boolean checkBits = false;
        for (int i : flagPositions) {
            if (args[i].equals(config.getFLAG_CHECK_BITS())) {
                checkBits = true;
            } else {
                displayError("Unsupported flag found [" + args[i] + "]", args);
                System.exit(1);
            }
        }

        // If checkBits flag exists remove it from args list for processing
        if (checkBits) {
            // Arrays.asList(..) returns a custom ArrayList where #remove() is unsupported.
            List<String> argsList = new ArrayList<>();
            argsList.addAll(Arrays.asList(args));
            argsList.remove(flagPositions[0]);
            args = argsList.toArray(new String[argsList.size()]);
        }

        System.out.println("Checking...");
        if (args.length == 1) {
            // check everything
            try {
                String[] collections =  store.listBookCollections();
                for (String collectionName : collections) {
                    BookCollection collection = store.loadBookCollection(collectionName, errors);
                    System.out.println(collectionName);
                    store.check(collection, checkBits, errors);

                    for (String bookName : store.listBooks(collectionName)) {
                        Book book = store.loadBook(collectionName, bookName, errors);
                        System.out.println("  " + bookName);
                        store.check(book, checkBits, errors);
                    }
                }
            } catch (IOException e) {
                displayError("Error: Unable to check archive.", args);
            }
        } else if (args.length == 2) {
            // check collection
            try {
                BookCollection collection = store.loadBookCollection(args[1], errors);
                store.check(collection, checkBits, errors);
            } catch (IOException e) {
                displayError("Error: Unable to load collection. [" + args[1] + "]", args);
            }
        } else if (args.length == 3) {
            // check book
            try {
                Book book = store.loadBook(args[1], args[2], errors);
                store.check(book, checkBits, errors);
            } catch (IOException e) {
                displayError("Error: Unable to load book. [" + args[1] + ":" + args[2] + "]", args);
            }
        } else {
            displayError("Too many arguments. USAGE: check <collectionId> <bookId>", args);
        }

        System.out.println("Errors: ");
        for (String err : errors) {
            System.out.println("  " + err);
        }
    }

    /**
     * Get the index of all flags
     *
     * @param args command entered into command line
     * @return index of each flag
     */
    private int[] findFlags(String[] args) {
        List<Integer> indecies = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                indecies.add(i);
            }
        }

        return ArrayUtils.toPrimitive(
                indecies.toArray(new Integer[indecies.size()])
        );
    }

}
