package edu.lehigh.cse216.anl225.backend;

// Import the Spark package, so that we can make use of the "get" function to 
// create an HTTP GET route
import spark.Spark;

import java.util.Collections;
import java.util.HashMap;
//Hashmap import
import java.util.Map;

// Import Google's JSON library
import com.google.gson.*;

//concurrent hashmap import 
import java.util.concurrent.ConcurrentHashMap;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import java.security.GeneralSecurityException;
import java.util.UUID;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
//This is all extra imports for the google authentication 
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;



/**
 * For now, our app creates an HTTP server that can only get and add data.
 */
public class App {
    private static final String DEFAULT_PORT_DB = "5432";
    private static final int DEFAULT_PORT_SPARK = 4567;
    private static final String GOOGLE_CLIENT_ID = "1095439428573-ld3aj1sang6a4380808vbkiif2gn36cb.apps.googleusercontent.com";
    private static final String GOOGLE_CLIENT_SECRET = "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";

    //Stores the user session that we are attempting to authenticate with google 
    final static ConcurrentHashMap<String, String> userSessions = new ConcurrentHashMap<>();

    /**
     * Get an integer environment variable if it exists, and otherwise return the
     * default value.
     * 
     * @envar The name of the environment variable to get.
     * @defaultVal The integer value to use as the default if envar isn't found
     * 
     * @returns The best answer we could come up with for a value for envar
     */
    static int getIntFromEnv(String envar, int defaultVal) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get(envar) != null) {
            return Integer.parseInt(processBuilder.environment().get(envar));
        }
        return defaultVal;
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
     * Set up CORS headers for the OPTIONS verb, and for every response that the
     * server sends.  This only needs to be called once.
     * 
     * @param origin The server that is allowed to send requests to this server
     * @param methods The allowed HTTP verbs from the above origin
     * @param headers The headers that can be sent with a request from the above
     *                origin
     */

    private static void enableCORS(String origin, String methods, String headers) {
        // Create an OPTIONS route that reports the allowed CORS headers and methods
        Spark.options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });

        // 'before' is a decorator, which will run before any 
        // get/post/put/delete.  In our case, it will put three extra CORS
        // headers into the response
        Spark.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
        });
    }

    /**
     * The main function for the server.  It sets up the routes for the server,
     * and then starts the server.
     * 
     * @param args Command-line arguments.  We ignore these.
     */
    public static void main(String[] args) {

        // Replace "YOUR_CLIENT_ID" with your actual Google client ID
        //String clientId = "1095439428573-ld3aj1sang6a4380808vbkiif2gn36cb.apps.googleusercontent.com";
        // JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        // GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), jsonFactory)
        // .setAudience(Collections.singletonList(clientId))
        // .build();

        // Get the port on which to listen for requests
        Spark.port(getIntFromEnv("PORT", DEFAULT_PORT_SPARK));

        // Set up the location for serving static files. If the STATIC_LOCATION
        // environment variable is set, we will serve from it. Otherwise, serve
        // from "/web"

        String static_location_override = System.getenv("STATIC_LOCATION");
        if (static_location_override == null) {
            Spark.staticFileLocation("/web");
        } else {
            Spark.staticFiles.externalLocation(static_location_override);
        }

        if ("True".equalsIgnoreCase(System.getenv("CORS_ENABLED"))) {
            final String acceptCrossOriginRequestsFrom = "*";
            final String acceptedCrossOriginRoutes = "GET,PUT,POST,DELETE,OPTIONS";
            final String supportedRequestHeaders = "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin";
            enableCORS(acceptCrossOriginRequestsFrom, acceptedCrossOriginRoutes, supportedRequestHeaders);
        }

        // Set up a route for serving the main page
        Spark.get("/", (req, res) -> {
            res.redirect("/index.html");
            return "";
        });
        // Get the database connection
        Database db = getDatabaseConnection();

        // gson provides us with a way to turn JSON into objects, and objects
        // into JSON.
        //
        // NB: it must be final, so that it can be accessed from our lambdas
        //
        // NB: Gson is thread-safe. See
        // https://stackoverflow.com/questions/10380835/is-it-ok-to-use-gson-instance-as-a-static-field-in-a-model-bean-reuse
        final Gson gson = new Gson();

        // dataStore holds all of the data that has been provided via HTTP
        // requests
        //
        // NB: every time we shut down the server, we will lose all data, and
        // every time we start the server, we'll have an empty dataStore,
        // with IDs starting over from 0.
        final DataStore dataStore = new DataStore();

        // GET route that returns all message titles and Ids. All we do is get
        // the data, embed it in a StructuredResponse, turn it into JSON, and
        // return it. If there's no data, we return "[]", so there's no need
        // for error handling.
        Spark.get("/messages", (request, response) -> {
            // ensure status 200 OK, with a MIME type of JSON
            response.status(200);
            response.type("application/json");
            return gson.toJson(new StructuredResponse("ok", null, db.selectAll()));
        });

        // GET route that returns everything for a single row in the DataStore.
        // The ":id" suffix in the first parameter to get() becomes
        // request.params("id"), so that we can get the requested row ID. If
        // ":id" isn't a number, Spark will reply with a status 500 Internal
        // Server Error. Otherwise, we have an integer, and the only possible
        // error is that it doesn't correspond to a row with data.
        Spark.get("/messages/:id", (request, response) -> {
            int idx = Integer.parseInt(request.params("id"));
            // ensure status 200 OK, with a MIME type of JSON
            response.type("application/json");
            DataRow data = db.selectOne(idx);
            if (data == null) {
                response.status(404); // not found
                return gson.toJson(new StructuredResponse("error", idx + " not found", null));
            } else {
                response.status(200);
                return gson.toJson(new StructuredResponse("ok", ""+data.mContent, null));
            }
        });

        // POST route for adding a new element to the DataStore. This will read
        // JSON from the body of the request, turn it into a SimpleRequest
        // object, extract the title and message, insert them, and return the
        // ID of the newly created row.
        Spark.post("/messages", (request, response) -> {
            // NB: if gson.Json fails, Spark will reply with status 500 Internal
            // Server Error
            SimpleRequest req = gson.fromJson(request.body(), SimpleRequest.class);
            response.type("application/json");
            // NB: createEntry checks for null title and message
            int newId = db.insertRow(req.mTitle, req.mMessage);
            if (newId == -1) {
                response.status(500); // internal server error
                return gson.toJson(new StructuredResponse("error", "error performing insertion", null));
            } else {
                response.status(200);
                return gson.toJson(new StructuredResponse("ok", "" + newId, null));
            }
        });

        // ADD LIKES route for adding a new like to the DataStore. This will read
        // JSON from the body of the request, turn it into a SimpleRequest
        // Use the mId from the request to update the likes in the database
        Spark.put("/likes/:id", (request, response) -> {
            SimpleRequest req = gson.fromJson(request.body(), SimpleRequest.class); // should look something like this: {"mId":1}
            response.type("application/json");
            int likeResult = db.updateLikes(req.mId); // update likes and should return 1 because it updated 1 row
            DataRow data = db.selectOne(req.mId); // SOMETHING IS WRONG HERE
            int numLikes = data.mLikes; // get the number of likes from the row, defined in DataRow.java
            if (likeResult == -1) { 
                response.status(500); // internal server errorl
                return gson.toJson(new StructuredResponse("error", "error performing insertion, mId doesn't exist", null)); // error
            } else {
                response.status(200); 
                return gson.toJson(new StructuredResponse("ok, added likes", "Liked Message", null)); // success should look like this: {"mStatus":"ok","mMessage":"1"}
            }
        });


        // UNLIKE route for unliking from the DataStore. This will read
        // JSON from the body of the request, turn it into a SimpleRequest
        // Use the mId from the request to update the likes in the database
        Spark.put("/dislikes/:id", (request, response) -> {
            // NB: if gson.Json fails, Spark will reply with status 500 Internal
            // Server Error
            SimpleRequest req = gson.fromJson(request.body(), SimpleRequest.class);
            response.type("application/json"); 
            int dislikeResult = db.deleteLikes(req.mId); // update likes and should return 1 because it updated 1 row
            DataRow data = db.selectOne(req.mId); 
            int numLikes = data.mLikes;
            if (dislikeResult == -1) {
                response.status(500); // internal server error
                return gson.toJson(new StructuredResponse("error", "error performing insertion, mId doesn't exist", null));
            } else {
                response.status(200);
                return gson.toJson(new StructuredResponse("ok, removed likes", "Unliked Message", null));
            }
        });
        

        // PUT route for updating a row in the DataStore. This is almost
        // exactly the same as POST
        Spark.put("/messages/:id", (request, response) -> {
            // If we can't get an ID or can't parse the JSON, Spark will send
            // a status 500
            int idx = Integer.parseInt(request.params("id"));
            SimpleRequest req = gson.fromJson(request.body(), SimpleRequest.class); 
            response.type("application/json");
            int result = db.updateOne(idx, req.mMessage);
            if (result == -1) {
                response.status(500); // internal server error
                return gson.toJson(new StructuredResponse("error", "unable to update row " + idx, null));
            } else {
                response.status(200);
                return gson.toJson(new StructuredResponse("ok updated message", "" + req.mMessage, null));
            }
        });

        // DELETE route for removing a row from the DataStore
        Spark.delete("/messages/:id", (request, response) -> {
            // If we can't get an ID, Spark will send a status 500
            int idx = Integer.parseInt(request.params("id"));
            response.type("application/json");
            // NB: we won't concern ourselves too much with the quality of the message sent on a successful delete
            int result = db.deleteRow(idx); 
            // NB: we don't check for a result of 0, because we don't care if the row didn't exist in the first place
            if (result == -1) {
                response.status(500); // internal server error
                return gson.toJson(new StructuredResponse("error", "unable to delete row: " + idx, null));
            } else {
                response.status(200);
                return gson.toJson(new StructuredResponse("ok", null, null));
            }
        });

 
        //------------------------NEW STUFF-------------------------------//
        
        //Google authentication function 
        Spark.post("/login", (request, response) -> {
            // Extract the authorization code from the request body
            SimpleRequest req = gson.fromJson(request.body(), SimpleRequest.class);
            String authorizationCode = req.mAuthCode;
        
            // Configure the Google API client
            HttpTransport transport = new NetHttpTransport();
            JsonFactory jsonFactory = new GsonFactory();
            GoogleAuthorizationCodeTokenRequest tokenRequest = new GoogleAuthorizationCodeTokenRequest(
                    transport,
                    jsonFactory,
                    GOOGLE_CLIENT_ID,
                    GOOGLE_CLIENT_SECRET,
                    authorizationCode,
                    "postmessage" // The redirect URI should match the one used in the frontend
            );
        
            // Exchange the authorization code for an access token and ID token
            GoogleTokenResponse tokenResponse = tokenRequest.execute();
            GoogleIdToken idToken = tokenResponse.parseIdToken();
            Payload payload = idToken.getPayload();
        
            // Extract the user information from the payload
            String userId = payload.getSubject();
            String email = payload.getEmail();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");
            String dateOfBirth = (String) payload.get("birthday");
        
            // Extracted information to create or update the user in your database
            Map<String, Object> userData = new HashMap<>();
            userData.put("mKey", userId);
            userData.put("email", email);
            userData.put("firstName", firstName);
            userData.put("lastName", lastName);
            userData.put("birthday", dateOfBirth);
        
            response.status(200);
            response.type("application/json");
            return gson.toJson(new StructuredResponse("ok", null, userData));
        });

        // Google authentication function
        Spark.post("/login", (request, response) -> {
            // Extract the authorization code from the request body
            SimpleRequest req = gson.fromJson(request.body(), SimpleRequest.class);
            String authorizationCode = req.mAuthCode;
        
            // Configure the Google API client
            HttpTransport transport = new NetHttpTransport();
            JsonFactory jsonFactory = new GsonFactory();
            GoogleAuthorizationCodeTokenRequest tokenRequest = new GoogleAuthorizationCodeTokenRequest(
                    transport,
                    jsonFactory,
                    GOOGLE_CLIENT_ID,
                    GOOGLE_CLIENT_SECRET,
                    authorizationCode,
                    "postmessage" // The redirect URI should match the one used in the frontend
            );
        
            // Exchange the authorization code for an access token and ID token
            GoogleTokenResponse tokenResponse = tokenRequest.execute();
            GoogleIdToken idToken = tokenResponse.parseIdToken();
            Payload payload = idToken.getPayload();
        
            // Extract the user information from the payload
            String userId = payload.getSubject();
            String email = payload.getEmail();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");
            String dateOfBirth = (String) payload.get("birthday");

            // Extract the ID token string
            String idTokenString = idToken.toString();

            // Extracted information to create or update the user in your database
            Map<String, Object> userData = new HashMap<>();
            userData.put("mKey", userId);
            userData.put("email", email);
            userData.put("firstName", firstName);
            userData.put("lastName", lastName);
            userData.put("birthday", dateOfBirth);
            userData.put("idToken", idTokenString);

            response.status(200);
            response.type("application/json");
            return gson.toJson(new StructuredResponse("ok", null, userData));
        });


        //This handles adding comments to an idea
        Spark.post("/ideas/:id/comments", (request, response) -> {
            int ideaId = Integer.parseInt(request.params(":id"));
            int userId = getUserIdFromSession(request); // You need to implement this method
            String commentText = request.queryParams("commentText");
        
            int commentId = db.addComment(userId, ideaId, commentText); //this needs to be made 
            response.status(201);
            return gson.toJson(new StructuredResponse("ok", "Comment added", commentId));
        });
       
        //This handles editing comments 
        Spark.put("/comments/:id", (request, response) -> {
            int commentId = Integer.parseInt(request.params(":id"));
            String newCommentText = request.queryParams("commentText");
        
            db.editComment(commentId, newCommentText);
            response.status(200);
            return gson.toJson(new StructuredResponse("ok", "Comment updated", null));
        });
        

        //Retrieves the comments 
        Spark.get("/ideas/:id/comments", (request, response) -> {
            int ideaId = Integer.parseInt(request.params(":id"));
            List<Comment> comments = database.getCommentsForIdea(ideaId);
        
            response.status(200);
            return gson.toJson(new StructuredResponse("ok", "Comments for idea", comments));
        });
        

        //Allows the user to create a profile 
        Spark.post("/api/createprofile", (req, res) -> {
            // Parse the JSON request to obtain necessary information
            SimpleUserRequest userRequest = gson.fromJson(req.body(), SimpleUserRequest.class);
            String userName = userRequest.mUserName;
            String email = userRequest.mEmail;
            String genderIdentity = userRequest.mGI;
            String sexualOrientation = userRequest.mSO;
            String idTokenString = userRequest.mNote; //the google string 
        
            // Verify the ID token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                    .build();
            GoogleIdToken idToken = verifier.verify(idTokenString);
            JsonObject response = new JsonObject();
        
            if (idToken != null) {
                // Get the user data from the idToken
                Payload payload = idToken.getPayload();
                String googleUserId = payload.getSubject();
                String googleEmail = payload.getEmail();
        
                // Check if the email from the idToken matches the provided email
                if (email.equals(googleEmail)) {
                    // Call the insertUser method to create a new user in the database
                    int userId = db.insertUser(googleUserId, email, genderIdentity, sexualOrientation, idTokenString);
        
                    // Prepare the JSON response
                    if (userId > 0) {
                        response.addProperty("success", true);
                        response.addProperty("userId", userId);
                    } else {
                        response.addProperty("success", false);
                        response.addProperty("error", "Failed to create profile");
                    }
                } else {
                    response.addProperty("success", false);
                    response.addProperty("error", "Invalid user data");
                }
            } else {
                response.addProperty("success", false);
                response.addProperty("error", "Invalid ID token");
            }
        
            res.type("application/json");
            return gson.toJson(response);
        });

        
        //Allows the user to edit their profile 
        Spark.post("/api/updateprofile", (req, res) -> {
            // Parse the JSON request to obtain necessary information
            SimpleUserRequest userRequest = gson.fromJson(req.body(), SimpleUserRequest.class);
            int userId = userRequest.mUserId;
            String userName = userRequest.mUserName;
            String email = userRequest.mEmail;
            String genderIdentity = userRequest.mGI;
            String sexualOrientation = userRequest.mSO;
            String idTokenString = userRequest.mNote; //the google string 
        
            // Verify the ID token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                    .build();
            GoogleIdToken idToken = verifier.verify(idTokenString);
            JsonObject response = new JsonObject();
        
            if (idToken != null) {
                // Get the user data from the idToken
                Payload payload = idToken.getPayload();
                String googleUserId = payload.getSubject();
                String googleEmail = payload.getEmail();
        
                // Check if the user data from the idToken matches the provided user data
                if (userId == Integer.parseInt(googleUserId) && email.equals(googleEmail)) {
                    // Call the updateUser method to update the user's information in the database
                    int count = db.updateUser(userId, userName, email, genderIdentity, sexualOrientation, idTokenString);
        
                    // Prepare the JSON response
                    if (count > 0) {
                        response.addProperty("success", true);
                    } else {
                        response.addProperty("success", false);
                        response.addProperty("error", "Update failed");
                    }
                } else {
                    response.addProperty("success", false);
                    response.addProperty("error", "Invalid user data");
                }
            } else {
                response.addProperty("success", false);
                response.addProperty("error", "Invalid ID token");
            }
        
            res.type("application/json");
            return gson.toJson(response);
        });
        

        
        

        /**
         * BACKLOG:
         * addComment - This function would add a comment to a particular idea.
            getComments - This function would retrieve all the comments associated with a particular idea.
            getUserProfile - This function would retrieve a user's profile based on their user ID.
            editComment - This function would allow a user to edit a comment they previously made.
            getIdeaVotes - This function would retrieve the upvotes and downvotes for a particular idea.
            getUserVotes - This function would retrieve the votes (upvotes and downvotes) a particular user has made.
            You may also need to modify your existing functions (likeIdea and dislikeIdea) to follow the new upvote and downvote guidelines.
         */





    }
}
