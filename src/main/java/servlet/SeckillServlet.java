package servlet;

import com.google.gson.Gson;
import dao.SeckillDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(name = "seckillServlet", value = "/seckill")
public class SeckillServlet extends HttpServlet {

    private static final Gson GSON = new Gson();
    private final SeckillDao seckillDao = new SeckillDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("productId", SeckillDao.PRODUCT_ID);
        request.setAttribute("stock", seckillDao.getStock(SeckillDao.PRODUCT_ID));
        request.getRequestDispatcher("/seckill.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        Map<String, Object> result = new LinkedHashMap<>();

        switch (action == null ? "" : action) {
            case "init": {
                int stock = parseIntOrDefault(request.getParameter("stock"), 100);
                seckillDao.initStock(SeckillDao.PRODUCT_ID, stock);
                result.put("success", true);
                result.put("stock", stock);
                result.put("soldOut", false);
                break;
            }
            case "seckill": {
                boolean ok = seckillDao.seckill(SeckillDao.PRODUCT_ID);
                int remain = seckillDao.getStock(SeckillDao.PRODUCT_ID);
                result.put("success", ok);
                result.put("stock", remain);
                result.put("soldOut", remain <= 0);
                break;
            }
            case "replenish": {
                int n = parseIntOrDefault(request.getParameter("stock"), 0);
                seckillDao.replenish(SeckillDao.PRODUCT_ID, n);
                result.put("success", true);
                result.put("stock", seckillDao.getStock(SeckillDao.PRODUCT_ID));
                result.put("soldOut", false);
                break;
            }
            default: {
                result.put("success", false);
                result.put("message", "unknown action");
                break;
            }
        }

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(GSON.toJson(result));
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
}
