package com.me.LootSplit.database;

import org.sqlite.SQLiteConfig;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
                    "user_id INTEGER," +
                    "username TEXT NOT NULL," +
                    "balance INTEGER NOT NULL," +
                    "guild_name TEXT NOT NULL," +
                    "registered BOOLEAN NOT NULL," +
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
                    "user_id INTEGER NOT NULL," +
                    "role_name TEXT NOT NULL," +
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
                    "upload_id TEXT NOT NULL," +
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

    // getPlayerBalance(long userId)
    public int getPlayerBalance(long userId) throws SQLException {
        Connection connection = null;
        int balance = 0;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("SELECT balance FROM players WHERE user_id = ?")) {
                statement.setLong(1, userId);
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
    public void createNewLootSplitSession(String splitId, String name, long guildId) throws SQLException {
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

    // getLootSplitId(long guildId)
    public String getLootSplitId(long guildId) throws SQLException {
        Connection connection = null;
        String splitId = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("SELECT split_id FROM lootsplit_sessions WHERE guild_id = ?")) {
                statement.setLong(1, guildId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        splitId = resultSet.getString("split_id");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting loot split id: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return splitId;
    }


    // addPlayerToLootSplit(String splitId, String playerName, long guildId)
    public void addPlayerToLootSplit(String splitId, String playerName, long guildId) throws SQLException {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            // add if not exists
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO lootsplit_players (split_id, username, balance, is_halved, guild_id)\n" +
                    "SELECT ?, ?, 0, 0, ?\n" +
                    "WHERE NOT EXISTS (" +
                    "SELECT 1 FROM lootsplit_players WHERE split_id = ? AND username = ?" +
                    ") AND EXISTS (" +
                    "SELECT 1 FROM players WHERE username = ? AND guild_id = ?" +
                    ");")) {
                statement.setString(1, splitId);
                statement.setString(2, playerName);
                statement.setLong(3, guildId);
                statement.setString(4, splitId);
                statement.setString(5, playerName);
                statement.setString(6, playerName);
                statement.setLong(7, guildId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error adding player to loot split: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // addPlayersToLootSplit(String uploadId, String splitId, List<String> playerNames, long guildId) using addBatch() and executeBatch()
    public void addPlayersToLootSplit(String uploadId, String splitId, List<String> playerNames, long guildId) throws SQLException {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO lootsplit_players (split_id, upload_id, username, balance, is_halved, guild_id)\n" +
                    "SELECT ?, ?, ?, 0, 0, ?\n" +
                    "WHERE NOT EXISTS (" +
                    "SELECT 1 FROM lootsplit_players WHERE split_id = ? AND username = ?" +
                    ") AND EXISTS (" +
                    "SELECT 1 FROM players WHERE username = ? AND guild_id = ?" +
                    ");")) {
                for (String playerName : playerNames) {
                    statement.setString(1, splitId);
                    statement.setString(2, uploadId);
                    statement.setString(3, playerName);
                    statement.setLong(4, guildId);
                    statement.setString(5, splitId);
                    statement.setString(6, playerName);
                    statement.setString(7, playerName);
                    statement.setLong(8, guildId);
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
    public void removePlayerFromLootSplit(String splitId, String playerName) throws SQLException {
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
    public void setLootSplitSilverAndItems(String splitID, Integer silver, Integer items) throws SQLException {
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
    public List<Integer> getLootSplitSilverAndItems(String splitID) throws SQLException {
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
    public void makePlayerBalanceHalvedInLootSplit(String splitId, String playerName) throws SQLException {
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

    // a function named spellFixMatch that takes a string and checks if it is similar to any of the words in the database
    // if it is, return the word from the database (first match if multiple matches)
    // if it is not, return null
    public String spellFixMatch(String word, Long guildID) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        SQLiteConfig config = new SQLiteConfig();
        ResultSet resultSet = null;
        String match = null;
        String[] forbidden_words = {"Cluster", "preferrad", "Access", "Priority", "This", "setting", "determines", "who", "gets", "preferred", "access", "overcrowded", "clusters.", "Party", "Priority", "First", "Access)", "Party's", "Priority", "within", "Alliance/Guild:", "Dit)", "Party", "Member", "Priority",
                "First", "Access)"};
        // Check if the word is in the forbidden words list
        for (String forbidden_word : forbidden_words) {
            if (word.equals(forbidden_word)) {
                return null;
            }
        }
        try {
            config.enableLoadExtension(true);
            connection = DriverManager.getConnection("jdbc:sqlite:" + path, config.toProperties());
            try (Statement statement = connection.createStatement();) {
                statement.setQueryTimeout(30);
                statement.execute("SELECT load_extension('spellfix')");
                statement.execute("CREATE VIRTUAL TABLE IF NOT EXISTS demo USING spellfix1;");
            } catch (SQLException e) {
                System.out.println("Error loading extension: " + e.getMessage());
            }
            config.enableLoadExtension(false);
            preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM demo");
            resultSet = preparedStatement.executeQuery();
            if (resultSet.getInt(1) == 0) {
                System.out.println("No words in the database");
                return null;
            }
            // SELECT word, editdist3(LOWER(demo.word), LOWER('word'), 800) AS distance FROM demo WHERE editdist3(LOWER(demo.word), LOWER('word'), 800) < 350 ORDER BY distance ASC LIMIT 1;
            preparedStatement = connection.prepareStatement("SELECT demo.word, editdist3(LOWER(demo.word), LOWER(?), 800) AS distance FROM demo JOIN players ON players.username = demo.word WHERE editdist3(LOWER(demo.word), LOWER(?), 800) < 350 AND LENGTH(?) > 3 AND players.guild_id = ? ORDER BY distance ASC LIMIT 1");
            preparedStatement.setString(1, word);
            preparedStatement.setString(2, word);
            preparedStatement.setString(3, word);
            preparedStatement.setLong(4, guildID);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                match = resultSet.getString("word");
            }
        } catch (SQLException e) {
            System.out.println("Spellfix Exception: Error Code=" + e.getErrorCode());
            e.printStackTrace();
        }
        finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return match;
    }

    // addBalanceToAllLootSplitPlayers(String splitId, Integer amount)
    public void addBalanceToAllLootSplitPlayers(String splitId, Integer amount) throws SQLException {
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
    public void removeUserAmount(String username, Integer amount) throws SQLException {
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
    public void giveUserAmount(String username, Integer amount) throws SQLException {
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
    public List<String> getHighestBalanceUsers() throws SQLException {
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

    // registerTheUser(String username, long userId)
    public void registerTheUser(String username, long userId) throws SQLException {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET registered = 1, user_id = ? WHERE username = ?")) {
                statement.setString(1, username);
                statement.setLong(2, userId);
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

    public void postProcessPlayersTable() throws SQLException {
        /**
         * This function will be used to post-process the users table after the data has been uploaded.
         * The function will:
         *   Copy the character names from the "players" table to the demo table
         *   to be used for spellfix1 extension
         */
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        SQLiteConfig config = new SQLiteConfig();
        try {
            config.enableLoadExtension(true);
            connection = DriverManager.getConnection("jdbc:sqlite:" + path, config.toProperties());
            try (Statement statement = connection.createStatement();) {
                statement.setQueryTimeout(30);
                statement.execute("SELECT load_extension('spellfix')");
                statement.execute("CREATE VIRTUAL TABLE IF NOT EXISTS demo USING spellfix1;");
            } catch (SQLException e) {
                System.out.println("Error loading extension: " + e.getMessage());
            }
            config.enableLoadExtension(false);
            preparedStatement = connection.prepareStatement("INSERT INTO demo(word) SELECT username FROM players " +
                    "WHERE NOT EXISTS (SELECT 1 FROM demo WHERE word = players.username);");
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Post Process Exception: Error Code=" + e.getErrorCode());
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // uploadGuildList(String guildName, List<String> names, long guildId)
    public void uploadGuildList(String guildName, List<String> names, long guildId) throws SQLException {
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

    // getLootSplitPlayersWithUploadId(String uploadId, String splitId, long guildId)
    public List<String> getLootSplitPlayersWithUploadId(String uploadId, String splitId, long guildId) {
        Connection connection = null;
        List<String> players = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("SELECT username FROM lootsplit_players WHERE split_id = ? AND guild_id = ? AND upload_id = ?")) {
                statement.setString(1, splitId);
                statement.setLong(2, guildId);
                statement.setString(3, uploadId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        players.add(resultSet.getString("username"));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting loot split players: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
        return players;
    }

    // removeGuildList(String guildName, long guildId)
    public void removeGuildList(String guildName, long guildId) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM players WHERE LOWER(guild_name) = LOWER(?) AND guild_id = ?")) {
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

    // getRoleDuration(String roleName, long guildId)
    public Integer getRoleDuration(String roleName, long guildId) {
        Connection connection = null;
        int duration = 0;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("SELECT duration_in_min FROM roles WHERE role_name = ? AND guild_id = ?")) {
                statement.setString(1, roleName);
                statement.setLong(2, guildId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        duration = resultSet.getInt("duration_in_min");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting role duration: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
        return duration;
    }

    // getPlayersWithExpiredRoles(long guildId) -> username, role_name
    public List<String> getPlayersWithExpiredRoles(long guildId) {
        Connection connection = null;
        List<String> playersWithExpiredRoles = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("SELECT user_id, role_name FROM players_roles WHERE time_left <= 0 AND guild_id = ?")) {
                statement.setLong(1, guildId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        playersWithExpiredRoles.add(resultSet.getString("user_id") + "|||" + resultSet.getString("role_name"));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting players with expired roles: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
        return playersWithExpiredRoles;
    }

    // givePlayerRole(long userId, long guildId, int timeLeft)
    public void givePlayerRole(long userId, long guildId, String roleName, int timeLeft) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO players_roles (user_id, role_name, time_left, guild_id) VALUES (?, ?, ?, ?)")) {
                statement.setLong(1, userId);
                statement.setString(2, roleName);
                statement.setInt(3, timeLeft);
                statement.setLong(4, guildId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error giving player role: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
    }

    // removePlayerRole(String playerName, long guildId)
    public void removePlayerRole(long userId, long guildId) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM players_roles WHERE user_id = ? AND guild_id = ?")) {
                statement.setLong(1, userId);
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

    // decrementTimeLeft(long guildId)
    public void decrementTimeLeft(long guildId) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE players_roles SET time_left = time_left - 1 WHERE guild_id = ?")) {
                statement.setLong(1, guildId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error decrementing time left: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {}
        }
    }

    // isThereActiveSession(long guildId)
    public boolean isThereActiveSession(long guildId) {
        Connection connection = null;
        boolean isActive = false;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM lootsplit_sessions WHERE guild_id = ?")) {
                statement.setLong(1, guildId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        isActive = true;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking if there is an active session: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
        return isActive;
    }

    // isGuildListExists(String guildName, long guildId)
    public boolean isGuildListExists(String guildName, long guildId) throws SQLException {
        Connection connection = null;
        boolean isExists = false;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE LOWER(guild_name) = LOWER(?) AND guild_id = ?")) {
                statement.setString(1, guildName);
                statement.setLong(2, guildId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        isExists = true;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking if guild list exists: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
        return isExists;
    }

    // isUserRegistered(long userId)
    public boolean isUserRegistered(long userId) throws SQLException {
        Connection connection = null;
        boolean isRegistered = false;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("SELECT registered FROM players WHERE user_id = ?")) {
                statement.setLong(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        isRegistered = resultSet.getBoolean("registered");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking if user is registered: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
        return isRegistered;
    }

    // isUserExists(String username)
    public boolean isUserExists(String username) throws SQLException {
        Connection connection = null;
        boolean isExists = false;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE username = ?")) {
                statement.setString(1, username);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        isExists = true;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking if user exists: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
        return isExists;
    }
}