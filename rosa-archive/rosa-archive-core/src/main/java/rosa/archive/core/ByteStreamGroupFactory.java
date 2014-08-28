package rosa.archive.core;

public class ByteStreamGroupFactory {
    public static ByteStreamGroup create(String base) {
        return new ByteStreamGroupImpl(base);
    }
}
