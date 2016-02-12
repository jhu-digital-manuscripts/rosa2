package rosa.search.core;

public class ParserUtil {

    public static void skipWhitespace(ParserInput input)
            throws ParseException {
        while (input.more() && Character.isWhitespace(input.peek())) {
            input.next();
        }
    }

    public static String parseWord(ParserInput input)
            throws ParseException {
        skipWhitespace(input);
        input.mark();

        do {
            char c = input.peek();

            if (Character.isLetter(c) || c == '_' || c == '-') {
                input.next();
            } else {
                break;
            }
        } while (input.more());

        String s = input.marked();

        if (s.isEmpty()) {
            throw new ParseException(input, "Expecting word");
        }

        return s;
    }
    
    // Parse double ending at first character not digit, "." , or "-".

    public static double parseDouble(ParserInput input) throws ParseException {
        input.mark();

        do {
            char c = input.peek();

            if (Character.isDigit(c) || c == '.' || c == '-') {
                input.next();
            } else {
                break;
            }
        } while (input.more());

        try {
            return Double.parseDouble(input.marked());
        } catch (NumberFormatException e) {
            throw new ParseException(input, "double", e.getMessage());
        }
    }

    public static String parseString(ParserInput input)
            throws ParseException {
        StringBuilder s = new StringBuilder();
        char c = input.next();

        if (c != '\'') {
            throw new ParseException(input, "string",
                    "String must start with '");
        }

        boolean escaped = false;

        for (;;) {
            c = input.next();

            if (escaped) {
                s.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '\'') {
                return s.toString();
            } else {
                s.append(c);
            }
        }
    }

    public static long parseLong(ParserInput input) throws ParseException {
        input.mark();

        char c = input.peek();

        if (c == '-') {
            input.next();
        }

        while (input.more()) {
            c = input.peek();

            if (Character.isDigit(c)) {
                input.next();
            } else {
                break;
            }
        }

        try {
            return Long.parseLong(input.marked());
        } catch (NumberFormatException e) {
            throw new ParseException(input, "long", e.getMessage());
        }
    }
}
