package dao;

import jdbcUtil.RedisHelper;

public class SeckillDao {

    // 秒杀商品 id（示例固定一个）
    public static final int PRODUCT_ID = 1001;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    private String stockKey(int productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    /**
     * 初始化（预热）库存
     */
    public void initStock(int productId, int stock) {
        RedisHelper.set(stockKey(productId), String.valueOf(stock));
    }

    /**
     * 查询当前剩余库存，无则返回 0
     */
    public int getStock(int productId) {
        String v = RedisHelper.get(stockKey(productId));
        if (v == null) {
            return 0;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 抢购一次：库存 > 0 才扣减成功（Redis Lua 原子操作，防超卖）
     *
     * @return true=抢到，false=已售罄
     */
    public boolean seckill(int productId) {
        return RedisHelper.decrStockByLua(stockKey(productId)) == 1;
    }

    /**
     * 回补库存（超时未支付等场景）
     */
    public void replenish(int productId, int n) {
        for (int i = 0; i < n; i++) {
            RedisHelper.incr(stockKey(productId));
        }
    }
}
