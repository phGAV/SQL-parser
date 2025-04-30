package sqlparser.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

//class Query {
//    private List<String> columns;
//    private List<Source> fromSources;
//    private List<Join> joins;
//    private List<WhereClause> whereClauses;
//    private List<String> groupByColumns;
//    private List<Sort> sortColumns;
//    private Integer limit;
//    private Integer offset;
//}

/**
 * Represents a SQL SELECT query with all its components.
 */
public class Query {
    private List<Column> columns = new ArrayList<>();           // SELECT columns
    private List<TableSource> fromSources = new ArrayList<>();  // FROM tables/subqueries
    private List<Join> joins = new ArrayList<>();               // JOIN clauses
    private Condition whereCondition;                           // WHERE clause
    private List<Column> groupByColumns = new ArrayList<>();    // GROUP BY columns
    private Condition havingCondition;                          // HAVING clause
    private List<OrderByColumn> orderByColumns = new ArrayList<>(); // ORDER BY columns
    private Integer limit;                                      // LIMIT clause
    private Integer offset;                                     // OFFSET clause
    private boolean isDistinct = false;                         // Whether SELECT DISTINCT is used

    public Query() {
    }

    // Getters and setters
    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public void addColumn(Column column) {
        this.columns.add(column);
    }

    public List<TableSource> getFromSources() {
        return fromSources;
    }

    public void setFromSources(List<TableSource> fromSources) {
        this.fromSources = fromSources;
    }

    public void addFromSource(TableSource tableSource) {
        this.fromSources.add(tableSource);
    }

    public List<Join> getJoins() {
        return joins;
    }

    public void setJoins(List<Join> joins) {
        this.joins = joins;
    }

    public void addJoin(Join join) {
        this.joins.add(join);
    }

    public Condition getWhereCondition() {
        return whereCondition;
    }

    public void setWhereCondition(Condition whereCondition) {
        this.whereCondition = whereCondition;
    }

    public List<Column> getGroupByColumns() {
        return groupByColumns;
    }

    public void setGroupByColumns(List<Column> groupByColumns) {
        this.groupByColumns = groupByColumns;
    }

    public void addGroupByColumn(Column column) {
        this.groupByColumns.add(column);
    }

    public Condition getHavingCondition() {
        return havingCondition;
    }

    public void setHavingCondition(Condition havingCondition) {
        this.havingCondition = havingCondition;
    }

    public List<OrderByColumn> getOrderByColumns() {
        return orderByColumns;
    }

    public void setOrderByColumns(List<OrderByColumn> orderByColumns) {
        this.orderByColumns = orderByColumns;
    }

    public void addOrderByColumn(OrderByColumn orderByColumn) {
        this.orderByColumns.add(orderByColumn);
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public boolean isDistinct() {
        return isDistinct;
    }

    public void setDistinct(boolean distinct) {
        isDistinct = distinct;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SELECT ");

        if (isDistinct) {
            sb.append("DISTINCT ");
        }

        // Columns
        if (columns.isEmpty()) {
            sb.append("*");
        } else {
            for (int i = 0; i < columns.size(); i++) {
                sb.append(columns.get(i));
                if (i < columns.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        // FROM clause
        if (!fromSources.isEmpty()) {
            sb.append(" FROM ");
            for (int i = 0; i < fromSources.size(); i++) {
                sb.append(fromSources.get(i));
                if (i < fromSources.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        // JOIN clauses
        for (Join join : joins) {
            sb.append(" ").append(join);
        }

        // WHERE clause
        if (whereCondition != null) {
            sb.append(" WHERE ").append(whereCondition);
        }

        // GROUP BY clause
        if (!groupByColumns.isEmpty()) {
            sb.append(" GROUP BY ");
            for (int i = 0; i < groupByColumns.size(); i++) {
                sb.append(groupByColumns.get(i).getExpression());
                if (i < groupByColumns.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        // HAVING clause
        if (havingCondition != null) {
            sb.append(" HAVING ").append(havingCondition);
        }

        // ORDER BY clause
        if (!orderByColumns.isEmpty()) {
            sb.append(" ORDER BY ");
            for (int i = 0; i < orderByColumns.size(); i++) {
                sb.append(orderByColumns.get(i));
                if (i < orderByColumns.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        // LIMIT and OFFSET clauses
        if (limit != null) {
            sb.append(" LIMIT ").append(limit);
        }

        if (offset != null) {
            sb.append(" OFFSET ").append(offset);
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Query query = (Query) o;
        return isDistinct == query.isDistinct &&
               Objects.equals(columns, query.columns) &&
               Objects.equals(fromSources, query.fromSources) &&
               Objects.equals(joins, query.joins) &&
               Objects.equals(whereCondition, query.whereCondition) &&
               Objects.equals(groupByColumns, query.groupByColumns) &&
               Objects.equals(havingCondition, query.havingCondition) &&
               Objects.equals(orderByColumns, query.orderByColumns) &&
               Objects.equals(limit, query.limit) &&
               Objects.equals(offset, query.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columns, fromSources, joins, whereCondition,
                            groupByColumns, havingCondition, orderByColumns,
                            limit, offset, isDistinct);
    }
}