package servlet;

import dao.TransactionDao;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionServletTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/transactions.jsp")).thenReturn(dispatcher);
    }

    @Test
    void doGetShouldComputePaginationAndSetAttributes() throws Exception {
        when(request.getParameter("page")).thenReturn("2");
        when(request.getParameter("pageSize")).thenReturn("10");

        try (MockedConstruction<TransactionDao> mocked = mockConstruction(TransactionDao.class, (mock, ctx) -> {
            when(mock.getTransactionCount(any(), any(), any(), any(), any(), any())).thenReturn(25);
            when(mock.getTransaction(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
        })) {
            new TransactionServlet().doGet(request, response);

            TransactionDao dao = mocked.constructed().get(0);
            // offset = (2-1)*10 = 10
            verify(dao).getTransaction(any(), any(), any(), any(), any(), any(), eq(10), eq(10));

            verify(request).setAttribute("currentPage", 2);
            verify(request).setAttribute("pageSize", 10);
            verify(request).setAttribute("totalRecords", 25);
            verify(request).setAttribute("totalPages", 3);
            verify(dispatcher).forward(request, response);
        }
    }

    @Test
    void doGetShouldDefaultToFirstPageWhenParamsMissing() throws Exception {
        try (MockedConstruction<TransactionDao> mocked = mockConstruction(TransactionDao.class, (mock, ctx) -> {
            when(mock.getTransactionCount(any(), any(), any(), any(), any(), any())).thenReturn(0);
            when(mock.getTransaction(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
        })) {
            new TransactionServlet().doGet(request, response);

            TransactionDao dao = mocked.constructed().get(0);
            verify(dao).getTransaction(any(), any(), any(), any(), any(), any(), eq(0), eq(5));

            verify(request).setAttribute("currentPage", 1);
            verify(request).setAttribute("totalPages", 0);
        }
    }

    @Test
    void doGetShouldClampPageToLastPageWhenOutOfRange() throws Exception {
        when(request.getParameter("page")).thenReturn("99");

        try (MockedConstruction<TransactionDao> mocked = mockConstruction(TransactionDao.class, (mock, ctx) -> {
            when(mock.getTransactionCount(any(), any(), any(), any(), any(), any())).thenReturn(25);
            when(mock.getTransaction(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
        })) {
            new TransactionServlet().doGet(request, response);

            TransactionDao dao = mocked.constructed().get(0);
            // totalPages = 5，page 被钳制到 5，offset = (5-1)*5 = 20
            verify(dao).getTransaction(any(), any(), any(), any(), any(), any(), eq(20), eq(5));

            verify(request).setAttribute("currentPage", 5);
            verify(request).setAttribute("totalPages", 5);
        }
    }
}
