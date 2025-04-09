package pt.unl.fct.di.apdc.firstwebapp.resources;

// Java Util Logging
import java.util.logging.Level;
import java.util.logging.Logger;

// Java Util Collections & Maps
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set; // Necessário para validação em ChangeRoleData (se fizeres lá) ou lógica interna

// Datastore Imports
import com.google.cloud.Timestamp; // Se usares Timestamp (ex: para logs ou updates)
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException; // Necessário nos catches
import com.google.cloud.datastore.DatastoreOptions; // Necessário para inicialização
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;             // Necessário se usares Key diretamente
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;            // Classe Query principal
import com.google.cloud.datastore.QueryResults;
// Importar PropertyFilter diretamente após importar StructuredQuery
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter; // Import específico para PropertyFilter
import com.google.cloud.datastore.Transaction;// Necessário para transações

// JAX-RS Imports
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam; // Para ler o token do header
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PathParam;

// Gson Import (Serialização JSON)
import com.google.gson.Gson;

// Commons Codec (Password Hashing)
import org.apache.commons.codec.digest.DigestUtils; // Necessário para changePassword

// Util Classes (DTOs e Token)
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangePasswordData;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeRoleData;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeStateData;
import pt.unl.fct.di.apdc.firstwebapp.util.UpdateAttributesData;



