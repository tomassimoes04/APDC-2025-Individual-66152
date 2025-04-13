


package pt.unl.fct.di.apdc.firstwebapp.util;

public class ChangeStateData {
    public String targetUser;
    public String newState;

    public ChangeStateData() { }

    public ChangeStateData(String targetUser, String newState) {
        this.targetUser = targetUser;
        this.newState = newState;
    }


    public boolean isValid() {
        return targetUser != null && !targetUser.isBlank() &&
                newState != null && (newState.equalsIgnoreCase("ATIVADA") || newState.equalsIgnoreCase("DESATIVADA"));
    }
}
