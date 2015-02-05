package rosa.archive.core.serialize;

import org.apache.commons.io.IOUtils;
import rosa.archive.core.ArchiveConstants;
import rosa.archive.model.FileMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileMapSerializer implements Serializer<FileMap>, ArchiveConstants {


    @Override
    public FileMap read(InputStream is, List<String> errors) throws IOException {
        FileMap filemap = new FileMap();
        Map<String, String> map = filemap.getMap() == null ? new HashMap<String, String>() : filemap.getMap();

        List<String> lines = IOUtils.readLines(is, UTF_8);
        for (String line : lines) {
            // Ignore comments
            if (line.startsWith("#")) {
                continue;
            } else if (line.contains("#")) {
                line = line.substring(0, line.indexOf('#'));
            }

            String[] parts = line.split(",");
            if (parts.length != 2) {
                continue;
            }

            map.put(parts[0], parts[1]);
        }

        return filemap;
    }

    @Override
    public void write(FileMap map, OutputStream out) throws IOException {
        List<Map.Entry<String, String>> list = new ArrayList<>(map.getMap().entrySet());
        // Sort list according to keys
        Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        for (Map.Entry<String, String> entry : list) {
            String line = entry.getKey() + "," + entry.getValue() + "\n";
            out.write(line.getBytes(UTF_8));
        }
        out.flush();
    }

    @Override
    public Class<FileMap> getObjectType() {
        return FileMap.class;
    }
}
