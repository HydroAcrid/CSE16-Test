package edu.lehigh.cse216.admin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Map;

import edu.lehigh.cse216.admin.Database.UserData;
import edu.lehigh.cse216.admin.Database.VoteData;
import edu.lehigh.cse216.admin.Database.CommentData;

/**
 * App is our basic admin app.  For now, it is a demonstration of the six key 
 * operations on a database: connect, insert, update, query, delete, disconnect
 */
public class App {

    private static final String DEFAULT_PORT_DB = "5432";
    private static final int DEFAULT_PORT_SPARK = 4567;

    /**
     * Print the menu for our program
     */
    static void menu() {
        System.out.println("Main Menu");
        System.out.println("--------------------");
        System.out.println("  [T] Create tblData for messages");
        System.out.println("  [D] Drop tblData");
        System.out.println("  [#] Query for a specific row");
        System.out.println("  [*] Query for all rows");
        System.out.println("  [+] Insert a new row");
        System.out.println("  [-] Delete a row");
        System.out.println("  [~] Update a row");
        System.out.println("--------------------");
        System.out.println("  [U] Create user table for storing users' information");
        System.out.println("  [C] Drop user table");
        System.out.println("  [S] Query for a specific user");
        System.out.println("  [R] List all users");
        System.out.println("  [A] Add a new user");
        System.out.println("  [X] Delete a user");
        System.out.println("  [M] Update a user");
        System.out.println("--------------------");
        System.out.println("  [G] Create vote table for storing votes");
        System.out.println("  [H] Drop vote table");
        System.out.println("  [I] Query for a specific vote");
        System.out.println("  [J] List all votes");
        System.out.println("  [K] Add a new vote");
        System.out.println("  [L] Delete a vote");
        System.out.println("  [N] Update a vote");
        System.out.println("--------------------");
        System.out.println("  [1] Create comment table for storing comments");
        System.out.println("  [2] Drop comment table");
        System.out.println("  [3] Query for a specific comment");
        System.out.println("  [4] List all comments");
        System.out.println("  [5] Add a new comment");
        System.out.println("  [6] Delete a comment");
        System.out.println("  [7] Update a comment");
        System.out.println("--------------------");
        System.out.println("  [?] Help (this message)");
        System.out.println("  [q] Quit");
    }

