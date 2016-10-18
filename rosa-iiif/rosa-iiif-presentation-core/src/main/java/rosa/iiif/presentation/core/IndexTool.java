package rosa.iiif.presentation.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import rosa.archive.core.Store;
import rosa.search.core.SearchService;
import rosa.search.tool.Tool;

import java.io.IOException;

public class IndexTool {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Only one argument expected. Usage: <tool> <index.path>");
            System.exit(1);
        }

        Injector injector = Guice.createInjector(new ToolModule());

        Store store = injector.getInstance(Store.class);
        Tool tool = new Tool(
                store,
                injector.getInstance(SearchService.class),
                System.out
        );

        try {
            for (String col : store.listBookCollections()) {
                System.out.println("## Generating search index (" + col + ")");
                tool.process(new String[] {col});
            }
        } catch (IOException e) {
            System.out.println("Failed to list collections in archive.");
            e.printStackTrace(System.out);
        }
    }

}
