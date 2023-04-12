package edu.lehigh.cse216.anl225.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;

public class Database {
    /**
     * The connection to the database.  When there is no connection, it should
     * be null.  Otherwise, there is a valid open connection
     */
    private Connection mConnection;

    /**
     * A prepared statement for getting all data in the database
     */
    private PreparedStatement mSelectAll;

    /**
     * A prepared statement for getting one row from the database
     */
    private PreparedStatement mSelectOne;

    /**
     * A prepared statement for deleting a row from the database
     */
    private PreparedStatement mDeleteOne;

    /**
     * A prepared statement for inserting into the database
     */
    private PreparedStatement mInsertOne;

    /**
    * A prepared statement for updating likes in our database
    */
    private PreparedStatement mUpdateLike;

    /**
     * A prepared statement for deleting likes in our database
     */
    private PreparedStatement mDeleteLike;

    /**
     * A prepared statement for updating a single row in the database
     */
    private PreparedStatement mUpdateOne;

    /**
     * A prepared statement for creating the table in our database
     */
    private PreparedStatement mCreateTable;

    /**
     * A prepared statement for dropping the table in our database
     */
    private PreparedStatement mDropTable;

    //---------------------------------------------------------------
    // New prepared statements for the user table
    private PreparedStatement mInsertUser;
    private PreparedStatement mSelectAllUsers;
    private PreparedStatement mSelectOneUser;
    private PreparedStatement mUpdateUser;
    private PreparedStatement mDeleteUser;
    private PreparedStatement mCreateUserTable;
    private PreparedStatement mDropUserTable;
    //---------------------------------------------------------------

    /**
     * RowData is like a struct in C: we use it to hold data, and we allow 
     * direct access to its fields.  In the context of this Database, RowData 
     * represents the data we'd see in a row.
     * 
     * We make RowData a static class of Database because we don't really want
     * to encourage users to think of RowData as being anything other than an
     * abstract representation of a row of the database.  RowData and the 
     * Database are tightly coupled: if one changes, the other should too.
     */
    public static class RowData {
        /**
         * The ID of this row of the database
         */
        int mId;
        /**
         * The subject stored in this row
         */
        String mSubject;
        /**
         * The message stored in this row
         */
        String mMessage;

        /**
         * Construct a RowData object by providing values for its fields
         */
        public RowData(int id, String subject, String message) {
            mId = id;
            mSubject = subject;
            mMessage = message;
        }
    }

    //---------------------------------------------------------------
    // New inner class UserData for user table
    public static class UserData {
        int mUserId;
        String mUserName;
        String mEmail;
        String mGI;
        String mSO;
        String mNote;

        public UserData(int userId, String userName, String email, String genderIdentity, String sexualOrientation, String note) {
            mUserId = userId;
            mUserName = userName;
            mEmail = email;
            mGI = genderIdentity;
            mSO = sexualOrientation;
            mNote = note;
        }
    }
    //---------------------------------------------------------------

    /**
     * The Database constructor is private: we only create Database objects 
     * through the getDatabase() method.
     */
    Database() {
        
    }

    /**
     * Get a fully-configured connection to the database
     * 
     * @param host The IP address or hostname of the database server
     * @param port The port on the database server to which connection requests
     *             should be sent
     * @param path The path to use, can be null
     * @param user The user ID to use when connecting
     * @param pass The password to use when connecting
     * 
     * @return A Database object, or null if we cannot connect properly
     */
    static Database getDatabase(String host, String port, String path, String user, String pass) {
        if (path == null || "".equals(path))
            path = "/";
            
        // Create an un-configured Database object
        Database db = new Database();

        // Give the Database object a connection, fail if we cannot get one
        try {
            String dbUrl = "jdbc:postgresql://" + host + ':' + port + path;
            Connection conn = DriverManager.getConnection(dbUrl, user, pass);
            if (conn == null) {
                System.err.println("Error: DriverManager.getConnection() returned a null object");
                return null;
            }
            db.mConnection = conn;
        } catch (SQLException e) {
            System.err.println("Error: DriverManager.getConnection() threw a SQLException");
            e.printStackTrace();
            return null;
        }

        db = db.createPreparedStatements();
        return db;
    }

