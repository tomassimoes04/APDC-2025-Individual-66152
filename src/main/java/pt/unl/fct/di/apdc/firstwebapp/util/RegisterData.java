package pt.unl.fct.di.apdc.firstwebapp.util;

// 1. Adicionar import do Logger
import java.util.logging.Logger;

public class RegisterData {

	// 2. Adicionar instância estática do Logger
	private static final Logger LOG = Logger.getLogger(RegisterData.class.getName());

	public String username;
	public String password;
	public String confirmation;
	public String email;
	public String name;
	public String telefone; // Campo obrigatório ADC OP1
	public String profile;  // Campo obrigatório ADC OP1 (espera "publico" ou "privado")

	// Construtor completo
	public RegisterData(String username, String password, String confirmation, String email, String name, String telefone, String profile) {
		this.username = username;
		this.password = password;
		this.confirmation = confirmation;
		this.email = email;
		this.name = name;
		this.telefone = telefone;
		this.profile = profile;
	}

	// Construtor vazio
	public RegisterData() { }

	/**
	 * Verifica se uma string não é nula e não está vazia ou apenas com espaços em branco.
	 */
	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}

	/**
	 * Valida os dados de registo de acordo com as regras definidas.
	 */
	public boolean validRegistration() {

		// 1. Verifica campos básicos
		boolean baseValid = nonEmptyOrBlankField(username) &&
				nonEmptyOrBlankField(password) &&
				nonEmptyOrBlankField(confirmation) &&
				nonEmptyOrBlankField(email) &&
				nonEmptyOrBlankField(name) &&
				nonEmptyOrBlankField(telefone) &&
				nonEmptyOrBlankField(profile);

		if (!baseValid) {
			// 3. Substituir System.err por LOG.warning
			LOG.warning("Validation failed: Basic field missing or blank. User: " + username);
			return false;
		}

		// 2. Verifica formato do email
		boolean emailValid = email.contains("@") && email.contains(".");
		if (!emailValid) {
			// 3. Substituir System.err por LOG.warning
			LOG.warning("Validation failed: Invalid email format for: " + email + ". User: " + username);
			return false;
		}

		// 3. Verifica password e confirmação
		boolean passwordConfirmed = password.equals(confirmation);
		if (!passwordConfirmed) {
			// 3. Substituir System.err por LOG.warning
			LOG.warning("Validation failed: Passwords do not match for user: " + username);
			return false;
		}

		// 4. Verifica critérios da password
		// Passando username para a função de complexidade para logging
		boolean passwordMeetsCriteria = checkPasswordComplexity(password, username);
		if (!passwordMeetsCriteria) {
			// 3. Substituir System.err por LOG.warning (log detalhado já está em checkPasswordComplexity)
			LOG.warning("Validation failed: Password does not meet complexity requirements for user: " + username);
			return false;
		}

		// 5. Verifica profile
		boolean profileValid = profile.equalsIgnoreCase("publico") || profile.equalsIgnoreCase("privado");
		if (!profileValid) {
			// 3. Substituir System.err por LOG.warning
			LOG.warning("Validation failed: Profile must be 'publico' or 'privado'. Received: " + profile + " for user: " + username);
			return false;
		}

		// Se tudo passou, é válido (log informativo opcional)
		// LOG.info("Registration data validated successfully for user: " + username);
		return true;
	}

	/**
	 * Verifica a complexidade da password.
	 * @param password A password a verificar.
	 * @param username O username associado (para logging).
	 * @return true se a password cumprir os critérios, false caso contrário.
	 */
	// Adicionado parâmetro username para logging
	private boolean checkPasswordComplexity(String password, String username) {
		if (password == null || password.length() < 8) {
			// 3. Substituir System.err por LOG.warning
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
			// 3. Substituir System.err por LOG.warning
			LOG.warning("Password complexity failed: Criteria count < 3 (Upper: " + hasUpper + ", Lower: " + hasLower + ", Digit: " + hasDigit + ", Special: " + hasSpecial + "). User: " + username);
		}
		return passed;
	}
}