package main.java.sqlparser.model;

import java.util.Objects;

/**
 * Represents a table source in the FROM clause of a SQL query.
 * Can be a table name or a subquery.
 */
public class TableSource {
    private String tableName;   // Table name or null if this is a subquery
    private String alias;       // Optional alias
    private Query subquery;     // Non-null if this is a subquery

    // Constructor for a table name
    public TableSource(String tableName) {
        this.tableName = tableName;
    }

    // Constructor for a table name with alias
    public TableSource(String tableName, String alias) {
        this.tableName = tableName;
        this.alias = alias;
    }

    // Constructor for a subquery
    public TableSource(Query subquery, String alias) {
        this.subquery = subquery;
        this.alias = alias;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Query getSubquery() {
        return subquery;
    }

    public void setSubquery(Query subquery) {
        this.subquery = subquery;
    }

    public boolean isSubquery() {
        return subquery != null;
    }

    public boolean hasAlias() {
        return alias != null && !alias.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (isSubquery()) {
            sb.append("(").append(subquery).append(")");
        } else {
            sb.append(tableName);
        }

        if (hasAlias()) {
            sb.append(" AS ").append(alias);
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableSource that = (TableSource) o;
        return Objects.equals(tableName, that.tableName) &&
               Objects.equals(alias, that.alias) &&
               Objects.equals(subquery, that.subquery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, alias, subquery);
    }
}
