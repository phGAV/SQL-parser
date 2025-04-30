package sqlparser.model;

import java.util.Objects;

/**
 * Represents a JOIN clause in a SQL query.
 */
public class Join {
    private TableSource tableSource;  // Table being joined
    private JoinType type;            // Type of join (INNER, LEFT, RIGHT, FULL, CROSS, IMPLICIT)
    private Condition onCondition;    // ON condition (can be null for CROSS joins)
    private String usingColumns;      // USING columns (alternative to ON condition)

    public Join(TableSource tableSource, JoinType type) {
        this.tableSource = tableSource;
        this.type = type;
    }

    public Join(TableSource tableSource, JoinType type, Condition onCondition) {
        this.tableSource = tableSource;
        this.type = type;
        this.onCondition = onCondition;
    }

    public TableSource getTableSource() {
        return tableSource;
    }

    public void setTableSource(TableSource tableSource) {
        this.tableSource = tableSource;
    }

    public JoinType getType() {
        return type;
    }

    public void setType(JoinType type) {
        this.type = type;
    }

    public Condition getOnCondition() {
        return onCondition;
    }

    public void setOnCondition(Condition onCondition) {
        this.onCondition = onCondition;
    }

    public String getUsingColumns() {
        return usingColumns;
    }

    public void setUsingColumns(String usingColumns) {
        this.usingColumns = usingColumns;
    }

    public boolean hasOnCondition() {
        return onCondition != null;
    }

    public boolean hasUsingColumns() {
        return usingColumns != null && !usingColumns.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(type.toString()).append(" ").append(tableSource);

        if (hasOnCondition()) {
            sb.append(" ON ").append(onCondition);
        } else if (hasUsingColumns()) {
            sb.append(" USING (").append(usingColumns).append(")");
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Join join = (Join) o;
        return Objects.equals(tableSource, join.tableSource) &&
               type == join.type &&
               Objects.equals(onCondition, join.onCondition) &&
               Objects.equals(usingColumns, join.usingColumns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableSource, type, onCondition, usingColumns);
    }
}
