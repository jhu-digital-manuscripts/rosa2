package rosa.archive.aor;

import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AorStatsAdapter {

    public static Stats adaptAnnotatedPage(AnnotatedPage ap, String pageId) {
        Stats bs = new Stats(pageId);
        List<String> marginalia_words = get_marginalia_words(ap);

        bs.marginalia += ap.getMarginalia().size();
        bs.marginalia_words += marginalia_words.size();
        bs.underlines += ap.getUnderlines().size();
        bs.underline_words += count_underline_words(ap);
        bs.marks += ap.getMarks().size();
        bs.mark_words += count_mark_words(ap);
        bs.symbols += ap.getSymbols().size();
        bs.symbol_words += count_symbol_words(ap);
        bs.drawings += ap.getDrawings().size();
        bs.numerals += ap.getNumerals().size();
        bs.books += count_marginalia_books(ap);
        bs.people += count_marginalia_people(ap);
        bs.locations += count_marginalia_locations(ap);

        bs.marginalia_vocab = getMarginaliaWords(ap);
        bs.underlines_vocab = getUnderlinedWords(ap);
        bs.marks_vocab = getMarkedWords(ap);
        bs.symbols_vocab = getSymbolWords(ap);

        return bs;
    }

    private static Vocab getMarginaliaWords(AnnotatedPage page) {
        Vocab vocab = new Vocab();

        for (Marginalia marg : page.getMarginalia()) {
            for (MarginaliaLanguage lang : marg.getLanguages()) {
                for (Position pos : lang.getPositions()) {
                    for (String text : pos.getTexts()) {
                        vocab.update(lang.getLang(), Arrays.asList(AoRVocabUtil.parse_text(text)));
                    }
                }
            }
        }

        return vocab;
    }

    private static Vocab getUnderlinedWords(AnnotatedPage page) {
        Vocab vocab = new Vocab();

        for (Underline underline : page.getUnderlines()) {
            vocab.update(
                    underline.getLanguage(),
                    Arrays.asList(AoRVocabUtil.parse_text(underline.getReferencedText()))
            );
        }

        return vocab;
    }

    private static Vocab getMarkedWords(AnnotatedPage page) {
        Vocab vocab = new Vocab();

        for (Mark mark : page.getMarks()) {
            // TODO can associate words with certain Marks
            vocab.update(
                    mark.getLanguage(),
                    Arrays.asList(AoRVocabUtil.parse_text(mark.getReferencedText()))
            );
        }

        return vocab;
    }

    private static Vocab getSymbolWords(AnnotatedPage page) {
        Vocab vocab = new Vocab();

        for (Symbol symbol : page.getSymbols()) {
            // TODO can associate words with certain Symbols
            vocab.update(
                    symbol.getLanguage(),
                    Arrays.asList(AoRVocabUtil.parse_text(symbol.getReferencedText()))
            );
        }

        return vocab;
    }

    private static List<String> get_marginalia_words(AnnotatedPage ap) {
        List<String> words = new ArrayList<>();

        for (Marginalia m: ap.getMarginalia()) {
            for (MarginaliaLanguage ml: m.getLanguages()) {
                for (Position p: ml.getPositions()) {
                    for (String text: p.getTexts()) {
                        words.addAll(Arrays.asList(AoRVocabUtil.parse_text(text)));
                    }
                }
            }
        }

        return words;
    }

    private static int count_marginalia_books(AnnotatedPage ap) {
        int count = 0;

        for (Marginalia m: ap.getMarginalia()) {
            for (MarginaliaLanguage ml: m.getLanguages()) {
                for (Position p: ml.getPositions()) {
                    count += p.getBooks().size();
                }
            }
        }

        return count;
    }

    private static int count_marginalia_people(AnnotatedPage ap) {
        int count = 0;

        for (Marginalia m: ap.getMarginalia()) {

            for (MarginaliaLanguage ml: m.getLanguages()) {
                for (Position p: ml.getPositions()) {
                    count += p.getPeople().size();
                }
            }
        }

        return count;
    }

    private static int count_marginalia_locations(AnnotatedPage ap) {
        int count = 0;

        for (Marginalia m: ap.getMarginalia()) {

            for (MarginaliaLanguage ml: m.getLanguages()) {
                for (Position p: ml.getPositions()) {
                    count += p.getLocations().size();
                }
            }
        }

        return count;
    }

    private static int count_underline_words(AnnotatedPage ap) {
        int count = 0;

        for (Underline ul: ap.getUnderlines()) {
            count += AoRVocabUtil.count_words(ul.getReferencedText());
        }

        return count;
    }

    private static int count_mark_words(AnnotatedPage ap) {
        int count = 0;

        for (Mark m: ap.getMarks()) {
            count += AoRVocabUtil.count_words(m.getReferencedText());
        }

        return count;
    }

    private static int count_symbol_words(AnnotatedPage ap) {
        int count = 0;

        for (Symbol s: ap.getSymbols()) {
            count += AoRVocabUtil.count_words(s.getReferencedText());
        }

        return count;
    }
}
