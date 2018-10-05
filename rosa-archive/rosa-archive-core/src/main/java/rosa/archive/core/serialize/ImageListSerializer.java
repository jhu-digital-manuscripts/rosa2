package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.util.BookImageComparator;
import rosa.archive.core.util.CSV;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;

/**
 *
 */
public class ImageListSerializer implements Serializer<ImageList> {
    private final ArchiveNameParser parser;

    public ImageListSerializer() {
        parser = new ArchiveNameParser();
    }

    @Override
    public ImageList read(InputStream is, List<String> errors) throws IOException {
        ImageList images = new ImageList();

        List<BookImage> imgList = images.getImages();
        List<String> rows = IOUtils.readLines(is, UTF_8);

        for (String row : rows) {
            BookImage image = buildBookImage(CSV.parse(row), errors);

            if (image != null) {
                // Add image to the list!
                imgList.add(image);
            }
        }

        return images;
    }

    @Override
    public void write(ImageList imageList, OutputStream out) throws IOException {
        List<BookImage> images = imageList.getImages();

        images.sort(BookImageComparator.instance());

        for (BookImage image : imageList) {
            boolean hasLabel = image.getName() != null && !image.getName().isEmpty();
            String line = image.isMissing() ? MISSING_PREFIX : "";
            line += image.getId() + "," + image.getWidth() + "," + image.getHeight() +
                    (hasLabel ? "," + image.getName() : "") + System.lineSeparator();
            IOUtils.write(line, out, UTF_8);
        }
    }

    @Override
    public Class<ImageList> getObjectType() {
        return ImageList.class;
    }

    private BookImage buildBookImage(String[] csvRow, List<String> errors) {
        BookImage image = new BookImage();
        // Check for the "missing image" prefix.
        // If present, remove the prefix from name before setting ID.
        String id = csvRow[0];
        boolean missing = parser.isMissing(id);

        if (missing) {
            id = id.substring(MISSING_PREFIX.length());
        }

        image.setId(id);
        image.setMissing(missing);
        image.setLocation(parser.location(id));
        image.setRole(parser.role(id));
        image.setName(parser.shortName(id));

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
                return null;
            }
        }
        image.setWidth(width);
        image.setHeight(height);

        if (csvRow.length > 3) {
            String label = csvRow[3];

            if (!label.isEmpty()) {
                image.setName(label);
            }
        }

        return image;
    }
}
