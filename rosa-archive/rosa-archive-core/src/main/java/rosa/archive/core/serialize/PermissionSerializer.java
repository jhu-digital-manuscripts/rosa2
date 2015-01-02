package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

import rosa.archive.model.Permission;

/**
 *
 */
public class PermissionSerializer implements Serializer<Permission> {

    @Override
    public Permission read(InputStream is, List<String> errors) throws IOException {
        List<String> lines = IOUtils.readLines(is, UTF_8);
        StringBuilder content = new StringBuilder();

        for (String line : lines) {
            content.append(line);
        }

        Permission permission = new Permission();
        permission.setPermission(content.toString());

        return permission;
    }

    @Override
    public void write(Permission permission, OutputStream out) throws IOException {
        IOUtils.write(permission.getPermission(), out, UTF_8);
    }

    @Override
    public Class<Permission> getObjectType() {
        return Permission.class;
    }
}
