package io.github.nickid2018.atribot.plugins.wiki;

import com.j256.ormlite.dao.Dao;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.database.DatabaseManager;
import io.github.nickid2018.atribot.core.plugin.AbstractAtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;
import io.github.nickid2018.atribot.plugins.wiki.persist.StartWikiEntry;
import io.github.nickid2018.atribot.plugins.wiki.persist.WikiEntry;
import io.github.nickid2018.atribot.plugins.wiki.resolve.InterwikiStorage;
import io.github.nickid2018.atribot.util.Configuration;

public class WikiPlugin extends AbstractAtriBotPlugin {

    public DatabaseManager databaseManager;
    public Dao<StartWikiEntry, String> startWikis;
    public Dao<WikiEntry, Object> wikiEntries;

    private WikiResolver resolver;

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            "atribot-plugin-wiki",
            "Wiki",
            "1.0",
            "Nickid2018",
            "A plugin to search wiki pages"
        );
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return resolver;
    }

    @Override
    public void onPluginPreload() throws Exception {
        super.onPluginPreload();
        databaseManager = new DatabaseManager(Configuration.getStringOrElse(
            "database.wiki",
            "database/wiki.db"
        ));
        startWikis = databaseManager.getTable(StartWikiEntry.class);
        wikiEntries = databaseManager.getTable(WikiEntry.class);
        resolver = new WikiResolver(this, new InterwikiStorage(this));
    }

    @Override
    public void onPluginUnload() throws Exception {
        databaseManager.close();
        super.onPluginUnload();
    }
}
