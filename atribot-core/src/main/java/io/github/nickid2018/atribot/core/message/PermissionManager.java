package io.github.nickid2018.atribot.core.message;

import com.j256.ormlite.dao.Dao;
import io.github.nickid2018.atribot.core.message.persist.PermissionEntry;
import io.github.nickid2018.atribot.network.message.TargetData;
import lombok.SneakyThrows;

import java.sql.SQLException;

public class PermissionManager {

    private final Dao<PermissionEntry, String> permissionDao;

    public PermissionManager(MessageManager messageManager) throws SQLException {
        permissionDao = messageManager.getMessageDatabase().getTable(PermissionEntry.class);
    }

    public boolean checkTargetData(TargetData target, PermissionLevel permission) {
        return target != null && target.isUserSpecified() && hasPermission(target.getTargetUser(), permission);
    }

    @SneakyThrows
    public boolean hasPermission(String user, PermissionLevel permission) {
        PermissionEntry entry = permissionDao.queryForId(user);
        if (entry != null && entry.timestamp < System.currentTimeMillis()) {
            permissionDao.delete(entry);
            entry = null;
        }
        PermissionLevel level = entry == null ? PermissionLevel.USER : entry.level;
        return permission.ordinal() >= level.ordinal();
    }

    @SneakyThrows
    public void setPermission(String user, PermissionLevel permission) {
        PermissionEntry entry = new PermissionEntry();
        entry.id = user;
        entry.level = permission;
        entry.timestamp = Long.MAX_VALUE;
        permissionDao.createOrUpdate(entry);
    }

    @SneakyThrows
    public void setPermission(String user, PermissionLevel permission, long time) {
        PermissionEntry entry = new PermissionEntry();
        entry.id = user;
        entry.level = permission;
        entry.timestamp = System.currentTimeMillis() + time;
        permissionDao.createOrUpdate(entry);
    }
}
