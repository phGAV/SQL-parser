package main.java.sqlparser.model;

import java.util.Objects;

/**
 * Represents an ORDER BY column in a SQL query.
 */
public class OrderByColumn {
    private String column;       // Column or expression to sort by
    private boolean ascending;   // Sort direction (true = ASC, false = DESC)

    public OrderByColumn(String column, boolean ascending) {
        this.column = column;
        this.ascending = ascending;
    }

    public OrderByColumn(String column) {
        this.column = column;
        this.ascending = true;  // Default to ascending
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public String toString() {
        return column + (ascending ? " ASC" : " DESC");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderByColumn that = (OrderByColumn) o;
        return ascending == that.ascending &&
               Objects.equals(column, that.column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, ascending);
    }
}
