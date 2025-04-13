package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.Set;

public class ChangeRoleData {
    public String targetUser; // Username do utilizador a modificar
    public String newRole;    // Novo role desejado

    // Conjunto de roles válidos na aplicação
    private static final Set<String> VALID_ROLES = Set.of("ENDUSER", "BACKOFFICE", "ADMIN", "PARTNER");

    public ChangeRoleData() { } // Construtor vazio para JAX-RS

    public ChangeRoleData(String targetUser, String newRole) {
        this.targetUser = targetUser;
        this.newRole = newRole;
    }

    // Validação simples
    public boolean isValid() {

        return targetUser != null && !targetUser.isBlank() &&
                newRole != null && !newRole.isBlank() &&
                VALID_ROLES.contains(newRole.toUpperCase());
    }
}