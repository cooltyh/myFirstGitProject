package dao;

import jdbcUtil.RedisHelper;

public class NameDao {


    public static void main(String[] args) {
        String RedisName = getName();
        System.out.println(RedisName);
    }

    /**
     * 查询 Redis 中 name 键的值
     *
     * @return name 对应的值；键不存在或 Redis 不可用时返回 null
     */
    public static String getName() {
        return RedisHelper.get("name");
    }

    /**
     * 修改 Redis 中 name 键的值
     *
     * @param value 新的值
     */
    public static void setName(String value) {
        RedisHelper.set("name", value);
    }

    /**
     * 删除 Redis 中 name 键
     */
    public static void deleteName() {
        RedisHelper.del("name");
    }
}
