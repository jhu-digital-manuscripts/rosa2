package rosa.archive.core.serialize;

import org.apache.commons.io.IOUtils;
import rosa.archive.core.RoseConstants;
import rosa.archive.core.util.CSV;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 *
 */
public class ImageListSerializer implements Serializer<ImageList> {

    public ImageListSerializer() {  }

    @Override
    public ImageList read(InputStream is) throws IOException {
        ImageList images = new ImageList();

        List<BookImage> imgList = images.getImages();
        List<String> rows = IOUtils.readLines(is);

        for (String row : rows) {
            String[] csvRow = CSV.parse(row);

            BookImage image = new BookImage();

            // Check for the "missing image" prefix.
            // If present, remove the prefix from name before setting ID.
            String id = csvRow[0];
            boolean missing = id.startsWith(RoseConstants.MISSING_PREFIX);

            if (missing) {
                id = id.substring(RoseConstants.MISSING_PREFIX.length());
            }

            image.setId(id);
            image.setMissing(missing);

            // Set image dimensions.
            int width = 0;
            int height = 0;

            // Missing images may only have the ID column.
            if (csvRow.length > 1) {
                try {
                    width = Integer.parseInt(csvRow[1]);
                    height = Integer.parseInt(csvRow[2]);
                } catch (NumberFormatException e) {
                    // TODO log
                    continue;
                }
            }
            image.setWidth(width);
            image.setHeight(height);

            // Add image to the list!
            imgList.add(image);
        }

        return images;
    }

    @Override
    public void write(ImageList object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
