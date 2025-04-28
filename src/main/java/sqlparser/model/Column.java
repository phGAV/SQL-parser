package main.java.sqlparser.model;

import java.util.Objects;

/**
 * Represents a column in a SQL query, which may have an alias.
 */
public class Column {
    private String expression; // Column expression
    private String alias;      // Optional alias for the column

    public Column(String expression) {
        this.expression = expression;
    }

    public Column(String expression, String alias) {
        this.expression = expression;
        this.alias = alias;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean hasAlias() {
        return alias != null && !alias.isEmpty();
    }

    @Override
    public String toString() {
        if (hasAlias()) {
            return expression + " AS " + alias;
        }
        return expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Column column = (Column) o;
        return Objects.equals(expression, column.expression) &&
               Objects.equals(alias, column.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, alias);
    }
}
