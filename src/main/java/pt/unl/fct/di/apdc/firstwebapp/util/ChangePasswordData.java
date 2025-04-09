// Nova classe: ChangePasswordData.java (no pacote .util)
package pt.unl.fct.di.apdc.firstwebapp.util;

// Importar Logger se quiseres adicionar logs na validação de complexidade
// import java.util.logging.Logger;

public class ChangePasswordData {

    // private static final Logger LOG = Logger.getLogger(ChangePasswordData.class.getName()); // Opcional

    public String currentPassword;
    public String newPassword;
    public String confirmation;

    public ChangePasswordData() { } // Construtor vazio

    public ChangePasswordData(String currentPassword, String newPassword, String confirmation) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmation = confirmation;
    }

    /**
     * Verifica se os dados são minimamente válidos (campos presentes e confirmação OK).
     * Não verifica a currentPassword aqui (isso é feito no recurso).
     * Verifica a complexidade da nova password.
     */
    public boolean isValid() {
        if (currentPassword == null || currentPassword.isBlank() ||
                newPassword == null || newPassword.isBlank() ||
                confirmation == null || confirmation.isBlank()) {
            // LOG.warning("ChangePassword validation failed: Missing fields."); // Opcional
            return false; // Garante que todos os campos estão presentes
        }
        if (!newPassword.equals(confirmation)) {
            // LOG.warning("ChangePassword validation failed: New password and confirmation do not match."); // Opcional
            return false; // Nova password e confirmação devem ser iguais
        }
        if (!checkNewPasswordComplexity(newPassword)) {
            // LOG.warning("ChangePassword validation failed: New password does not meet complexity criteria."); // Opcional
            return false; // Nova password deve ser complexa
        }
        return true;
    }

    /**
     * Verifica a complexidade da nova password.
     * Pode reutilizar ou adaptar a lógica de RegisterData.
     */
    private boolean checkNewPasswordComplexity(String password) {
        if (password == null || password.length() < 8) {
            // LOG.fine("New password complexity failed: Length < 8"); // Usar FINE para debug
            return false;
        }
        int criteriaMet = 0;
        if (password.matches(".*[A-Z].*")) criteriaMet++; // Maiúscula
        if (password.matches(".*[a-z].*")) criteriaMet++; // Minúscula
        if (password.matches(".*\\d.*")) criteriaMet++;    // Dígito
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) criteriaMet++; // Especial

        boolean passed = criteriaMet >= 3; // Exige pelo menos 3 dos 4 critérios
        if (!passed) {
            // LOG.fine("New password complexity failed: Criteria count < 3"); // Usar FINE para debug
        }
        return passed;
    }
}
