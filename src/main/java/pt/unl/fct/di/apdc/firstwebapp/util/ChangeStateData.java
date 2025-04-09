

// Nova classe: ChangeStateData.java (no pacote .util)
package pt.unl.fct.di.apdc.firstwebapp.util;

public class ChangeStateData {
    public String targetUser; // Username do utilizador a modificar
    public String newState;   // Novo estado desejado ("ATIVADA" ou "DESATIVADA")

    public ChangeStateData() { } // Construtor vazio para JAX-RS

    public ChangeStateData(String targetUser, String newState) {
        this.targetUser = targetUser;
        this.newState = newState;
    }

    // Validação simples (pode ser melhorada)
    public boolean isValid() {
        return targetUser != null && !targetUser.isBlank() &&
                newState != null && (newState.equalsIgnoreCase("ATIVADA") || newState.equalsIgnoreCase("DESATIVADA"));
    }
}
