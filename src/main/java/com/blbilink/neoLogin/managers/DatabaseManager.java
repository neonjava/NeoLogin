package com.blbilink.neoLogin.managers;

import com.blbilink.neoLibrary.utils.DatabaseUtil;
import com.blbilink.neoLogin.NeoLogin;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private final NeoLogin plugin;
    private final ConfigManager configManager;
    private DatabaseUtil databaseUtil;

    public DatabaseManager(NeoLogin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    /**
     * 初始化数据库连接并创建所需的数据表。
     */
    public void init() {
        ConfigurationSection dbConfig = configManager.getDatabaseSection();
        if (dbConfig == null) {
            plugin.getLogger().severe("Could not find 'database' section in config.yml! Database features will not work.");
            return;
        }

        String dbType = dbConfig.getString("type", "sqlite");
        plugin.getLogger().info("Initializing database, type: " + dbType + "...");

        this.databaseUtil = new DatabaseUtil(plugin);

        // 修正 1: initialize() 返回布尔值，不抛出异常
        boolean success = databaseUtil.initialize(dbConfig);

        if (success) {
            plugin.getLogger().info("Database connection pool initialized successfully!");
            // 初始化成功后，创建数据表
            setupTables();
        } else {
            plugin.getLogger().severe("Database initialization failed! Please check your config.yml settings and database driver.");
        }
    }

    /**
     * 创建插件所需的用户数据表。
     */
    private void setupTables() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS neologin_users ("
                + "uuid VARCHAR(36) NOT NULL PRIMARY KEY,"
                + "username VARCHAR(16) NOT NULL,"
                + "password_hash VARCHAR(255) NOT NULL,"
                + "ip_address VARCHAR(45),"
                + "registration_date BIGINT NOT NULL,"
                + "last_login_date BIGINT"
                + ");";
        try {
            // 使用 executeUpdate 方法
            databaseUtil.executeUpdate(createTableSQL);
            plugin.getLogger().info("Table structure verified/created.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating tables!");
            e.printStackTrace();
        }
    }

    /**
     * 关闭数据库连接池。
     * 应在插件卸载时调用。
     */
    public void close() {
        if (databaseUtil != null) {
            // 修正 3: 使用 close 方法
            databaseUtil.close();
        }
    }

    /**
     * 从连接池获取一个数据库连接。
     * <p>
     * <b>重要提示：</b> 推荐使用 try-with-resources 语句来确保连接被正确关闭和归还。
     * </p>
     *
     * @return 数据库连接对象, 如果发生错误则返回 null
     */
    public Connection getConnection() {
        // 增加 isInitialized() 判断，更加健壮
        if (databaseUtil == null || !databaseUtil.isInitialized()) {
            plugin.getLogger().severe("Database not initialized or closed, cannot obtain connection!");
            return null;
        }
        try {
            return databaseUtil.getConnection();
        } catch (SQLException e) {
            plugin.getLogger().severe("Unable to get database connection from connection pool!");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取 DatabaseUtil 的实例，方便在其他地方直接调用其高级功能（如异步方法）。
     *
     * @return DatabaseUtil 实例，如果未初始化则可能为 null
     */
    public DatabaseUtil getDatabaseUtil() {
        return databaseUtil;
    }
}