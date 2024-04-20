package io.github.nickid2018.atribot.plugins.wiki.persist;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "start_wiki")
public class StartWikiEntry {

    @DatabaseField(id = true, columnName = "group", dataType = DataType.STRING, canBeNull = false, unique = true, index = true)
    public String group;
    @DatabaseField(columnName = "wiki_key", dataType = DataType.STRING, canBeNull = false)
    public String wikiKey;
}
