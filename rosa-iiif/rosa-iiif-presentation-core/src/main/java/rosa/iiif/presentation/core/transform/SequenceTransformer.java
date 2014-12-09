package rosa.iiif.presentation.core.transform;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.model.Sequence;

import java.util.HashMap;
import java.util.Map;

public class SequenceTransformer implements Transformer<Sequence> {
    private static final Map<String, Sequence> sequenceCache = new HashMap<>();

    public SequenceTransformer() {}

    @Override
    public Sequence transform(BookCollection collection, Book book, String id) {
        // Check cache to see if it has been loaded already
        String cacheId = collection.getId() + book.getId();
        if (sequenceCache.containsKey(cacheId)) {
            return sequenceCache.get(cacheId);
        }

        // Create a Sequence from the book/image list
        Sequence seq = new Sequence();
        seq.setId(id);
        // do stuff here
        sequenceCache.put(cacheId, seq);
        return seq;
    }

}
