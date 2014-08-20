package rosa.archive.core.serialize;

import org.apache.commons.io.IOUtils;
import rosa.archive.core.RoseConstants;
import rosa.archive.model.CropData;
import rosa.archive.model.CropInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @see rosa.archive.model.CropInfo
 */
public class CropInfoSerializer implements Serializer<CropInfo> {

    public CropInfoSerializer() {  }

    @Override
    public CropInfo read(InputStream is, List<String> errors) throws IOException {
        CropInfo info = new CropInfo();

        List<String> lines = IOUtils.readLines(is, RoseConstants.CHARSET);
        for (String line : lines) {

            String[] parts = line.split("\\s+");
            if (parts.length != 5) {
                errors.add("Malformed line in crop info: [" + line + "]. Should have 5 columns, but has ("
                        + parts.length + ").");
                continue;
            }

            CropData data = new CropData();

            data.setId(parts[0]);
            try {
                data.setLeft(Double.parseDouble(parts[1]));
                data.setRight(Double.parseDouble(parts[2]));
                data.setTop(Double.parseDouble(parts[3]));
                data.setBottom(Double.parseDouble(parts[4]));
            } catch (NumberFormatException e) {
                errors.add("Failed to parse crop data as decimal [" + line + "]");
                continue;
            }

            info.addCropData(data);
        }

        return info;
    }

    @Override
    public void write(CropInfo object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented!");
    }
}
