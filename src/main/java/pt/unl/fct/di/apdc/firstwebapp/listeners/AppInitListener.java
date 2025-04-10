package pt.unl.fct.di.apdc.firstwebapp.listeners; // Ou outro pacote

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener; // Importante para registo automático

import java.util.logging.Level;
import java.util.logging.Logger;

// Imports Datastore
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

// Import DigestUtils
import org.apache.commons.codec.digest.DigestUtils;

@WebListener // Esta anotação regista o listener automaticamente no Servlet Container
public class AppInitListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppInitListener.class.getName());
    // Usar inicialização simples, pois corre no ambiente App Engine (local ou cloud)
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    // --- DEFINIÇÕES DO ROOT USER ---
    private static final String ROOT_USERNAME = "root";
    // MUDA ESTA PASSWORD PARA ALGO SEGURO!
    private static final String ROOT_PASSWORD = "superSecretPassword123!";
    // -----------------------------

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.info("Application context initialized. Checking/Creating root user...");

        Key rootUserKey = datastore.newKeyFactory().setKind("User").newKey(ROOT_USERNAME);
        Transaction txn = datastore.newTransaction(); // Usar transação para segurança
        try {
            Entity rootUser = txn.get(rootUserKey);

            if (rootUser == null) {
                // Root user não existe, vamos criá-lo
                LOG.info("Root user not found. Creating root user...");

                rootUser = Entity.newBuilder(rootUserKey)
                        .set("user_name", "Root Administrator") // Nome completo
                        .set("user_pwd", DigestUtils.sha512Hex(ROOT_PASSWORD)) // Password hasheada
                        .set("user_email", "root@" + getAppId() + ".appspotmail.com") // Email único
                        .set("user_telefone", "+00000000000")    // Telefone placeholder
                        .set("user_profile", "privado")          // Perfil privado por default
                        .set("user_role", "ADMIN")               // Role ADMIN
                        .set("user_state", "ATIVADA")            // Estado ATIVADO
                        .set("user_creation_time", Timestamp.now()) // Data criação
                        // Adicionar outros campos não-nulos se a entidade exigir
                        // .set("user_nif", "") // Exemplo
                        .build();

                txn.put(rootUser);
                txn.commit();
                LOG.info("Root user created successfully.");

            } else {
                // Root user já existe
                LOG.info("Root user already exists. No action taken.");
                // Poderias verificar se o estado/role estão corretos e corrigir se necessário
                if (!"ADMIN".equals(rootUser.getString("user_role")) || !"ATIVADA".equals(rootUser.getString("user_state"))) {
                    LOG.warning("Root user exists but has incorrect role/state. Attempting to fix...");
                    Entity fixedRoot = Entity.newBuilder(rootUser)
                            .set("user_role", "ADMIN")
                            .set("user_state", "ATIVADA")
                            .build();
                    txn.put(fixedRoot); // Coloca na transação (se não foi commitada ainda)
                    if(!txn.isActive()) txn = datastore.newTransaction(); // Reinicia transação se necessário
                    txn.put(fixedRoot);
                    txn.commit(); // Tenta commitar a correção
                    LOG.info("Fixed root user role/state.");
                } else {
                    if(txn.isActive()) txn.rollback(); // Rollback se não houve alterações
                }

            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error during root user initialization", e);
            if (txn.isActive()) {
                txn.rollback();
            }
        } finally {
            // Garantir que transação não fica aberta
            if (txn != null && txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("Application context destroyed.");
        // Lógica de cleanup se necessário ao parar a aplicação
    }

    /**
     * Obtém o ID da aplicação App Engine (Project ID).
     * Útil para criar emails únicos.
     * NOTA: Em alguns ambientes muito restritos, isto pode falhar.
     */
    private String getAppId() {
        try {
            // Tenta obter via API do App Engine (se disponível no classpath)
            // Se com.google.appengine.api.utils.SystemProperty não estiver disponível, isto falhará
            // return com.google.appengine.api.utils.SystemProperty.applicationId.get();

            // Alternativa: Tenta obter das opções do Datastore
            String projectId = datastore.getOptions().getProjectId();
            if (projectId != null) return projectId;

            // Alternativa final: Variável de ambiente (comum na cloud)
            String envProjectId = System.getenv("GOOGLE_CLOUD_PROJECT");
            if (envProjectId != null) return envProjectId;

        } catch (Throwable t) { // Captura Throwable caso a API GAE não exista
            LOG.log(Level.WARNING, "Could not automatically determine App ID for root email.", t);
        }
        // Fallback se nada funcionar
        return "default-project-id";
    }
}
