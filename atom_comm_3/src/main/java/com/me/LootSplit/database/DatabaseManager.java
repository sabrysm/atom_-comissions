package com.me.LootSplit.database;

import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.map.ListOrderedMap;
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
                    "balance FLOAT NOT NULL," +
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
                    "status TEXT NOT NULL," +
                    "name TEXT NOT NULL," +
                    "silver INTEGER NOT NULL," +
                    "items INTEGER NOT NULL," +
                    "amount FLOAT NOT NULL," +
                    "session_creator TEXT NOT NULL," +
                    "guild_id INTEGER NOT NULL" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS lootsplit_players (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "split_id TEXT NOT NULL," +
                    "upload_id TEXT NOT NULL," +
                    "username TEXT NOT NULL," +
                    "balance FLOAT NOT NULL," +
                    "is_halved BOOLEAN NOT NULL," +
                    "guild_id INTEGER NOT NULL" +
                    ")");
        } catch (SQLException e) {
            System.out.println("Error creating tables: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // getPlayerName(long userId)
    public String getPlayerName(long userId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String playerName = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT username FROM players WHERE user_id = ?");
            statement.setLong(1, userId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                playerName = resultSet.getString("username");
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return playerName;
    }

    // getFullSplitPlayers(String splitId)
    public List<String> getFullSplitPlayers(String splitId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<String> fullPlayers = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT username FROM lootsplit_players WHERE split_id = ? AND is_halved = 0");
            statement.setString(1, splitId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                fullPlayers.add(resultSet.getString("username"));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return fullPlayers;
    }

    // getHalfSplitPlayers(String splitId)
    public List<String> getHalfSplitPlayers(String splitId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<String> halvedPlayers = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT username FROM lootsplit_players WHERE split_id = ? AND is_halved = 1");
            statement.setString(1, splitId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                halvedPlayers.add(resultSet.getString("username") + " (halved)");
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return halvedPlayers;
    }

    // isPlayerInLootSplit(String splitId, String playerName)
    public boolean isPlayerInLootSplit(String splitId, String playerName) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        boolean exists = false;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT COUNT(*) FROM lootsplit_players WHERE split_id = ? AND username = ?");
            statement.setString(1, splitId);
            statement.setString(2, playerName);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                exists = resultSet.getInt(1) > 0;
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return exists;
    }

    // getPlayerBalance(long userId)
    public int getPlayerBalance(long userId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        int balance = 0;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT balance FROM players WHERE user_id = ?");
            statement.setLong(1, userId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                balance = resultSet.getInt("balance");
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return balance;
    }

    // createNewLootSplitSession(String splitId, String name, long guildId)
    public void createNewLootSplitSession(String splitId, String name, long guildId, String sessionCreator) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("INSERT INTO lootsplit_sessions (split_id, status, name, amount, silver, items, guild_id, session_creator) VALUES (?, ?, ?, 0, 0, 0, ?, ?)");
            statement.setString(1, splitId);
            statement.setString(2, "active");
            statement.setString(3, name);
            statement.setLong(4, guildId);
            statement.setString(5, sessionCreator);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // getLootSplitId(long guildId)
    public String getLootSplitId(long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String splitId = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT split_id FROM lootsplit_sessions WHERE guild_id = ? AND status = 'active'");
            statement.setLong(1, guildId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                splitId = resultSet.getString("split_id");
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return splitId;
    }


    // addPlayerToLootSplit(String splitId, String uploadId, String playerName, long guildId)
    public void addPlayerToLootSplit(String splitId, String uploadId, String playerName, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        String query = "INSERT INTO lootsplit_players (split_id, upload_id, username, balance, is_halved, guild_id)\n" +
                "SELECT ?, ?, ?, 0, 0, ? WHERE NOT EXISTS (" +
                "SELECT 1 FROM lootsplit_players WHERE split_id = ? AND username = ?" +
                ") AND EXISTS (" +
                "SELECT 1 FROM players WHERE username = ? AND guild_id = ?" +
                ");";
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement(query);
            statement.setString(1, splitId);
            statement.setString(2, uploadId);
            statement.setString(3, playerName);
            statement.setLong(4, guildId);
            statement.setString(5, splitId);
            statement.setString(6, playerName);
            statement.setString(7, playerName);
            statement.setLong(8, guildId);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // addPlayersToLootSplit(String uploadId, String splitId, List<String> playerNames, long guildId) using addBatch() and executeBatch()
    public void addPlayersToLootSplit(String uploadId, String splitId, List<String> playerNames, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        String query = "INSERT INTO lootsplit_players (split_id, upload_id, username, balance, is_halved, guild_id)\n" +
                "SELECT ?, ?, ?, 0, 0, ?\n" +
                "WHERE NOT EXISTS (" +
                "SELECT 1 FROM lootsplit_players WHERE split_id = ? AND username = ?" +
                ") AND EXISTS (" +
                "SELECT 1 FROM players WHERE username = ? AND guild_id = ?" +
                ");";
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement(query);
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
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // removePlayerFromLootSplit(String splitId, String playerName)
    public void removePlayerFromLootSplit(String splitId, String playerName) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("DELETE FROM lootsplit_players WHERE split_id = ? AND username = ?");
            statement.setString(1, splitId);
            statement.setString(2, playerName);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // setLootSplitSilverAndItems(String splitID, Integer silver, Integer items)
    public void setLootSplitSilverAndItems(String splitID, Integer silver, Integer items) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("UPDATE lootsplit_sessions SET silver = ?, items = ? WHERE split_id = ?");
            statement.setInt(1, silver);
            statement.setInt(2, items);
            statement.setString(3, splitID);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // getLootSplitSilverAndItems(String splitID)
    public List<Integer> getLootSplitSilverAndItems(String splitID) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Integer> silverAndItems = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT silver, items FROM lootsplit_sessions WHERE split_id = ?");
            statement.setString(1, splitID);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                silverAndItems.add(resultSet.getInt("silver"));
                silverAndItems.add(resultSet.getInt("items"));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return silverAndItems;
    }

    // makePlayerBalanceHalvedInLootSplit(String splitId, String playerName)
    public void makePlayerBalanceHalvedInLootSplit(String splitId, String playerName) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("UPDATE lootsplit_players SET is_halved = 1 WHERE split_id = ? AND username = ?");
            statement.setString(1, splitId);
            statement.setString(2, playerName);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
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

    // getLootSplitSessionInfo(String splitId)
    public OrderedMap<String, Object> getLootSplitSessionInfo(String splitId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        OrderedMap<String, Object> sessionInfo = new ListOrderedMap<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT * FROM lootsplit_sessions WHERE split_id = ?");
            statement.setString(1, splitId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                sessionInfo.put("name", resultSet.getString("name"));
                sessionInfo.put("split_id", resultSet.getString("split_id"));
                sessionInfo.put("status", resultSet.getString("status"));
                sessionInfo.put("silver", resultSet.getInt("silver"));
                sessionInfo.put("items", resultSet.getInt("items"));
                sessionInfo.put("amount", resultSet.getDouble("amount"));
                sessionInfo.put("session_creator", resultSet.getString("session_creator"));
                sessionInfo.put("guild_id", resultSet.getLong("guild_id"));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return sessionInfo;
    }

    // addBalanceToAllLootSplitPlayers(String splitId, Integer amount)
    public void addBalanceToAllLootSplitPlayers(String splitId, double amount) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try {
                statement = connection.prepareStatement("UPDATE lootsplit_sessions SET amount = amount + ? WHERE split_id = ?");
                statement.setDouble(1, amount);
                statement.setString(2, splitId);
                statement.executeUpdate();
            } finally {
                if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            }
            // halve the balance if the player is marked as halved
            try {
                statement = connection.prepareStatement("UPDATE lootsplit_players SET balance = balance + ? WHERE split_id = ? AND is_halved = 0");
                statement.setDouble(1, amount);
                statement.setString(2, splitId);
                statement.executeUpdate();
            } finally {
                if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            }

            try {
                statement = connection.prepareStatement("UPDATE lootsplit_players SET balance = balance + ? WHERE split_id = ? AND is_halved = 1");
                statement.setDouble(1, amount/2);
                statement.setString(2, splitId);
                statement.executeUpdate();
            } finally {
                if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            }
            // update these players in the players table (JOIN ON username) and add the balance
            try {
                statement = connection.prepareStatement("UPDATE players SET balance = balance + ? WHERE username IN (SELECT username FROM lootsplit_players WHERE split_id = ? AND is_halved = 1)");
                statement.setDouble(1, amount/2);
                statement.setString(2, splitId);
                statement.executeUpdate();
            } finally {
                if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            }
            try {
                statement = connection.prepareStatement("UPDATE players SET balance = balance + ? WHERE username IN (SELECT username FROM lootsplit_players WHERE split_id = ? AND is_halved = 0)");
                statement.setDouble(1, amount);
                statement.setString(2, splitId);
                statement.executeUpdate();
            } finally {
                if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            }
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // removeUserAmount(String username, Double amount)
    public void removeUserAmount(String username, Double amount) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("UPDATE players SET balance = balance - ? WHERE username = ?");
            statement.setDouble(1, amount);
            statement.setString(2, username);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // giveUserAmount(String username, Double amount)
    public void giveUserAmount(String username, Double amount) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("UPDATE players SET balance = balance + ? WHERE username = ?");
            statement.setDouble(1, amount);
            statement.setString(2, username);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // getLeaderboard(int numberOfUsers)
    public OrderedMap<String, Double> getLeaderboard(int numberOfUsers) throws SQLException {
        Connection connection = null;
        OrderedMap<String, Double> leaderboard = new ListOrderedMap<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT username, balance FROM players ORDER BY balance DESC LIMIT ?");
            statement.setInt(1, numberOfUsers);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                leaderboard.put(resultSet.getString("username"), resultSet.getDouble("balance"));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return leaderboard;
    }

    // roleExists(String roleName, long guildId)
    public boolean roleExists(String roleName, long guildId) throws SQLException {
        Connection connection = null;
        boolean exists = false;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT COUNT(*) FROM roles WHERE role_name = ? AND guild_id = ?");
            statement.setString(1, roleName);
            statement.setLong(2, guildId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                exists = resultSet.getInt(1) > 0;
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return exists;
    }

    // registerTheUser(String username, long userId)
    public void registerTheUser(String username, long userId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("UPDATE players SET user_id = ?, registered = 1 WHERE username = ?");
            statement.setLong(1, userId);
            statement.setString(2, username);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
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
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // uploadGuildList(String guildName, List<String> names, long guildId)
    public void uploadGuildList(String guildName, List<String> names, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("INSERT INTO players (username, balance, registered, guild_name, guild_id) SELECT ?, 0, 0, ?, ? WHERE NOT EXISTS (SELECT 1 FROM players WHERE username = ? AND guild_id = ?)");
            for (String name : names)
            {
                statement.setString(1, name);
                statement.setString(2, guildName);
                statement.setLong(3, guildId);
                statement.setString(4, name);
                statement.setLong(5, guildId);
                statement.addBatch();
            }
            statement.executeBatch();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }
    // getLootSplitPlayers(String splitId, long guildId)
    public List<String> getLootSplitPlayers(String splitId, long guildId) throws SQLException {
        Connection connection = null;
        List<String> players = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT username FROM lootsplit_players WHERE split_id = ? AND guild_id = ?");
            statement.setString(1, splitId);
            statement.setLong(2, guildId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                players.add(resultSet.getString("username"));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return players;
    }

    // getLootSplitPlayersWithUploadId(String uploadId, String splitId, long guildId)
    public List<String> getLootSplitPlayersWithUploadId(String uploadId, String splitId, long guildId) throws SQLException {
        Connection connection = null;
        List<String> players = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT username FROM lootsplit_players WHERE upload_id = ? AND split_id = ? AND guild_id = ?");
            statement.setString(1, uploadId);
            statement.setString(2, splitId);
            statement.setLong(3, guildId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                players.add(resultSet.getString("username"));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return players;
    }

    // removeGuildList(String guildName, long guildId)
    public void removeGuildList(String guildName, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("DELETE FROM players WHERE guild_name = ? AND guild_id = ?");
            statement.setString(1, guildName);
            statement.setLong(2, guildId);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // addNewRole(String roleName, Integer durationInMin, long guildId)
    public void addNewRole(String roleName, Integer durationInMin, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("INSERT INTO roles (role_name, duration_in_min, guild_id) VALUES (?, ?, ?)");
            statement.setString(1, roleName);
            statement.setInt(2, durationInMin);
            statement.setLong(3, guildId);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // getRoleDuration(String roleName, long guildId)
    public Integer getRoleDuration(String roleName, long guildId) throws SQLException {
        Connection connection = null;
        int duration = 0;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT duration_in_min FROM roles WHERE role_name = ? AND guild_id = ?");
            statement.setString(1, roleName);
            statement.setLong(2, guildId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                duration = resultSet.getInt("duration_in_min");
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return duration;
    }

    // getPlayersWithExpiredRoles(long guildId) -> username, role_name
    public List<String> getPlayersWithExpiredRoles(long guildId) throws SQLException {
        Connection connection = null;
        List<String> playersWithExpiredRoles = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT user_id, role_name FROM players_roles WHERE time_left <= 0 AND guild_id = ?");
            statement.setLong(1, guildId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                playersWithExpiredRoles.add(resultSet.getLong("user_id") + "|||" + resultSet.getString("role_name"));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return playersWithExpiredRoles;
    }

    // givePlayerRole(long userId, long guildId, int timeLeft)
    public void givePlayerRole(long userId, long guildId, String roleName, int timeLeft) throws SQLException{
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("INSERT INTO players_roles (user_id, role_name, time_left, guild_id) VALUES (?, ?, ?, ?)");
            statement.setLong(1, userId);
            statement.setString(2, roleName);
            statement.setInt(3, timeLeft);
            statement.setLong(4, guildId);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // removePlayerRole(String playerName, long guildId)
    public void removePlayerRole(long userId, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("DELETE FROM players_roles WHERE user_id = ? AND guild_id = ?");
            statement.setLong(1, userId);
            statement.setLong(2, guildId);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // decrementTimeLeft(long guildId)
    public void decrementTimeLeft(long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("UPDATE players_roles SET time_left = time_left - 1 WHERE guild_id = ?");
            statement.setLong(1, guildId);
            statement.executeUpdate();
        } finally {
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // isThereActiveSession(long guildId)
    public boolean isThereActiveSession(long guildId) throws SQLException {
        Connection connection = null;
        boolean isActive = false;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT COUNT(*) FROM lootsplit_sessions WHERE status = 'active' AND guild_id = ?");
            statement.setLong(1, guildId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                isActive = resultSet.getInt(1) > 0;
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return isActive;
    }

    // isGuildListExists(String guildName, long guildId)
    public boolean isGuildListExists(String guildName, long guildId) throws SQLException {
        Connection connection = null;
        boolean exists = false;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            // case insensitive
            statement = connection.prepareStatement("SELECT COUNT(*) FROM players WHERE LOWER(guild_name) = LOWER(?) AND guild_id = ?");
            statement.setString(1, guildName);
            statement.setLong(2, guildId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                exists = resultSet.getInt(1) > 0;
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return exists;
    }

    // isUserRegistered(long userId)
    public boolean isUserRegistered(long userId) throws SQLException {
        Connection connection = null;
        boolean isRegistered = false;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT registered FROM players WHERE user_id = ?");
            statement.setLong(1, userId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                isRegistered = resultSet.getBoolean("registered");
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (statement != null) try { statement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return isRegistered;
    }

    // isUserExists(String username)
    public boolean isUserExists(String username) throws SQLException {
        Connection connection = null;
        boolean isExists = false;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT COUNT(*) FROM players WHERE username = ?");
            statement.setString(1, username);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                isExists = resultSet.getInt(1) > 0;
            }
        } finally {
            if (resultSet != null) try {
                resultSet.close();
            } catch (SQLException ignore) {
            }
            if (statement != null) try {
                statement.close();
            } catch (SQLException ignore) {
            }
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
        return isExists;
    }

    // endLootSplitSession(long guildId)
    public void endLootSplitSession(long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("UPDATE lootsplit_sessions SET status = 'inactive' WHERE guild_id = ?");
            statement.setLong(1, guildId);
            statement.executeUpdate();
        } finally {
            if (statement != null) try {
                statement.close();
            } catch (SQLException ignore) {
            }
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
    }

    // getLootSplitSessionCreator(long guildId)
    public String getLootSplitSessionCreator(long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String sessionCreator = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT session_creator FROM lootsplit_sessions WHERE guild_id = ? AND status = 'active'");
            statement.setLong(1, guildId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                sessionCreator = resultSet.getString("session_creator");
            }
        } finally {
            if (resultSet != null) try {
                resultSet.close();
            } catch (SQLException ignore) {
            }
            if (statement != null) try {
                statement.close();
            } catch (SQLException ignore) {
            }
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
        return sessionCreator;
    }

    // getLootSplitPlayersCount(String splitId, long guildId)
    public int getLootSplitPlayersCount(String splitId, long guildId) throws SQLException {
        Connection connection = null;
        int count = 0;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            statement = connection.prepareStatement("SELECT COUNT(*) FROM lootsplit_players WHERE split_id = ? AND guild_id = ?");
            statement.setString(1, splitId);
            statement.setLong(2, guildId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                count = resultSet.getInt(1);
            }
        } finally {
            if (resultSet != null) try {
                resultSet.close();
            } catch (SQLException ignore) {
            }
            if (statement != null) try {
                statement.close();
            } catch (SQLException ignore) {
            }
            if (connection != null) try {
                connection.close();
            } catch (SQLException ignore) {
            }
        }
        return count;
    }
}