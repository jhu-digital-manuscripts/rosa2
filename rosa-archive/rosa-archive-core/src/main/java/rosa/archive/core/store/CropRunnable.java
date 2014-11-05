package rosa.archive.core.store;

import rosa.archive.model.BookImage;
import rosa.archive.model.CropData;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CropRunnable implements Runnable {
    private BookImage image;
    private CropData crop;
    private List<String> errors;

    private Path basePath;
    private String cropDir;

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
        boolean success = true;
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            success = p.waitFor(30, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException e) {
            errors.add("Failed to crop image. [" + cmd + "]");
        }

        if (!success) {
            errors.add("Image crop timed out. [" + cmd + "]");
        }
    }

    protected String buildCommand() {
        // top, bottom, left, right
        int[] points = calcPoints();

        int cropWidth = points[3] - points[2];
        int cropHeight = points[1] - points[0];

        Path source = basePath.resolve(image.getId());
        Path destination = basePath.resolve(cropDir).resolve(image.getId());

        // convert <source_image>: -crop WIDTHxHEIGHT+X+Y +repage <target_image>
        return "convert "
                + source.toString() + ": -crop "
                + cropWidth + "x" + cropHeight
                + "+" + points[2] + "+" + points[0]
                + " +repage "
                + destination.toString();
    }

    protected int[] calcPoints() {
        return new int[] {
                (int) (image.getHeight() * crop.getTop()),
                (int) (image.getHeight() - (image.getHeight() * crop.getBottom())),
                (int) (image.getWidth() * crop.getLeft()),
                (int) (image.getWidth() - (image.getWidth() * crop.getRight()))
        };
    }
}