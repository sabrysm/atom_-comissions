package com.me.GuildBot.database;

import com.me.GuildBot.utils.CTA;
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
    private final String path = "src/main/java/com/me/GuildBot/database/database.db";
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
            statement.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "char_name TEXT NOT NULL," +
                    "cta_id TEXT NOT NULL," +
                    "last_seen DATETIME NOT NULL," +
                    "guild_id INTEGER NOT NULL," +
                    "status TEXT NOT NULL," +
                    "party_number INTEGER" +
                    ")");
            // cta table (id, cta_id, guild_id, start_time, end_time, attendees, report (default null))
            statement.execute("CREATE TABLE IF NOT EXISTS cta (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "cta_id TEXT NOT NULL," +
                    "status TEXT DEFAULT 'active'," +
                    "guild_id INTEGER NOT NULL," +
                    "start_time DATETIME NOT NULL," +
                    "end_time DATETIME NOT NULL," +
                    "attendees INTEGER DEFAULT 0," +
                    "absentees INTEGER DEFAULT 0," +
                    "lates INTEGER DEFAULT 0," +
                    "report TEXT" +
                    ")");
        } catch (SQLException e) {
            System.out.println("Error creating tables: " + e.getMessage());
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    public Timestamp getTimestampFromUTCString(String dateString) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateString, formatter);
        return Timestamp.from(localDateTime.atZone(ZoneId.of("UTC")).toInstant());
    }

    // a function named spellFixMatch that takes a string and checks if it is similar to any of the words in the database
    // if it is, return the word from the database (first match if multiple matches)
    // if it is not, return null
    public String spellFixMatch(String word, Long guildID, String ctaID) throws SQLException {
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
            // SELECT word, editdist3(LOWER(word), LOWER(?), 800) AS distance FROM demo WHERE editdist3(LOWER(word), LOWER(?), 800) < 350 AND LENGTH(?) > 3 ORDER BY distance ASC LIMIT 1
            preparedStatement = connection.prepareStatement("SELECT demo.word, editdist3(LOWER(demo.word), LOWER(?), 800) AS distance FROM demo JOIN users ON users.char_name = demo.word WHERE editdist3(LOWER(demo.word), LOWER(?), 800) < 350 AND LENGTH(?) > 3 AND users.guild_id = ? AND users.cta_id = ? ORDER BY distance ASC LIMIT 1");
            preparedStatement.setString(1, word);
            preparedStatement.setString(2, word);
            preparedStatement.setString(3, word);
            preparedStatement.setLong(4, guildID);
            preparedStatement.setString(5, ctaID);
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

    public static void main(String[] args) {
        try {
            DatabaseManager databaseManager = new DatabaseManager();
            // SELECT word FROM demo WHERE word MATCH 'OAthreyifebane';
            String match = databaseManager.spellFixMatch("J AthreyLifebane", 1L, "1234");
            if (match!= null) {
                System.out.println("Match found: " + match);
            }
        } catch (SQLException e) {
            System.out.println("Error creating database manager: " + e.getMessage());
        }
    }

    public List<List<String>> getAllCTAs(long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<List<String>> ctas = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT cta_id, start_time, end_time, status FROM cta WHERE guild_id = ?");
            preparedStatement.setLong(1, guildId);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                List<String> cta = new ArrayList<>();
                cta.add(resultSet.getString("cta_id"));
                cta.add(CTA.getStringFromInstant(resultSet.getTimestamp("start_time").toInstant()));
                cta.add(CTA.getStringFromInstant(resultSet.getTimestamp("end_time").toInstant()));
                cta.add(resultSet.getString("status"));
                ctas.add(cta);
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return ctas;
    }


    public void deleteCTA(String ctaId, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("DELETE FROM cta WHERE cta_id = ? AND guild_id = ?");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            preparedStatement.executeUpdate();
            preparedStatement = connection.prepareStatement("DELETE FROM users WHERE cta_id = ? AND guild_id = ?");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            preparedStatement.executeUpdate();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }

    }

    public Instant getCTAStartTime(String ctaId, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Instant startTime = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT start_time FROM cta WHERE cta_id = ? AND guild_id = ?");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                startTime = resultSet.getTimestamp("start_time").toInstant();
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return startTime;
    }


    public Instant getLastSeen(String charName, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Instant lastSeen = null;
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
            preparedStatement = connection.prepareStatement("SELECT last_seen FROM users WHERE LOWER(char_name) = ? AND guild_id = ?");
            preparedStatement.setString(1, charName.toLowerCase());
            preparedStatement.setLong(2, guildId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                lastSeen = resultSet.getTimestamp("last_seen").toInstant();
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return lastSeen;
    }


    public void createCTA(String ctaId, long guildId, String startTime, String endTime) throws SQLException {
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            Timestamp startTimestamp = getTimestampFromUTCString(startTime);
            Timestamp endTimestamp = getTimestampFromUTCString(endTime);
            System.out.println("Start Timestamp: for " + startTime + " is " + startTimestamp);
            System.out.println("End Timestamp: for " + endTime + " is " + endTimestamp);
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("INSERT INTO cta (cta_id, guild_id, start_time, end_time) VALUES (?, ?, ?, ?)");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            preparedStatement.setTimestamp(3, startTimestamp);
            preparedStatement.setTimestamp(4, endTimestamp);
            preparedStatement.executeUpdate();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // updateCTAFinishTime(ctaID, guildID, endTime: Instant);
    public void updateCTAFinishTime(String ctaId, long guildId, Instant endTime) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("UPDATE cta SET end_time = ? WHERE cta_id = ? AND guild_id = ?");
            preparedStatement.setTimestamp(1, Timestamp.from(endTime));
            preparedStatement.setString(2, ctaId);
            preparedStatement.setLong(3, guildId);
            preparedStatement.executeUpdate();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    public void updateCTAStatus(String ctaId, String status) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try  {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("UPDATE cta SET status = ? WHERE cta_id = ?");
            preparedStatement.setString(1, status);
            preparedStatement.setString(2, ctaId);
            preparedStatement.executeUpdate();
            System.out.println("CTA Status Updated");
        } finally {
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
        }
    }

    public List<String> getJustFinishedActiveCTAs() throws SQLException {
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        List<String> finishedCTAs = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT cta_id FROM cta WHERE status = 'active'");
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                finishedCTAs.add(resultSet.getString("cta_id"));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return finishedCTAs;
    }


    public String getCTAID(long guildId) throws SQLException {
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        String ctaID = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT cta_id FROM cta WHERE guild_id = ? AND status = 'active'");
            preparedStatement.setLong(1, guildId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                ctaID = resultSet.getString("cta_id");
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return ctaID;
    }

    public void uploadGuildData(long guildId, String cta, List<String> charNames, List<String> lastSeen, String status) throws SQLException {
        String query = "INSERT INTO users (guild_id, cta_id, char_name, last_seen, status) VALUES (?, ?, ?, ?, ?)";
        Connection connection = null;
        SQLiteConfig config = new SQLiteConfig();
        PreparedStatement preparedStatement = null;
        try {
            config.enableLoadExtension(true);
            connection = DriverManager.getConnection("jdbc:sqlite:" + path, config.toProperties());
            try (Statement statement = connection.createStatement();) {
                statement.setQueryTimeout(30);
                statement.execute("SELECT load_extension('spellfix')");
            } catch (SQLException e) {
                System.out.println("Error loading extension: " + e.getMessage());
            }
            config.enableLoadExtension(false);
            preparedStatement = connection.prepareStatement(query);
            for (int i = 0; i < charNames.size(); i++) {
                preparedStatement.setLong(1, guildId);
                preparedStatement.setString(2, cta);
                preparedStatement.setString(3, charNames.get(i));
                preparedStatement.setTimestamp(4, getTimestampFromUTCString(lastSeen.get(i)));
                preparedStatement.setString(5, status);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            System.out.println("Upload Data Exception: Error Code=" + e.getErrorCode());
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // updateUserStatus(guildID, ctaID, onlineUser, "online");
    public void updateUserStatus(long guildId, String cta, String charName, String status) throws SQLException {
        String query = "UPDATE users SET status = ? WHERE guild_id = ? AND cta_id = ? AND char_name = ?";
        Connection connection = null;
        SQLiteConfig config = new SQLiteConfig();
        PreparedStatement preparedStatement = null;
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
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, status);
            preparedStatement.setLong(2, guildId);
            preparedStatement.setString(3, cta);
            preparedStatement.setString(4, charName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Update Data Exception: Error Code=" + e.getErrorCode());
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // updateGuildData(guildID, ctaID, charNamesBatch, lastSeenBatch, "offline");
    public void updateLastSeenGuildData(long guildId, String cta, List<String> charNames, List<String> lastSeen) throws SQLException {
        String query = "UPDATE users SET last_seen = ? WHERE guild_id = ? AND cta_id = ? AND char_name = ?";
        Connection connection = null;
        SQLiteConfig config = new SQLiteConfig();
        PreparedStatement preparedStatement = null;
        try {
            config.enableLoadExtension(true);
            connection = DriverManager.getConnection("jdbc:sqlite:" + path, config.toProperties());
            try (Statement statement = connection.createStatement();) {
                statement.setQueryTimeout(30);
                statement.execute("SELECT load_extension('spellfix')");
            } catch (SQLException e) {
                System.out.println("Error loading extension: " + e.getMessage());
            }
            config.enableLoadExtension(false);
            preparedStatement = connection.prepareStatement(query);
            for (int i = 0; i < charNames.size(); i++) {
                preparedStatement.setTimestamp(1, getTimestampFromUTCString(lastSeen.get(i)));
                preparedStatement.setLong(2, guildId);
                preparedStatement.setString(3, cta);
                preparedStatement.setString(4, charNames.get(i));
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            System.out.println("Update Data Exception: Error Code=" + e.getErrorCode());
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // getUsersWithLastSeenForGuild(guildID, ctaID); i.e. char_name, last_seen FROM users WHERE guild_id = ? AND cta_id = ? ORDER BY last_seen DESC
    public List<String> getUsersWithLastSeenForGuildOrderedDesc(long guildId, String ctaId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<String> users = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT char_name, last_seen FROM users WHERE guild_id = ? AND cta_id = ? ORDER BY last_seen DESC");
            preparedStatement.setLong(1, guildId);
            preparedStatement.setString(2, ctaId);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                users.add(resultSet.getString("char_name") + "|||" + CTA.getStringFromInstant(resultSet.getTimestamp("last_seen").toInstant()));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return users;
    }

    public void postProcessUsersTable() throws SQLException {
        /**
         * This function will be used to post-process the users table after the data has been uploaded.
         * The function will:
         *   Copy the character names from the users table to the demo table
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
            preparedStatement = connection.prepareStatement("INSERT INTO demo(word) SELECT char_name FROM users");
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Post Process Exception: Error Code=" + e.getErrorCode());
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // getPlayerStatus(guildID, ctaID, charName); i.e. status FROM users WHERE guild_id = ? AND cta_id = ? AND char_name = ?
    public String getPlayerStatus(long guildId, String ctaId, String charName) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String status = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT status FROM users WHERE guild_id = ? AND cta_id = ? AND char_name = ?");
            preparedStatement.setLong(1, guildId);
            preparedStatement.setString(2, ctaId);
            preparedStatement.setString(3, charName);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                status = resultSet.getString("status");
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return status;
    }

    public List<Integer> calcCTAStats(String ctaId, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<Integer> stats = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT status FROM users WHERE cta_id = ? AND guild_id = ?");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            resultSet = preparedStatement.executeQuery();
            int attendees = 0;
            int lates = 0;
            int absentees = 0;
            while (resultSet.next()) {
                String status = resultSet.getString("status");
                switch (status) {
                    case "present":
                        attendees++;
                        break;
                    case "late":
                        lates++;
                        break;
                    case "absent":
                        absentees++;
                        break;
                }
            }
            stats.add(attendees);
            stats.add(lates);
            stats.add(absentees);
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return stats;
    }


    public void updateCTAStats(String ctaId, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            List<Integer> stats = calcCTAStats(ctaId, guildId);
            preparedStatement = connection.prepareStatement("UPDATE cta SET attendees = ?, lates = ?, absentees = ? WHERE cta_id = ? AND guild_id = ?");
            preparedStatement.setInt(1, stats.get(0));
            preparedStatement.setInt(2, stats.get(1));
            preparedStatement.setInt(3, stats.get(2));
            preparedStatement.setString(4, ctaId);
            preparedStatement.setLong(5, guildId);
            preparedStatement.executeUpdate();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    public Integer getAttendeesForCTA(String ctaId, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        int attendees = 0;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT attendees FROM cta WHERE cta_id = ? AND guild_id = ?");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                attendees = resultSet.getInt("attendees");
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return attendees;
    }

    public Integer getLatesForCTA(String ctaId, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        int lates = 0;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT lates FROM cta WHERE cta_id = ? AND guild_id = ?");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                lates = resultSet.getInt("lates");
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return lates;
    }

    public Integer getAbsenteesForCTA(String ctaId, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        int absentees = 0;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT absentees FROM cta WHERE cta_id = ? AND guild_id = ?");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                absentees = resultSet.getInt("absentees");
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return absentees;
    }

    public boolean CTAExists(String ctaId, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT * FROM cta WHERE cta_id = ? AND guild_id = ?");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    public String getReportForCTA(String ctaId, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String report = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT report FROM cta WHERE cta_id = ? AND guild_id = ?");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                report = resultSet.getString("report");
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return report;
    }


    public Instant getCTAEndTime(String ctaId, Long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Instant endTime = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);

            if (guildId == null) {
                preparedStatement = connection.prepareStatement("SELECT end_time FROM cta WHERE cta_id = ?");
            }
            else {
                preparedStatement = connection.prepareStatement("SELECT end_time FROM cta WHERE cta_id = ? AND guild_id = ?");
            }
            preparedStatement.setString(1, ctaId);
            if (guildId != null) {
                preparedStatement.setLong(2, guildId);
            }
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                endTime = resultSet.getTimestamp("end_time").toInstant();
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return endTime;
    }

    public Integer getAttendancesForUser(String charName, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        int attendances = 0;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE LOWER(char_name) = ? AND guild_id = ? AND status = 'present'");
            preparedStatement.setString(1, charName.toLowerCase());
            preparedStatement.setLong(2, guildId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                attendances = resultSet.getInt(1);
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return attendances;
    }

    public Integer getLatesForUser(String charName, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        int lates = 0;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE LOWER(char_name) = ? AND guild_id = ? AND status = 'late'");
            preparedStatement.setString(1, charName.toLowerCase());
            preparedStatement.setLong(2, guildId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                lates = resultSet.getInt(1);
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return lates;
    }

    public Integer getAbsenteesForUser(String charName, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        int absentees = 0;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE LOWER(char_name) = ? AND guild_id = ? AND status = 'absent'");
            preparedStatement.setString(1, charName.toLowerCase());
            preparedStatement.setLong(2, guildId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                absentees = resultSet.getInt(1);
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return absentees;
    }



    public void clearUsersDataForParty(long guildId, String ctaId, Integer partyNumber) throws SQLException {
        // load config
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
            preparedStatement = connection.prepareStatement("UPDATE users SET party_number = NULL WHERE guild_id = ? AND cta_id = ? AND party_number = ?");
            preparedStatement.setLong(1, guildId);
            preparedStatement.setString(2, ctaId);
            if (partyNumber != null) {
                preparedStatement.setInt(3, partyNumber);
            } else {
                preparedStatement.setNull(3, Types.INTEGER);
            }
            preparedStatement.executeUpdate();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }

    }

    public void addPlayerToParty(String charName, long guildId, String ctaId, Integer partyNumber, String status) throws SQLException {
        // Update the status of the player to "present" and assign a party number
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        SQLiteConfig config = new SQLiteConfig();
        if (status.equals("Unknown") || charName == null) {
            return;
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
            // update the status of the player to "present" and assign a party number
            preparedStatement = connection.prepareStatement("UPDATE users SET status = ?, party_number = ? WHERE LOWER(char_name) = ? AND guild_id = ? AND cta_id = ?");
            preparedStatement.setString(1, status);
            if (partyNumber != null) {
                preparedStatement.setInt(2, partyNumber);
            } else {
                preparedStatement.setNull(2, Types.INTEGER);
            }
            preparedStatement.setString(3, charName.toLowerCase());
            preparedStatement.setLong(4, guildId);
            preparedStatement.setString(5, ctaId);
            preparedStatement.executeUpdate();
            System.out.println("Player: " + charName + " added to party: " + partyNumber);
        } catch (SQLException e) {
            System.out.println("SQL Exception:\nError adding player to party: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("General Exception:\nError adding player to party: " + e.getMessage());
        }
        finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }

    }
    
    public void removePlayerFromParty(String charName, long guildId, String ctaId, Integer partyNumber) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("UPDATE users SET status = 'absent', party_number = NULL WHERE LOWER(char_name) = ? AND guild_id = ? AND cta_id = ? AND party_number = ?");
            preparedStatement.setString(1, charName.toLowerCase(Locale.ROOT));
            preparedStatement.setLong(2, guildId);
            preparedStatement.setString(3, ctaId);
            if (partyNumber != null) {
                preparedStatement.setInt(4, partyNumber);
            } else {
                preparedStatement.setNull(4, Types.INTEGER);
            }
            preparedStatement.executeUpdate();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // Delete ALL users from the database for a specific guild CTA
    public void deleteUsersDataForGuild(long guildId, String ctaId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("DELETE FROM users WHERE guild_id = ? AND cta_id = ?");
            preparedStatement.setLong(1, guildId);
            preparedStatement.setString(2, ctaId);
            preparedStatement.executeUpdate();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // addReport(ctaID, event.getGuild().getIdLong(), report);
    public void addReport(String ctaId, long guildId, String report) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("UPDATE cta SET report = ? WHERE cta_id = ? AND guild_id = ?");
            preparedStatement.setString(1, report);
            preparedStatement.setString(2, ctaId);
            preparedStatement.setLong(3, guildId);
            preparedStatement.executeUpdate();
        } finally {
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
    }

    // playerHasParty(playerName, guildID, ctaID)
    public boolean playerHasParty(String charName, long guildId, String ctaId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        boolean hasParty = false;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT party_number FROM users WHERE LOWER(char_name) = ? AND guild_id = ? AND cta_id = ?");
            preparedStatement.setString(1, charName.toLowerCase());
            preparedStatement.setLong(2, guildId);
            preparedStatement.setString(3, ctaId);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                // Check if the party number is not null
                hasParty = resultSet.getInt("party_number") != 0;
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return hasParty;
    }

    // getAbsentees(guildID, ctaID)
    // i.e. char_name FROM users WHERE guild_id = ? AND cta_id = ? AND status = 'online' AND party_number IS NULL
    public List<String> getAbsentees(long guildId, String ctaId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<String> absentees = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT char_name FROM users WHERE guild_id = ? AND cta_id = ? AND status = 'absent' AND party_number IS NULL");
            preparedStatement.setLong(1, guildId);
            preparedStatement.setString(2, ctaId);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                absentees.add(resultSet.getString("char_name"));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return absentees;
    }

    public List<Integer> getParties(String ctaId, long guildId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<Integer> partyNumbers = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT DISTINCT party_number FROM users WHERE cta_id = ? AND guild_id = ? AND party_number IS NOT NULL");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                partyNumbers.add(resultSet.getInt("party_number"));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return partyNumbers;
    }

    public List<String> getPartyMembersNames(String ctaId, long guildId, Integer partyNumber) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<String> partyMembers = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT char_name FROM users WHERE cta_id = ? AND guild_id = ? AND party_number = ?");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            preparedStatement.setInt(3, partyNumber);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                partyMembers.add(resultSet.getString("char_name"));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return partyMembers;
    }


    public List<String> getPartyMembers(String ctaId, long guildId, Integer partyNumber) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<String> partyMembers = new ArrayList<>();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            preparedStatement = connection.prepareStatement("SELECT char_name, status, last_seen FROM users WHERE cta_id = ? AND guild_id = ? AND party_number = ? AND status IN ('absent', 'present') ORDER BY CASE status WHEN 'absent' THEN 1 WHEN 'present' THEN 2 END, char_name");
            preparedStatement.setString(1, ctaId);
            preparedStatement.setLong(2, guildId);
            preparedStatement.setInt(3, partyNumber);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                partyMembers.add(resultSet.getString("char_name") + "|||" + resultSet.getString("status") + "|||" + CTA.getStringFromInstant(resultSet.getTimestamp("last_seen").toInstant()));
            }
        } finally {
            if (resultSet != null) try { resultSet.close(); } catch (SQLException ignore) {}
            if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException ignore) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignore) {}
        }
        return partyMembers;
    }
}