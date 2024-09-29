package com.me.LootSplit.utils;

import com.me.LootSplit.database.DatabaseManager;

import java.sql.SQLException;

public class LootSplitSession {

    private final String id;
    private final long guildId;
    private final String name;

    public LootSplitSession(String lootSplitName, long guildId) {
        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        this.id = generateId(ALPHA_NUMERIC_STRING);
        this.name = lootSplitName;
        this.guildId = guildId;
    }

    public static String generateId(String ALPHA_NUMERIC_STRING) {
        StringBuilder builder = new StringBuilder();
        int count = 7;
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    public String getId() {
        return id;
    }

    public static String getLootSplitIdFromGuild(long guildId) throws SQLException {
        DatabaseManager databaseManager = new DatabaseManager();
        return databaseManager.getLootSplitId(guildId);
    }
}
