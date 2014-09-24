package rosa.archive.core;

import com.google.inject.Inject;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.config.AppConfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class PropertyInjectionTest {

    @Inject
    private AppConfig context;

    @Test
    public void test() {
        System.out.println(context.getCHARSET());
        System.out.println(Arrays.toString(context.languages()));
    }

//    @Test
//    public void generateTextFiles() throws Exception {
//        URL originalLocation = getClass().getClassLoader()
//                .getResource("data/LudwigXV7/LudwigXV7.001r.tif");
//        assertNotNull(originalLocation);
//
//        URI originalURI = originalLocation.toURI();
//        Path originalPath = Paths.get(originalURI);
//        assertNotNull(originalPath);
//
//        // Define new file names
//        List<String> names = new ArrayList<>();
//        for (int i = 1; i <= 10; i++) {
//            for (int j = 0; j < 2; j++) {
//                names.add(
//                        "LudwigXV7."
//                        + String.format("%03d", i)
//                        + (j == 0 ? "r" : "v")
//                        + ".tif"
//                );
//            }
//        }
//
//        copyFiles(names, originalPath);
//    }

    private void copyFiles(List<String> newNames, Path originalPath) throws Exception {
        // Create the new files
        for (String name : newNames) {
            Path nPath = originalPath.getParent().resolve(name);
            // Do not copy if file already exists
            if (nPath.toFile().exists()) {
                continue;
            }
            Files.copy(originalPath, nPath);
        }
    }

//    @Test
//    public void writeChecksums() throws Exception {
//
//        final String SHA1SUM = "LudwigXV7.SHA1SUM";
//        Path base = Paths.get(getClass().getClassLoader()
//                .getResource("data/LudwigXV7/LudwigXV7.crop.txt")
//                .toURI()).getParent();
//
//        try (
//                OutputStream out = Files.newOutputStream(base.resolve(base.resolve(SHA1SUM)));
//                DirectoryStream<Path> ds = Files.newDirectoryStream(base)
//            ) {
//
//            for (Path path : ds) {
//                if (Files.isRegularFile(path) && !path.getFileName().toString().equals(SHA1SUM)) {
//                    try (InputStream in = Files.newInputStream(path)) {
//                        MessageDigest md = DigestUtils.getSha1Digest();
//                        DigestUtils.updateDigest(md, in);
//
//                        String hexStr =
//                                Hex.encodeHexString(md.digest())
//                                + "  "
//                                + path.getFileName()
//                                + "\n";
//
//                        out.write(hexStr.getBytes("UTF-8"));
//                        out.flush();
//                    }
//                }
//            }
//        }
//    }

}
