package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {

	// Tempo de expiração do token em milissegundos (ex: 2 horas)
	public static final long EXPIRATION_TIME = 1000 * 60 * 60 * 2; // 2h

	public String username;
	public String role; // NOVO: Campo para o role
	public String tokenID;
	public long creationData;
	public long expirationData;


	public AuthToken() {
	}


	public AuthToken(String username, String role) { // Parâmetro 'role' adicionado
		this.username = username;
		this.role = role; // 'role' atribuído
		this.tokenID = UUID.randomUUID().toString();
		this.creationData = System.currentTimeMillis();
		// Cálculo da expiração CORRIGIDO:
		this.expirationData = this.creationData + EXPIRATION_TIME;
	}


}