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

        if (command.equals("stats")) {
            if (args.length < 2) {
                System.err.println("Usage: stats aor_book_dir...");
                System.exit(1);
            }

            AorStatsCollector stats = new AorStatsCollector();

            for (int i = 1; i < args.length; i++) {
                Path book_path = Paths.get(args[i]);
                String book_id = book_path.getFileName().toString();

                stats.collectBookStats(book_id, book_path);
            }

            stats.writeBookStats(Paths.get("."));
        } else if (command.equals("validate")) {
            XmlValidator.validate(args);
        } else {
            System.err.println("Invalid command\nUsage: stats|validate arg...");
            System.exit(1);
        }
    }
}
