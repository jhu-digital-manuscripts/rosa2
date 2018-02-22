package rosa.archive.aor;

import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.GraphText;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Table;
import rosa.archive.model.aor.Underline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AorStatsAdapter {

    public static Stats adaptAnnotatedPage(AnnotatedPage ap, String pageId) {
        Stats bs = new Stats(pageId);
        List<String> marginalia_words = get_marginalia_words(ap);

        // Get word annotation/word counts
        bs.marginalia += ap.getMarginalia().size();
        bs.marginalia_words += marginalia_words.size();
        bs.underlines += ap.getUnderlines().size();
        bs.underline_words += count_underline_words(ap);
        bs.marks += ap.getMarks().size();
        bs.mark_words += count_mark_words(ap);
        bs.symbols += ap.getSymbols().size();
        bs.symbol_words += count_symbol_words(ap);
        bs.drawings += ap.getDrawings().size();
        bs.drawing_words += count_drawing_words(ap);
        bs.numerals += ap.getNumerals().size();
        bs.calculations += ap.getCalculations().size();
        bs.graphs += ap.getGraphs().size();
        bs.graph_words += count_graph_words(ap);
        bs.tables += ap.getTables().size();
        bs.table_words += count_table_words(ap);
        bs.books += count_books(ap);
        bs.people += count_people(ap);
        bs.locations += count_locations(ap);

        // Get vocab data
        bs.marginalia_vocab = getMarginaliaWords(ap);
        bs.underlines_vocab = getUnderlinedWords(ap);
        bs.marks_vocab = getMarkedWords(ap);
        bs.symbols_vocab = getSymbolWords(ap);
        bs.drawing_vocab = getDrawingWords(ap);
        bs.graph_vocab = getGraphWords(ap);
        bs.table_vocab = getTableWords(ap);

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

    private static Vocab getDrawingWords(AnnotatedPage page) {
        Vocab v = new Vocab();
        for (Drawing d : page.getDrawings()) {
            d.getTexts().forEach(t -> v.update(t.getLanguage(), Arrays.asList(AoRVocabUtil.parse_text(t.getText()))));
        }
        return v;
    }

    private static Vocab getGraphWords(AnnotatedPage page) {
        Vocab v = new Vocab();
        for (Graph g : page.getGraphs()) {
            for (GraphText t : g.getGraphTexts()) {
                t.getNotes().forEach(n -> v.update(n.language, Arrays.asList(AoRVocabUtil.parse_text(n.content))));
            }
        }
        return v;
    }

    private static Vocab getTableWords(AnnotatedPage page) {
        Vocab v = new Vocab();
        for (Table t : page.getTables()) {
            t.getTexts().forEach(txt -> v.update(txt.getLanguage(), Arrays.asList(AoRVocabUtil.parse_text(txt.getText()))));
        }
        return v;
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

    private static int count_books(AnnotatedPage ap) {
        int count = 0;

        for (Marginalia m: ap.getMarginalia()) {
            for (MarginaliaLanguage ml: m.getLanguages()) {
                for (Position p: ml.getPositions()) {
                    count += p.getBooks().size();
                }
            }
        }

        for (Graph g : ap.getGraphs()) {
            count += g.getGraphTexts().stream().mapToInt(t -> t.getBooks().size()).sum();
        }

        count += ap.getDrawings().stream().mapToInt(d -> d.getBooks().size()).sum();
        count += ap.getTables().stream().mapToInt(t -> t.getBooks().size()).sum();

        return count;
    }

    private static int count_people(AnnotatedPage ap) {
        int count = 0;

        for (Marginalia m: ap.getMarginalia()) {
            for (MarginaliaLanguage ml: m.getLanguages()) {
                for (Position p: ml.getPositions()) {
                    count += p.getPeople().size();
                }
            }
        }

        for (Graph g : ap.getGraphs()) {
            count += g.getGraphTexts().stream().mapToInt(t -> t.getPeople().size()).sum();
        }

        count += ap.getDrawings().stream().mapToInt(d -> d.getPeople().size()).sum();
        count += ap.getTables().stream().mapToInt(t -> t.getPeople().size()).sum();

        return count;
    }

    private static int count_locations(AnnotatedPage ap) {
        int count = 0;

        for (Marginalia m: ap.getMarginalia()) {
            for (MarginaliaLanguage ml: m.getLanguages()) {
                for (Position p: ml.getPositions()) {
                    count += p.getLocations().size();
                }
            }
        }

        for (Graph g : ap.getGraphs()) {
            count += g.getGraphTexts().stream().mapToInt(t -> t.getLocations().size()).sum();
        }

        count += ap.getDrawings().stream().mapToInt(d -> d.getLocations().size()).sum();
        count += ap.getTables().stream().mapToInt(t -> t.getLocations().size()).sum();

        return count;
    }

    /*
     * -------------------------------------------------------------------------------------------------------
     * ----- Word counts -------------------------------------------------------------------------------------
     * -------------------------------------------------------------------------------------------------------
     */


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

    private static int count_drawing_words(AnnotatedPage ap) {
        int count = 0;
        for (Drawing d : ap.getDrawings()) {
            count += d.getTexts().stream().mapToInt(t -> AoRVocabUtil.count_words(t.getAnchor_text() + ' ' + t.getText())).sum();
            count += AoRVocabUtil.count_words(d.getTranslation());
        }
        return count;
    }

    private static int count_graph_words(AnnotatedPage ap) {
        int count = 0;
        for (Graph g : ap.getGraphs()) {
            for (GraphText t : g.getGraphTexts()) {
                count += t.getNotes().stream()
                        .mapToInt(n -> AoRVocabUtil.count_words(n.content + ' ' + n.anchorText))
                        .sum();
                count += t.getTranslations().stream().mapToInt(AoRVocabUtil::count_words).sum();
            }
        }
        return count;
    }

    private static int count_table_words(AnnotatedPage ap) {
        int count = 0;
        for (Table t : ap.getTables()) {
            count += AoRVocabUtil.count_words(t.getTranslation());
            count += t.getTexts().stream()
                    .mapToInt(txt -> AoRVocabUtil.count_words(txt.getText() + ' ' + txt.getAnchor_text()))
                    .sum();
        }
        return count;
    }
}
