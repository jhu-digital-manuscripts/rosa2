package rosa.archive.aor;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

//TODO Stats tool needs a cleanup. Refactor to work directly off of archive.

public class Tool {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: stats|git-stats|validate|annotation-stats arg...");
            System.exit(1);
        }

        String command = args[0];

        GitStatCollector statCollector = new GitStatCollector();
        switch (command) {
        case "stats":
            if (args.length < 2) {
                exitOnError("Usage: stats aor_book_dir...");
            }

            AorStatsCollector stats = new AorStatsCollector();

            for (int i = 1; i < args.length; i++) {
                Path book_path = Paths.get(args[i]);
                String book_id = book_path.getFileName().toString();

                stats.collectBookStats(book_id, book_path);
            }

            stats.writeBookStats(Paths.get("."));
            break;
        case "annotation-stats":
            if (args.length < 2) {
                exitOnError("Usage: annotation-stats aor_book_dir...");
            }

            AnnotationStatsWriter asw = new AnnotationStatsWriter();
            PrintWriter out = new PrintWriter(System.out);
            
            asw.writeStatsHeader(out);
            
            for (int i = 1; i < args.length; i++) {
                Path book_path = Paths.get(args[i]);

                asw.writeStats(book_path, out);
            }
            
            out.flush();

            break;            
            case "git-stats":
                if (args.length < 2) {
                    exitOnError("Usage: git-stats <repository_url>");
                }

                statCollector.run(args);
                
                break;
            case "validate":
                XmlValidator.validate(args);
                break;
            default:
                exitOnError("Invalid command\nUsage: stats|git-stats|validate arg...");
                break;
        }
    }

    private static void exitOnError(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
