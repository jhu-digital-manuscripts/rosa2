package rosa.archive.core.check;

import rosa.archive.core.ByteStreamGroup;

import java.util.List;

/**
 * Interface for checking the validity and consistency of archive data.
 */
public interface Checker<T> {

    // Single checker method that will check an object for consistency with itself, the archive, and bit integrity.
    boolean checkContent(T t, ByteStreamGroup bsg, boolean checkBits, List<String> errors);

}