@Path("/user") // Path base para operações de utilizador
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class UserResource {

    private static final Logger LOG = Logger.getLogger(UserResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    // --- ADICIONADO KeyFactory para Tokens ---
    private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("AuthToken");

    // --- ADICIONAR ESTAS CONSTANTES ---
    private static final String USER_ROLE_PROPERTY = "user_role";
    private static final String USER_STATE_PROPERTY = "user_state";
    private static final String USER_PWD_PROPERTY = "user_pwd";


    public UserResource() { }

    @POST
    @Path("/changestate") // Endpoint: POST /rest/user/changestate
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeAccountState(@HeaderParam("Authorization") String authorizationHeader, ChangeStateData data) {

        // 1. Validação do Token (usando a função implementada abaixo)
        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            LOG.warning("ChangeState failed: Invalid or expired token provided.");
            // Retornar 403 em vez de 401 pode ser preferível para não indicar se o token existe mas expirou
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        // Log da tentativa
        LOG.info("ChangeState attempt by user: " + token.username + " (Role: " + token.role + ") for target: " + (data != null ? data.targetUser : "null"));

        // 2. Validação do Input Data
        if (data == null || !data.isValid()) {
            LOG.warning("ChangeState failed: Invalid input data. User: " + token.username);
            return Response.status(Status.BAD_REQUEST).entity("Invalid input data. Provide targetUser and newState (ATIVADA/DESATIVADA).").build();
        }
        String newStateUpper = data.newState.toUpperCase(); // Normaliza para maiúsculas

        // 3. Verificar Permissões do Utilizador Autenticado (baseado no token)
        if (!(token.role.equals("ADMIN") || token.role.equals("BACKOFFICE"))) {
            LOG.warning("ChangeState failed: User " + token.username + " (Role: " + token.role + ") does not have permission.");
            return Response.status(Status.FORBIDDEN).entity("User does not have permission for this operation.").build();
        }

        // 4. Lógica da Operação (dentro de uma transação)
        Transaction txn = datastore.newTransaction();
        try {
            // Obter a entidade do utilizador alvo
            Key targetUserKey = userKeyFactory.newKey(data.targetUser);
            Entity targetUser = txn.get(targetUserKey); // Lê dentro da transação

            // Verifica se o utilizador alvo existe
            if (targetUser == null) {
                txn.rollback();
                LOG.warning("ChangeState failed: Target user " + data.targetUser + " not found. Requested by: " + token.username);
                return Response.status(Status.NOT_FOUND).entity("Target user not found.").build();
            }

            // Obtém o role do utilizador alvo para aplicar restrições
            String targetUserRole = targetUser.contains("user_role") ? targetUser.getString("user_role") : "enduser";

            // 5. Aplicar Restrições Adicionais para BACKOFFICE
            if (token.role.equals("BACKOFFICE")) {
                // BACKOFFICE não pode alterar ADMINs ou outros BACKOFFICE
                if (targetUserRole.equals("ADMIN") || targetUserRole.equals("BACKOFFICE")) {
                    txn.rollback();
                    LOG.warning("ChangeState failed: BACKOFFICE user " + token.username + " attempted to change state of ADMIN/BACKOFFICE user " + data.targetUser);
                    return Response.status(Status.FORBIDDEN).entity("BACKOFFICE users cannot change state of ADMIN or other BACKOFFICE users.").build();
                }
            }
            // (ADMIN pode alterar qualquer um)

            // 6. Atualizar a Entidade do Utilizador Alvo
            Entity updatedUser = Entity.newBuilder(targetUser) // Constrói a partir da entidade existente
                    .set("user_state", newStateUpper) // Atualiza apenas o estado
                    .build();
            txn.put(updatedUser); // Coloca a entidade atualizada na transação
            txn.commit(); // Efetiva a alteração

            LOG.info("User " + token.username + " successfully changed state of user " + data.targetUser + " to " + newStateUpper);
            return Response.ok().entity("User state changed successfully.").build();

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "ChangeState Datastore error for target " + data.targetUser + " by user " + token.username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing user state (Datastore).").build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "ChangeState Unexpected error for target " + data.targetUser + " by user " + token.username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing user state (Unexpected).").build();
        } finally {
            // Garante rollback se algo correu mal e a transação ficou ativa
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    /**
     * Método auxiliar para validar o token de autenticação lendo do Datastore.
     *
     * @param authorizationHeader O conteúdo do header Authorization (ex: "Bearer <tokenID>")
     * @return O objeto AuthToken se o token for válido e não expirado, null caso contrário.
     */
    private AuthToken validateToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            LOG.fine("Token validation failed: Missing or invalid Authorization header format.");
            return null;
        }

        String tokenId = authorizationHeader.substring(7).trim(); // Extrai o tokenID
        if (tokenId.isEmpty()) {
            LOG.fine("Token validation failed: Token ID is empty.");
            return null;
        }

        LOG.fine("Validating TokenID: " + tokenId);

        try {
            Key tokenKey = tokenKeyFactory.newKey(tokenId);
            Entity tokenEntity = datastore.get(tokenKey); // Lê a entidade do token

            if (tokenEntity == null) {
                LOG.warning("Token validation failed: TokenID not found in Datastore: " + tokenId);
                return null; // Token não encontrado
            }

            // Recria o objeto AuthToken a partir da entidade
            AuthToken token = new AuthToken(); // Usa construtor vazio
            token.tokenID = tokenId;
            // Usar .contains() para segurança caso algum campo falte na entidade
            token.username = tokenEntity.contains("username") ? tokenEntity.getString("username") : null;
            token.role = tokenEntity.contains("role") ? tokenEntity.getString("role") : null;
            token.creationData = tokenEntity.contains("creationData") ? tokenEntity.getLong("creationData") : 0L;
            token.expirationData = tokenEntity.contains("expirationData") ? tokenEntity.getLong("expirationData") : 0L;

            // Verifica se campos essenciais foram carregados
            if (token.username == null || token.role == null || token.expirationData == 0L) {
                LOG.severe("Token validation failed: Missing essential data in token entity for TokenID: " + tokenId);
                // Considerar remover este token inválido do Datastore
                // datastore.delete(tokenKey);
                return null;
            }


            // Verifica a expiração
            long currentTime = System.currentTimeMillis();
            if (currentTime > token.expirationData) {
                LOG.warning("Token validation failed: Token expired for user " + token.username + " (TokenID: " + tokenId + ")");
                // Opcional: Remover o token expirado do Datastore para limpeza
                // try { datastore.delete(tokenKey); } catch (DatastoreException delEx) { LOG.warning("Failed to delete expired token: " + tokenId); }
                return null; // Token expirado
            }

            // Token é válido e não expirado
            LOG.info("Token validated successfully for user: " + token.username + " (Role: " + token.role + ")");
            return token;

        } catch (DatastoreException e) {
            LOG.log(Level.SEVERE, "Datastore error during token validation for TokenID: " + tokenId, e);
            return null;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unexpected error during token validation for TokenID: " + tokenId, e);
            return null;
        }
    }

    @POST
    @Path("/changerole") // Endpoint: POST /rest/user/changerole
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeRole(@HeaderParam("Authorization") String authorizationHeader, ChangeRoleData data) {

        // 1. Validação do Token
        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            LOG.warning("ChangeRole failed: Invalid or expired token provided.");
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        LOG.info("ChangeRole attempt by user: " + token.username + " (Role: " + token.role + ") for target: " + (data != null ? data.targetUser : "null"));

        // 2. Validação do Input Data
        if (data == null || !data.isValid()) {
            LOG.warning("ChangeRole failed: Invalid input data. User: " + token.username);
            // Informar sobre roles válidos pode ser útil
            return Response.status(Status.BAD_REQUEST).entity("Invalid input data. Provide targetUser and a valid newRole (ENDUSER, BACKOFFICE, ADMIN, PARTNER).").build();
        }
        String newRoleUpper = data.newRole.toUpperCase(); // Normaliza para maiúsculas

        // 3. Verificar Permissões do Utilizador Autenticado (Caller)
        // Apenas ADMIN e BACKOFFICE podem tentar mudar roles
        if (!(token.role.equals("ADMIN") || token.role.equals("BACKOFFICE"))) {
            LOG.warning("ChangeRole failed: User " + token.username + " (Role: " + token.role + ") does not have permission.");
            return Response.status(Status.FORBIDDEN).entity("User does not have permission for this operation.").build();
        }

        // Não permitir que um utilizador mude o seu próprio role
        if (token.username.equals(data.targetUser)) {
            LOG.warning("ChangeRole failed: User " + token.username + " attempted to change their own role.");
            return Response.status(Status.BAD_REQUEST).entity("Users cannot change their own role.").build();
        }


        // 4. Lógica da Operação (dentro de uma transação)
        Transaction txn = datastore.newTransaction();
        try {
            // Obter a entidade do utilizador alvo
            Key targetUserKey = userKeyFactory.newKey(data.targetUser);
            Entity targetUser = txn.get(targetUserKey);

            // Verifica se o utilizador alvo existe
            if (targetUser == null) {
                txn.rollback();
                LOG.warning("ChangeRole failed: Target user " + data.targetUser + " not found. Requested by: " + token.username);
                return Response.status(Status.NOT_FOUND).entity("Target user not found.").build();
            }

            // Obtém o role atual do utilizador alvo
            String currentTargetRole = targetUser.contains(USER_ROLE_PROPERTY) ? targetUser.getString(USER_ROLE_PROPERTY) : "ENDUSER"; // Assume ENDUSER se faltar

            // 5. Aplicar Restrições Adicionais de Permissão

            // ADMIN pode fazer (quase) tudo, exceto talvez remover o último ADMIN? (Lógica não implementada aqui)
            if (token.role.equals("ADMIN")) {
                // Potencial verificação: Não deixar remover o último ADMIN?
                // if (currentTargetRole.equals("ADMIN") && newRoleUpper.equals("ENDUSER") && /* é o último admin? */) {
                //      txn.rollback(); return Response.status(Status.FORBIDDEN).entity("Cannot remove the last ADMIN.").build();
                // }
                LOG.info("ADMIN " + token.username + " proceeding to change role of " + data.targetUser + " from " + currentTargetRole + " to " + newRoleUpper);
            }
            // BACKOFFICE tem restrições específicas
            else if (token.role.equals("BACKOFFICE")) {
                // BACKOFFICE não pode modificar ADMINs ou outros BACKOFFICE
                if (currentTargetRole.equals("ADMIN") || currentTargetRole.equals("BACKOFFICE")) {
                    txn.rollback();
                    LOG.warning("ChangeRole failed: BACKOFFICE user " + token.username + " attempted to change role of ADMIN/BACKOFFICE user " + data.targetUser);
                    return Response.status(Status.FORBIDDEN).entity("BACKOFFICE users cannot change role of ADMIN or other BACKOFFICE users.").build();
                }
                // BACKOFFICE só pode mudar ENDUSER para PARTNER ou vice-versa (conforme enunciado OP3)
                boolean isValidBackofficeChange = (currentTargetRole.equals("ENDUSER") && newRoleUpper.equals("PARTNER")) ||
                        (currentTargetRole.equals("PARTNER") && newRoleUpper.equals("ENDUSER"));
                if (!isValidBackofficeChange) {
                    txn.rollback();
                    LOG.warning("ChangeRole failed: BACKOFFICE user " + token.username + " attempted invalid role change for " + data.targetUser + " (from " + currentTargetRole + " to " + newRoleUpper + ")");
                    return Response.status(Status.FORBIDDEN).entity("BACKOFFICE users can only change roles between ENDUSER and PARTNER.").build();
                }
                LOG.info("BACKOFFICE " + token.username + " proceeding to change role of " + data.targetUser + " from " + currentTargetRole + " to " + newRoleUpper);
            }


            // 6. Atualizar a Entidade do Utilizador Alvo
            Entity updatedUser = Entity.newBuilder(targetUser)
                    .set(USER_ROLE_PROPERTY, newRoleUpper) // Atualiza apenas o role
                    .build();
            txn.put(updatedUser);
            txn.commit();

            LOG.info("User " + token.username + " successfully changed role of user " + data.targetUser + " to " + newRoleUpper);
            return Response.ok().entity("User role changed successfully.").build();

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "ChangeRole Datastore error for target " + data.targetUser + " by user " + token.username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing user role (Datastore).").build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "ChangeRole Unexpected error for target " + data.targetUser + " by user " + token.username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing user role (Unexpected).").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }


    @POST
    @Path("/changepwd") // Endpoint: POST /rest/user/changepwd
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePassword(@HeaderParam("Authorization") String authorizationHeader, ChangePasswordData data) {

        // 1. Validação do Token (Obtém dados do utilizador autenticado)
        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            LOG.warning("ChangePassword failed: Invalid or expired token provided.");
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        // O username do token é o utilizador que está a fazer o pedido
        String username = token.username;
        LOG.info("ChangePassword attempt by user: " + username);

        // 2. Validação do Input Data (Verifica se campos existem, confirmação e complexidade)
        if (data == null || !data.isValid()) {
            LOG.warning("ChangePassword failed: Invalid input data for user: " + username);
            return Response.status(Status.BAD_REQUEST).entity("Invalid input data. Provide currentPassword, newPassword, confirmation, and ensure new password meets complexity rules.").build();
        }

        // 3. Lógica da Operação (dentro de uma transação)
        Transaction txn = datastore.newTransaction();
        try {
            // Obter a entidade do utilizador autenticado (que está a pedir a mudança)
            Key userKey = userKeyFactory.newKey(username);
            Entity user = txn.get(userKey);

            // Verifica se o utilizador existe (deveria, pois está autenticado, mas é uma segurança extra)
            if (user == null) {
                txn.rollback(); // Não deveria acontecer se o token é válido, mas por segurança
                LOG.severe("ChangePasswordConsistencyError: User " + username + " authenticated but not found in Datastore.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("User authenticated but not found.").build();
            }

            // 4. Verificar a Password Atual
            String currentHashedPwd = user.getString(USER_PWD_PROPERTY);
            String providedCurrentHash = DigestUtils.sha512Hex(data.currentPassword);

            if (currentHashedPwd == null || !currentHashedPwd.equals(providedCurrentHash)) {
                txn.rollback();
                LOG.warning("ChangePassword failed: Incorrect current password provided by user: " + username);
                return Response.status(Status.FORBIDDEN).entity("Incorrect current password.").build(); // Usar 403 para não dar pistas se user existe
            }

            // Se a password atual está correta, podemos proceder

            // 5. Gerar o Hash da Nova Password
            String newHashedPwd = DigestUtils.sha512Hex(data.newPassword);

            // 6. Atualizar a Entidade do Utilizador
            Entity updatedUser = Entity.newBuilder(user)
                    .set(USER_PWD_PROPERTY, newHashedPwd) // Define a nova password hasheada
                    // Opcional: Poderia invalidar outros tokens ativos para este user aqui
                    .build();
            txn.put(updatedUser);
            txn.commit();

            LOG.info("User " + username + " successfully changed their password.");
            // Opcional: Invalidar o token atual que foi usado para esta operação?
            // Se invalidar, o user precisaria fazer login novamente.
            // Se não invalidar, o token atual continua válido até expirar.
            // Vamos manter o token atual válido por simplicidade.
            return Response.ok().entity("Password changed successfully.").build();

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "ChangePassword Datastore error for user: " + username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing password (Datastore).").build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "ChangePassword Unexpected error for user: " + username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing password (Unexpected).").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST // Usando POST para consistência com passagem de token
    @Path("/listusers")
    @Consumes(MediaType.APPLICATION_JSON) // Mesmo que o corpo possa ser vazio, definimos
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response listUsers(@HeaderParam("Authorization") String authorizationHeader) {

        // 1. Validação do Token
        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            LOG.warning("ListUsers failed: Invalid or expired token provided.");
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        LOG.info("ListUsers attempt by user: " + token.username + " (Role: " + token.role + ")");

        // 2. Construir a Query Datastore com base no Role do Caller
        List<Map<String, Object>> usersData = new ArrayList<>();
        final String defaultNotDefined = "NOT DEFINED"; // Valor para campos não definidos

        try {
            // Usar EntityQuery.Builder (com nomes completos ou import)
            com.google.cloud.datastore.EntityQuery.Builder queryBuilder = com.google.cloud.datastore.Query.newEntityQueryBuilder();
            queryBuilder.setKind("User"); // Sempre vamos buscar Users

            // Aplica filtros baseados no role de quem pede
            switch (token.role) {
                case "ENDUSER":
                case "PARTNER":
                    // --- CORREÇÃO: Usar CompositeFilter.and ---
                    queryBuilder.setFilter(
                            // Cria um filtro composto com AND
                            StructuredQuery.CompositeFilter.and(
                                    // Primeiro filtro: role = ENDUSER
                                    StructuredQuery.PropertyFilter.eq(USER_ROLE_PROPERTY, "ENDUSER"),
                                    // Segundo filtro: state = ATIVADA
                                    StructuredQuery.PropertyFilter.eq(USER_STATE_PROPERTY, "ATIVADA"),
                                    // Terceiro filtro: profile = publico
                                    StructuredQuery.PropertyFilter.eq("user_profile", "publico")
                            )
                    );
                    // --- FIM DA CORREÇÃO ---
                    break;

                case "BACKOFFICE":
                    // Filtra apenas por: role=ENDUSER
                    queryBuilder.setFilter(StructuredQuery.PropertyFilter.eq(USER_ROLE_PROPERTY, "ENDUSER"));
                    break;

                case "ADMIN":
                    // Sem filtros, lista todos os utilizadores
                    break;

                default:
                    // Role desconhecido não pode listar ninguém
                    LOG.warning("ListUsers failed: Unknown role " + token.role + " for user " + token.username);
                    return Response.status(Status.FORBIDDEN).entity("User role cannot perform this operation.").build();
            }

            Query<Entity> query = queryBuilder.build(); // Constrói a query final
            QueryResults<Entity> results = datastore.run(query);

            // 3. Processar Resultados e Construir a Lista de Saída
            while (results.hasNext()) {
                Entity userEntity = results.next();
                Map<String, Object> userData = new HashMap<>();

                // Adiciona atributos com base no role do caller
                switch (token.role) {
                    case "ENDUSER":
                    case "PARTNER":
                        userData.put("username", userEntity.getKey().getName());
                        userData.put("email", userEntity.contains("user_email") ? userEntity.getString("user_email") : defaultNotDefined);
                        userData.put("name", userEntity.contains("user_name") ? userEntity.getString("user_name") : defaultNotDefined);
                        break;

                    case "BACKOFFICE":
                    case "ADMIN":
                        // Todos os atributos (exceto password!)
                        userData.put("username", userEntity.getKey().getName());
                        userData.put("email", userEntity.contains("user_email") ? userEntity.getString("user_email") : defaultNotDefined);
                        userData.put("name", userEntity.contains("user_name") ? userEntity.getString("user_name") : defaultNotDefined);
                        userData.put("telefone", userEntity.contains("user_telefone") ? userEntity.getString("user_telefone") : defaultNotDefined);
                        userData.put("profile", userEntity.contains("user_profile") ? userEntity.getString("user_profile") : defaultNotDefined);
                        userData.put("role", userEntity.contains(USER_ROLE_PROPERTY) ? userEntity.getString(USER_ROLE_PROPERTY) : defaultNotDefined);
                        userData.put("state", userEntity.contains(USER_STATE_PROPERTY) ? userEntity.getString(USER_STATE_PROPERTY) : defaultNotDefined);
                        userData.put("nif", userEntity.contains("user_nif") ? userEntity.getString("user_nif") : defaultNotDefined);
                        userData.put("morada", userEntity.contains("user_morada") ? userEntity.getString("user_morada") : defaultNotDefined);
                        userData.put("funcao", userEntity.contains("user_funcao") ? userEntity.getString("user_funcao") : defaultNotDefined);
                        userData.put("entidade_empregadora", userEntity.contains("user_entidade_empregadora") ? userEntity.getString("user_entidade_empregadora") : defaultNotDefined);
                        userData.put("nif_entidade_empregadora", userEntity.contains("user_nif_entidade_empregadora") ? userEntity.getString("user_nif_entidade_empregadora") : defaultNotDefined);
                        //userData.put("creation_time", userEntity.contains("user_creation_time") ? userEntity.getTimestamp("user_creation_time").toString() : defaultNotDefined);
                        break;
                }
                // Adiciona o mapa de dados do user à lista final, apenas se não estiver vazio
                if (!userData.isEmpty()) {
                    usersData.add(userData);
                }
            }

            LOG.info("ListUsers successful for user: " + token.username + ". Returned " + usersData.size() + " users.");
            // Usa a instância 'g' da classe, assumindo que ela existe
             Gson g = new Gson(); // Remover se 'g' for membro da classe
            return Response.ok(g.toJson(usersData)).build();

        } catch (DatastoreException e) {
            LOG.log(Level.SEVERE, "ListUsers Datastore error for user: " + token.username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error listing users (Datastore).").build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "ListUsers Unexpected error for user: " + token.username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error listing users (Unexpected).").build();
        }
    }

    @DELETE // Usando o método HTTP DELETE
    @Path("/{username_to_delete}") // O username a apagar vem no path: /rest/user/someuser
    public Response removeUserAccount(@HeaderParam("Authorization") String authorizationHeader,
                                      @PathParam("username_to_delete") String usernameToDelete) {

        // 1. Validação do Token (Obtém dados do utilizador que está a fazer o pedido)
        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            LOG.warning("RemoveUser failed: Invalid or expired token provided.");
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        LOG.info("RemoveUser attempt by user: " + token.username + " (Role: " + token.role + ") for target: " + usernameToDelete);

        // 2. Validação Básica do Input
        if (usernameToDelete == null || usernameToDelete.isBlank()) {
            LOG.warning("RemoveUser failed: Missing username to delete in path. Requested by: " + token.username);
            return Response.status(Status.BAD_REQUEST).entity("Username to delete must be provided in the URL path.").build();
        }

        // 3. Não permitir que um utilizador se remova a si próprio (regra de segurança comum)
        if (token.username.equals(usernameToDelete)) {
            LOG.warning("RemoveUser failed: User " + token.username + " attempted to remove themselves.");
            return Response.status(Status.FORBIDDEN).entity("Users cannot remove their own account via this operation.").build();
            // Poderia haver uma operação separada "deleteMyAccount" com regras diferentes.
        }

        // 4. Verificar Permissões do Caller e do Alvo (dentro de uma transação)
        Transaction txn = datastore.newTransaction();
        try {
            // Obter a entidade do utilizador alvo
            Key targetUserKey = userKeyFactory.newKey(usernameToDelete);
            Entity targetUser = txn.get(targetUserKey);

            // Verifica se o utilizador alvo existe
            if (targetUser == null) {
                txn.rollback();
                LOG.warning("RemoveUser failed: Target user " + usernameToDelete + " not found. Requested by: " + token.username);
                // Usar 403 em vez de 404 para não revelar se user existe? Ou 404? Vamos com 403 por segurança.
                return Response.status(Status.FORBIDDEN).entity("Target user not found or insufficient permissions.").build();
            }

            String targetUserRole = targetUser.contains(USER_ROLE_PROPERTY) ? targetUser.getString(USER_ROLE_PROPERTY) : "ENDUSER";

            // Aplicar regras de permissão
            boolean canDelete = false;
            switch (token.role) {
                case "ADMIN":
                    // ADMIN pode remover qualquer um (exceto ele mesmo, já verificado)
                    // Poderia adicionar lógica para não remover o último ADMIN aqui.
                    canDelete = true;
                    LOG.info("ADMIN " + token.username + " authorized to remove target " + usernameToDelete + " (Role: " + targetUserRole + ")");
                    break;
                case "BACKOFFICE":
                    // BACKOFFICE pode remover ENDUSER ou PARTNER
                    if (targetUserRole.equals("ENDUSER") || targetUserRole.equals("PARTNER")) {
                        canDelete = true;
                        LOG.info("BACKOFFICE " + token.username + " authorized to remove target " + usernameToDelete + " (Role: " + targetUserRole + ")");
                    } else {
                        LOG.warning("RemoveUser failed: BACKOFFICE " + token.username + " cannot remove target " + usernameToDelete + " (Role: " + targetUserRole + ")");
                    }
                    break;
                case "ENDUSER":
                case "PARTNER":
                    // Já foram bloqueados de remover outros, e bloqueados de se removerem a si mesmos.
                    // Esta condição não deve ser atingida devido à verificação anterior, mas por segurança:
                    LOG.warning("RemoveUser failed: ENDUSER/PARTNER " + token.username + " should not reach this point.");
                    canDelete = false;
                    break;
                default:
                    LOG.warning("RemoveUser failed: Unknown role " + token.role + " for user " + token.username);
                    canDelete = false;
                    break;
            }

            if (!canDelete) {
                txn.rollback();
                return Response.status(Status.FORBIDDEN).entity("User does not have permission to remove the target user.").build();
            }

            // 5. Se tem permissão, Apagar a Entidade do Utilizador Alvo
            txn.delete(targetUserKey);

            // 6. (Opcional, mas Recomendado) Apagar Tokens Associados ao Utilizador Removido
            // Isto requer uma query para encontrar tokens pelo username.
            try {
                Query<Key> tokenQuery = Query.newKeyQueryBuilder()
                        .setKind("AuthToken")
                        .setFilter(StructuredQuery.PropertyFilter.eq("username", usernameToDelete))
                        .build();
                QueryResults<Key> tokenKeys = txn.run(tokenQuery); // Executa dentro da mesma transação
                int deletedTokens = 0;
                while(tokenKeys.hasNext()) {
                    txn.delete(tokenKeys.next());
                    deletedTokens++;
                }
                if (deletedTokens > 0) {
                    LOG.info("Deleted " + deletedTokens + " associated tokens for removed user: " + usernameToDelete);
                }
            } catch (Exception e) {
                // Logar erro na limpeza de tokens, mas não falhar a operação principal
                LOG.log(Level.WARNING, "Error while trying to delete tokens for removed user: " + usernameToDelete, e);
            }


            // 7. Commit da Transação (apaga user e tokens)
            txn.commit();

            LOG.info("User " + token.username + " successfully removed user: " + usernameToDelete);
            // Usar 204 No Content para DELETE bem sucedido é comum, ou 200 OK com mensagem.
            return Response.ok().entity("User removed successfully.").build();
            // return Response.noContent().build();

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "RemoveUser Datastore error for target " + usernameToDelete + " by user " + token.username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error removing user (Datastore).").build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "RemoveUser Unexpected error for target " + usernameToDelete + " by user " + token.username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error removing user (Unexpected).").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/updateatts") // Endpoint: POST /rest/user/updateatts
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAttributes(@HeaderParam("Authorization") String authorizationHeader, UpdateAttributesData data) {

        // 1. Validação do Token
        AuthToken token = validateToken(authorizationHeader);
        if (token == null) {
            LOG.warning("UpdateAtts failed: Invalid or expired token provided.");
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        LOG.info("UpdateAtts attempt by user: " + token.username + " (Role: " + token.role + ") for target: " + (data != null ? data.targetUser : "null"));

        // 2. Validação do Input Data Básico
        if (data == null || !data.hasTargetUser()) {
            LOG.warning("UpdateAtts failed: Invalid input data (missing targetUser). User: " + token.username);
            return Response.status(Status.BAD_REQUEST).entity("Invalid input data. Provide targetUser and attributes to update.").build();
        }

        // 3. Verificar Permissões Gerais e Obter Entidade Alvo (Transação)
        boolean canUpdate;
        Entity targetUser;
        Transaction txn = datastore.newTransaction();

        try {
            // Obter a entidade alvo
            Key targetUserKey = userKeyFactory.newKey(data.targetUser);
            targetUser = txn.get(targetUserKey);

            if (targetUser == null) {
                txn.rollback();
                LOG.warning("UpdateAtts failed: Target user " + data.targetUser + " not found. Requested by: " + token.username);
                return Response.status(Status.NOT_FOUND).entity("Target user not found.").build();
            }

            String targetUserRole = targetUser.contains(USER_ROLE_PROPERTY) ? targetUser.getString(USER_ROLE_PROPERTY) : "ENDUSER";

            // Verifica se o utilizador pode atualizar o alvo especificado
            switch (token.role) {
                case "ADMIN":
                    canUpdate = true; // ADMIN pode tudo
                    break;
                case "BACKOFFICE":
                    // BACKOFFICE só pode alterar ENDUSER ou PARTNER
                    canUpdate = targetUserRole.equals("ENDUSER") || targetUserRole.equals("PARTNER");
                    if (!canUpdate) {
                        LOG.warning("UpdateAtts failed: BACKOFFICE " + token.username + " cannot update target " + data.targetUser + " (Role: " + targetUserRole + ")");
                    }
                    break;
                case "ENDUSER":
                case "PARTNER":
                    // ENDUSER/PARTNER só podem alterar a si próprios
                    canUpdate = token.username.equals(data.targetUser);
                    if (!canUpdate) {
                        LOG.warning("UpdateAtts failed: User " + token.username + " cannot update target " + data.targetUser);
                    }
                    break;
                default:
                    canUpdate = false; // Roles desconhecidos não podem fazer nada
                    LOG.warning("UpdateAtts failed: Unknown role " + token.role + " for user " + token.username);
                    break;
            }

            if (!canUpdate) {
                txn.rollback();
                return Response.status(Status.FORBIDDEN).entity("User does not have permission to update the target user.").build();
            }

            // 4. Construir a Entidade Atualizada (Aplicando Restrições de Campos)
            Entity.Builder updatedUserBuilder = Entity.newBuilder(targetUser); // Começa com a entidade existente
            boolean updated = false; // Flag para saber se algo foi realmente alterado

            // Aplica as atualizações recebidas, verificando permissões específicas de campos
            if (data.hasName()) {
                updatedUserBuilder.set("user_name", data.name);
                updated = true;
            }
            if (data.hasTelefone()) {
                updatedUserBuilder.set("user_telefone", data.telefone);
                updated = true;
            }
            if (data.hasProfile()) {
                if (data.profile.equalsIgnoreCase("publico") || data.profile.equalsIgnoreCase("privado")) {
                    updatedUserBuilder.set("user_profile", data.profile.toLowerCase());
                    updated = true;
                } else {
                    txn.rollback();
                    LOG.warning("UpdateAtts failed: Invalid profile value '" + data.profile + "' provided by user " + token.username);
                    return Response.status(Status.BAD_REQUEST).entity("Invalid profile value. Use 'publico' or 'privado'.").build();
                }
            }
            // Adicionar os outros campos opcionais que definiste em UpdateAttributesData
            if (data.hasNif()) {
                updatedUserBuilder.set("user_nif", data.nif);
                updated = true;
            }
            if (data.hasMorada()) {
                updatedUserBuilder.set("user_morada", data.morada);
                updated = true;
            }
            if (data.hasEntidadeEmpregadora()) {
                updatedUserBuilder.set("user_entidade_empregadora", data.entidade_empregadora);
                updated = true;
            }
            if (data.hasNifEntidadeEmpregadora()) {
                updatedUserBuilder.set("user_nif_entidade_empregadora", data.nif_entidade_empregadora);
                updated = true;
            }
            if (data.hasFuncao()) {
                updatedUserBuilder.set("user_funcao", data.funcao);
                updated = true;
            }
            // Adicionar foto_url se necessário

            // Restrição de Email para ENDUSER/PARTNER
            if (data.hasEmail()) {
                if (token.role.equals("ADMIN") || token.role.equals("BACKOFFICE")) {
                    if (data.email.contains("@") && data.email.contains(".")) { // Validação simples
                        updatedUserBuilder.set("user_email", data.email);
                        updated = true;
                    } else {
                        txn.rollback();
                        LOG.warning("UpdateAtts failed: Invalid email format '" + data.email + "' provided by user " + token.username);
                        return Response.status(Status.BAD_REQUEST).entity("Invalid email format provided.").build();
                    }
                } else { // ENDUSER ou PARTNER tentaram mudar email
                    txn.rollback();
                    LOG.warning("UpdateAtts failed: User " + token.username + " (Role: " + token.role + ") attempted to change email.");
                    return Response.status(Status.FORBIDDEN).entity("Users cannot change their own email address.").build();
                }
            }

            // 5. Gravar Alterações (se houver)
            if (updated) {
                Entity finalUpdatedUser = updatedUserBuilder.build();
                txn.put(finalUpdatedUser);
                txn.commit();
                LOG.info("User " + token.username + " successfully updated attributes for user " + data.targetUser);
                return Response.ok().entity("User attributes updated successfully.").build();
            } else {
                txn.rollback(); // Não precisa commitar nada
                LOG.info("UpdateAtts info: No valid attributes provided to update for user " + data.targetUser + ". Requested by: " + token.username);
                return Response.ok().entity("No attributes were updated.").build();
            }

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "UpdateAtts Datastore error for target " + data.targetUser + " by user " + token.username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error updating attributes (Datastore).").build();
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "UpdateAtts Unexpected error for target " + data.targetUser + " by user " + token.username, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error updating attributes (Unexpected).").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }





}