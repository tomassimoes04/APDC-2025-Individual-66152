package pt.unl.fct.di.apdc.firstwebapp.listeners;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

import org.apache.commons.codec.digest.DigestUtils;

@WebListener
public class AppInitListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppInitListener.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private static final String ROOT_USERNAME = "root";
    private static final String ROOT_PASSWORD = "superSecretPassword123!";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.info("Application context initialized. Checking/Creating root user...");

        Key rootUserKey = datastore.newKeyFactory().setKind("User").newKey(ROOT_USERNAME);
        Transaction txn = datastore.newTransaction();
        try {
            Entity rootUser = txn.get(rootUserKey);

            if (rootUser == null) {
                LOG.info("Root user not found. Creating root user...");

                rootUser = Entity.newBuilder(rootUserKey)
                        .set("user_name", "Root Administrator")
                        .set("user_pwd", DigestUtils.sha512Hex(ROOT_PASSWORD))
                        .set("user_email", "root@" + getAppId() + ".appspotmail.com")
                        .set("user_telefone", "+00000000000")
                        .set("user_profile", "privado")
                        .set("user_role", "ADMIN")
                        .set("user_state", "ATIVADA")
                        .set("user_creation_time", Timestamp.now())
                        .build();

                txn.put(rootUser);
                txn.commit();
                LOG.info("Root user created successfully.");

            } else {
                LOG.info("Root user already exists. No action taken.");
                if (!"ADMIN".equals(rootUser.getString("user_role")) || !"ATIVADA".equals(rootUser.getString("user_state"))) {
                    LOG.warning("Root user exists but has incorrect role/state. Attempting to fix...");
                    Entity fixedRoot = Entity.newBuilder(rootUser)
                            .set("user_role", "ADMIN")
                            .set("user_state", "ATIVADA")
                            .build();
                    txn.put(fixedRoot);
                    if(!txn.isActive()) txn = datastore.newTransaction();
                    txn.put(fixedRoot);
                    txn.commit();
                    LOG.info("Fixed root user role/state.");
                } else {
                    if(txn.isActive()) txn.rollback();
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error during root user initialization", e);
            if (txn.isActive()) {
                txn.rollback();
            }
        } finally {
            if (txn != null && txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("Application context destroyed.");
    }

    private String getAppId() {
        try {
            String projectId = datastore.getOptions().getProjectId();
            if (projectId != null) return projectId;

            String envProjectId = System.getenv("GOOGLE_CLOUD_PROJECT");
            if (envProjectId != null) return envProjectId;

        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Could not automatically determine App ID for root email.", t);
        }
        return "default-project-id";
    }
}