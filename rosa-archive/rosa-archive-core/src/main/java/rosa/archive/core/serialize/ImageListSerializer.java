package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;

import rosa.archive.core.util.BookImageComparator;
import rosa.archive.core.util.CSV;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;

/**
 *
 */
public class ImageListSerializer implements Serializer<ImageList> {
    @Override
    public ImageList read(InputStream is, List<String> errors) throws IOException {
        ImageList images = new ImageList();

        List<BookImage> imgList = images.getImages();
        List<String> rows = IOUtils.readLines(is, UTF_8);

        for (String row : rows) {
            String[] csvRow = CSV.parse(row);

            BookImage image = new BookImage();

            // Check for the "missing image" prefix.
            // If present, remove the prefix from name before setting ID.
            String id = csvRow[0];
            boolean missing = id.startsWith(MISSING_PREFIX);

            if (missing) {
                id = id.substring(MISSING_PREFIX.length());
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
                    errors.add("Error parsing image dimensions as integers: [" + Arrays.toString(csvRow) + "]");
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
    public void write(ImageList imageList, OutputStream out) throws IOException {
        List<BookImage> images = imageList.getImages();

        Collections.sort(images, BookImageComparator.instance());

        for (BookImage image : imageList) {
            String line = image.isMissing() ? "*" : "";
            line += image.getId() + "," + image.getWidth() + "," + image.getHeight() + "\n";
            IOUtils.write(line, out, UTF_8);
        }
    }

    @Override
    public Class<ImageList> getObjectType() {
        return ImageList.class;
    }
}
