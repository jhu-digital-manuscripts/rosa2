package rosa.search.core;

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
 * <pre>
 * Query -> Term | "(" Query Operation Query ")"
 * Operation -> "&" | "|"
 * Term -> Field ~ ":" ~ Value
 * Field -> [\w_-]+
 * Value -> "'" ~ .* ~ "'" (backslash is escape character)
 * </pre>
 */
public class QueryParser {
    /**
     * @return the next query present in input.
     * @throws ParseException
     */
    public static Query parseQuery(ParserInput input) throws ParseException {
        ParserUtil.skipWhitespace(input);

        char c = input.peek();

        if (c == '(') {
            return parseOperation(input);
        } else {
            return parseTerm(input);
        }
    }
    
    public static Query parseQuery(CharSequence s) throws ParseException {
        return parseQuery(new CharSequenceParserInput(s));
    }

    private static Query parseOperation(ParserInput input) throws ParseException {
        if (input.next() != '(') {
            throw new ParseException(input, "operation", "Operation must start with '('");
        }

        Query q1 = parseQuery(input);

        ParserUtil.skipWhitespace(input);
        char c = input.next();
        QueryOperation op = null;

        if (c == '&') {
            op = QueryOperation.AND;
        } else if (c == '|') {
            op = QueryOperation.OR;
        } else {
            throw new ParseException(input, "operation", "Invalid operation. Must be & or |");
        }

        Query q2 = parseQuery(input);

        ParserUtil.skipWhitespace(input);

        if (input.next() != ')') {
            throw new ParseException(input, "operation", "Operation must end with ')'");
        }

        return new Query(op, q1, q2);
    }

    private static Query parseTerm(ParserInput input) throws ParseException {
        String field = ParserUtil.parseWord(input);

        if (input.next() != ':') {
            throw new ParseException(input, "term", "Term must have : after field name");
        }

        String value = ParserUtil.parseString(input);

        if (input.more()) {
            char c = input.peek();
            
            if (c != ')' && !Character.isWhitespace(c)) {
                throw new ParseException(input, "term",
                        " must succeeded by have ) or whitespace");
            }
        }

        return new Query(field, value);
    }
}
