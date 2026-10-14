package dao;

import bean.TransactionEntity;
import jdbcUtil.DbHelper;
import jdbcUtil.RedisHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDao {
    // 缓存过期时间（秒）
    private static final int CACHE_TTL_SECONDS = 60;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(java.sql.Date.class, new JsonSerializer<java.sql.Date>() {
                @Override
                public JsonElement serialize(java.sql.Date src, Type typeOfSrc, JsonSerializationContext context) {
                    return new JsonPrimitive(src.toString());
                }
            })
            .registerTypeAdapter(java.sql.Date.class, new JsonDeserializer<java.sql.Date>() {
                @Override
                public java.sql.Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                    return java.sql.Date.valueOf(json.getAsString());
                }
            })
            .create();

    private String listKey(String name, String type, String dateFrom, String dateTo,
                           BigDecimal minAmount, BigDecimal maxAmount, int offset, int limit) {
        return "tx:list|" + name + "|" + type + "|" + dateFrom + "|" + dateTo
                + "|" + minAmount + "|" + maxAmount + "|" + offset + "|" + limit;
    }

    private String countKey(String name, String type, String dateFrom, String dateTo,
                            BigDecimal minAmount, BigDecimal maxAmount) {
        return "tx:count|" + name + "|" + type + "|" + dateFrom + "|" + dateTo
                + "|" + minAmount + "|" + maxAmount;
    }

    public List<TransactionEntity> getTransaction(String name, String type,
                                          String dateFrom, String dateTo, BigDecimal minAmount, BigDecimal maxAmount,
                                          int offset, int limit) {
        String cacheKey = listKey(name, type, dateFrom, dateTo, minAmount, maxAmount, offset, limit);
        String cached = RedisHelper.get(cacheKey);
        if (cached != null) {
            return GSON.fromJson(cached, new TypeToken<List<TransactionEntity>>() {}.getType());
        }

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
                RedisHelper.setex(cacheKey, CACHE_TTL_SECONDS, GSON.toJson(list));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 查询符合条件的总记录数（用于分页）
    public int getTransactionCount(String name, String type,
                                   String dateFrom, String dateTo, BigDecimal minAmount, BigDecimal maxAmount) {
        String cacheKey = countKey(name, type, dateFrom, dateTo, minAmount, maxAmount);
        String cached = RedisHelper.get(cacheKey);
        if (cached != null) {
            return Integer.parseInt(cached);
        }

        QueryCondition condition = buildWhere(name, type, dateFrom, dateTo, minAmount, maxAmount);
        String sql = "SELECT COUNT(*) FROM transactions" + condition.where;
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < condition.params.size(); i++) {
                ps.setObject(i + 1, condition.params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                int count = rs.next() ? rs.getInt(1) : 0;
                RedisHelper.setex(cacheKey, CACHE_TTL_SECONDS, String.valueOf(count));
                return count;
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
            RedisHelper.delByPattern("tx:*");
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
            RedisHelper.delByPattern("tx:*");
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
            RedisHelper.delByPattern("tx:*");
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
