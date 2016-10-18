package rosa.archive.tool;

import com.google.inject.Guice;
import com.google.inject.Injector;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CopyArchiveTool {
    public static void main(String[] args) {

        // Need archive path
        //  - Create Store
        // Pass in target directory as arg
        // Do Store copy.
        //  - If collection is available, use that as collection name
        Injector injector = Guice.createInjector(new CopyArchiveToolModule(), new ArchiveCoreModule());
        Store store = injector.getInstance(Store.class);

        if (args.length != 1 && args.length != 2) {
            System.err.println("One or two argument can be given. Usage: <tool> <target_path> <optional_collection_name>");
            System.exit(1);
        }

        try {
            Path targetPath = Paths.get(args[0]);
            String collection = null;

            if (args.length == 2) {
                collection = args[1];
                targetPath.resolve(collection);
            }

            if (Files.notExists(targetPath)) {
                System.out.println("## Target directory does not exist. Creating target directory. (" + targetPath + ")");
                Files.createDirectories(targetPath);
            }

            System.out.println("## Copying " + (collection != null ? collection : "archive") + " --> " + targetPath.toString());
            store.shallowCopy(collection, new FSByteStreamGroup(targetPath));

        } catch (IOException e) {
            System.err.println("Failed to copy data.");
            e.printStackTrace(System.err);
        }
    }
}
