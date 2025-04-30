package sqlparser.util;

import sqlparser.model.*;

import java.util.List;

/**
 * Utility class for formatting Query objects into readable SQL strings.
 */
public class SQLFormatter {

    private static final String INDENT = "  ";

    /**
     * Formats a Query object into a readable SQL string with proper indentation.
     *
     * @param query The Query object to format
     * @return A formatted SQL string
     */
    public static String format(Query query) {
        StringBuilder sb = new StringBuilder();
        formatQuery(query, sb, 0);
        return sb.toString();
    }

    /**
     * Recursively formats a Query object with proper indentation.
     *
     * @param query The Query object to format
     * @param sb The StringBuilder to append to
     * @param indentLevel The current indent level
     */
    private static void formatQuery(Query query, StringBuilder sb, int indentLevel) {
        // SELECT clause
        appendIndent(sb, indentLevel);
        sb.append("SELECT ");

        if (query.isDistinct()) {
            sb.append("DISTINCT ");
        }

        // Columns
        formatColumns(query.getColumns(), sb, indentLevel);

        // FROM clause
        if (!query.getFromSources().isEmpty()) {
            sb.append("\n");
            appendIndent(sb, indentLevel);
            sb.append("FROM ");
            formatTableSources(query.getFromSources(), sb, indentLevel);
        }

        // JOIN clauses
        if (!query.getJoins().isEmpty()) {
            formatJoins(query.getJoins(), sb, indentLevel);
        }

        // WHERE clause
        if (query.getWhereCondition() != null) {
            sb.append("\n");
            appendIndent(sb, indentLevel);
            sb.append("WHERE ");
            formatCondition(query.getWhereCondition(), sb, indentLevel + 1);
        }

        // GROUP BY clause
        if (!query.getGroupByColumns().isEmpty()) {
            sb.append("\n");
            appendIndent(sb, indentLevel);
            sb.append("GROUP BY ");
            formatGroupByColumns(query.getGroupByColumns(), sb, indentLevel);
        }

        // HAVING clause
        if (query.getHavingCondition() != null) {
            sb.append("\n");
            appendIndent(sb, indentLevel);
            sb.append("HAVING ");
            formatCondition(query.getHavingCondition(), sb, indentLevel + 1);
        }

        // ORDER BY clause
        if (!query.getOrderByColumns().isEmpty()) {
            sb.append("\n");
            appendIndent(sb, indentLevel);
            sb.append("ORDER BY ");
            formatOrderByColumns(query.getOrderByColumns(), sb, indentLevel);
        }

        // LIMIT clause
        if (query.getLimit() != null) {
            sb.append("\n");
            appendIndent(sb, indentLevel);
            sb.append("LIMIT ").append(query.getLimit());
        }

        // OFFSET clause
        if (query.getOffset() != null) {
            sb.append("\n");
            appendIndent(sb, indentLevel);
            sb.append("OFFSET ").append(query.getOffset());
        }
    }

