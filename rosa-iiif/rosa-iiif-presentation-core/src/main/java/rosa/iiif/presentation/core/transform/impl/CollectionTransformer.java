package rosa.iiif.presentation.core.transform.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.jhsearch.JHSearchService;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.Service;
import rosa.iiif.presentation.model.TextValue;

import java.util.ArrayList;
import java.util.List;

public class CollectionTransformer extends BasePresentationTransformer {

    @Inject
    public CollectionTransformer(@Named("formatter.presentation") IIIFPresentationRequestFormatter presRequestFormatter) {
        super(presRequestFormatter);
    }

    public Collection collection(BookCollection collection) {
        Collection col = new Collection();

        col.setId(urlId(collection.getId(), null, collection.getId(), PresentationRequestType.COLLECTION));
        col.setLabel(collection.getId(), "en");
        col.setType(SC_COLLECTION);

        List<Reference> refs = new ArrayList<>();
        for (String title : collection.books()) {
            Reference ref = new Reference();

            ref.setType(SC_MANIFEST);
            ref.setLabel(new TextValue(title, "en"));
            ref.setReference(urlId(collection.getId(), title, null, PresentationRequestType.MANIFEST));

            refs.add(ref);
        }

        col.setManifests(refs);
        
        // TODO Set search service

        return col;
    }

    public Collection topCollection(List<BookCollection> collections) {
        Collection col = new Collection();

        col.setId(urlId("top", null, "top", PresentationRequestType.COLLECTION));
        col.setLabel("top", "en");
        col.setDescription("Top level collection bringing together all other collections in this archive.", "en");
        col.setType(SC_COLLECTION);

        List<Reference> cols = new ArrayList<>();
        for (BookCollection c : collections) {
            Reference ref = new Reference();

            ref.setType(SC_COLLECTION);
            ref.setLabel(new TextValue(c.getId(), "en"));
            ref.setReference(urlId(c.getId(), null, c.getId(), PresentationRequestType.COLLECTION));

            cols.add(ref);
        }

        col.setCollections(cols);
        return col;
    }
}
