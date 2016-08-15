package rosa.search.core;

import java.util.ArrayList;
import java.util.List;

import rosa.search.model.Query;
import rosa.search.model.QueryOperation;

/**
 * Grammar syntax:
 * <pre>
 * S ~ T indicates T directly follow S
 * S T indicates T follows S, but may be separated by whitespace
 * S* indicates a sequence of 0 or more S, possibly separated by whitespace
 * S+ indicates a sequence of 1 0r more S, possibly separated by whitespace
 * </pre>
 * 
 * Grammar:
 *
 * Query -&gt; Term | "(" Query (Operation Query)+ ")" [Must use same operation]
 * Operation -&gt; "&amp;" | "|"
 * Term -&gt; Field ~ ":" ~ Value
 * Field -&gt; [\w_-]+
 * Value -&gt; "'" ~ .* ~ "'" [Backslash is escape character]
 */
public class QueryParser {
    /**
     * @param input .
     * @return the next query present in input.
     * @throws ParseException .
     */
    public static Query parseQuery(ParserInput input) throws ParseException {
       return parseQuery(input, true);
    }
    
    private static Query parseQuery(ParserInput input, boolean forbid_trailing_input) throws ParseException {
        ParserUtil.skipWhitespace(input);

        char c = input.peek();
        Query query;
        
        if (c == '(') {
            query = parseOperation(input);
        } else {
            query = parseTerm(input);
        }
        
        if (forbid_trailing_input) {
            ParserUtil.skipWhitespace(input);
            
            if (input.more()) {
                throw new ParseException(input, "Extra content after end of Query.");
            }
        }
        
        return query;
    }
    
    public static Query parseQuery(CharSequence s) throws ParseException {
        return parseQuery(new CharSequenceParserInput(s), true);
    }

    private static Query parseOperation(ParserInput input) throws ParseException {
        if (input.next() != '(') {
            throw new ParseException(input, "operation", "Operation must start with '('");
        }

        List<Query> subqueries =  new ArrayList<>();
        
        subqueries.add(parseQuery(input, false));
        
        QueryOperation op = null;
        
        for (;;) {
            ParserUtil.skipWhitespace(input);
            
            if (!input.more()) {
                throw new ParseException(input, "operation", "Operation must end with )");
            }
            
            char c = input.next();
            
            if (c == '&') {
                if (op != null && op != QueryOperation.AND) {
                    throw new ParseException(input, "operation", "Operation must be &");
                }
                
                op = QueryOperation.AND;
            } else if (c == '|') {
                if (op != null && op != QueryOperation.OR) {
                    throw new ParseException(input, "operation", "Operation must be |");
                }
                
                op = QueryOperation.OR;
            } else if (c == ')') {
                break;
            } else {
                throw new ParseException(input, "operation", "Invalid operation. Must be & or |");
            }
            
            subqueries.add(parseQuery(input, false));
        }
        
        if (subqueries.size() == 0) {
            throw new ParseException(input, "operation", "Invalid operation. Must have at least two queries.");
        }
        
        return new Query(op, subqueries.toArray(new Query[]{}));
    }

    private static Query parseTerm(ParserInput input) throws ParseException {
        String field = ParserUtil.parseWord(input);

        if (input.next() != ':') {
            throw new ParseException(input, "term", "Term must have : after field name");
        }

        String value = ParserUtil.parseString(input);

        // TODO
//        if (input.more()) {
//            char c = input.peek();
//            
//            if (c != ')' && !Character.isWhitespace(c)) {
//                throw new ParseException(input, "term",
//                        " must succeeded by ) or whitespace");
//            }
//        }

        return new Query(field, value);
    }
}
