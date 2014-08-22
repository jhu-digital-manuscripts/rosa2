package rosa.archive.model;

/**
 *
 */
public class Permission {

    private String permission;

    public Permission() {  }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission)) return false;

        Permission that = (Permission) o;

        if (permission != null ? !permission.equals(that.permission) : that.permission != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return permission != null ? permission.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Permission{" +
                "permission='" + permission + '\'' +
                '}';
    }
}
