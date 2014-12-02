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

    public int numberOfByteStreams() throws IOException;

    public int numberOfByteStreamGroups() throws IOException;

    /**
     * @return list of ByteStream IDs
     */
    public List<String> listByteStreamIds() throws IOException;

    /**
     * @return list of ByteStream names
     */
    public List<String> listByteStreamNames() throws IOException;

    public List<String> listByteStreamGroupIds() throws IOException;

    public List<String> listByteStreamGroupNames() throws IOException;

    public List<ByteStreamGroup> listByteStreamGroups() throws IOException;

    public boolean hasByteStream(String name);

    public boolean hasByteStreamGroup(String name);

    public InputStream getByteStream(String name) throws IOException;

    public OutputStream getOutputStream(String name) throws IOException;

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
