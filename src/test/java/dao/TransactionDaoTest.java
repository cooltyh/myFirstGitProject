package dao;

import bean.TransactionEntity;
import jdbcUtil.DbHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TransactionDaoTest {

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void getTransactionShouldAppendLimitOffsetAndMapRows() throws Exception {
        try (MockedStatic<DbHelper> db = mockStatic(DbHelper.class)) {
            db.when(() -> DbHelper.getConnection()).thenReturn(connection);

            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getInt("id")).thenReturn(1);
            when(resultSet.getString("customer_id")).thenReturn("C001");
            when(resultSet.getString("name")).thenReturn("张三");
            when(resultSet.getString("transaction_type")).thenReturn("Cash");
            when(resultSet.getTimestamp("datetime")).thenReturn(Timestamp.valueOf("2025-01-01 10:00:00"));
            when(resultSet.getBigDecimal("transaction_amount")).thenReturn(new BigDecimal("100.00"));

            List<TransactionEntity> list = new TransactionDao()
                    .getTransaction(null, null, null, null, null, null, 0, 10);

            assertEquals(1, list.size());
            assertEquals("C001", list.get(0).getCustomerId());

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(connection).prepareStatement(sqlCaptor.capture());
            assertTrue(sqlCaptor.getValue().contains("ORDER BY datetime DESC LIMIT ? OFFSET ?"));

            // 无过滤条件时，第 1、2 个参数分别为 limit、offset
            verify(preparedStatement).setInt(1, 10);
            verify(preparedStatement).setInt(2, 0);
        }
    }

    @Test
    void getTransactionShouldBindFilterParamsBeforeLimitAndOffset() throws Exception {
        try (MockedStatic<DbHelper> db = mockStatic(DbHelper.class)) {
            db.when(() -> DbHelper.getConnection()).thenReturn(connection);
            when(resultSet.next()).thenReturn(false);

            new TransactionDao().getTransaction("张三", "Cash", "2025-01-01", "2025-01-31",
                    new BigDecimal("10"), new BigDecimal("100"), 10, 5);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(connection).prepareStatement(sqlCaptor.capture());
            assertEquals("SELECT * FROM transactions WHERE 1=1 AND name LIKE ? AND transaction_type = ? "
                    + "AND datetime >= ? AND datetime <= ? AND transaction_amount >= ? AND transaction_amount <= ? "
                    + "ORDER BY datetime DESC LIMIT ? OFFSET ?", sqlCaptor.getValue());

            verify(preparedStatement).setObject(1, "%张三%");
            verify(preparedStatement).setObject(2, "Cash");
            verify(preparedStatement).setObject(3, "2025-01-01");
            verify(preparedStatement).setObject(4, "2025-01-31");
            verify(preparedStatement).setObject(5, new BigDecimal("10"));
            verify(preparedStatement).setObject(6, new BigDecimal("100"));
            verify(preparedStatement).setInt(7, 5);   // limit
            verify(preparedStatement).setInt(8, 10);  // offset
        }
    }

    @Test
    void getTransactionCountShouldReturnTotalRows() throws Exception {
        try (MockedStatic<DbHelper> db = mockStatic(DbHelper.class)) {
            db.when(() -> DbHelper.getConnection()).thenReturn(connection);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt(1)).thenReturn(25);

            int count = new TransactionDao().getTransactionCount(null, null, null, null, null, null);

            assertEquals(25, count);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(connection).prepareStatement(sqlCaptor.capture());
            assertTrue(sqlCaptor.getValue().startsWith("SELECT COUNT(*) FROM transactions"));
        }
    }
}
