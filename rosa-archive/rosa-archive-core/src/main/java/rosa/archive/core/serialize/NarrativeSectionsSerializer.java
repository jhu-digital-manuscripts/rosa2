package rosa.archive.core.serialize;

import rosa.archive.model.NarrativeSections;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class NarrativeSectionsSerializer implements Serializer<NarrativeSections> {

    NarrativeSectionsSerializer() {  }

    @Override
    public NarrativeSections read(InputStream is) {
        return null;
    }

    @Override
    public void write(NarrativeSections object, OutputStream out) {

    }
}
