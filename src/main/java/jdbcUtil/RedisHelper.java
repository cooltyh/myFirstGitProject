package jdbcUtil;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

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
