package rosa.archive.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 */
public class ByteStreamGroupImpl implements ByteStreamGroup {

    private Path base;

    private List<Path> files;
    private Map<String, ByteStreamGroup> directories;

    public ByteStreamGroupImpl(String base) {
        this(Paths.get(base));
    }

    ByteStreamGroupImpl(Path base) {
        this.base = base;
        this.files = new ArrayList<>();
        this.directories = new HashMap<>();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(base)) {
            for (Path path : ds) {
                if (Files.isRegularFile(path)) {
                    files.add(path);
                } else if (Files.isDirectory(path)) {
                    directories.put(path.toString(), new ByteStreamGroupImpl(path));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to create ByteStreamGroup.", e);
        }
    }

    /**
     * @return full path location of this ByteStreamGroup
     */
    @Override
    public String id() {
        return base.toString();
    }

    /**
     * @return simple name
     */
    @Override
    public String name() {
        return base.getFileName().toString();
    }

    @Override
    public int numberOfByteStreams() {
        return files.size();
    }

    @Override
    public int numberOfByteStreamGroups() {
        return directories.size();
    }

    /**
     * @return list of ByteStream IDs
     */
    @Override
    public List<String> listByteStreamIds() {
        List<String> ids = new ArrayList<>();
        for (Path path : files) {
            ids.add(path.toString());
        }
        return ids;
    }

    /**
     * @return list of ByteStream names
     */
    @Override
    public List<String> listByteStreamNames() {
        List<String> names = new ArrayList<>();
        for (Path path : files) {
            names.add(path.getFileName().toString());
        }
        return names;
    }

    @Override
    public List<String> listByteStreamGroupIds() {
        List<String> ids = new ArrayList<>();
        for (String id : directories.keySet()) {
            ids.add(id);
        }
        return ids;
    }

    @Override
    public List<String> listByteStreamGroupNames() {
        List<String> names = new ArrayList<>();
        for (ByteStreamGroup bsg : listByteStreamGroups()) {
            names.add(bsg.name());
        }
        return names;
    }

    @Override
    public List<ByteStreamGroup> listByteStreamGroups() {
        List<ByteStreamGroup> bsg = new ArrayList<>();

        for (Entry<String, ByteStreamGroup> entry : directories.entrySet()) {
            bsg.add(entry.getValue());
        }

        return bsg;
    }

    @Override
    public boolean hasByteStream(String name) {
        Path relative = base.resolve(name);
        return files.contains(relative);
    }

    @Override
    public boolean hasByteStreamGroup(String name) {
        return listByteStreamGroupNames().contains(name);
    }

    @Override
    public InputStream getByteStream(String name) throws IOException {
        Path path = base.resolve(name);
        return Files.newInputStream(path);
    }

    @Override
    public ByteStreamGroup getByteStreamGroup(String name) {
        Path path = base.resolve(name);
        return directories.get(path.toString());
    }

}
