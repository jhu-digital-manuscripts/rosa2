package rosa.archive.tool.derivative;

import rosa.archive.core.store.Store;

import java.io.IOException;
import java.io.PrintStream;

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

    public abstract void updateChecksum(boolean force) throws IOException;

    protected void reportError(String message, Exception e) {
        report.println("[Error] " + message);
        e.printStackTrace(report);
    }

    protected void reportError(String ... errors) {
        for (String err : errors) {
            report.println("[Error] " + err);
        }
    }

}