    /**
    * Get a fully-configured connection to the database
    * 
    * @param db_url The url to the database
    * @param port_default port to use if absent in db_url
    * 
    * @return A Database object, or null if we cannot connect properly
    */
    static Database getDatabase(String db_url, String port_default) {
        try {
            URI dbUri = new URI(db_url);
            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String host = dbUri.getHost();
            String path = dbUri.getPath();
            String port = dbUri.getPort() == -1 ? port_default : Integer.toString(dbUri.getPort());
            return getDatabase(host, port, path, username, password);
        } catch (URISyntaxException s) {
            System.out.println("URI Syntax Error");
            return null;
        }
    }

    // The following methods are used to interact with the database
    private Database createPreparedStatements() { // add the table name to the queries
        try {
            mSelectAll = mConnection.prepareStatement("SELECT * FROM tbldata");
            mSelectOne = mConnection.prepareStatement("SELECT * FROM tbldata WHERE id = ?");
            mDeleteOne = mConnection.prepareStatement("DELETE FROM tbldata WHERE id = ?");
            mInsertOne = mConnection.prepareStatement("INSERT INTO tbldata (subject, message) VALUES (?, ?)");
            mUpdateOne = mConnection.prepareStatement("UPDATE tbldata SET message = ? WHERE id = ?");
            mUpdateLike = mConnection.prepareStatement("UPDATE tbldata SET likes = likes + 1 WHERE id = ?");
            mDeleteLike = mConnection.prepareStatement("UPDATE tbldata SET likes = likes - 1 WHERE id = ?");
            mCreateTable = mConnection.prepareStatement("CREATE TABLE tbldata (id SERIAL PRIMARY KEY, subject TEXT, message TEXT, likes INTEGER DEFAULT 0)");
            mDropTable = mConnection.prepareStatement("DROP TABLE tbldata");
            //---------------------------------------------------------------
            // New prepared statements for the user table
            mInsertUser = mConnection.prepareStatement("INSERT INTO tbluser (username, email, gender_identity, sexual_orientation, note) VALUES (?, ?, ?, ?, ?)");
            mSelectAllUsers = mConnection.prepareStatement("SELECT * FROM tbluser");
            mSelectOneUser = mConnection.prepareStatement("SELECT * FROM tbluser WHERE user_id = ?");
            mUpdateUser = mConnection.prepareStatement("UPDATE tbluser SET username = ?, email = ?, gender_identity = ?, sexual_orientation = ?, note = ? WHERE user_id = ?");
            mDeleteUser = mConnection.prepareStatement("DELETE FROM tbluser WHERE user_id = ?");
            mCreateUserTable = mConnection.prepareStatement("CREATE TABLE tbluser (user_id SERIAL PRIMARY KEY, username TEXT, email TEXT, gender_identity TEXT, sexual_orientation TEXT, note TEXT)");
            mDropUserTable = mConnection.prepareStatement("DROP TABLE tbluser");
            //---------------------------------------------------------------
        } catch (SQLException e) {
            System.err.println("Error: Connection.prepareStatement() threw a SQLException");
            e.printStackTrace();
            return null;
        }
        return this;
    }


    /**
     * Close the current connection to the database, if one exists.
     * 
     * NB: The connection will always be null after this call, even if an 
     *     error occurred during the closing operation.
     * 
     * @return True if the connection was cleanly closed, false otherwise
     */
    boolean disconnect() {
        if (mConnection == null) {
            System.err.println("Unable to close connection: Connection was null");
            return false;
        }
        try {
            mConnection.close();
        } catch (SQLException e) {
            System.err.println("Error: Connection.close() threw a SQLException");
            e.printStackTrace();
            mConnection = null;
            return false;
        }
        mConnection = null;
        return true;
    }

