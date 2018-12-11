package rosa.archive.model;

import java.util.Objects;

public class ObjectRef {
    private String name;
    private String uri;

    public ObjectRef() {}

    public ObjectRef(String name, String uri) {
        this.name = name;
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectRef)) return false;
        ObjectRef objectRef = (ObjectRef) o;
        return Objects.equals(name, objectRef.name) &&
                Objects.equals(uri, objectRef.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, uri);
    }

    @Override
    public String toString() {
        return "ObjectRef{" +
                "name='" + name + '\'' +
                ", uri='" + uri + '\'' +
                '}';
    }
}
