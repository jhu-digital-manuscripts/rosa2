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
public class ByteStreamGroup {

    private Path base;

    private List<Path> files;
    private Map<String, ByteStreamGroup> directories;

    public ByteStreamGroup(String base) {
        this(Paths.get(base));
    }

    public ByteStreamGroup(Path base) {
        this.base = base;
        this.files = new ArrayList<>();
        this.directories = new HashMap<>();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(base)) {
            for (Path path : ds) {
                if (Files.isRegularFile(path)) {
                    files.add(path);
                } else if (Files.isDirectory(path)) {
                    directories.put(path.toString(), new ByteStreamGroup(path));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to create ByteStreamGroup.", e);
        }
    }

    /**
     * @return full path location of this ByteStreamGroup
     */
    public String id() {
        return base.toString();
    }

    /**
     * @return simple name
     */
    public String name() {
        return base.getFileName().toString();
    }

    public int numberOfByteStreams() {
        return files.size();
    }

    public int numberOfByteStreamGroups() {
        return directories.size();
    }

    /**
     * @return list of ByteStream IDs
     */
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
    public List<String> listByteStreamNames() {
        List<String> names = new ArrayList<>();
        for (Path path : files) {
            names.add(path.getFileName().toString());
        }
        return names;
    }

    public List<String> listByteStreamGroupIds() {
        List<String> ids = new ArrayList<>();
        for (String id : directories.keySet()) {
            ids.add(id);
        }
        return ids;
    }

    public List<String> listByteStreamGroupNames() {
        List<String> names = new ArrayList<>();
        for (ByteStreamGroup bsg : listByteStreamGroups()) {
            names.add(bsg.name());
        }
        return names;
    }

    public List<ByteStreamGroup> listByteStreamGroups() {
        List<ByteStreamGroup> bsg = new ArrayList<>();

        for (Entry<String, ByteStreamGroup> entry : directories.entrySet()) {
            bsg.add(entry.getValue());
        }

        return bsg;
    }

    public boolean hasByteStream(String name) {
        Path relative = base.resolve(name);
        return files.contains(relative);
    }

    public boolean hasByteStreamGroup(String name) {
        return listByteStreamGroupNames().contains(name);
    }

    // TODO search recursively?
    public InputStream getByteStream(String name) throws IOException {
        Path path = base.resolve(name);
        return Files.newInputStream(path);
    }

    public ByteStreamGroup getByteStreamGroup(String name) {
        Path path = base.resolve(name);
        return directories.get(path.toString());
    }

}
