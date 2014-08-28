package rosa.archive.core;

import java.io.IOException;
import java.io.InputStream;
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

    public int numberOfByteStreams();

    public int numberOfByteStreamGroups();

    /**
     * @return list of ByteStream IDs
     */
    public List<String> listByteStreamIds();

    /**
     * @return list of ByteStream names
     */
    public List<String> listByteStreamNames();

    public List<String> listByteStreamGroupIds();

    public List<String> listByteStreamGroupNames();

    public List<ByteStreamGroup> listByteStreamGroups();

    public boolean hasByteStream(String name);

    public boolean hasByteStreamGroup(String name);

    public InputStream getByteStream(String name) throws IOException;

    public ByteStreamGroup getByteStreamGroup(String name);

}
