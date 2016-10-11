package rosa.archive.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Implementation of {@link ByteStreamGroup} backed by a directory in a file system.
 */
public class FSByteStreamGroup implements ByteStreamGroup {
    /**
     * Enum of files that can be copied when doing a shallow copy of this ByteStreamGroup.
     */
    private enum SHALLOW_COPY_TYPES {
        XML("application/xml", "xml"), TXT("text/plain", "txt"), HTML("text/html", "html"), CSV("text/csv", "csv");

        final String type;
        final String extension;

        SHALLOW_COPY_TYPES(String type, String extension) {
            this.type = type;
            this.extension = extension;
        }

        static boolean canCopy(String type) {
            for (SHALLOW_COPY_TYPES t : values()) {
                if (t.type.equals(type)) {
                    return true;
                }
            }
            return false;
        }
    }

    private Path base;

    /**
     * @param base base path of the byte stream group
     */
    public FSByteStreamGroup(String base) {
        this(Paths.get(base));
    }

    /**
     * @param base base path of the byte stream group
     */
    public FSByteStreamGroup(Path base) {
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
     * @return simple file name
     */
    @Override
    public String name() {
        return base.getFileName().toString();
    }

    @Override
    public String resolveName(String childName) {
        if (hasByteStream(childName) || hasByteStreamGroup(childName)) {
            return id() + FileSystems.getDefault().getSeparator() + childName;
        } else {
            return null;
        }
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

    @Override
    public ByteStreamGroup newByteStreamGroup(String name) throws IOException {
        if (hasByteStreamGroup(name)) {
            return getByteStreamGroup(name);
        }

        Path group = Files.createDirectory(base.resolve(name));
        return new FSByteStreamGroup(group);
    }

    @Override
    public void copyTo(ByteStreamGroup targetGroup) throws IOException {
        if (targetGroup == null || targetGroup.id() == null || targetGroup.id().isEmpty()) {
            throw new IOException("No target specified.");
        }

        // TODO if target already has a directory with the same name? Overwrite or do nothing?
        // Current behavior is to overwrite
        if (!targetGroup.hasByteStreamGroup(name())) {
            targetGroup.newByteStreamGroup(name());
        }

        Path targetPath = Paths.get(targetGroup.id()).resolve(name());
        Files.copy(base, targetPath, StandardCopyOption.REPLACE_EXISTING);
        Files.walkFileTree(
                base, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Path targetDir = targetPath.resolve(base.relativize(dir));

                        Files.copy(dir, targetDir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path targetDir = targetPath.resolve(base.relativize(file));

                        String type = Files.probeContentType(file);
                        if (!file.toString().startsWith(".") && SHALLOW_COPY_TYPES.canCopy(type)) {
                            Files.copy(file, targetDir);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }

    @Override
    public void copyByteStream(String sourceStream, ByteStreamGroup targetGroup) throws IOException {
        copyByteStream(sourceStream, sourceStream, targetGroup);
    }

    @Override
    public void copyByteStream(String sourceStream, String targetStream, ByteStreamGroup targetGroup) throws IOException {
        InputStream source = getByteStream(sourceStream);
        Path target = Paths.get(targetGroup.id()).resolve(targetStream);

        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void renameByteStream(String originalStream, String targetStream) throws IOException {
        if (!hasByteStream(originalStream)) {
            return;
        } else if (hasByteStream(targetStream)) {
            return;
        }

        Path original = Paths.get(id()).resolve(originalStream);
        Path target = Paths.get(id()).resolve(targetStream);

        Files.move(original, target);
    }
}
