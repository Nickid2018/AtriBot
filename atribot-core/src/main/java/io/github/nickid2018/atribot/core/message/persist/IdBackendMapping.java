package io.github.nickid2018.atribot.core.message.persist;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@DatabaseTable(tableName = "id_backend_mapping")
public class IdBackendMapping {

    @DatabaseField(id = true, columnName = "id", dataType = DataType.STRING, canBeNull = false, unique = true, index = true)
    private String id;
    @DatabaseField(columnName = "backend", dataType = DataType.STRING, canBeNull = false)
    private String backend;
}
