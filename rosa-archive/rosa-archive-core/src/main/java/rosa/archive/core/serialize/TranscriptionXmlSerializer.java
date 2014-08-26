package rosa.archive.core.serialize;

import org.apache.commons.io.IOUtils;
import rosa.archive.core.RoseConstants;
import rosa.archive.model.Transcription;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Read / write to file &lt;ID&gt;.transcription.xml
 */
public class TranscriptionXmlSerializer implements Serializer<Transcription> {

    @Override
    public Transcription read(InputStream is) throws IOException {

        List<String> lines = IOUtils.readLines(is, RoseConstants.CHARSET);

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
