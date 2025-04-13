
package pt.unl.fct.di.apdc.firstwebapp.util;



public class ChangePasswordData {



    public String currentPassword;
    public String newPassword;
    public String confirmation;

    public ChangePasswordData() { } // Construtor vazio

    public ChangePasswordData(String currentPassword, String newPassword, String confirmation) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmation = confirmation;
    }


    public boolean isValid() {
        if (currentPassword == null || currentPassword.isBlank() ||
                newPassword == null || newPassword.isBlank() ||
                confirmation == null || confirmation.isBlank()) {

            return false;
        }
        if (!newPassword.equals(confirmation)) {
            return false;
        }
        if (!checkNewPasswordComplexity(newPassword)) {
            return false;
        }
        return true;
    }

    /**
     * Verifica a complexidade da nova password.
     * Pode reutilizar ou adaptar a lógica de RegisterData.
     */
    private boolean checkNewPasswordComplexity(String password) {
        if (password == null || password.length() < 8) {

            return false;
        }
        int criteriaMet = 0;
        if (password.matches(".*[A-Z].*")) criteriaMet++; // Maiúscula
        if (password.matches(".*[a-z].*")) criteriaMet++; // Minúscula
        if (password.matches(".*\\d.*")) criteriaMet++;    // Dígito
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) criteriaMet++; // Especial

        boolean passed = criteriaMet >= 3;
        if (!passed) {

        }
        return passed;
    }
}
