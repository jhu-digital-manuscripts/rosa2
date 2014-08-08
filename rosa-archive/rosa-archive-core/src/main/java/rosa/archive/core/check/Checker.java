package rosa.archive.core.check;

/**
 * Interface for checking the validity and consistency of archive data.
 *
 * @see rosa.archive.core.check.DataChecker
 */
public interface Checker {

    /**
     * Checks the bit integrity of an object that has been persisted in the archive. The
     * hash of the object is computed and compared to known hash values stored in the archive.
     * This method does not necessarily check the structure of the data, instead ensures that
     * the data in an object is, on the bit level, exactly the data that is stored in the
     * archive. For a check of the data's structure, use <code>checkContent(obj)</code>.
     *
     * @param o
     *          object to check
     * @throws java.lang.ClassCastException
     * @return
     *          TRUE if the objects bit values validate, FALSE if object contains bit errors
     */
    boolean checkBits(Object o);

    /**
     * Checks the data consistency of a data model object. This method does not guarantee a bit
     * level check of the data.
     *
     * Instead, this method checks the structure of the data to see if is consistent with the
     * structure present in the archive. It will also check to see if all persisted data
     * associated with this input object can be loaded. This method sees if the data is readable
     * and looks right. To check if the data is what you expect on a bit level, use
     * <code>checkBits(obj)</code>.
     *
     * @param o
     *          object to check
     * @throws java.lang.ClassCastException
     * @return
     *          TRUE if content of object is consistent with data model and persistent data store
     */
    boolean checkContent(Object o);

}
