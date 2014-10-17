package rosa.archive.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class FSByteStreamGroup implements ByteStreamGroup {

    private Path base;

    public FSByteStreamGroup(String base) throws IOException{
        this(Paths.get(base));
    }

    FSByteStreamGroup(Path base) {
        this.base = base;
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
    public int numberOfByteStreams() throws IOException {
        int filesCount = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(base)) {
            for (Path path : ds) {
                if (Files.isRegularFile(path)) {
                    filesCount++;
                }
            }
        }
        return filesCount;
    }

    @Override
    public int numberOfByteStreamGroups() throws IOException {
        int dirCount = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(base)) {
            for (Path path : ds) {
                if (Files.isDirectory(path)) {
                    dirCount++;
                }
            }
        }
        return dirCount;
    }

    /**
     * @return list of ByteStream IDs
     */
    @Override
    public List<String> listByteStreamIds() throws IOException {
        List<String> ids = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(base)) {
            for (Path path : ds) {
                if (Files.isRegularFile(path)) {
                    ids.add(path.toString());
                }
            }
        }
        return ids;
    }

    /**
     * @return list of ByteStream names
     */
    @Override
    public List<String> listByteStreamNames() throws IOException {
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(base)) {
            for (Path path : ds) {
                if (Files.isRegularFile(path)) {
                    names.add(path.getFileName().toString());
                }
            }
        }
        return names;
    }

    @Override
    public List<String> listByteStreamGroupIds() throws IOException {
        List<String> ids = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(base)) {
            for (Path path : ds) {
                if (Files.isDirectory(path)) {
                    ids.add(path.toString());
                }
            }
        }
        return ids;
    }

    @Override
    public List<String> listByteStreamGroupNames() throws IOException {
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(base)) {
            for (Path path : ds) {
                if (Files.isDirectory(path)) {
                    names.add(path.getFileName().toString());
                }
            }
        }
        return names;
    }

    @Override
    public List<ByteStreamGroup> listByteStreamGroups() throws IOException {
        List<ByteStreamGroup> bsg = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(base)) {
            for (Path path : ds) {
                if (Files.isDirectory(path)) {
                    bsg.add(new FSByteStreamGroup(path));
                }
            }
        }
        return bsg;
    }

    @Override
    public boolean hasByteStream(String name) {
        Path relative = base.resolve(name);
        return Files.exists(relative) && Files.isRegularFile(relative);
    }

    @Override
    public boolean hasByteStreamGroup(String name) {
        Path relative = base.resolve(name);
        return Files.exists(relative) && Files.isDirectory(relative);
    }

    @Override
    public InputStream getByteStream(String name) throws IOException {
        Path path = base.resolve(name);
        return Files.newInputStream(path);
    }

    @Override
    public OutputStream getOutputStream(String name) throws IOException {
        Path path = base.resolve(name);
        return Files.newOutputStream(path);
    }

    @Override
    public ByteStreamGroup getByteStreamGroup(String name) {
        Path path = base.resolve(name);
        return new FSByteStreamGroup(path);
    }

    @Override
    public long getLastModified(String streamName) {
        if (streamName == null) {
            return -1L;
        }

        Path path = base.resolve(streamName);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return -1L;
        }

        return path.toFile().lastModified();
    }

}
