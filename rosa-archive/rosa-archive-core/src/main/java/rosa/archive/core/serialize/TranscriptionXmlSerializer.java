package rosa.archive.core.serialize;

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import rosa.archive.core.config.AppConfig;
import rosa.archive.model.Transcription;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Read / write to file &lt;ID&gt;.transcription.xml
 */
public class TranscriptionXmlSerializer implements Serializer<Transcription> {

    private AppConfig config;

    @Inject
    TranscriptionXmlSerializer(AppConfig config) {
        this.config = config;
    }

    @Override
    public Transcription read(InputStream is, List<String> errors) throws IOException {

        List<String> lines = IOUtils.readLines(is, config.CHARSET);

        StringBuilder content = new StringBuilder();
        for (String line : lines) {
            content.append(line);
        }

        Transcription transcription = new Transcription();
        transcription.setContent(content.toString());

        return transcription;
    }

    @Override
    public void write(Transcription object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

}
