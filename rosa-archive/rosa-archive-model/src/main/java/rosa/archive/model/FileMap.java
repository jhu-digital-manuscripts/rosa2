package rosa.archive.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class FileMap implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Map<String, String> map;

    public FileMap() {
        map = new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> lines) {
        this.map = lines;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileMap)) return false;

        FileMap fileMap = (FileMap) o;

        if (id != null ? !id.equals(fileMap.id) : fileMap.id != null) return false;
        if (map != null ? !map.equals(fileMap.map) : fileMap.map != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (map != null ? map.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FileMap{" +
                "id='" + id + '\'' +
                ", map=" + map +
                '}';
    }
}
