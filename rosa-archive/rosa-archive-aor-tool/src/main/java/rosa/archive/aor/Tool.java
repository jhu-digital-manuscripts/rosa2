package rosa.archive.aor;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Tool {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: stats|validate arg...");
            System.exit(1);
        }

        String command = args[0];

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
            case "git-stats":
                if (args.length < 2) {
                    exitOnError("Usage: git-stats <repository_url>");
                }


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