    /**
     * Formats a list of columns.
     *
     * @param columns The columns to format
     * @param sb The StringBuilder to append to
     * @param indentLevel The current indent level
     */
    private static void formatColumns(List<Column> columns, StringBuilder sb, int indentLevel) {
        if (columns.isEmpty()) {
            sb.append("*");
        } else {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    sb.append(",\n").append(INDENT);
                    appendIndent(sb, indentLevel);
                }

                Column column = columns.get(i);
                sb.append(column.getExpression());

                if (column.hasAlias()) {
                    sb.append(" AS ").append(column.getAlias());
                }
            }
        }
    }

    /**
     * Formats a list of table sources.
     *
     * @param tableSources The table sources to format
     * @param sb The StringBuilder to append to
     * @param indentLevel The current indent level
     */
    private static void formatTableSources(List<TableSource> tableSources, StringBuilder sb, int indentLevel) {
        for (int i = 0; i < tableSources.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }

            TableSource tableSource = tableSources.get(i);

            if (tableSource.isSubquery()) {
                sb.append("(\n");
                formatQuery(tableSource.getSubquery(), sb, indentLevel + 1);
                sb.append("\n");
                appendIndent(sb, indentLevel);
                sb.append(")");
            } else {
                sb.append(tableSource.getTableName());
            }

            if (tableSource.hasAlias()) {
                sb.append(" AS ").append(tableSource.getAlias());
            }
        }
    }

    /**
     * Formats a list of joins.
     *
     * @param joins The joins to format
     * @param sb The StringBuilder to append to
     * @param indentLevel The current indent level
     */
    private static void formatJoins(List<Join> joins, StringBuilder sb, int indentLevel) {
        for (Join join : joins) {
            sb.append("\n");
            appendIndent(sb, indentLevel);

            sb.append(join.getType().toString()).append(" ");

            TableSource tableSource = join.getTableSource();

            if (tableSource.isSubquery()) {
                sb.append("(\n");
                formatQuery(tableSource.getSubquery(), sb, indentLevel + 1);
                sb.append("\n");
                appendIndent(sb, indentLevel);
                sb.append(")");
            } else {
                sb.append(tableSource.getTableName());
            }

            if (tableSource.hasAlias()) {
                sb.append(" AS ").append(tableSource.getAlias());
            }

            if (join.hasOnCondition()) {
                sb.append(" ON ");
                formatCondition(join.getOnCondition(), sb, indentLevel + 1);
            } else if (join.hasUsingColumns()) {
                sb.append(" USING (").append(join.getUsingColumns()).append(")");
            }
        }
    }

    /**
     * Formats a condition.
     *
     * @param condition The condition to format
     * @param sb The StringBuilder to append to
     * @param indentLevel The current indent level
     */
    private static void formatCondition(Condition condition, StringBuilder sb, int indentLevel) {
        if (condition.isNegated()) {
            sb.append("NOT ");
        }

        if (condition.hasNestedConditions()) {
            sb.append("(");

            List<Condition> nestedConditions = condition.getNestedConditions();
            for (int i = 0; i < nestedConditions.size(); i++) {
                if (i > 0) {
                    sb.append(" ");
                    sb.append(nestedConditions.get(i - 1).getLogicalOperator());
                    sb.append(" ");
                }

                formatCondition(nestedConditions.get(i), sb, indentLevel);
            }

            sb.append(")");
        } else {
            sb.append(condition.getLeftExpression())
              .append(" ")
              .append(condition.getOperator())
              .append(" ")
              .append(condition.getRightExpression());
        }

        if (condition.getNextCondition() != null) {
            sb.append("\n");
            appendIndent(sb, indentLevel - 1);
            sb.append(condition.getLogicalOperator()).append(" ");
            formatCondition(condition.getNextCondition(), sb, indentLevel);
        }
    }

    /**
     * Formats a list of GROUP BY columns.
     *
     * @param columns The GROUP BY columns to format
     * @param sb The StringBuilder to append to
     * @param indentLevel The current indent level
     */
    private static void formatGroupByColumns(List<Column> columns, StringBuilder sb, int indentLevel) {
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }

            sb.append(columns.get(i).getExpression());
        }
    }

    /**
     * Formats a list of ORDER BY columns.
     *
     * @param columns The ORDER BY columns to format
     * @param sb The StringBuilder to append to
     * @param indentLevel The current indent level
     */
    private static void formatOrderByColumns(List<OrderByColumn> columns, StringBuilder sb, int indentLevel) {
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }

            OrderByColumn column = columns.get(i);
            sb.append(column.getColumn());

            if (!column.isAscending()) {
                sb.append(" DESC");
            }
        }
    }

    /**
     * Appends the specified number of indents to the StringBuilder.
     *
     * @param sb The StringBuilder to append to
     * @param indentLevel The number of indents to append
     */
    private static void appendIndent(StringBuilder sb, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            sb.append(INDENT);
        }
    }
}
