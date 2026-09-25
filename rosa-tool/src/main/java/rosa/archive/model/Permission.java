package rosa.archive.model;

import java.util.Objects;

/**
 * A permission statement describing the usage rights for a book,
 * typically provided in a specific language.
 */
public class Permission implements HasId {

    private String id;
    private String permission;

    /**
     * Creates an empty Permission.
     */
    public Permission() {}

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the permission statement text.
     *
     * @return the permission text, or {@code null} if not set
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Sets the permission statement text.
     *
     * @param permission the permission text
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, permission);
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id='" + id + '\'' +
                ", permission='" + permission + '\'' +
                '}';
    }
}
