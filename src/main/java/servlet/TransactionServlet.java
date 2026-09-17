package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

import bean.TransactionEntity;
import dao.TransactionDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet(name = "transactionServlet", value = "/transaction-servlet")
public class TransactionServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 获取查询参数
        String name = request.getParameter("name");
        String type = request.getParameter("type");
        String dateFrom = request.getParameter("dateFrom");
        String dateTo = request.getParameter("dateTo");
        String minAmountStr = request.getParameter("minAmount");
        String maxAmountStr = request.getParameter("maxAmount");

        // 处理金额参数
        BigDecimal minAmount = (minAmountStr != null && !minAmountStr.isEmpty())
                ? new BigDecimal(minAmountStr) : null;
        BigDecimal maxAmount = (maxAmountStr != null && !maxAmountStr.isEmpty())
                ? new BigDecimal(maxAmountStr) : null;

        // 处理分页参数
        int page = parseIntOrDefault(request.getParameter("page"), 1);
        int pageSize = parseIntOrDefault(request.getParameter("pageSize"), 5);
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 5;

        TransactionDao dao = new TransactionDao();
        int totalRecords = dao.getTransactionCount(name, type, dateFrom, dateTo, minAmount, maxAmount);
        int totalPages = totalRecords == 0 ? 0 : (int) Math.ceil((double) totalRecords / pageSize);
        if (page > totalPages && totalPages > 0) {
            page = totalPages;
        }
        int offset = (page - 1) * pageSize;

        List<TransactionEntity> transactions = dao.getTransaction(name, type, dateFrom, dateTo, minAmount, maxAmount, offset, pageSize);

        request.setAttribute("transactions", transactions);
        request.setAttribute("currentPage", page);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("totalRecords", totalRecords);
        request.setAttribute("totalPages", totalPages);
        request.getRequestDispatcher("/transactions.jsp").forward(request, response);
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");

        switch (action) {
            case "add":
                handleAdd(req);
                break;
            case "update":
                handleUpdate(req);
                break;
            case "delete":
                handleDelete(req);
                break;
            default:
                break;
        }
        // 重定向
        resp.sendRedirect(req.getContextPath() + "/transaction-servlet");
    }

    // 新增
    private void handleAdd(HttpServletRequest req) {
        TransactionEntity t = new TransactionEntity();
        t.setCustomerId(req.getParameter("customerId"));
        t.setName(req.getParameter("name"));
        t.setTransactionType(req.getParameter("transactionType"));
        t.setTransactionAmount(new BigDecimal(req.getParameter("transactionAmount")));
        t.setTransactionDatetime(Date.valueOf(req.getParameter("transactionDatetime")));
        TransactionDao dao = new TransactionDao();
        dao.insert(t);
    }

    // 编辑
    private void handleUpdate(HttpServletRequest req) {
        TransactionEntity t = new TransactionEntity();
        t.setId(Integer.parseInt(req.getParameter("id")));
        t.setCustomerId(req.getParameter("customerId"));
        t.setName(req.getParameter("name"));
        t.setTransactionType(req.getParameter("transactionType"));
        t.setTransactionAmount(new BigDecimal(req.getParameter("transactionAmount")));
        t.setTransactionDatetime(Date.valueOf(req.getParameter("transactionDatetime")));
        TransactionDao dao = new TransactionDao();
        dao.update(t);
    }

    // 删除
    private void handleDelete(HttpServletRequest req) {
        int id = Integer.parseInt(req.getParameter("id"));
        TransactionDao dao = new TransactionDao();
        dao.deleteByCustomerId(id);
    }

    public void destroy() {
    }
}