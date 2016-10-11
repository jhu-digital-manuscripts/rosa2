package rosa.archive.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 *
 */
public interface ByteStreamGroup {

    /**
     * @return the fully qualified ID of the byte stream group
     */
    String id();

    /**
     * @return simple name
     */
    String name();

    /**
     * Get the fully qualified path of a child node of this byte stream
     * group. If the given childName does not exist as within this group,
     * NULL is returned.
     *
     * @param childName name of child node
     * @return fully qualified name, or NULL if child name does not exist.
     * @throws NullPointerException if childName is NULL
     */
    String resolveName(String childName);

    /**
     * @return number of byte streams contained in this group
     * @throws IOException if the base of the byte stream group cannot be accessed
     */
    int numberOfByteStreams() throws IOException;

    /**
     * @return number of sub groups contained in this group
     * @throws IOException if the base of the byte stream group cannot be accessed
     */
    int numberOfByteStreamGroups() throws IOException;

    /**
     * @return list of ByteStream IDs
     * @throws IOException if the base of the byte stream group cannot be accessed
     */
    List<String> listByteStreamIds() throws IOException;

    /**
     * @return list of ByteStream names
     * @throws IOException
     *          if the base of the byte stream group cannot be accessed
     *          or a byte stream in this group is inaccessible
     */
    List<String> listByteStreamNames() throws IOException;

    /**
     * @return list of the fully qualified identifiers of the sub groups contained
     *          in this group
     * @throws IOException
     *          if the base of the byte stream group cannot be accessed or
     *          a byte stream group in this group is inaccessible
     */
    List<String> listByteStreamGroupIds() throws IOException;

    /**
     * @return list of the names of the sub groups contained in this group
     * @throws IOException
     *          if the base of the byte stream group cannot be accessed or
     *          a byte stream group in this group is inaccessible
     */
    List<String> listByteStreamGroupNames() throws IOException;

    /**
     * @return list of the byte stream (sub)groups contained in this group
     * @throws IOException
     *          if the base of the byte stream group cannot be accessed or
     *          a byte stream group in this group is inaccessible
     */
    List<ByteStreamGroup> listByteStreamGroups() throws IOException;

    /**
     * @param name name of a byte stream
     * @return does this byte stream group contain the named byte stream?
     */
    boolean hasByteStream(String name);

    /**
     * @param name name of a byte stream group
     * @return does this byte stream group contain the named byte stream (sub)group?
     */
    boolean hasByteStreamGroup(String name);

    /**
     * @param name name of a byte stream
     * @return the named byte stream as an InputStream, so that it may be read
     * @throws IOException
     *          if the base of the byte stream group cannot be accessed or
     *          the byte stream is inaccessible
     */
    InputStream getByteStream(String name) throws IOException;

    /**
     * @param name name of a byte stream
     * @return the named byte stream as an OutputStream, so that it may be written to
     * @throws IOException
     *          if the base of the byte stream group cannot be accessed or
     *          the byte stream is inaccessible
     */
    OutputStream getOutputStream(String name) throws IOException;

    /**
     * @param name name of a byte stream group
     * @return the named byte stream (sub)group
     */
    ByteStreamGroup getByteStreamGroup(String name);

    /**
     * @param streamName name of byte stream of interest
     * @return last time the source of a byte stream was modified
     */
    long getLastModified(String streamName);

    /**
     * Create a new byte stream group relative to this group.
     *
     * @param name name of new group
     * @return the new byte stream group
     * @throws IOException
     *          if the base of the byte stream group cannot be accessed or
     *          a byte stream group in this group is inaccessible
     */
    ByteStreamGroup newByteStreamGroup(String name) throws IOException;

    /**
     * Copy this ByteStreamGroup to another location.
     *
     * By default, this method will copy all files of type:
     *  - XML (application/xml)
     *  - TXT  (text/plain)
     *  - HTML (text/html)
     *  - CSV (text/csv)
     *
     *  All other files will not be copied.
     *
     * TODO if target already has a directory with the same name? Overwrite or do nothing?
     *
     * @param targetGroup target name
     * @throws IOException if this or the target ByteStreamGroup is not available
     */
    void copyTo(ByteStreamGroup targetGroup) throws IOException;

    /**
     * Copy a byte stream from this group to the target group. The copied stream will
     * keep its name in the target group.
     *
     * @param sourceStream name of source
     * @param targetGroup name of destination group
     * @throws IOException
     *          if the base of the byte stream group cannot be accessed or
     *          the byte stream or target group is inaccessible
     */
    void copyByteStream(String sourceStream, ByteStreamGroup targetGroup) throws IOException;

    /**
     * Copy a byte stream from this byte stream group to the target byte stream group. Rename
     * the byte stream in the target.
     *
     * @param sourceStream name of source
     * @param targetStream name of destination
     * @param targetGroup target group to copy file
     * @throws IOException
     *          if the base of the byte stream group cannot be accessed or
     *          the byte streams or target group are inaccessible
     *
     */
    void copyByteStream(String sourceStream, String targetStream, ByteStreamGroup targetGroup)
            throws IOException;

    /**
     * Rename a byte stream in this byte stream group. The contents of the byte stream are
     * not touched, but it gets a new name.
     *
     * @param originalStream original name
     * @param targetStream rename the original byte stream to this name
     * @throws IOException
     *          if the base of the byte stream group cannot be accessed or
     *          the byte streams are inaccessible
     */
    void renameByteStream(String originalStream, String targetStream) throws IOException;
}
