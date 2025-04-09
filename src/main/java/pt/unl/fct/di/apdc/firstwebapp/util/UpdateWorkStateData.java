package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.Set;
import java.util.logging.Logger;

public class UpdateWorkStateData {

    private static final Logger LOG = Logger.getLogger(UpdateWorkStateData.class.getName());

    public String referencia;  // Referência da folha de obra a atualizar
    public String newWorkState; // Novo estado: "NÃO INICIADO", "EM CURSO", "CONCLUÍDO"

    private static final Set<String> VALID_WORK_STATES = Set.of("NÃO INICIADO", "EM CURSO", "CONCLUÍDO");

    public UpdateWorkStateData() {}

    public UpdateWorkStateData(String referencia, String newWorkState) {
        this.referencia = referencia;
        this.newWorkState = newWorkState;
    }

    public boolean isValid() {
        if (referencia == null || referencia.isBlank() ||
                newWorkState == null || newWorkState.isBlank()) {
            LOG.warning("UpdateWorkState validation failed: Missing referencia or newWorkState.");
            return false;
        }
        if (!VALID_WORK_STATES.contains(newWorkState.toUpperCase())) {
            LOG.warning("UpdateWorkState validation failed: Invalid newWorkState: " + newWorkState);
            return false;
        }
        return true;
    }
}
