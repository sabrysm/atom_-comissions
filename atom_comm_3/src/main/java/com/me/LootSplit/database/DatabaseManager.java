package com.me.LootSplit.database;

import com.me.LootSplit.utils.CTA;
import org.sqlite.SQLiteConfig;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatabaseManager {
    private final String path = "src/main/java/com/me/LootSplit/database/database.db";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
    public DatabaseManager() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        Connection connection = null;
        config.enableLoadExtension(true);
        connection = DriverManager.getConnection("jdbc:sqlite:" + path, config.toProperties());
        // this.connection.createStatement().execute("SELECT load_extension('spellfix')");
        try (Statement statement = connection.createStatement();) {
            statement.setQueryTimeout(30);
            statement.execute("SELECT load_extension('spellfix')");
        } catch (SQLException e) {
            System.out.println("Error loading extension: " + e.getMessage());
        }
        config.enableLoadExtension(false);
        try (Statement statement = connection.createStatement();)  {
            // users table (id, guild_id, cta, character name, last seen, status, party number)
            statement.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL," +
                    "balance INTEGER NOT NULL," +
                    "registered BOOLEAN NOT NULL," +
                    "guild_name TEXT NOT NULL," +
                    "guild_id INTEGER NOT NULL" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS roles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "role_name TEXT NOT NULL," +
                    "duration_in_min INTEGER," +
                    "guild_id INTEGER NOT NULL" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS players_roles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL," +
                    "time_left INTEGER," +
                    "guild_id INTEGER NOT NULL" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS lootsplit_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "split_id TEXT NOT NULL," +
                    "name TEXT NOT NULL," +
                    "silver INTEGER NOT NULL," +
                    "items INTEGER NOT NULL," +
                    "guild_id INTEGER NOT NULL" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS lootsplit_players (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "split_id TEXT NOT NULL," +
                    "username TEXT NOT NULL," +
                    "balance INTEGER NOT NULL," +
                    "is_halved BOOLEAN NOT NULL," +
                    "guild_id INTEGER NOT NULL" +
                    ")");
        } catch (SQLException e) {
            System.out.println("Error creating tables: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // getPlayerBalance(String playerName)
    public int getPlayerBalance(String playerName) {
        Connection connection = null;
        int balance = 0;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("SELECT balance FROM players WHERE username = ?")) {
                statement.setString(1, playerName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        balance = resultSet.getInt("balance");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting player balance: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return balance;
    }

    // createNewLootSplitSession(String splitId, String name, long guildId)
    public void createNewLootSplitSession(String splitId, String name, long guildId) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO lootsplit_sessions (split_id, name, silver, items, guild_id) VALUES (?, ?, 0, 0, ?)")) {
                statement.setString(1, splitId);
                statement.setString(2, name);
                statement.setLong(3, guildId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error creating new loot split session: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // addPlayerToLootSplit(String splitId, String playerName, long guildId)
    public void addPlayerToLootSplit(String splitId, String playerName, long guildId) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO lootsplit_players (split_id, username, balance, is_halved, guild_id) VALUES (?, ?, 0, 0, ?)")) {
                statement.setString(1, splitId);
                statement.setString(2, playerName);
                statement.setLong(3, guildId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error adding player to loot split: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // addPlayersToLootSplit(String splitId, List<String> playerNames, long guildId) using addBatch() and executeBatch()
    public void addPlayersToLootSplit(String splitId, List<String> playerNames, long guildId) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO lootsplit_players (split_id, username, balance, is_halved, guild_id) VALUES (?, ?, 0, 0, ?)")) {
                for (String playerName : playerNames) {
                    statement.setString(1, splitId);
                    statement.setString(2, playerName);
                    statement.setLong(3, guildId);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            System.out.println("Error adding players to loot split: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // removePlayerFromLootSplit(String splitId, String playerName)
    public void removePlayerFromLootSplit(String splitId, String playerName) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM lootsplit_players WHERE split_id = ? AND username = ?")) {
                statement.setString(1, splitId);
                statement.setString(2, playerName);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error removing player from loot split: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // setLootSplitSilverAndItems(String splitID, Integer silver, Integer items)
    public void setLootSplitSilverAndItems(String splitID, Integer silver, Integer items) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE lootsplit_sessions SET silver = ?, items = ? WHERE split_id = ?")) {
                statement.setInt(1, silver);
                statement.setInt(2, items);
                statement.setString(3, splitID);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error setting loot split silver and items: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // getLootSplitSilverAndItems(String splitID)
    public List<Integer> getLootSplitSilverAndItems(String splitID) {
        Connection connection = null;
        List<Integer> silverAndItems = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("SELECT silver, items FROM lootsplit_sessions WHERE split_id = ?")) {
                statement.setString(1, splitID);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        silverAndItems.add(resultSet.getInt("silver"));
                        silverAndItems.add(resultSet.getInt("items"));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting loot split silver and items: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return silverAndItems;
    }

    // makePlayerBalanceHalvedInLootSplit(String splitId, String playerName)
    public void makePlayerBalanceHalvedInLootSplit(String splitId, String playerName) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE lootsplit_players SET is_halved = 1 WHERE split_id = ? AND username = ?")) {
                statement.setString(1, splitId);
                statement.setString(2, playerName);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error making player halved in loot split: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // addBalanceToAllLootSplitPlayers(String splitId, Integer amount)
    public void addBalanceToAllLootSplitPlayers(String splitId, Integer amount) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE lootsplit_players SET balance = balance + ? WHERE split_id = ?")) {
                statement.setInt(1, amount);
                statement.setString(2, splitId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error adding balance to all loot split players: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // removeUserAmount(String username, Integer amount)
    public void removeUserAmount(String username, Integer amount) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET balance = balance - ? WHERE username = ?")) {
                statement.setInt(1, amount);
                statement.setString(2, username);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error removing user amount: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // giveUserAmount(String username, Integer amount)
    public void giveUserAmount(String username, Integer amount) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET balance = balance + ? WHERE username = ?")) {
                statement.setInt(1, amount);
                statement.setString(2, username);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error giving user amount: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // getHighestBalanceUsers()
    public List<String> getHighestBalanceUsers() {
        Connection connection = null;
        List<String> highestBalanceUsers = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("SELECT username FROM players ORDER BY balance DESC LIMIT 10")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        highestBalanceUsers.add(resultSet.getString("username"));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting highest balance users: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return highestBalanceUsers;
    }

    // registerTheUser(String username)
    public void registerTheUser(String username) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET registered = 1 WHERE username = ?")) {
                statement.setString(1, username);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error registering the user: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
    }

    // uploadGuildList(String guildName, List<String> names, long guildId)
    public void uploadGuildList(String guildName, List<String> names, long guildId) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO players (username, balance, registered, guild_name, guild_id) VALUES (?, 0, 0, ?, ?)")) {
                for (String name : names) {
                    statement.setString(1, name);
                    statement.setString(2, guildName);
                    statement.setLong(3, guildId);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            System.out.println("Error uploading guild list: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
    }

    // removeGuildList(String guildName, long guildId)
    public void removeGuildList(String guildName, long guildId) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM players WHERE guild_name = ? AND guild_id = ?")) {
                statement.setString(1, guildName);
                statement.setLong(2, guildId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error removing guild list: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
    }

    // addNewRole(String roleName, Integer durationInMin, long guildId)
    public void addNewRole(String roleName, Integer durationInMin, long guildId) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO roles (role_name, duration_in_min, guild_id) VALUES (?, ?, ?)")) {
                statement.setString(1, roleName);
                statement.setInt(2, durationInMin);
                statement.setLong(3, guildId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error adding new role: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
    }

    // removePlayerRole(String playerName, long guildId)
    public void removePlayerRole(String playerName, long guildId) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM players_roles WHERE username = ? AND guild_id = ?")) {
                statement.setString(1, playerName);
                statement.setLong(2, guildId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error removing player role: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
    }
}