package rosa.archive.tool.derivative;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.io.FileUtils;

import rosa.archive.core.Store;
import rosa.archive.core.util.CSV;

/**
 *
 */
public abstract class AbstractDerivative {

    protected PrintStream report;
    protected Store store;

    AbstractDerivative(PrintStream report, Store store) {
        this.report = report;
        this.store = store;
    }

    public abstract void list();

    public abstract void updateChecksum(boolean force) throws IOException;

    public abstract void check(boolean checkBits) throws IOException;

    public abstract void generateAndWriteImageList(boolean force) throws IOException;

    public abstract void validateXml() throws IOException;

    public abstract void renameImages(boolean changeId, boolean reverse) throws IOException;

    public abstract void renameTranscriptions(boolean reverse) throws IOException;

    protected void reportError(String message, Exception e) {
        report.println("  [Error] " + message);
        e.printStackTrace(report);
    }

    protected void reportError(String ... errors) {
        for (String err : errors) {
            report.println("    " + err);
        }
    }

    protected void reportError(String title, List<String> errors) {
        report.println("\n  " + title);
        reportError(errors.toArray(new String[errors.size()]));
    }
    

    public static void renameFiles(File dir, File csvmap) throws IOException {
        String[][] table = CSV.parseTable(new FileReader(csvmap));

        for (String[] entry : table) {
            if (entry.length == 0) {
                continue;
            }

            if (entry.length != 2) {
                throw new IOException("File map must be oldpath, newname: ");
            }

            File oldfile = new File(dir, entry[0].trim());
            File newfile = new File(dir, entry[1].trim());

            if (!oldfile.exists()) {
                System.err.println("Old file does not exist: " + oldfile);
                continue;
            }

            if (newfile.exists()) {
                System.err.println("Stopping because new file exists: "
                        + newfile + " for " + oldfile);
                break;
            }

            System.out.println(oldfile + " -> " + newfile);

            if (!oldfile.renameTo(newfile)) {
               FileUtils.copyFile(oldfile,newfile);
            }
        }
    }
}
