package rosa.iiif.presentation.core.transform.impl;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.jhsearch.JHSearchService;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.Service;
import rosa.iiif.presentation.model.TextValue;
import rosa.iiif.presentation.model.Within;

public class CollectionTransformer extends BasePresentationTransformer {
    public static final String TOP_COLLECTION_LABEL = "All JHU IIIF Collections";

    @Inject
    public CollectionTransformer(@Named("formatter.presentation") IIIFPresentationRequestFormatter presRequestFormatter) {
        super(presRequestFormatter);
    }

    public Collection collection(BookCollection collection) {
        Collection col = new Collection();

        col.setId(urlId(collection.getId(), null, collection.getId(), PresentationRequestType.COLLECTION));
        col.setLabel(collection.getLabel(), "en");
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

        col.addService(new Service(
                JHSearchService.CONTEXT_URI,
                urlId(col.getId(), null, col.getId(), PresentationRequestType.COLLECTION)
                        + JHSearchService.RESOURCE_PATH,
                IIIF_SEARCH_PROFILE,
                col.getLabel("en")
        ));
        col.addService(new Service(
                JHSearchService.CONTEXT_URI,
                urlId("top", null, "top", PresentationRequestType.COLLECTION)
                        + JHSearchService.RESOURCE_PATH,
                IIIF_SEARCH_PROFILE,
                TOP_COLLECTION_LABEL
        ));

        col.setWithin(new Within(
                urlId("top", null, "top", PresentationRequestType.COLLECTION)
        ));

        return col;
    }

    public Collection topCollection(List<BookCollection> collections) {
        Collection col = new Collection();

        col.setId(urlId("top", null, "top", PresentationRequestType.COLLECTION));
        col.setLabel(TOP_COLLECTION_LABEL, "en");
        col.setDescription("Top level collection bringing together all other collections in this archive.", "en");
        col.setType(SC_COLLECTION);

        List<Reference> cols = new ArrayList<>();
        for (BookCollection c : collections) {
            Reference ref = new Reference();

            ref.setType(SC_COLLECTION);
            ref.setLabel(new TextValue(c.getLabel(), "en"));
            ref.setReference(urlId(c.getId(), null, c.getId(), PresentationRequestType.COLLECTION));

            cols.add(ref);
        }

        col.addService(new Service(
                JHSearchService.CONTEXT_URI,
                col.getId() + JHSearchService.RESOURCE_PATH,
                IIIF_SEARCH_PROFILE
        ));

        col.setCollections(cols);
        return col;
    }
}
