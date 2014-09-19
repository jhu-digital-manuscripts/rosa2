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
            System.err.println("Unknown command.");
        }
    }

    /**
     *
     *
     * @param args command
     */
    private void list(String[] args) {
        // list
        if (args.length == 1) {
            // list
            System.out.println("Collections: ");
            try {
                String[] collectionNames = store.listBookCollections();
                for (String name : collectionNames) {
                    System.out.println("  " + name);
                }
            } catch (IOException e) {
                System.err.println("  Error: Unable to read collection names.");
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
                System.err.println("  Error: Unable to read book names in collection [" + args[1] + "]");
            }
        } else if (args.length == 3) {
            // list <collectionId> <bookId>
            System.out.println("Stuff in " + args[1] + ":" + args[2]);
            try {
                Book book = store.loadBook(args[1], args[2]);
                for (String item : book.getContent()) {
                    System.out.println("  " + item);
                }
            } catch (IOException e) {
                System.err.println("  Error: Unable to load book [" + args[1] + ":" + args[2] + "]");
            }
        } else {
            System.err.println("Too many arguments. USAGE: list <collectionId> <bookId>");
        }
    }

    /**
     *
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
                    BookCollection collection = store.loadBookCollection(collectionName);
                    System.out.println("\n" + collectionName);
                    store.check(collection, checkBits, errors);
                }
            } catch (IOException e) {
                System.err.println("  Error: Unable to check archive.");
            }
        } else if (args.length == 2) {
            // check collection
            try {
                BookCollection collection = store.loadBookCollection(args[1]);
                store.check(collection, checkBits, errors);
            } catch (IOException e) {
                System.err.println("  Error: Unable to load collection. [" + args[1] + "]");
            }
        } else if (args.length == 3) { System.out.println(Arrays.toString(args));
            // check book
            try {
                Book book = store.loadBook(args[1], args[2]);
                store.check(book, checkBits, errors);
            } catch (IOException e) {
                System.err.println("  Error: Unable to load book. [" + args[1] + ":" + args[2] + "]");
            }
        } else {
            System.err.println("  Too many arguments. USAGE: check <collectionId> <bookId>");
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
