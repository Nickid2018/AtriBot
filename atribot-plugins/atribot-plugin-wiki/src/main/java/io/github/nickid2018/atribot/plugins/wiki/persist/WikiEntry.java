package io.github.nickid2018.atribot.plugins.wiki.persist;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "wiki_entries")
public class WikiEntry {

    @DatabaseField(columnName = "group", dataType = DataType.STRING, canBeNull = false, index = true)
    public String group;
    @DatabaseField(columnName = "wiki_key", dataType = DataType.STRING, canBeNull = false)
    public String wikiPrefix;
    @DatabaseField(columnName = "wiki_url", dataType = DataType.STRING, canBeNull = false)
    public String baseURL;

    @DatabaseField(columnName = "wiki_render_width", dataType = DataType.INTEGER, defaultValue = "0")
    public int renderWidth;
    @DatabaseField(columnName = "wiki_render_height", dataType = DataType.INTEGER, defaultValue = "0")
    public int renderHeight;
    @DatabaseField(columnName = "wiki_render_trust", dataType = DataType.BOOLEAN, defaultValue = "false")
    public boolean trustRender;
}
