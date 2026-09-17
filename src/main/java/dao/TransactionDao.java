package dao;

import bean.TransactionEntity;
import jdbcUtil.DbHelper;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDao {
    public List<TransactionEntity> getTransaction(String name, String type,
                                          String dateFrom, String dateTo, BigDecimal minAmount, BigDecimal maxAmount,
                                          int offset, int limit) {
        QueryCondition condition = buildWhere(name, type, dateFrom, dateTo, minAmount, maxAmount);
        String sql = "SELECT * FROM transactions" + condition.where + " ORDER BY datetime DESC LIMIT ? OFFSET ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < condition.params.size(); i++) {
                ps.setObject(i + 1, condition.params.get(i));
            }
            ps.setInt(condition.params.size() + 1, limit);
            ps.setInt(condition.params.size() + 2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<TransactionEntity> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 查询符合条件的总记录数（用于分页）
    public int getTransactionCount(String name, String type,
                                   String dateFrom, String dateTo, BigDecimal minAmount, BigDecimal maxAmount) {
        QueryCondition condition = buildWhere(name, type, dateFrom, dateTo, minAmount, maxAmount);
        String sql = "SELECT COUNT(*) FROM transactions" + condition.where;
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < condition.params.size(); i++) {
                ps.setObject(i + 1, condition.params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 动态拼接查询条件（查询与计数共用）
    private QueryCondition buildWhere(String name, String type,
                                      String dateFrom, String dateTo, BigDecimal minAmount, BigDecimal maxAmount) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (name != null && !name.trim().isEmpty()) {
            where.append(" AND name LIKE ?");
            params.add("%" + name.trim() + "%");
        }
        if (type != null && !type.isEmpty()) {
            where.append(" AND transaction_type = ?");
            params.add(type);
        }
        if (dateFrom != null && !dateFrom.isEmpty()) {
            where.append(" AND datetime >= ?");
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            where.append(" AND datetime <= ?");
            params.add(dateTo);
        }
        if (minAmount != null) {
            where.append(" AND transaction_amount >= ?");
            params.add(minAmount);
        }
        if (maxAmount != null) {
            where.append(" AND transaction_amount <= ?");
            params.add(maxAmount);
        }
        return new QueryCondition(where.toString(), params);
    }

    // 查询条件载体：where 片段 + 对应的参数列表
    private static class QueryCondition {
        final String where;
        final List<Object> params;

        QueryCondition(String where, List<Object> params) {
            this.where = where;
            this.params = params;
        }
    }
    // 新增
    public void insert(TransactionEntity t) {
        String sql = "INSERT INTO transactions (customer_id, name, transaction_type, transaction_amount, datetime)" +
                " VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getCustomerId());
            ps.setString(2, t.getName());
            ps.setString(3, t.getTransactionType());
            ps.setBigDecimal(4, t.getTransactionAmount());
            ps.setDate(5, t.getTransactionDatetime());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("新增交易记录失败", e);
        }
    }

    // 修改
    public void update(TransactionEntity t) {
        String sql = "UPDATE transactions SET name=?, customer_id=?, transaction_type=?, transaction_amount=?, datetime=? " +
                "WHERE id=?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getCustomerId());
            ps.setString(3, t.getTransactionType());
            ps.setBigDecimal(4, t.getTransactionAmount());
            ps.setDate(5, t.getTransactionDatetime());
            ps.setInt(6, t.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新交易记录失败", e);
        }
    }

    // 删除
    public void deleteByCustomerId(int id) {
        String sql = "DELETE FROM transactions WHERE id=?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("删除交易记录失败", e);
        }
    }

    // 对应字段映射关系
    public TransactionEntity mapRow(ResultSet rs) throws SQLException {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(rs.getInt("id"));
        entity.setCustomerId(rs.getString("customer_id"));
        entity.setName(rs.getString("name"));
        entity.setTransactionType(rs.getString("transaction_type"));
        entity.setTransactionDatetime(new Date(rs.getTimestamp("datetime").getTime()));
        entity.setTransactionAmount(rs.getBigDecimal("transaction_amount"));
        return entity;
    }
}
