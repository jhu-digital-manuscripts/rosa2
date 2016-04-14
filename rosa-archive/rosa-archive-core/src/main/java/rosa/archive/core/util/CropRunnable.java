package rosa.archive.core.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import rosa.archive.model.BookImage;
import rosa.archive.model.CropData;

import javax.imageio.ImageIO;

public class CropRunnable implements Runnable {
    private BookImage image;
    private CropData crop;
    private List<String> errors;

    private Path basePath;
    private String cropDir;

    /**
     * @param basePath base location of the book
     * @param image image to crop
     * @param crop crop information
     * @param cropDir directory to save the cropped image
     * @param errors list of errors
     */
    public CropRunnable(String basePath, BookImage image, CropData crop, String cropDir, List<String> errors) {
        this.image = image;
        this.crop = crop;
        this.errors = errors;
        this.cropDir = cropDir;

        this.basePath = Paths.get(basePath);
    }

    @Override
    public void run() {
        Path sourcePath = basePath.resolve(image.getId());
        Path destPath = basePath.resolve(cropDir).resolve(image.getId());

        BufferedImage im;

        try {
            im = ImageIO.read(Files.newInputStream(sourcePath));
        } catch (IOException e) {
            errors.add("Failed to read image. [" + basePath.toString() + "]");
            return;
        }

        // Check image dimensions against BookImage
        if (image.getWidth() != im.getWidth() || image.getHeight() != im.getHeight()) {
            errors.add("Image dimensions from Book does not match actual image dimensions.");
        }

        int[] points = calcPoints();

        int x = points[2];
        int y = points[0];
        int w = points[3] - points[2];
        int h = points[1] - points[0];

        try {
            // Create crop directory if it does not already exist
            if (!Files.exists(basePath.resolve(cropDir))) {
                Files.createDirectory(basePath.resolve(cropDir));
            }

            ImageIO.write(im.getSubimage(x, y, w, h), "tif", Files.newOutputStream(destPath));

        } catch (IOException e) {
            errors.add("Failed to write cropped image to file. [" + destPath.toString() + "]");
        }
    }

    /**
     * @return array (top, bottom, left, right)
     */
    int[] calcPoints() {
        return new int[] {
                (int) (image.getHeight() * crop.getTop()),
                (int) (image.getHeight() - (image.getHeight() * crop.getBottom())),
                (int) (image.getWidth() * crop.getLeft()),
                (int) (image.getWidth() - (image.getWidth() * crop.getRight()))
        };
    }
}