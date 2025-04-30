package sqlparser.parser;

/**
 * Custom exception for SQL parsing errors.
 */
public class SQLParserException extends RuntimeException {

    public SQLParserException(String message) {
        super(message);
    }

    public SQLParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public SQLParserException(String message, int position) {
        super(message + " at position " + position);
    }
}
