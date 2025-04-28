package main.java.sqlparser.model;

/**
 * Represents the different types of SQL joins.
 */
public enum JoinType {
    INNER,
    LEFT,
    RIGHT,
    FULL,
    CROSS,
    IMPLICIT; // For implicit joins in the FROM clause (e.g., FROM table1, table2)

    @Override
    public String toString() {
        return switch (this) {
            case INNER -> "INNER JOIN";
            case LEFT -> "LEFT JOIN";
            case RIGHT -> "RIGHT JOIN";
            case FULL -> "FULL JOIN";
            case CROSS -> "CROSS JOIN";
            case IMPLICIT -> ",";
        };
    }
}