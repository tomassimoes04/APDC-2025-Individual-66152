package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
// Removido import não utilizado: import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PathParam;
// Removido import não utilizado: import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;
import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

@Path("/register")
public class RegisterResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	// Restaura a inicialização simples do Datastore para rodar na cloud
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	// Removida instância Gson não utilizada aqui
	// private final Gson g = new Gson();


	public RegisterResource() {}	// Default constructor, nothing to do


	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerUser(RegisterData data) {

		// --- INÍCIO DA VALIDAÇÃO DIRETA PARA DEBUG ---
		// Log inicial para ver o que foi recebido (pode precisar do Gson de volta se quiser formatar)
		LOG.info("Received registration attempt for user: " + (data != null ? data.username : "null data object"));

		if (data == null) {
			LOG.warning("Validation failed: Received null data object.");
			return Response.status(Status.BAD_REQUEST).entity("Invalid registration data: No data provided.").build();
		}
		// Função auxiliar para verificar campos (pode estar em RegisterData ou aqui)
		java.util.function.Predicate<String> isInvalid = field -> (field == null || field.isBlank());

		if (isInvalid.test(data.username)) {
			LOG.warning("Validation failed: Username is missing or blank.");
			return Response.status(Status.BAD_REQUEST).entity("Username required.").build();
		}
		if (isInvalid.test(data.password)) {
			LOG.warning("Validation failed: Password is missing or blank. User: " + data.username);
			return Response.status(Status.BAD_REQUEST).entity("Password required.").build();
		}
		if (isInvalid.test(data.confirmation)) {
			LOG.warning("Validation failed: Confirmation is missing or blank. User: " + data.username);
			return Response.status(Status.BAD_REQUEST).entity("Password confirmation required.").build();
		}
		if (isInvalid.test(data.email) || !data.email.contains("@") || !data.email.contains(".")) {
			LOG.warning("Validation failed: Email invalid. User: " + data.username + ", Email: " + data.email);
			return Response.status(Status.BAD_REQUEST).entity("Valid email required.").build();
		}
		if (isInvalid.test(data.name)) {
			LOG.warning("Validation failed: Name is missing or blank. User: " + data.username);
			return Response.status(Status.BAD_REQUEST).entity("Name required.").build();
		}
		if (isInvalid.test(data.telefone)) {
			LOG.warning("Validation failed: Telefone is missing or blank. User: " + data.username);
			return Response.status(Status.BAD_REQUEST).entity("Telephone required.").build();
		}
		if (isInvalid.test(data.profile) || !(data.profile.equalsIgnoreCase("publico") || data.profile.equalsIgnoreCase("privado"))) {
			LOG.warning("Validation failed: Profile invalid. User: " + data.username + ", Profile: " + data.profile);
			return Response.status(Status.BAD_REQUEST).entity("Profile must be 'publico' or 'privado'.").build();
		}
		if (!data.password.equals(data.confirmation)) {
			LOG.warning("Validation failed: Passwords do not match. User: " + data.username);
			return Response.status(Status.BAD_REQUEST).entity("Passwords do not match.").build();
		}

		// Validação de complexidade da password (copiada/adaptada de RegisterData)
		boolean passwordMeetsCriteria;
		{ // Bloco para limitar escopo das variáveis de complexidade
			String password = data.password;
			if (password.length() < 8) {
				passwordMeetsCriteria = false;
				LOG.warning("Password complexity failed: Length < 8. User: " + data.username);
			} else {
				int criteriaMet = 0;
				if (password.matches(".*[A-Z].*")) criteriaMet++;
				if (password.matches(".*[a-z].*")) criteriaMet++;
				if (password.matches(".*\\d.*")) criteriaMet++;
				if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) criteriaMet++;

				passwordMeetsCriteria = criteriaMet >= 3; // Exige pelo menos 3 dos 4 critérios
				if (!passwordMeetsCriteria) {
					LOG.warning("Password complexity failed: Criteria count < 3. User: " + data.username);
				}
			}
		} // Fim do bloco de complexidade

		if (!passwordMeetsCriteria) {
			return Response.status(Status.BAD_REQUEST).entity("Password does not meet complexity requirements.").build();
		}

		LOG.info("Direct validation passed for user: " + data.username);
		// --- FIM DA VALIDAÇÃO DIRETA PARA DEBUG ---


		// Comentada a chamada original à validação, já que a fizemos manualmente acima
		/*
		if (!data.validRegistration()) {
			LOG.warning("Registration validation failed for user: " + data.username);
			return Response.status(Status.BAD_REQUEST).entity("Invalid registration data. Check parameters and password complexity.").build();
		}
		*/

		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = txn.get(userKey);

			if (user != null) {
				txn.rollback();
				LOG.warning("Registration failed: Username " + data.username + " already exists.");
				return Response.status(Status.CONFLICT).entity("Username already exists.").build();
			} else {
				user = Entity.newBuilder(userKey)
						.set("user_name", data.name)
						.set("user_pwd", DigestUtils.sha512Hex(data.password))
						.set("user_email", data.email)
						.set("user_telefone", data.telefone)
						.set("user_profile", data.profile.toLowerCase()) // Guarda em minúsculas
						.set("user_role", "ENDUSER")
						.set("user_state", "DESATIVADA")
						.set("user_creation_time", Timestamp.now())
						.build();

				txn.put(user);
				txn.commit();
				LOG.info("User registered successfully: " + data.username);
				return Response.ok().entity("User registered successfully.").build();
			}
		} catch (DatastoreException e) {
			if (txn.isActive()) txn.rollback(); // Garante rollback
			LOG.log(Level.SEVERE, "Datastore error during registration for user: " + data.username, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Datastore error: " + e.getMessage()).build();
		} catch (Exception e) {
			if (txn.isActive()) txn.rollback(); // Garante rollback
			LOG.log(Level.SEVERE, "Unexpected error during registration for user: " + data.username, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unexpected error: " + e.getMessage()).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				LOG.warning("Transaction was still active in finally block for user: " + data.username + "; rolling back.");
			}
		}
	}
}