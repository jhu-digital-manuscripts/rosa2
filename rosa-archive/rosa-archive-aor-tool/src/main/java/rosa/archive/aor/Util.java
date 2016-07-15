package rosa.archive.aor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.model.aor.AnnotatedPage;

public class Util {
    private static final AORAnnotatedPageSerializer aor_serializer = new AORAnnotatedPageSerializer();
    
    /**
     * @param xmlPath fully qualified path to AOR page transcription
     * @return AOR annotated page
     * @throws IOException
     */
    public static AnnotatedPage readAorPage(String xmlPath) throws IOException {
        List<String> errors = new ArrayList<>();
        AnnotatedPage page;

        try(InputStream in = Files.newInputStream(Paths.get(xmlPath))) {
            try {
                page = aor_serializer.read(in, errors);
            } catch (IOException e) {
                System.err.println("Skipping " + xmlPath + " due to error: " + e.getMessage());
                return null;
            }
        }

        if (errors.size() > 0) {
            System.err.println("Errors reading " + xmlPath);

            for (String err : errors) {
                System.err.println(err);
            }
        }

        return page;
    }
}
