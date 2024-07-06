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

    public boolean hasPermission(String user, PermissionLevel permission) {
        return permission.ordinal() >= getPermissionLevel(user).ordinal();
    }

    @SneakyThrows
    public PermissionLevel getPermissionLevel(String user) {
        PermissionEntry entry = permissionDao.queryForId(user);
        entry = checkExpired(entry);
        return entry == null ? PermissionLevel.USER : entry.level;
    }

    @SneakyThrows
    public long getPermissionTimestamp(String user) {
        PermissionEntry entry = permissionDao.queryForId(user);
        entry = checkExpired(entry);
        return entry == null ? Long.MAX_VALUE : entry.timestamp;
    }

    @SneakyThrows
    public void clearPermission(String user) {
        permissionDao.deleteById(user);
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

    @SneakyThrows
    private PermissionEntry checkExpired(PermissionEntry entry) {
        if (entry == null)
            return null;
        long time = System.currentTimeMillis();
        if (entry.timestamp < time) {
            entry.level = PermissionLevel.USER;
            entry.timestamp = Long.MAX_VALUE;
            permissionDao.delete(entry);
            return entry;
        }
        return entry;
    }
}
