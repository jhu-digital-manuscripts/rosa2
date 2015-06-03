package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

import rosa.archive.model.Transcription;

/**
 * Read / write to file &lt;ID&gt;.transcription.xml
 */
public class TranscriptionXmlSerializer implements Serializer<Transcription> {
    @Override
    public Transcription read(InputStream is, List<String> errors) throws IOException {

        List<String> lines = IOUtils.readLines(is, UTF_8);

        StringBuilder content = new StringBuilder();
        for (String line : lines) {
            content.append(line);
        }

        Transcription transcription = new Transcription();
        transcription.setXML(content.toString());

        return transcription;
    }

    @Override
    public void write(Transcription object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Class<Transcription> getObjectType() {
        return Transcription.class;
    }

}
