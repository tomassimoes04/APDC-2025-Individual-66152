package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.*;
import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;

import com.google.gson.Gson;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	private static final String MESSAGE_INVALID_CREDENTIALS = "Incorrect username or password.";

	private static final String LOG_MESSAGE_LOGIN_ATTEMPT = "Login attempt by user: ";
	private static final String LOG_MESSAGE_LOGIN_SUCCESSFUL = "Login successful for user: ";
	private static final String LOG_MESSAGE_WRONG_PASSWORD = "Wrong password provided for user: ";
	private static final String LOG_MESSAGE_USER_NOT_FOUND = "Login failed: User not found: ";
	private static final String LOG_MESSAGE_INACTIVE_ACCOUNT = "Login failed: Account inactive or invalid state for user: ";

	private static final String USER_PWD_PROPERTY = "user_pwd";
	private static final String USER_STATE_PROPERTY = "user_state";
	private static final String USER_ROLE_PROPERTY = "user_role";
	private static final String USER_LOGIN_TIME_PROPERTY = "user_login_time";

	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("AuthToken");

	private final Gson g = new Gson();

	public LoginResource() {
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response doLogin(LoginData data) {
		if (data == null || data.username == null || data.password == null || data.username.isBlank() || data.password.isBlank()) {
			LOG.warning("Login failed: Missing username or password in request body.");
			return Response.status(Status.BAD_REQUEST).entity("Missing username or password.").build();
		}

		LOG.info(LOG_MESSAGE_LOGIN_ATTEMPT + data.username);
		Key userKey = userKeyFactory.newKey(data.username);

		try {
			Entity user = datastore.get(userKey);

			if (user != null) {
				LOG.info("User entity found for: " + data.username);
				String hashedPWD_from_DB = user.getString(USER_PWD_PROPERTY);
				String hashedPWD_from_Input = DigestUtils.sha512Hex(data.password);

				LOG.info("DB Hash for " + data.username + ": " + hashedPWD_from_DB);
				LOG.info("Input Hash for " + data.username + ": " + hashedPWD_from_Input);

				if (hashedPWD_from_DB != null && hashedPWD_from_DB.equals(hashedPWD_from_Input)) {
					LOG.info("Password match successful for user: " + data.username);

					String userState = user.contains(USER_STATE_PROPERTY) ? user.getString(USER_STATE_PROPERTY) : "DESATIVADA";
					LOG.info("Account state for " + data.username + ": " + userState);

					if (!"ATIVADA".equalsIgnoreCase(userState)) {
						LOG.warning(LOG_MESSAGE_INACTIVE_ACCOUNT + data.username + " (State: " + userState + ")");
						return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS).build();
					}

					String userRole = user.contains(USER_ROLE_PROPERTY) ? user.getString(USER_ROLE_PROPERTY) : "enduser";
					LOG.info("Account role for " + data.username + ": " + userRole);

					try {
						Entity updatedUser = Entity.newBuilder(user)
								.set(USER_LOGIN_TIME_PROPERTY, Timestamp.now())
								.build();
						datastore.update(updatedUser);
					} catch (DatastoreException e) {
						LOG.log(Level.WARNING, "Non-critical error: Failed to update login time for user " + data.username, e);
					}

					AuthToken token = new AuthToken(data.username, userRole);
					LOG.info("AuthToken created for user: " + data.username + " with TokenID: " + token.tokenID);

					try {
						Key tokenKey = tokenKeyFactory.newKey(token.tokenID);
						Entity tokenEntity = Entity.newBuilder(tokenKey)
								.set("username", token.username)
								.set("role", token.role)
								.set("creationData", token.creationData)
								.set("expirationData", token.expirationData)
								.set("creationTimestamp", Timestamp.of(new java.util.Date(token.creationData)))
								.set("expirationTimestamp", Timestamp.of(new java.util.Date(token.expirationData)))
								.build();
						datastore.put(tokenEntity);
						LOG.info("Token persisted to Datastore for TokenID: " + token.tokenID);
					} catch (DatastoreException e) {
						LOG.log(Level.SEVERE, "Failed to persist token to Datastore for user: " + data.username, e);
					}

					LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + data.username + " (Role: " + userRole + ")");
					return Response.ok(g.toJson(token)).build();

				} else {
					LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.username);
					return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS).build();
				}
			} else {
				LOG.warning(LOG_MESSAGE_USER_NOT_FOUND + data.username);
				return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS).build();
			}
		} catch (DatastoreException e) {
			LOG.log(Level.SEVERE, "Datastore error during login for user: " + data.username, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Datastore error during login.").build();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Unexpected error during login for user: " + data.username, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unexpected error during login.").build();
		}
	}

	@POST
	@Path("/logout")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogout(@HeaderParam("Authorization") String authorizationHeader) {

		LOG.fine("Logout attempt received.");

		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			LOG.warning("Logout failed: Missing or invalid Authorization header format.");
			return Response.status(Status.BAD_REQUEST).entity("Invalid Authorization header.").build();
		}
		String tokenId = authorizationHeader.substring(7).trim();
		if (tokenId.isEmpty()) {
			LOG.warning("Logout failed: Token ID is empty.");
			return Response.status(Status.BAD_REQUEST).entity("Empty token ID.").build();
		}

		LOG.info("Logout attempt for TokenID: " + tokenId);

		try {
			Key tokenKey = tokenKeyFactory.newKey(tokenId);

			datastore.delete(tokenKey);

			LOG.info("Logout successful: Token removed from Datastore for TokenID: " + tokenId);
			return Response.ok().entity("Logout successful.").build();

		} catch (DatastoreException e) {
			LOG.log(Level.SEVERE, "Logout Datastore error for TokenID: " + tokenId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error during logout (Datastore).").build();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Logout Unexpected error for TokenID: " + tokenId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error during logout (Unexpected).").build();
		}
	}
}