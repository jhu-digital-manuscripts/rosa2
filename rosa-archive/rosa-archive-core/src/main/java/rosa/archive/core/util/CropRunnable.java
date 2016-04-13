package rosa.archive.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.IOUtils;
import rosa.archive.model.BookImage;
import rosa.archive.model.CropData;

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

        String cmd = buildCommand();
        int success = 0;
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            // Java8 only
//            success = p.waitFor(60, TimeUnit.SECONDS);
            // Java7
            success = p.waitFor();
        } catch (IOException | InterruptedException e) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(out));
            errors.add("Failed to crop image. [" + cmd + "]\n" + out.toString());
        } finally {
            if (p != null) {
                p.destroy();
            }
        }

        if (success != 0) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                IOUtils.copy(p.getErrorStream(), out);
                errors.add("Error in cropping images. [" + cmd + "]: " + out.toString());
            } catch (IOException e) {
                System.err.println("Failed to get errors.");
            }
        }
    }

    /**
     * @return the CLI command to Image Magick that does the actual cropping.
     */
    protected String buildCommand() {
        // top, bottom, left, right
        int[] points = calcPoints();

        int cropWidth = points[3] - points[2];
        int cropHeight = points[1] - points[0];

        Path source = basePath.resolve(image.getId());
        Path destination = basePath.resolve(cropDir).resolve(image.getId());

        // convert <source_image>: -crop WIDTHxHEIGHT+X+Y +repage <target_image>
        return "convert "
                + source.toString() + " -crop "
                + cropWidth + "x" + cropHeight
                + "+" + points[2] + "+" + points[0]
                + " +repage "
                + destination.toString();
    }

    /**
     * @return array (x, y, width, height)
     */
    protected int[] calcPoints() {
        return new int[] {
                (int) (image.getHeight() * crop.getTop()),
                (int) (image.getHeight() - (image.getHeight() * crop.getBottom())),
                (int) (image.getWidth() * crop.getLeft()),
                (int) (image.getWidth() - (image.getWidth() * crop.getRight()))
        };
    }
}