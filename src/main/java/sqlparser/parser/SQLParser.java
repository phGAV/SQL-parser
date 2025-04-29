package main.java.sqlparser.parser;

import main.java.sqlparser.model.*;

import java.util.*;

/**
 * Parser for SQL SELECT queries.
 * Parses a SQL query string into a structured Query object.
 */
public class SQLParser {
    private List<Tokenizer.Token> tokens;
    private int currentTokenIndex;

    /**
     * Parses a SQL query string.
     *
     * @param sql The SQL query string to parse
     * @return The parsed Query object
     * @throws SQLParserException if the query cannot be parsed
     */
    public Query parse(String sql) {
        // Tokenize the SQL query
        tokens = Tokenizer.tokenize(sql);
        currentTokenIndex = 0;

        // Parse the SELECT statement
        return parseSelectStatement();
    }

    /**
     * Gets the current token without advancing.
     *
     * @return The current token
     * @throws SQLParserException if there are no more tokens
     */
    private Tokenizer.Token currentToken() {
        if (currentTokenIndex >= tokens.size()) {
            throw new SQLParserException("Unexpected end of query");
        }
        return tokens.get(currentTokenIndex);
    }

    /**
     * Gets the current token and advances to the next token.
     *
     * @return The current token
     * @throws SQLParserException if there are no more tokens
     */
    private Tokenizer.Token consumeToken() {
        Tokenizer.Token token = currentToken();
        currentTokenIndex++;
        return token;
    }

    /**
     * Checks if the current token matches the given type and value.
     *
     * @param type The token type to match
     * @param value The token value to match (case-insensitive)
     * @return true if the current token matches, false otherwise
     */
    private boolean matchToken(Tokenizer.TokenType type, String value) {
        // Check if we're at the end of tokens first
        if (currentTokenIndex >= tokens.size()) {
            return false;
        }

        Tokenizer.Token token = tokens.get(currentTokenIndex);
        return token.getType() == type && token.getValue().equalsIgnoreCase(value);
    }

    /**
     * Consumes the current token if it matches the given type and value.
     *
     * @param type The token type to match
     * @param value The token value to match (case-insensitive)
     * @return true if the token was consumed, false otherwise
     */
    private boolean consumeIfMatch(Tokenizer.TokenType type, String value) {
        if (currentTokenIndex >= tokens.size()) {
            return false;
        }

        Tokenizer.Token token = tokens.get(currentTokenIndex);
        if (token.getType() == type && token.getValue().equalsIgnoreCase(value)) {
            currentTokenIndex++;
            return true;
        }
        return false;
    }

    /**
     * Expects the current token to match the given type and value, and consumes it.
     *
     * @param type The token type to match
     * @param value The token value to match (case-insensitive)
     * @throws SQLParserException if the current token does not match
     */
    private void expectToken(Tokenizer.TokenType type, String value) {
        if (currentTokenIndex >= tokens.size()) {
            throw new SQLParserException("Expected '" + value + "', but reached end of query");
        }

        Tokenizer.Token token = tokens.get(currentTokenIndex);
        if (token.getType() == type && token.getValue().equalsIgnoreCase(value)) {
            currentTokenIndex++;
        } else {
            throw new SQLParserException("Expected '" + value + "', but got '" +
                                         token.getValue() + "'", token.getPosition());
        }
    }

    /**
     * Parses a SELECT statement.
     *
     * @return The parsed Query object
     */
    private Query parseSelectStatement() {
        Query query = new Query();

        // Parse SELECT
        expectToken(Tokenizer.TokenType.KEYWORD, "SELECT");

        // Parse DISTINCT if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "DISTINCT")) {
            query.setDistinct(true);
        }

        // Parse columns
        parseSelectColumns(query);

