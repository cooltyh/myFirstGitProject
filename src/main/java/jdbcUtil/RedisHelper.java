package jdbcUtil;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RedisHelper {
    private static final Logger LOGGER = Logger.getLogger(RedisHelper.class.getName());

    // Redis 连接配置（本机 WSL2 里的 Redis，无密码）
    private static final String HOST = "localhost";
    private static final int PORT = 6379;

    private static final JedisPool POOL;

    static {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(8);
        config.setMaxIdle(8);
        config.setMinIdle(0);
        POOL = new JedisPool(config, HOST, PORT);
    }

    /**
     * 读缓存，Redis 不可用时返回 null（视为未命中），不影响业务
     */
    public static String get(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis 读取失败，降级为未命中: " + key, e);
            return null;
        }
    }

    /**
     * 写缓存并设置过期时间（秒）
     */
    public static void setex(String key, int seconds, String value) {
        try (Jedis jedis = POOL.getResource()) {
            jedis.setex(key, seconds, value);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis 写入失败: " + key, e);
        }
    }

    /**
     * 写入键值（无过期时间）
     */
    public static void set(String key, String value) {
        try (Jedis jedis = POOL.getResource()) {
            jedis.set(key, value);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis 写入失败: " + key, e);
        }
    }

    /**
     * 删除单个键
     */
    public static void del(String key) {
        try (Jedis jedis = POOL.getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis 删除失败: " + key, e);
        }
    }

    /**
     * 自增 1（用于库存回补）
     */
    public static long incr(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.incr(key);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis incr 失败: " + key, e);
            return -1;
        }
    }

    /**
     * Lua 原子扣减：库存 > 0 才减 1，返回 1=成功 / 0=已售罄 / -1=异常
     */
    public static long decrStockByLua(String key) {
        String lua = "local s = tonumber(redis.call('get', KEYS[1])) "
                + "if s == nil or s <= 0 then return 0 end "
                + "redis.call('decr', KEYS[1]) return 1";
        try (Jedis jedis = POOL.getResource()) {
            Object result = jedis.eval(lua, Collections.singletonList(key), Collections.emptyList());
            return ((Number) result).longValue();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis Lua 扣减失败: " + key, e);
            return -1;
        }
    }

    /**
     * 按前缀删除缓存键（用于写操作后失效）
     */
    public static void delByPattern(String pattern) {
        try (Jedis jedis = POOL.getResource()) {
            Set<String> keys = jedis.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis 清除缓存失败: " + pattern, e);
        }
    }
}
