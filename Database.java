package edu.lehigh.cse216.admin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;


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

    /**
     * A prepared statement for invalidating a message.  
     */
    private PreparedStatement mInvalidMessage; 

    private PreparedStatement mSelectInvalid;

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

    //---------------------------------------------------------------
    //New prepared statements for the upvote/downvote table
    private PreparedStatement mInsertVote;
    private PreparedStatement mSelectAllVotes;
    private PreparedStatement mSelectOneVote;
    private PreparedStatement mUpdateVote;
    private PreparedStatement mDeleteVote;
    private PreparedStatement mCreateVoteTable;
    private PreparedStatement mDropVoteTable;
    //---------------------------------------------------------------
    //New prepared statements for the comment table
    private PreparedStatement mInsertComment;
    private PreparedStatement mSelectAllComments;
    private PreparedStatement mSelectOneComment;
    private PreparedStatement mUpdateComment;
    private PreparedStatement mDeleteComment;
    private PreparedStatement mCreateCommentTable;
    private PreparedStatement mDropCommentTable;
    private PreparedStatement mInvalidComment;
    private PreparedStatement mSelectInvalidComment;

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

    //---------------------------------------------------------------
    // New inner class VoteData for upvote/downvote table
    public static class VoteData {
        //with id, email, upvote, and downvote
        int mVoteId;
        String mEmail;
        int mUpvote;
        int mDownvote;

        public VoteData(int voteId, String email, int upvote, int downvote) {
            mVoteId = voteId;
            mEmail = email;
            mUpvote = upvote;
            mDownvote = downvote;
        }
    }

    //---------------------------------------------------------------
    // New inner class CommentData for comment table
    public static class CommentData {
        int mCommentId;
        String mEmail;
        String mComment;

        public CommentData(int commentId, String email, String comment) {
            mCommentId = commentId;
            mEmail = email;
            mComment = comment;
        }
    }

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
            mSelectInvalid = mConnection.prepareStatement("SELECT * FROM tbldata WHERE isValid = 0");
            mSelectOne = mConnection.prepareStatement("SELECT * FROM tbldata WHERE id = ?");
            mDeleteOne = mConnection.prepareStatement("DELETE FROM tbldata WHERE id = ?");
            mInsertOne = mConnection.prepareStatement("INSERT INTO tbldata (subject, message) VALUES (?, ?)");
            mUpdateOne = mConnection.prepareStatement("UPDATE tbldata SET message = ? WHERE id = ?");
            mUpdateLike = mConnection.prepareStatement("UPDATE tbldata SET likes = likes + 1 WHERE id = ?");
            mDeleteLike = mConnection.prepareStatement("UPDATE tbldata SET likes = likes - 1 WHERE id = ?");
            mInvalidMessage = mConnection.prepareStatement("UPDATE tbldata SET isValid = isValid - 1 WHERE id = ?"); //NEW LINE HERE ------  
            mCreateTable = mConnection.prepareStatement("CREATE TABLE tbldata (id SERIAL PRIMARY KEY, subject TEXT, message TEXT, likes INTEGER DEFAULT 0, isValid INTEGER DEFAULT 1)");
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
            // New prepared statements for the vote table
            mInsertVote = mConnection.prepareStatement("INSERT INTO tblvote (email, upvote, downvote) VALUES (?, ?, ?)");
            mSelectAllVotes = mConnection.prepareStatement("SELECT * FROM tblvote");
            mSelectOneVote = mConnection.prepareStatement("SELECT * FROM tblvote WHERE vote_id = ?");
            mUpdateVote = mConnection.prepareStatement("UPDATE tblvote SET email = ?, upvote = ?, downvote = ? WHERE vote_id = ?");
            mDeleteVote = mConnection.prepareStatement("DELETE FROM tblvote WHERE vote_id = ?");
            mCreateVoteTable = mConnection.prepareStatement("CREATE TABLE tblvote (vote_id SERIAL PRIMARY KEY, email TEXT, upvote INTEGER, downvote INTEGER)");
            mDropVoteTable = mConnection.prepareStatement("DROP TABLE tblvote");
            //---------------------------------------------------------------
            // New prepared statements for the comment table
            mInsertComment = mConnection.prepareStatement("INSERT INTO tblcomment (email, comment) VALUES (?, ?)");
            mSelectAllComments = mConnection.prepareStatement("SELECT * FROM tblcomment");
            mSelectInvalidComment = mConnection.prepareStatement("SELECT * FROM tblcomment WHERE isValid = 0");
            mSelectOneComment = mConnection.prepareStatement("SELECT * FROM tblcomment WHERE comment_id = ?");
            mUpdateComment = mConnection.prepareStatement("UPDATE tblcomment SET email = ?, comment = ? WHERE comment_id = ?");
            mDeleteComment = mConnection.prepareStatement("DELETE FROM tblcomment WHERE comment_id = ?");
            mInvalidComment = mConnection.prepareStatement("UPDATE tblcomment SET isValid = isValid - 1 WHERE id = ?"); //NEW LINE HERE ------ 
            mCreateCommentTable = mConnection.prepareStatement("CREATE TABLE tblcomment (comment_id SERIAL PRIMARY KEY, email TEXT, comment TEXT, isValid INTEGER DEFAULT 1)");
            mDropCommentTable = mConnection.prepareStatement("DROP TABLE tblcomment");
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
     * Query the database for a list of all rows with an isValid value of 0
     * 
     * @return all invalid rows, as an ArrayList
     */
    ArrayList<RowData> selectAllInvalid() {
        ArrayList<RowData> res = new ArrayList<RowData>();
        try {
            ResultSet rs = mSelectInvalid.executeQuery();
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

        /**
     * Update/Add likes for a row/message in the database
     * 
     * @param mId The id of the row to update
     * 
     * @return The number of rows that were updated. -1 indicates an error.
     */
    int updateLikes(int mId) {
        int res = -1;
        try {
            mUpdateLike.setInt(1, mId); //updating the query and setting the id for that row
                                                       //parameterIndex is the question mark in the query, we are setting the value for that. 1 is the first question mark and 2 is second and so on. 
            res = mUpdateLike.executeUpdate(); //executing the query, executeUpdate() is used for insert, update and delete queries
            System.out.println("updated likes"); //debug
        } catch (SQLException e) {
            System.out.println("Error: Connection.prepareStatement() threw a SQLException"); //debug
            e.printStackTrace();
        }
        return res; //returning the number of rows that were updated
    }

    /**
     * Delete likes for a row/message in the database
     * 
     * @param mId The id of the row to update
     * 
     * @return The number of rows that were updated. -1 indicates an error.
     *
     */
    int deleteLikes(int mId) {
        int res = -1;
        try {
            mDeleteLike.setInt(1, mId); //updating the query and setting the id for that row
            res = mDeleteLike.executeUpdate(); //executing the query, executeUpdate() is used for insert, update and delete queries
            System.out.println("deleted likes"); //debug
        } catch (SQLException e) {
            System.out.println("Error: Connection.prepareStatement() threw a SQLException"); //debug
            e.printStackTrace();
        }
        return res; //returning the number of rows that were updated
    }

    /**
     * Makes a message invalid by changing "isValid" value from 1 to 0
     * 
     * @param mId The id of the row to update
     * 
     * @return The number of rows that were updated. -1 indicates an error.
     *
     */
    int invalidateMessage(int mId) {
        int res = -1;
        try {
            mInvalidMessage.setInt(1,mId);
            res = mInvalidMessage.executeUpdate();
            System.out.println("Made message Invalid");
        } catch(SQLException e) {
            System.out.println("Error: Connect.prepareStatement() threw an SQLException");
            e.printStackTrace();
        }
        return res;
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
     * @return The number of rows that were updated
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
     * @return The number of rows that were deleted
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
     * @return True if the table was created, false if an error occurred
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
     * @return True if the table was dropped, false if an error occurred
     *
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

    //--------------------------------------------------------------
    // New methods for the upvote/downvote table
    /**
     * Insert a new upvote/downvote into the database
     * @param userId
     * @param messageId
     * @param upvote
     * @param downvote
     * @return The number of rows that were inserted
     */
     int insertVote(String email, int upvote, int downvote) {
        int count = 0;
        try {
            mInsertVote.setString(1, email);
            mInsertVote.setInt(2, upvote);
            mInsertVote.setInt(3, downvote);
            count += mInsertVote.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
     }

    /**
     * Query the database for a list of all upvotes/downvotes and their IDs
     *  
     * @return All rows, as an ArrayList
     * 
     */
    ArrayList<VoteData> selectAllVotes() {
        ArrayList<VoteData> res = new ArrayList<VoteData>();
        try {
            ResultSet rs = mSelectAllVotes.executeQuery();
            while (rs.next()) {
                res.add(new VoteData(rs.getInt("vote_id"), rs.getString("email"), rs.getInt("upvote"), rs.getInt("downvote")));
            }
            rs.close();
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get all data for a specific upvote/downvote, by ID
     * 
     * @param voteId The id of the upvote/downvote being requested
     * 
     * @return The data for the requested upvote/downvote, or null if the ID was invalid
     */
    VoteData selectOneVote(int voteId) {
        VoteData res = null;
        try {
            mSelectOneVote.setInt(1, voteId);
            ResultSet rs = mSelectOneVote.executeQuery();
            if (rs.next()) {
                res = new VoteData(rs.getInt("vote_id"), rs.getString("email"), rs.getInt("upvote"), rs.getInt("downvote"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Update the data for an upvote/downvote in the database
     * @param voteId
     * @param email
     * @param upvote
     * @param downvote
     * @return The number of rows that were updated
     */
    int updateVote(int voteId, String email, int upvote, int downvote) {
        int count = 0;
        try {
            mUpdateVote.setString(1, email);
            mUpdateVote.setInt(2, upvote);
            mUpdateVote.setInt(3, downvote);
            mUpdateVote.setInt(4, voteId);
            count += mUpdateVote.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Delete an upvote/downvote from the database
     * @param voteId
     * @return The number of rows that were deleted
     */
    int deleteVote(int voteId) {
        int count = 0;
        try {
            mDeleteVote.setInt(1, voteId); 
            count += mDeleteVote.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Create the upvote/downvote table
     * @return True if the table was created, false if an error occurred
     */
    boolean createVoteTable() {
        try {
            mCreateVoteTable.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Drop the upvote/downvote table
     * @return True if the table was dropped, false if an error occurred
     */

    boolean dropVoteTable() {
        try {
            mDropVoteTable.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    //--------------------------------------------------------------
    // New methods for the comment table

    /**
     * Insert a new comment into the database
     * @param email
     * @param comment
     * @return The number of rows that were inserted
     */
     int insertComment(String email, String comment) {
        int count = 0;
        try {
            mInsertComment.setString(1, email);
            mInsertComment.setString(2, comment);
            count += mInsertComment.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
     }

    /**
     * Query the database for a list of all comments and their IDs
     *  
     * @return All rows, as an ArrayList
     * 
     */
    ArrayList<CommentData> selectAllComments() {
        ArrayList<CommentData> res = new ArrayList<CommentData>();
        try {
            ResultSet rs = mSelectAllComments.executeQuery();
            while (rs.next()) {
                res.add(new CommentData(rs.getInt("comment_id"), rs.getString("email"), rs.getString("comment")));
            }
            rs.close();
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Query the database for a list of all rows with an isValid value of 0
     * 
     * @return all invalid rows, as an ArrayList
     */
    ArrayList<CommentData> selectAllInvalidComments() {
        ArrayList<CommentData> res = new ArrayList<CommentData>();
        try {
            ResultSet rs = mSelectInvalidComment.executeQuery();
            while (rs.next()) {
                res.add(new CommentData(rs.getInt("comment_id"), rs.getString("email"), rs.getString("comment")));
            }
            rs.close();
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get all data for a specific comment, by ID
     * 
     * @param commentId The id of the comment being requested
     * 
     * @return The data for the requested comment, or null if the ID was invalid
     */
    CommentData selectOneComment(int commentId) {
        CommentData res = null;
        try {
            mSelectOneComment.setInt(1, commentId);
            ResultSet rs = mSelectOneComment.executeQuery();
            if (rs.next()) {
                res = new CommentData(rs.getInt("comment_id"), rs.getString("email"), rs.getString("comment"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Update the data for a comment in the database
     * @param commentId
     * @param email
     * @param comment
     * @return The number of rows that were updated
     */
    int updateComment(int commentId, String email, String comment) {
        int count = 0;
        try {
            mUpdateComment.setString(1, email);
            mUpdateComment.setString(2, comment);
            mUpdateComment.setInt(3, commentId);
            count += mUpdateComment.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Delete a comment from the database
     * @param commentId
     * @return The number of rows that were deleted
     */
    int deleteComment(int commentId) {
        int count = 0;
        try {
            mDeleteComment.setInt(1, commentId); 
            count += mDeleteComment.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Makes a comment invalid by changing "isValid" value from 1 to 0
     * 
     * @param mId The id of the row to update
     * 
     * @return The number of rows that were updated. -1 indicates an error.
     *
     */
    int invalidateComment(int mId) {
        int res = -1;
        try {
            mInvalidComment.setInt(1,mId);
            res = mInvalidComment.executeUpdate();
            System.out.println("Made comment Invalid");
        } catch(SQLException e) {
            System.out.println("Error: Connect.prepareStatement() threw an SQLException");
            e.printStackTrace();
        }
        return res;
    }


    /**
     * Create the comment table
     * @return True if the table was created, false if an error occurred
     */
    boolean createCommentTable() {
        try {
            mCreateCommentTable.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Drop the comment table
     * @return True if the table was dropped, false if an error occurred
     */
    boolean dropCommentTable() {
        try {
            mDropCommentTable.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    //---------NOTE: THIS STUFF MIGHT NOT WORK. BUT I HAD TO TRY TO DO THEM TO FUFILL MY REQUIREMENTS. I CANT FULLY FINISH THIS INTO BACKEND DOES IT SO DONT USE THESE FOR NOW  --------

    // Alter the table structure for new deployment without disrupting the current deployment
    public void alterTable(String tableName, String columnName, String columnType) throws SQLException {
        createTable();
        String query = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType;
        mCreateTable.executeUpdate(query);
        mCreateTable.close();
    }

    // Prepopulate the table with test data
    public void prepopulateTable() throws SQLException {
        // Insert test data into the message table
        insertRow("Test Document 1", "This is test document 1.");
        insertRow("Test Document 2", "This is test document 2.");
        insertRow("Test Document 3", "This is test document 3.");

        //Insert test data into the comment table 
        insertComment("rty267@lehigh.edu", "This is test comment 1.");
        insertComment("sub267@lehigh.edu", "This is test comment 2.");
        insertComment("uio267@lehigh.edu", "This is test comment 3.");


        //Insert test data into the user table
        insertUser("Stacy", "stu256@lehigh,edu", "H", "F", "?");
        insertUser("Brady", "brt256@lehigh,edu", "N", "M", "?");
        insertUser("Louis", "lsu256@lehigh,edu", "A", "O", "?");


        //Insert test data into the voting table 
        insertVote("stu256@lehigh.edu", 3, 5);
        insertVote("rtg256@lehigh.edu", 26, 83);
        insertVote("bjk256@lehigh.edu", 1, 300);

    }

    // Remove the least recently accessed uploaded content
    public void removeLeastRecentlyAccessed() throws SQLException {
        createTable();
        String query = "DELETE FROM tbldata WHERE last_accessed = (SELECT MIN(last_accessed) FROM tbldata)";
        mCreateTable.executeUpdate(query);
        mCreateTable.close();
      
    }

    // List documents, their original owners, and the most recent activity on those documents
    public ArrayList<DocumentData> listDocuments() throws SQLException {
        ArrayList<DocumentData> documents = new ArrayList<>();
        createTable();
        String query = "SELECT id, owner, title, last_accessed FROM tablename";
        ResultSet rs = mCreateTable.executeQuery(query);

        while (rs.next()) {
            int id = rs.getInt("id");
            String owner = rs.getString("owner");
            String title = rs.getString("title");
            Timestamp lastAccessed = rs.getTimestamp("last_accessed");
            documents.add(new DocumentData(id, owner, title, lastAccessed));
        }

        rs.close();
        mCreateTable.close();

        return documents;
    }

    // DocumentData class for holding document information
    public static class DocumentData {
        public int mId;
        public String mOwner;
        public String mTitle;
        public Timestamp mLastAccessed;

        public DocumentData(int id, String owner, String title, Timestamp lastAccessed) {
            mId = id;
            mOwner = owner;
            mTitle = title;
            mLastAccessed = lastAccessed;
        }
    }

}
