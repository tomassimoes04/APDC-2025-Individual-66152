package pt.unl.fct.di.apdc.firstwebapp.resources;

// Logging, Collections, Datastore, JAX-RS, Gson, Utils imports
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;
import java.util.Date; // Para converter Long para Date para Timestamp
// Datastore
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
// JAX-RS
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
// Gson
import com.google.gson.Gson;
// Util DTOs e AuthToken
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.WorkSheetData;
import pt.unl.fct.di.apdc.firstwebapp.util.UpdateWorkStateData;


@Path("/worksheet")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class WorkSheetResource {

    private static final Logger LOG = Logger.getLogger(WorkSheetResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory workSheetKeyFactory = datastore.newKeyFactory().setKind("WorkSheet");
    private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("AuthToken");
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    // Constantes para propriedades da entidade WorkSheet (mantidas como antes)
    private static final String WS_REF = "ws_ref";
    private static final String WS_DESC = "ws_desc";
    private static final String WS_TARGET_TYPE = "ws_target_type";
    private static final String WS_ADJUDICATION_STATE = "ws_adjudication_state";
    private static final String WS_ADJUDICATION_DATE = "ws_adjudication_date";
    private static final String WS_START_DATE = "ws_start_date";
    private static final String WS_END_DATE = "ws_end_date";
    private static final String WS_PARTNER_ACCOUNT = "ws_partner_account";
    private static final String WS_ENTITY_NAME = "ws_entity_name";
    private static final String WS_ENTITY_NIF = "ws_entity_nif";
    private static final String WS_WORK_STATE = "ws_work_state";
    private static final String WS_OBSERVATIONS = "ws_observations";
    private static final String WS_CREATION_TIME = "ws_creation_time";
    private static final String WS_LAST_UPDATE_TIME = "ws_last_update_time";
    // Constantes de User
    private static final String USER_ROLE_PROPERTY = "user_role";

    private final Gson g = new Gson();

    public WorkSheetResource() {}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveWorkSheet(@HeaderParam("Authorization") String authorizationHeader, WorkSheetData data) {
        // Código do saveWorkSheet como estava antes (já corrigido)
        // ... (valida token, permissão, input) ...
        AuthToken token = validateToken(authorizationHeader);
        if (token == null) { return Response.status(Status.FORBIDDEN).entity("Invalid/Expired token.").build(); }
        LOG.info("saveWorkSheet attempt by: " + token.username + "..."); // Simplificado
        if (!token.role.equals("BACKOFFICE") && !token.role.equals("ADMIN")) { /*...*/ return Response.status(Status.FORBIDDEN).build(); } // Simplificado
        if (data == null || !data.isValid()) { /*...*/ return Response.status(Status.BAD_REQUEST).build(); } // Simplificado
        String estadoAdjUpper = data.estadoAdjudicacao.toUpperCase();
        String tipoAlvoUpper = data.tipoAlvo.toUpperCase();
        Timestamp now = Timestamp.now();

        Transaction txn = datastore.newTransaction();
        try {
            Key workSheetKey = workSheetKeyFactory.newKey(data.referencia);
            Entity existingWorkSheet = txn.get(workSheetKey);
            Entity.Builder builder;
            boolean isCreating = (existingWorkSheet == null);

            if (isCreating) { /*...*/ builder = Entity.newBuilder(workSheetKey).set(WS_CREATION_TIME, now); }
            else { /*...*/ builder = Entity.newBuilder(existingWorkSheet); }

            // Set base fields
            builder.set(WS_REF, data.referencia);
            builder.set(WS_DESC, data.descricao);
            builder.set(WS_TARGET_TYPE, tipoAlvoUpper);
            builder.set(WS_ADJUDICATION_STATE, estadoAdjUpper);
            if (data.observacoes != null) { builder.set(WS_OBSERVATIONS, data.observacoes); }
            else if (!isCreating && existingWorkSheet.contains(WS_OBSERVATIONS)) { builder.remove(WS_OBSERVATIONS); }

            // Set adjudication fields or clear them
            if (estadoAdjUpper.equals("ADJUDICADO")) {
                // ... (set WS_ADJUDICATION_DATE, WS_START_DATE, etc. using Timestamp.of(new Date(...))) ...
                builder.set(WS_ADJUDICATION_DATE, Timestamp.of(new Date(data.dataAdjudicacao)));
                builder.set(WS_START_DATE, Timestamp.of(new Date(data.dataInicioPrevista)));
                builder.set(WS_END_DATE, Timestamp.of(new Date(data.dataFimPrevista)));
                builder.set(WS_PARTNER_ACCOUNT, data.contaEntidade);
                builder.set(WS_ENTITY_NAME, data.nomeEmpresa);
                builder.set(WS_ENTITY_NIF, data.nifEmpresa);
                // Set initial work state correctly
                boolean needsInitialWorkState = isCreating || !existingWorkSheet.contains(WS_WORK_STATE);
                if (needsInitialWorkState) { builder.set(WS_WORK_STATE, "NÃO INICIADO"); }
            } else {
                // Clear fields
                builder.remove(WS_ADJUDICATION_DATE);
                builder.remove(WS_START_DATE);
                builder.remove(WS_END_DATE);
                builder.remove(WS_PARTNER_ACCOUNT);
                builder.remove(WS_ENTITY_NAME);
                builder.remove(WS_ENTITY_NIF);
                builder.remove(WS_WORK_STATE);
            }
            builder.set(WS_LAST_UPDATE_TIME, now);
            txn.put(builder.build());
            txn.commit();
            String action = isCreating ? "created" : "updated";
            return Response.ok().entity("Worksheet " + action + " successfully.").build();
        } catch (Exception e) { if (txn.isActive()) txn.rollback(); LOG.log(Level.SEVERE, "saveWorkSheet Error", e); return Response.status(Status.INTERNAL_SERVER_ERROR).build(); } // Simplificado
        finally { if (txn.isActive()) { txn.rollback(); } }
    }


    // --- MÉTODO: updateWorkState CORRIGIDO ---
    @POST
    @Path("/updatestate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateWorkState(@HeaderParam("Authorization") String authorizationHeader, UpdateWorkStateData data) {

        AuthToken token = validateToken(authorizationHeader);
        if (token == null) { return Response.status(Status.FORBIDDEN).entity("Invalid/Expired token.").build(); }

        LOG.info("updateWorkState attempt by: " + token.username + " (Role: " + token.role + ") for ref: " + (data != null ? data.referencia : "null"));

        if (!token.role.equals("PARTNER")) {
            LOG.warning("updateWorkState failed: Permission denied for user " + token.username + " (Role: " + token.role + ")");
            return Response.status(Status.FORBIDDEN).entity("Only PARTNER users can update work state.").build();
        }

        if (data == null || !data.isValid()) {
            LOG.warning("updateWorkState failed: Invalid input data. User: " + token.username);
            return Response.status(Status.BAD_REQUEST).entity("Invalid input data. Provide referencia and newWorkState (NÃO INICIADO, EM CURSO, CONCLUÍDO).").build();
        }
        String newWorkStateUpper = data.newWorkState.toUpperCase();

        Transaction txn = datastore.newTransaction();
        try {
            Key workSheetKey = workSheetKeyFactory.newKey(data.referencia);
            Entity workSheet = txn.get(workSheetKey);

            if (workSheet == null) {
                txn.rollback();
                LOG.warning("updateWorkState failed: Worksheet " + data.referencia + " not found. Requested by: " + token.username);
                return Response.status(Status.NOT_FOUND).entity("Worksheet not found.").build();
            }


            String adjudicationState = workSheet.contains(WS_ADJUDICATION_STATE) ? workSheet.getString(WS_ADJUDICATION_STATE) : "NÃO ADJUDICADO"; // Default seguro
            if (!adjudicationState.equals("ADJUDICADO")) {
                txn.rollback();
                LOG.warning("updateWorkState failed: Worksheet " + data.referencia + " is not adjudicated (State: "+adjudicationState+"). Requested by: " + token.username);

                return Response.status(Status.BAD_REQUEST).entity("Worksheet is not adjudicated.").build();
            }



            String assignedPartner = workSheet.contains(WS_PARTNER_ACCOUNT) ? workSheet.getString(WS_PARTNER_ACCOUNT) : null;

            if (assignedPartner == null || !assignedPartner.equals(token.username)) {
                txn.rollback();
                LOG.warning("updateWorkState failed: User " + token.username + " is not the assigned partner for worksheet " + data.referencia + " (Assigned: " + assignedPartner + ")");
                // Retorna 403 FORBIDDEN se não for o parceiro correto
                return Response.status(Status.FORBIDDEN).entity("User is not the assigned partner for this worksheet.").build();
            }


            // Atualiza o estado da obra
            Entity updatedWorkSheet = Entity.newBuilder(workSheet)
                    .set(WS_WORK_STATE, newWorkStateUpper)
                    .set(WS_LAST_UPDATE_TIME, Timestamp.now())
                    .build();
            txn.put(updatedWorkSheet);
            txn.commit();

            LOG.info("User " + token.username + " successfully updated work state for worksheet " + data.referencia + " to " + newWorkStateUpper);
            return Response.ok().entity("Worksheet state updated successfully.").build();

        } catch (DatastoreException e) { // Catch específico para Datastore
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "updateWorkState Datastore error for ref " + data.referencia + " by " + token.username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error updating work state (Datastore).").build();
        } catch (Exception e) { // Catch genérico
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "updateWorkState Unexpected error for ref " + data.referencia + " by " + token.username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error updating work state (Unexpected).").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }



    private AuthToken validateToken(String authorizationHeader) {
        // ... (Implementação EXATAMENTE igual à da UserResource) ...
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) { return null; }
        String tokenId = authorizationHeader.substring(7).trim();
        if (tokenId.isEmpty()) { return null; }
        LOG.fine("Validating TokenID (Worksheet): " + tokenId);
        try {
            Key tokenKey = tokenKeyFactory.newKey(tokenId);
            Entity tokenEntity = datastore.get(tokenKey);
            if (tokenEntity == null) { LOG.warning("Token not found (Worksheet): " + tokenId); return null; }
            AuthToken token = new AuthToken();
            token.tokenID = tokenId;
            token.username = tokenEntity.contains("username") ? tokenEntity.getString("username") : null;
            token.role = tokenEntity.contains("role") ? tokenEntity.getString("role") : null;
            token.creationData = tokenEntity.contains("creationData") ? tokenEntity.getLong("creationData") : 0L;
            token.expirationData = tokenEntity.contains("expirationData") ? tokenEntity.getLong("expirationData") : 0L;
            if (token.username == null || token.role == null || token.expirationData == 0L) { LOG.severe("Token missing data (Worksheet): " + tokenId); return null; }
            if (System.currentTimeMillis() > token.expirationData) { LOG.warning("Token expired (Worksheet): " + tokenId); return null; }
            LOG.info("Token validated (Worksheet) for user: " + token.username);
            return token;
        } catch (Exception e) { LOG.log(Level.SEVERE, "Token validation error (Worksheet): " + tokenId, e); return null; }
    }

}