        // Parse FROM clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "FROM")) {
            parseFromClause(query);
        }

        // Parse JOIN clauses if present
        parseJoinClauses(query);

        // Parse WHERE clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "WHERE")) {
            query.setWhereCondition(parseCondition());
        }

        // Parse GROUP BY clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "GROUP")) {
            expectToken(Tokenizer.TokenType.KEYWORD, "BY");
            parseGroupByClause(query);
        }

        // Parse HAVING clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "HAVING")) {
            query.setHavingCondition(parseCondition());
        }

        // Parse ORDER BY clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "ORDER")) {
            expectToken(Tokenizer.TokenType.KEYWORD, "BY");
            parseOrderByClause(query);
        }

        // Parse LIMIT clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "LIMIT")) {
            Tokenizer.Token limitToken = consumeToken();
            if (limitToken.getType() != Tokenizer.TokenType.NUMERIC_LITERAL) {
                throw new SQLParserException("Expected numeric literal for LIMIT", limitToken.getPosition());
            }
            query.setLimit(Integer.parseInt(limitToken.getValue()));
        }

        // Parse OFFSET clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OFFSET")) {
            Tokenizer.Token offsetToken = consumeToken();
            if (offsetToken.getType() != Tokenizer.TokenType.NUMERIC_LITERAL) {
                throw new SQLParserException("Expected numeric literal for OFFSET", offsetToken.getPosition());
            }
            query.setOffset(Integer.parseInt(offsetToken.getValue()));
        }

        return query;
    }

    /**
     * Parses the column list in a SELECT clause.
     *
     * @param query The Query object to populate
     */
    private void parseSelectColumns(Query query) {
        // Check for SELECT *
        if (consumeIfMatch(Tokenizer.TokenType.STAR, "*")) {
            query.addColumn(new Column("*"));
            return;
        }

        // Parse column list
        do {
            Column column = parseColumn();
            query.addColumn(column);
        } while (consumeIfMatch(Tokenizer.TokenType.COMMA, ","));
    }

    /**
     * Parses a single column in a SELECT or GROUP BY clause.
     *
     * @return The parsed Column object
     */
    private Column parseColumn() {
        // Check if we have tokens before proceeding
        if (currentTokenIndex >= tokens.size()) {
            throw new SQLParserException("Expected column, but reached end of query");
        }

        StringBuilder expressionBuilder = new StringBuilder();
        String alias = null;

        // Handle special cases: COUNT(*), SUM(field), etc.
        if (currentToken().getType() == Tokenizer.TokenType.KEYWORD &&
            (currentToken().getValue().equalsIgnoreCase("COUNT") ||
             currentToken().getValue().equalsIgnoreCase("SUM") ||
             currentToken().getValue().equalsIgnoreCase("AVG") ||
             currentToken().getValue().equalsIgnoreCase("MIN") ||
             currentToken().getValue().equalsIgnoreCase("MAX"))) {

            String functionName = consumeToken().getValue();
            expressionBuilder.append(functionName);

            expectToken(Tokenizer.TokenType.PARENTHESIS_OPEN, "(");
            expressionBuilder.append("(");

            // Parse function argument
            if (consumeIfMatch(Tokenizer.TokenType.STAR, "*")) {
                expressionBuilder.append("*");
            } else {
                // Parse expression inside function
                while (currentTokenIndex < tokens.size() && !matchToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")")) {
                    expressionBuilder.append(consumeToken().getValue());
                }

                // If we've reached the end without finding a closing parenthesis
                if (currentTokenIndex >= tokens.size()) {
                    throw new SQLParserException("Unclosed function parenthesis");
                }
            }

            expectToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")");
            expressionBuilder.append(")");
        } else {
            // Parse column expression (could be a simple column name or a complex expression)
            expressionBuilder.append(parseSimpleExpression());
        }

        // Check for column alias only if we have more tokens
        if (currentTokenIndex < tokens.size()) {
            if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "AS")) {
                // AS keyword is present - make sure we have another token
                if (currentTokenIndex >= tokens.size()) {
                    throw new SQLParserException("Expected identifier for column alias after AS, but reached end of query");
                }

                if (currentToken().getType() == Tokenizer.TokenType.IDENTIFIER ||
                    currentToken().getType() == Tokenizer.TokenType.KEYWORD) {
                    alias = consumeToken().getValue();
                } else {
                    throw new SQLParserException("Expected identifier for column alias", currentToken().getPosition());
                }
            } else if (currentToken().getType() == Tokenizer.TokenType.IDENTIFIER &&
                       !matchToken(Tokenizer.TokenType.COMMA, ",") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "FROM") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "WHERE") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "GROUP") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "HAVING") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "ORDER") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "LIMIT") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "OFFSET")) {
                // Implicit alias (no AS keyword)
                alias = consumeToken().getValue();
            }
        }

        return new Column(expressionBuilder.toString(), alias);
    }

    /**
     * Parses a simple expression (column name, literal, or simple calculation).
     *
     * @return The parsed expression as a string
     */
    private String parseSimpleExpression() {
        StringBuilder expressionBuilder = new StringBuilder();

        // Parse the first token (could be an identifier, literal, or open parenthesis)
        Tokenizer.Token token = consumeToken();
        expressionBuilder.append(token.getValue());

        // Handle table.column notation
        if (token.getType() == Tokenizer.TokenType.IDENTIFIER &&
            consumeIfMatch(Tokenizer.TokenType.DOT, ".")) {
            expressionBuilder.append(".");
            Tokenizer.Token columnToken = consumeToken();
            expressionBuilder.append(columnToken.getValue());
        }

        return expressionBuilder.toString();
    }

    /**
     * Parses the FROM clause of a SELECT statement.
     *
     * @param query The Query object to populate
     */
    private void parseFromClause(Query query) {
        // Parse table sources
        do {
            TableSource tableSource = parseTableSource();
            query.addFromSource(tableSource);

            // Add implicit join for comma-separated tables
            if (consumeIfMatch(Tokenizer.TokenType.COMMA, ",")) {
                // There's another table after the comma, so we need to parse it
                // and create an implicit join
                TableSource rightTable = parseTableSource();
                Join implicitJoin = new Join(rightTable, JoinType.IMPLICIT);
                query.addJoin(implicitJoin);
            }
        } while (consumeIfMatch(Tokenizer.TokenType.COMMA, ","));
    }

    /**
     * Parses a table source in the FROM clause (table name or subquery).
     *
     * @return The parsed TableSource object
     */
    private TableSource parseTableSource() {
        // Check if we have tokens before proceeding
        if (currentTokenIndex >= tokens.size()) {
            throw new SQLParserException("Expected table source, but reached end of query");
        }

        if (consumeIfMatch(Tokenizer.TokenType.PARENTHESIS_OPEN, "(")) {
            // Subquery
            Query subquery = parseSelectStatement();
            expectToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")");

            // Check if we have tokens for the alias
            if (currentTokenIndex >= tokens.size()) {
                throw new SQLParserException("Subquery in FROM clause must have an alias, but reached end of query");
            }

            // Alias is required for subqueries
            String alias;
            if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "AS")) {
                // Explicit alias with AS
                if (currentTokenIndex >= tokens.size()) {
                    throw new SQLParserException("Expected identifier for subquery alias, but reached end of query");
                }

                if (currentToken().getType() == Tokenizer.TokenType.IDENTIFIER ||
                    currentToken().getType() == Tokenizer.TokenType.KEYWORD) {
                    alias = consumeToken().getValue();
                } else {
                    throw new SQLParserException("Expected identifier for subquery alias", currentToken().getPosition());
                }
            } else if (currentToken().getType() == Tokenizer.TokenType.IDENTIFIER ||
                       currentToken().getType() == Tokenizer.TokenType.KEYWORD) {
                // Implicit alias without AS
                alias = consumeToken().getValue();
            } else {
                throw new SQLParserException("Subquery in FROM clause must have an alias", currentToken().getPosition());
            }

            return new TableSource(subquery, alias);
        } else {
            // Table name
            if (currentToken().getType() != Tokenizer.TokenType.IDENTIFIER &&
                currentToken().getType() != Tokenizer.TokenType.KEYWORD) {
                throw new SQLParserException("Expected table name", currentToken().getPosition());
            }

            String tableName = consumeToken().getValue();
            String alias = null;

            // Check for alias - only if we have more tokens
            if (currentTokenIndex < tokens.size()) {
                if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "AS")) {
                    // Explicit alias with AS
                    if (currentTokenIndex >= tokens.size()) {
                        throw new SQLParserException("Expected identifier for table alias, but reached end of query");
                    }

                    if (currentToken().getType() == Tokenizer.TokenType.IDENTIFIER ||
                        currentToken().getType() == Tokenizer.TokenType.KEYWORD) {
                        alias = consumeToken().getValue();
                    } else {
                        throw new SQLParserException("Expected identifier for table alias", currentToken().getPosition());
                    }
                } else if (currentToken().getType() == Tokenizer.TokenType.IDENTIFIER ||
                           currentToken().getType() == Tokenizer.TokenType.KEYWORD) {
                    // Check if the next token is not a reserved keyword for clauses
                    if (!matchToken(Tokenizer.TokenType.KEYWORD, "ON") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "JOIN") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "INNER") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "LEFT") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "RIGHT") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "FULL") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "CROSS") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "WHERE") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "GROUP") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "HAVING") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "ORDER") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "LIMIT") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "OFFSET") &&
                        !matchToken(Tokenizer.TokenType.COMMA, ",")) {
                        // Implicit alias without AS
                        alias = consumeToken().getValue();
                    }
                }
            }

            return new TableSource(tableName, alias);
        }
    }

    /**
     * Parses JOIN clauses in a SELECT statement.
     *
     * @param query The Query object to populate
     */
    private void parseJoinClauses(Query query) {
        while (isJoinKeyword()) {
            JoinType joinType = parseJoinType();
            expectToken(Tokenizer.TokenType.KEYWORD, "JOIN");

            TableSource tableSource = parseTableSource();
            Condition onCondition = null;
            String usingColumns = null;

            // Parse ON or USING clause - only if we have more tokens
            if (currentTokenIndex < tokens.size()) {
                if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "ON")) {
                    onCondition = parseCondition();
                } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "USING")) {
                    expectToken(Tokenizer.TokenType.PARENTHESIS_OPEN, "(");
                    usingColumns = consumeToken().getValue();
                    expectToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")");
                } else if (joinType != JoinType.CROSS) {
                    // ON or USING is required for all join types except CROSS JOIN
                    throw new SQLParserException("Expected ON or USING clause after JOIN",
                                                 currentTokenIndex < tokens.size() ? currentToken().getPosition() : tokens.get(tokens.size() - 1).getPosition());
                }
            } else if (joinType != JoinType.CROSS) {
                // We're at the end of tokens but need an ON or USING clause
                throw new SQLParserException("Expected ON or USING clause after JOIN, but reached end of query");
            }

            Join join = new Join(tableSource, joinType, onCondition);
            join.setUsingColumns(usingColumns);
            query.addJoin(join);
        }
    }

    /**
     * Checks if the current token is a JOIN keyword.
     *
     * @return true if the current token is a JOIN keyword, false otherwise
     */
    private boolean isJoinKeyword() {
        if (currentTokenIndex >= tokens.size()) {
            return false;
        }
        return matchToken(Tokenizer.TokenType.KEYWORD, "JOIN") ||
               matchToken(Tokenizer.TokenType.KEYWORD, "INNER") ||
               matchToken(Tokenizer.TokenType.KEYWORD, "LEFT") ||
               matchToken(Tokenizer.TokenType.KEYWORD, "RIGHT") ||
               matchToken(Tokenizer.TokenType.KEYWORD, "FULL") ||
               matchToken(Tokenizer.TokenType.KEYWORD, "CROSS");
    }

    /**
     * Parses a JOIN type.
     *
     * @return The parsed JoinType
     */
    private JoinType parseJoinType() {
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "INNER")) {
            return JoinType.INNER;
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "LEFT")) {
            consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OUTER"); // OUTER is optional
            return JoinType.LEFT;
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "RIGHT")) {
            consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OUTER"); // OUTER is optional
            return JoinType.RIGHT;
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "FULL")) {
            consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OUTER"); // OUTER is optional
            return JoinType.FULL;
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "CROSS")) {
            return JoinType.CROSS;
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "JOIN")) {
            // If just "JOIN" is specified, it's an INNER JOIN
            currentTokenIndex--; // Put back the "JOIN" token
            return JoinType.INNER;
        } else {
            // Safety check for accessing the token
            if (currentTokenIndex >= tokens.size()) {
                throw new SQLParserException("Expected JOIN type, but reached end of query");
            }
            throw new SQLParserException("Expected JOIN type", currentToken().getPosition());
        }
    }

    /**
     * Parses a condition (for WHERE, HAVING, or ON clauses).
     *
     * @return The parsed Condition object
     */
    private Condition parseCondition() {
        Condition condition = new Condition();

        // Check for NOT
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "NOT")) {
            condition.setNegated(true);
        }

        // Check for parenthesized condition
        if (consumeIfMatch(Tokenizer.TokenType.PARENTHESIS_OPEN, "(")) {
            // Nested condition
            Condition nestedCondition = parseCondition();
            expectToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")");
            condition.addNestedCondition(nestedCondition);
        } else {
            // Simple condition
            Object leftExpr = parseExpression();
            String operator = parseOperator();
            Object rightExpr = parseExpression();

            condition.setLeftExpression(leftExpr);
            condition.setOperator(operator);
            condition.setRightExpression(rightExpr);
        }

        // Check for logical operators (AND, OR)
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "AND")) {
            condition.setLogicalOperator("AND");
            condition.setNextCondition(parseCondition());
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OR")) {
            condition.setLogicalOperator("OR");
            condition.setNextCondition(parseCondition());
        }

        return condition;
    }

    /**
     * Parses an expression (column, literal, function, etc.).
     *
     * @return The parsed expression as an Object
     */
    private Object parseExpression() {
        // Check if we have tokens before accessing
        if (currentTokenIndex >= tokens.size()) {
            throw new SQLParserException("Expected expression, but reached end of query");
        }

        // Check for aggregate functions
        if (currentToken().getType() == Tokenizer.TokenType.KEYWORD &&
            (currentToken().getValue().equalsIgnoreCase("COUNT") ||
             currentToken().getValue().equalsIgnoreCase("SUM") ||
             currentToken().getValue().equalsIgnoreCase("AVG") ||
             currentToken().getValue().equalsIgnoreCase("MIN") ||
             currentToken().getValue().equalsIgnoreCase("MAX"))) {

            StringBuilder expression = new StringBuilder();

            // Add the function name
            expression.append(consumeToken().getValue());

            // Add the opening parenthesis
            if (currentTokenIndex >= tokens.size() || !consumeIfMatch(Tokenizer.TokenType.PARENTHESIS_OPEN, "(")) {
                throw new SQLParserException("Expected '(' after aggregate function");
            }
            expression.append("(");

            // Handle the function arguments
            if (consumeIfMatch(Tokenizer.TokenType.STAR, "*")) {
                // Special case for COUNT(*)
                expression.append("*");
            } else {
                // Handle other arguments
                while (currentTokenIndex < tokens.size() && !matchToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")")) {
                    expression.append(consumeToken().getValue());
                }
            }

            // Add the closing parenthesis
            if (currentTokenIndex >= tokens.size() || !consumeIfMatch(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")")) {
                throw new SQLParserException("Expected ')' to close aggregate function");
            }
            expression.append(")");

            return expression.toString();
        }
        // For simplicity, we'll just parse simple expressions for now
        else if (currentToken().getType() == Tokenizer.TokenType.IDENTIFIER) {
            StringBuilder sb = new StringBuilder(consumeToken().getValue());

            // Handle table.column notation
            if (consumeIfMatch(Tokenizer.TokenType.DOT, ".")) {
                // Check if we have another token after the dot
                if (currentTokenIndex >= tokens.size()) {
                    throw new SQLParserException("Incomplete column reference after '.'");
                }
                sb.append(".").append(consumeToken().getValue());
            }

            return sb.toString();
        } else if (currentToken().getType() == Tokenizer.TokenType.STRING_LITERAL) {
            return consumeToken().getValue();
        } else if (currentToken().getType() == Tokenizer.TokenType.NUMERIC_LITERAL) {
            return consumeToken().getValue();
        } else {
            throw new SQLParserException("Expected expression", currentToken().getPosition());
        }
    }

    /**
     * Parses an operator.
     *
     * @return The parsed operator as a String
     */
    private String parseOperator() {
        // Check if we have tokens before accessing
        if (currentTokenIndex >= tokens.size()) {
            throw new SQLParserException("Expected operator, but reached end of query");
        }

        if (currentToken().getType() == Tokenizer.TokenType.OPERATOR) {
            return consumeToken().getValue();
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "IN")) {
            return "IN";
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "BETWEEN")) {
            return "BETWEEN";
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "LIKE")) {
            return "LIKE";
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "IS")) {
            if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "NOT")) {
                return "IS NOT";
            }
            return "IS";
        } else {
            throw new SQLParserException("Expected operator", currentToken().getPosition());
        }
    }

    /**
     * Parses the GROUP BY clause of a SELECT statement.
     *
     * @param query The Query object to populate
     */
    private void parseGroupByClause(Query query) {
        do {
            // Parse column expression
            String columnExpr = parseSimpleExpression();
            query.addGroupByColumn(new Column(columnExpr));
        } while (consumeIfMatch(Tokenizer.TokenType.COMMA, ","));
    }

    /**
     * Parses the ORDER BY clause of a SELECT statement.
     *
     * @param query The Query object to populate
     */
    private void parseOrderByClause(Query query) {
        do {
            // Parse column expression
            String columnExpr = parseSimpleExpression();
            boolean isAscending = true;

            // Parse optional ASC/DESC
            if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "DESC")) {
                isAscending = false;
            } else {
                consumeIfMatch(Tokenizer.TokenType.KEYWORD, "ASC"); // ASC is optional
            }

            query.addOrderByColumn(new OrderByColumn(columnExpr, isAscending));
        } while (consumeIfMatch(Tokenizer.TokenType.COMMA, ","));
    }
}