    /**
     * Ask the user to enter a menu option; repeat until we get a valid option
     * 
     * @param in A BufferedReader, for reading from the keyboard
     * 
     * @return The character corresponding to the chosen menu option
     */
    static char prompt(BufferedReader in) {
        // The valid actions (basically what a user can type in):
        String actions = "TD#*-+~q?UuCrRaAsSMmXxGgHhIiJjKkLlNn1234567";
    

        // We repeat until a valid single-character option is selected        
        while (true) {
            System.out.print("[" + actions + "] :> ");
            System.out.println("Enter '?' to bring up the menu");
            String action;
            try {
                action = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            if (action.length() != 1)
                continue;
            if (actions.contains(action)) {
                return action.charAt(0);
            }
            System.out.println("Invalid Command");
        }
    }

    /**
     * Ask the user to enter a String message
     * 
     * @param in A BufferedReader, for reading from the keyboard
     * @param message A message to display when asking for input
     * 
     * @return The string that the user provided.  May be "".
     */
    static String getString(BufferedReader in, String message, int max) {
        String s;
        try {
            System.out.print(message + " :> ");
            s = in.readLine();
            if (s.length() >= max) {
                System.out.println("Message was too large, length of " + s.length() + " exceeds " + max + " character limit");
                return getString(in, message, max);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return s;
    }

    /**
     * Ask the user to enter an integer
     * 
     * @param in A BufferedReader, for reading from the keyboard
     * @param message A message to display when asking for input
     * 
     * @return The integer that the user provided.  On error, it will be -1
     */
    static int getInt(BufferedReader in, String message) {
        int i = -1;
        try {
            System.out.print(message + " :> ");
            i = Integer.parseInt(in.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * Get a fully-configured connection to the database, or exit immediately
     * Uses the Postgres configuration from environment variables
     * 
     * NB: now when we shutdown the server, we no longer lose all data
     * 
     * @return null on failure, otherwise configured database object
     */

     private static Database getDatabaseConnection() {

        if (System.getenv("DATABASE_URL") != null)
            return Database.getDatabase(System.getenv("DATABASE_URL"), DEFAULT_PORT_DB);

        Map<String, String> env = System.getenv();

        String ip = env.get("POSTGRES_IP");
        String port = env.get("POSTGRES_PORT");
        String user = env.get("POSTGRES_USER");
        String pass = env.get("POSTGRES_PASS");

        return Database.getDatabase(ip, port, "", user, pass);
    }

    /**
     * The main routine runs a loop that gets a request from the user and
     * processes it
     * 
     * @param argv Command-line options.  Ignored by this program.
     */
    public static void main(String[] argv) {
        // get the Postgres configuration from the environment
        Map<String, String> env = System.getenv();
        String ip = env.get("POSTGRES_IP");
        String port = env.get("POSTGRES_PORT");
        String user = env.get("POSTGRES_USER");
        String pass = env.get("POSTGRES_PASS");

        // Get the database connection
        Database db = getDatabaseConnection();

        // Start our basic command-line interpreter:
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            // Get the user's request, and do it
            //
            // NB: for better testability, each action should be a separate
            //     function call
            char action = prompt(in);
            if (action == '?') { // Help
                menu();
            } else if (action == 'q' || action == 'Q') { // Quit
                break;
            } else if (action == 'T' || action == 't') {
                db.createTable();
            } else if (action == 'D' || action == 'd') {
                db.dropTable();
            } else if (action == '#') {
                int id = getInt(in, "Enter the row ID");
                if (id == -1)
                    continue;
                Database.RowData res = db.selectOne(id);
                if (res != null) {
                    System.out.println("  [" + res.mId + "] " + res.mSubject);
                    System.out.println("  --> " + res.mMessage);
                }
            } else if (action == '*') {
                ArrayList<Database.RowData> res = db.selectAll();
                if (res == null)
                    continue;
                System.out.println("  Current Database Contents");
                System.out.println("  -------------------------");
                for (Database.RowData rd : res) {
                    System.out.println("  [" + rd.mId + "] " + rd.mSubject);
                }
            } else if (action == '-') {
                int id = getInt(in, "Enter the row ID");
                if (id == -1)
                    continue;
                int res = db.deleteRow(id);
                if (res == -1)
                    continue;
                System.out.println("  " + res + " rows deleted");
            } else if (action == '+') {
                String subject = getString(in, "Enter the subject", 24);
                String message = getString(in, "Enter the message", 1024);
                if (subject.equals("") || message.equals(""))
                    continue;
                int res = db.insertRow(subject, message);
                System.out.println(res + " rows added");
            } else if (action == '~') {
                int id = getInt(in, "Enter the row ID :> ");
                if (id == -1)
                    continue;
                String newMessage = getString(in, "Enter the new message", 1024);
                int res = db.updateOne(id, newMessage);
                if (res == -1)
                    continue;
                System.out.println("  " + res + " rows updated");
            } else if (action == 'U' || action == 'u') {
                db.createUserTable();
            } else if (action == 'C' || action == 'c') {
                db.dropUserTable();
            } else if (action == 'R' || action == 'r') {
                ArrayList<UserData> res = db.selectAllUsers();
                if (res == null)
                    continue;
                System.out.println("  Current User Table Contents");
                System.out.println("  ---------------------------");
                for (UserData ud : res) {
                    System.out.println("  [" + ud.mUserId + "] " + ud.mUserName + " | " + ud.mEmail + " | " + ud.mGI + " | " + ud.mSO + " | " + ud.mNote);
                }
            } else if (action == 'A' || action == 'a') {
                String userName = getString(in, "Enter the user name", 50);
                String email = getString(in, "Enter the email", 100);
                String genderIdentity = getString(in, "Enter the gender identity", 50);
                String sexualOrientation = getString(in, "Enter the sexual orientation", 50);
                String note = getString(in, "Enter a note", 500);
                if (userName.equals("") || email.equals("") || genderIdentity.equals("") || sexualOrientation.equals(""))
                    continue;
                int res = db.insertUser(userName, email, genderIdentity, sexualOrientation, note);
                System.out.println(res + " users added");
            }
            else if (action == 'S' || action == 's') {
                int userId = getInt(in, "Enter the user ID");
                if (userId == -1)
                    continue;
                UserData res = db.selectOneUser(userId);
                if (res != null) {
                    System.out.println("  [" + res.mUserId + "] " + res.mUserName + " | " + res.mEmail + " | " + res.mGI + " | " + res.mSO + " | " + res.mNote);
                }
            } else if (action == 'M' || action == 'm') {
                int userId = getInt(in, "Enter the user ID");
                if (userId == -1)
                    continue;
                String newUserName = getString(in, "Enter the new username", 24);
                String newEmail = getString(in, "Enter the new email", 64);
                String newGenderIdentity = getString(in, "Enter the new gender identity", 24);
                String newSexualOrientation = getString(in, "Enter the new sexual orientation", 24);
                String newNote = getString(in, "Enter the new note", 1024);
                int res = db.updateUser(userId, newUserName, newEmail, newGenderIdentity, newSexualOrientation, newNote);
                if (res == -1)
                    continue;
                System.out.println("  " + res + " users updated");
            } else if (action == 'X' || action == 'x') {
                int userId = getInt(in, "Enter the user ID");
                if (userId == -1)
                    continue;
                int res = db.deleteUser(userId);
                if (res == -1)
                    continue;
                System.out.println("  " + res + " users deleted");
            } else if (action == 'G' || action == 'g') { // Create the vote table
                db.createVoteTable();
            } else if (action == 'H' || action == 'h') { // Drop the vote table
                db.dropVoteTable();
            } else if (action == 'I' || action == 'i') { // Query for a specific vote
                int voteId = getInt(in, "Enter the vote ID");
                if (voteId == -1)
                    continue;
                VoteData res = db.selectOneVote(voteId);
                if (res != null) {
                    System.out.println("  [" + res.mVoteId + "] " + res.mEmail + " | " + res.mUpvote + " | " + res.mDownvote);
                }
            } else if (action == 'J' || action == 'j') { // List all votes
                ArrayList<VoteData> res = db.selectAllVotes();
                if (res == null)
                    continue;
                System.out.println("  Current Vote Table Contents");
                System.out.println("  ---------------------------");
                for (VoteData vd : res) {
                    System.out.println("  [" + vd.mVoteId + "] " + vd.mEmail + " | " + vd.mUpvote + " | " + vd.mDownvote);
                }
            } else if (action == 'K' || action == 'k') { // Add a vote
                String email = getString(in, "Enter the email", 100);
                int upvote = getInt(in, "Enter the upvote");
                int downvote = getInt(in, "Enter the downvote");
                if (email.equals("") || upvote == -1 || downvote == -1) // If any of the inputs are invalid, skip this iteration
                    continue;
                int res = db.insertVote(email, upvote, downvote);
                System.out.println(res + " votes added");
            } else if (action == 'L' || action == 'l') { // Delete a vote
                int voteId = getInt(in, "Enter the vote ID");
                if (voteId == -1) // If the input is invalid, skip this iteration
                    continue;
                int res = db.deleteVote(voteId);
                if (res == -1)
                    continue;
                System.out.println("  " + res + " votes deleted");
            } else if (action == 'N' || action == 'n') { // Update a vote
                int voteId = getInt(in, "Enter the vote ID");
                if (voteId == -1) // If the input is invalid, skip this iteration
                    continue;
                String newEmail = getString(in, "Enter the new email", 100);
                int newUpvote = getInt(in, "Enter the new upvote");
                int newDownvote = getInt(in, "Enter the new downvote");
                int res = db.updateVote(voteId, newEmail, newUpvote, newDownvote);
                if (res == -1)
                    continue;
                System.out.println("  " + res + " votes updated");
            } else if (action == '1') {
                // Create the comment table
                db.createCommentTable();
            } else if (action == '2') {
                // Drop the comment table
                db.dropCommentTable();
            } else if (action == '3') {
                // Query for a specific comment
                int commentId = getInt(in, "Enter the comment ID");
                if (commentId == -1)
                    continue;
                CommentData res = db.selectOneComment(commentId);
                if (res != null) {
                    System.out.println("  [" + res.mCommentId + "] " + res.mEmail + " | " + res.mComment);
                }
            } else if (action == '4') {
                // List all comments
                ArrayList<CommentData> res = db.selectAllComments();
                if (res == null)
                    continue;
                System.out.println("  Current Comment Table Contents");
                System.out.println("  ---------------------------");
                for (CommentData cd : res) {
                    System.out.println("  [" + cd.mCommentId + "] " + cd.mEmail + " | " + cd.mComment);
                }
            } else if (action == '5') {
                // Add a comment
                String email = getString(in, "Enter the email", 100);
                String comment = getString(in, "Enter the comment", 1000);
                if (email.equals("") || comment.equals("")) // If any of the inputs are invalid, skip this iteration
                    continue;
                int res = db.insertComment(email, comment);
                System.out.println(res + " comments added");
            } else if (action == '6') {
                // Delete a comment
                int commentId = getInt(in, "Enter the comment ID");
                if (commentId == -1) // If the input is invalid, skip this iteration
                    continue;
                int res = db.deleteComment(commentId);
                if (res == -1)
                    continue;
                System.out.println("  " + res + " comments deleted");
            } else if (action == '7') {
                // Update a comment
                int commentId = getInt(in, "Enter the comment ID");
                if (commentId == -1) // If the input is invalid, skip this iteration
                    continue;
                String newEmail = getString(in, "Enter the new email", 100);
                String newComment = getString(in, "Enter the new comment", 1000);
                int res = db.updateComment(commentId, newEmail, newComment);
                if (res == -1)
                    continue;
                System.out.println("  " + res + " comments updated");
            } else {
                System.out.println("  Invalid action");
            }
        }
        // Always remember to disconnect from the database when the program 
        // exits
        db.disconnect();
    }
}
