package io.github.nickid2018.atribot.core.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.File;
import java.sql.SQLException;

public class DatabaseManager {

    static {
        DatabaseLoggerBackend.init();
    }

    private final ConnectionSource source;

    public DatabaseManager(String database) throws SQLException {
        File file = new File(database);
        if (!file.exists() && !file.getParentFile().exists())
            file.getParentFile().mkdirs();
        source = new JdbcConnectionSource("jdbc:sqlite:" + database);
    }

    public <T, ID> Dao<T, ID> getTable(Class<T> clazz) throws SQLException {
        TableUtils.createTableIfNotExists(source, clazz);
        return DaoManager.createDao(source, clazz);
    }

    public void close() throws Exception {
        source.close();
    }
}
