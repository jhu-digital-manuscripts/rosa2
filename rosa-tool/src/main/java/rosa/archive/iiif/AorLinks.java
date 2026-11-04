package rosa.archive.iiif;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import rosa.archive.aor.AorReferenceResolver;

/**
 * The links an AoR annotation body can carry: searches for the names it mentions and the
 * pages its internal references point to.
 *
 * @param site    the viewer site's URL forms
 * @param resolve resolves an internal reference target to a page, or null when the
 *                collection has no way to resolve them
 */
public record AorLinks(AorSearchLinks site, Function<String, Optional<AorReferenceResolver.Target>> resolve) {

    public AorLinks {
        Objects.requireNonNull(site, "site");
        if (resolve == null) {
            resolve = _ -> Optional.empty();
        }
    }

    /**
     * The advanced search URL for a field value, or null when the value is blank.
     */
    public String searchHref(String field, String value) {
        return site.href(field, value);
    }

    /**
     * The page an internal reference target points to, if it resolves.
     */
    public Optional<AorReferenceResolver.Target> target(String ref) {
        return ref == null || ref.isBlank() ? Optional.empty() : resolve.apply(ref);
    }

    /**
     * The viewer URL of the page an internal reference target points to, if it resolves.
     */
    public Optional<String> pageHref(String ref) {
        return target(ref).map(t -> site.pageHref(t.bookId(), t.canvasName()));
    }
}
