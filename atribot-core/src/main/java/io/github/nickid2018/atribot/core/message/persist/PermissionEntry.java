package io.github.nickid2018.atribot.core.message.persist;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import io.github.nickid2018.atribot.core.message.PermissionLevel;

@DatabaseTable(tableName = "permissions")
public class PermissionEntry {

    @DatabaseField(id = true, columnName = "id", dataType = DataType.STRING, canBeNull = false, unique = true, index = true)
    public String id;
    @DatabaseField(columnName = "level", dataType = DataType.ENUM_STRING, canBeNull = false)
    public PermissionLevel level;
    @DatabaseField(columnName = "timestamp", dataType = DataType.LONG, canBeNull = false)
    public long timestamp;
}