    /**
     * Insert a row into the database
     * 
     * @param subject The subject for this new row
     * @param message The message body for this new row
     * 
     * @return The number of rows that were inserted
     */
    int insertRow(String subject, String message) {
        int count = 0;
        try {
            mInsertOne.setString(1, subject);
            mInsertOne.setString(2, message);
            count += mInsertOne.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Query the database for a list of all subjects and their IDs
     * 
     * @return All rows, as an ArrayList
     */
    ArrayList<RowData> selectAll() {
        ArrayList<RowData> res = new ArrayList<RowData>();
        try {
            ResultSet rs = mSelectAll.executeQuery();
            while (rs.next()) {
                res.add(new RowData(rs.getInt("id"), rs.getString("subject"), null));
            }
            rs.close();
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get all data for a specific row, by ID
     * 
     * @param id The id of the row being requested
     * 
     * @return The data for the requested row, or null if the ID was invalid
     */
    RowData selectOne(int id) {
        RowData res = null;
        try {
            mSelectOne.setInt(1, id);
            ResultSet rs = mSelectOne.executeQuery();
            if (rs.next()) {
                res = new RowData(rs.getInt("id"), rs.getString("subject"), rs.getString("message"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Delete a row by ID
     * 
     * @param id The id of the row to delete
     * 
     * @return The number of rows that were deleted.  -1 indicates an error.
     */
    int deleteRow(int id) {
        int res = -1;
        try {
            mDeleteOne.setInt(1, id);
            res = mDeleteOne.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Update the message for a row in the database
     * 
     * @param id The id of the row to update
     * @param message The new message contents
     * 
     * @return The number of rows that were updated.  -1 indicates an error.
     */
    int updateOne(int id, String message) {
        int res = -1;
        try {
            mUpdateOne.setString(1, message);
            mUpdateOne.setInt(2, id);
            res = mUpdateOne.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Create tblData.  If it already exists, this will print an error
     */
    void createTable() {
        try {
            mCreateTable.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove tblData from the database.  If it does not exist, this will print
     * an error.
     */
    void dropTable() {
        try {
            mDropTable.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //--------------------------------------------------------------
    // New methods for the user table
    /**
     * Insert a new user into the database
     * @param userName
     * @param email
     * @param genderIdentity
     * @param sexualOrientation
     * @param note
     * @return
     */
    int insertUser(String userName, String email, String genderIdentity, String sexualOrientation, String note) {
        int count = 0;
        try {
            mInsertUser.setString(1, userName);
            mInsertUser.setString(2, email);
            mInsertUser.setString(3, genderIdentity);
            mInsertUser.setString(4, sexualOrientation);
            mInsertUser.setString(5, note);
            count += mInsertUser.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Query the database for a list of all users and their IDs
     * 
     * @return All rows, as an ArrayList
     *
     */
    ArrayList<UserData> selectAllUsers() {
        ArrayList<UserData> res = new ArrayList<UserData>();
        try {
            ResultSet rs = mSelectAllUsers.executeQuery();
            while (rs.next()) {
                res.add(new UserData(rs.getInt("user_id"), rs.getString("username"), rs.getString("email"), rs.getString("gender_identity"), rs.getString("sexual_orientation"), rs.getString("note")));
            }
            rs.close();
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get all data for a specific user, by ID
     * 
     * @param userId The id of the user being requested
     * 
     * @return The data for the requested user, or null if the ID was invalid
     */
    UserData selectOneUser(int userId) {
        UserData res = null;
        try {
            mSelectOneUser.setInt(1, userId);
            ResultSet rs = mSelectOneUser.executeQuery();
            if (rs.next()) {
                res = new UserData(rs.getInt("user_id"), rs.getString("username"), rs.getString("email"), rs.getString("gender_identity"), rs.getString("sexual_orientation"), rs.getString("note"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Update the data for a user in the database
     * @param userId
     * @param userName
     * @param email
     * @param genderIdentity
     * @param sexualOrientation
     * @param note
     * @return
     */
    int updateUser(int userId, String userName, String email, String genderIdentity, String sexualOrientation, String note) {
        int count = 0;
        try {
            mUpdateUser.setString(1, userName);
            mUpdateUser.setString(2, email);
            mUpdateUser.setString(3, genderIdentity);
            mUpdateUser.setString(4, sexualOrientation);
            mUpdateUser.setString(5, note);
            mUpdateUser.setInt(6, userId);
            count += mUpdateUser.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Delete a user from the database
     * @param userId
     * @return
     */
    int deleteUser(int userId) {
        int count = 0;
        try {
            mDeleteUser.setInt(1, userId);
            count += mDeleteUser.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Create the user table
     * @return
     */
    boolean createUserTable() {
        try {
            mCreateUserTable.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Drop the user table
     * @return
     */
    boolean dropUserTable() {
        try {
            mDropUserTable.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    //--------------------------------------------------------------
}