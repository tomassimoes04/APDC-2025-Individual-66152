
package pt.unl.fct.di.apdc.firstwebapp.util;

public class UpdateAttributesData {
    public String targetUser;


    public String name;
    public String email;
    public String telefone;
    public String profile; // "publico" ou "privado"
    public String nif;
    public String morada;
    public String entidade_empregadora;
    public String nif_entidade_empregadora;
    public String funcao;


    public UpdateAttributesData() {}

    // Validação básica - verifica apenas se targetUser existe

    public boolean hasTargetUser() {
        return targetUser != null && !targetUser.isBlank();
    }

    // Métodos auxiliares para verificar se um campo foi fornecido (não é null)
    public boolean hasName() { return name != null; }
    public boolean hasEmail() { return email != null; }
    public boolean hasTelefone() { return telefone != null; }
    public boolean hasProfile() { return profile != null; }
    public boolean hasNif() { return nif != null; }
    public boolean hasMorada() { return morada != null; }
    public boolean hasEntidadeEmpregadora() { return entidade_empregadora != null; }
    public boolean hasNifEntidadeEmpregadora() { return nif_entidade_empregadora != null; }
    public boolean hasFuncao() { return funcao != null; }


}
