package rosa.archive.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 *
 */
public interface ByteStreamGroup {

    public String id();

    /**
     * @return simple name
     */
    public String name();

    /**
     * @return number of byte streams contained in this group
     * @throws IOException
     */
    public int numberOfByteStreams() throws IOException;

    /**
     * @return number of sub groups contained in this group
     * @throws IOException
     */
    public int numberOfByteStreamGroups() throws IOException;

    /**
     * @return list of ByteStream IDs
     */
    public List<String> listByteStreamIds() throws IOException;

    /**
     * @return list of ByteStream names
     * @throws IOException
     */
    public List<String> listByteStreamNames() throws IOException;

    /**
     * @return list of the fully qualified identifiers of the sub groups contained
     *          in this group
     * @throws IOException
     */
    public List<String> listByteStreamGroupIds() throws IOException;

    /**
     * @return list of the names of the sub groups contained in this group
     * @throws IOException
     */
    public List<String> listByteStreamGroupNames() throws IOException;

    /**
     * @return list of the byte stream (sub)groups contained in this group
     * @throws IOException
     */
    public List<ByteStreamGroup> listByteStreamGroups() throws IOException;

    /**
     * @param name name of a byte stream
     * @return does this byte stream group contain the named byte stream?
     */
    public boolean hasByteStream(String name);

    /**
     * @param name name of a byte stream group
     * @return does this byte stream group contain the named byte stream (sub)group?
     */
    public boolean hasByteStreamGroup(String name);

    /**
     * @param name name of a byte stream
     * @return the named byte stream as an InputStream, so that it may be read
     * @throws IOException
     */
    public InputStream getByteStream(String name) throws IOException;

    /**
     * @param name name of a byte stream
     * @return the named byte stream as an OutputStream, so that it may be written to
     * @throws IOException
     */
    public OutputStream getOutputStream(String name) throws IOException;

    /**
     * @param name name of a byte stream group
     * @return the named byte stream (sub)group
     */
    public ByteStreamGroup getByteStreamGroup(String name);

    /**
     * @param streamName name of byte stream of interest
     * @return last time the source of a byte stream was modified
     */
    public long getLastModified(String streamName);

    /**
     * Create a new byte stream group relative to this group.
     *
     * @param name name of new group
     * @return the new byte stream group
     * @throws IOException
     */
    public ByteStreamGroup newByteStreamGroup(String name) throws IOException;

    /**
     * Copy a byte stream from this group to the target group. The copied stream will
     * keep its name in the target group.
     *
     * @param sourceStream name of source
     * @param targetGroup name of destination group
     * @throws IOException
     */
    public void copyByteStream(String sourceStream, ByteStreamGroup targetGroup) throws IOException;

    /**
     * Copy a byte stream from this byte stream group to the target byte stream group. Rename
     * the byte stream in the target.
     *
     * @param sourceStream name of source
     * @param targetStream name of destination
     * @param targetGroup target group to copy file
     * @throws IOException
     */
    public void copyByteStream(String sourceStream, String targetStream, ByteStreamGroup targetGroup)
            throws IOException;

}
