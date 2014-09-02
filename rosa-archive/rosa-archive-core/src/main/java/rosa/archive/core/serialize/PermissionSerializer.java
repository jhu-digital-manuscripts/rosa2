package rosa.archive.core.serialize;

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import rosa.archive.core.config.AppConfig;
import rosa.archive.model.Permission;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 *
 */
public class PermissionSerializer implements Serializer<Permission> {

    private AppConfig config;

    @Inject
    PermissionSerializer(AppConfig config) {
        this.config = config;
    }

    @Override
    public Permission read(InputStream is, List<String> errors) throws IOException {

        List<String> lines = IOUtils.readLines(is, config.CHARSET);
        StringBuilder content = new StringBuilder();

        for (String line : lines) {
            content.append(line);
        }

        Permission permission = new Permission();
        permission.setPermission(content.toString());

        return permission;
    }

    @Override
    public void write(Permission object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
