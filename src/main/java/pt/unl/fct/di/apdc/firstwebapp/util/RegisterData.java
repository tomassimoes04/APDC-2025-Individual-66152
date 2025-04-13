package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.logging.Logger;

public class RegisterData {

	private static final Logger LOG = Logger.getLogger(RegisterData.class.getName());

	public String username;
	public String password;
	public String confirmation;
	public String email;
	public String name;
	public String telefone;
	public String profile;

	public RegisterData(String username, String password, String confirmation, String email, String name, String telefone, String profile) {
		this.username = username;
		this.password = password;
		this.confirmation = confirmation;
		this.email = email;
		this.name = name;
		this.telefone = telefone;
		this.profile = profile;
	}

	public RegisterData() { }

	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}

	public boolean validRegistration() {

		boolean baseValid = nonEmptyOrBlankField(username) &&
				nonEmptyOrBlankField(password) &&
				nonEmptyOrBlankField(confirmation) &&
				nonEmptyOrBlankField(email) &&
				nonEmptyOrBlankField(name) &&
				nonEmptyOrBlankField(telefone) &&
				nonEmptyOrBlankField(profile);

		if (!baseValid) {
			LOG.warning("Validation failed: Basic field missing or blank. User: " + username);
			return false;
		}

		boolean emailValid = email.contains("@") && email.contains(".");
		if (!emailValid) {
			LOG.warning("Validation failed: Invalid email format for: " + email + ". User: " + username);
			return false;
		}

		boolean passwordConfirmed = password.equals(confirmation);
		if (!passwordConfirmed) {
			LOG.warning("Validation failed: Passwords do not match for user: " + username);
			return false;
		}

		boolean passwordMeetsCriteria = checkPasswordComplexity(password, username);
		if (!passwordMeetsCriteria) {
			LOG.warning("Validation failed: Password does not meet complexity requirements for user: " + username);
			return false;
		}

		boolean profileValid = profile.equalsIgnoreCase("publico") || profile.equalsIgnoreCase("privado");
		if (!profileValid) {
			LOG.warning("Validation failed: Profile must be 'publico' or 'privado'. Received: " + profile + " for user: " + username);
			return false;
		}

		return true;
	}

	private boolean checkPasswordComplexity(String password, String username) {
		if (password == null || password.length() < 8) {
			LOG.warning("Password complexity failed: Length < 8. User: " + username);
			return false;
		}
		boolean hasUpper = password.matches(".*[A-Z].*");
		boolean hasLower = password.matches(".*[a-z].*");
		boolean hasDigit = password.matches(".*\\d.*");
		boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

		int criteriaMet = 0;
		if (hasUpper) criteriaMet++;
		if (hasLower) criteriaMet++;
		if (hasDigit) criteriaMet++;
		if (hasSpecial) criteriaMet++;

		boolean passed = criteriaMet >= 3;
		if (!passed) {
			LOG.warning("Password complexity failed: Criteria count < 3 (Upper: " + hasUpper + ", Lower: " + hasLower + ", Digit: " + hasDigit + ", Special: " + hasSpecial + "). User: " + username);
		}
		return passed;
	}
}