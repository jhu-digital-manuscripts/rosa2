package rosa.archive.model.aor;

/**
 * A cross-reference within a marginalia annotation, linking a person to a title.
 *
 * @param person   the person referenced
 * @param title    the title referenced
 * @param text     the text content of the cross-reference
 * @param language the language code of the cross-reference
 */
public record XRef(String person, String title, String text, String language) {}
