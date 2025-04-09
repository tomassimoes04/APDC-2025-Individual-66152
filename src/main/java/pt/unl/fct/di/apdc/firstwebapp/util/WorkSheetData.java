package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.Set;
import java.util.logging.Logger;

public class WorkSheetData {

    private static final Logger LOG = Logger.getLogger(WorkSheetData.class.getName());

    // Atributos Obrigatórios Base
    public String referencia;          // Ex: "0234/CM/2024"
    public String descricao;
    public String tipoAlvo;            // "PROPRIEDADE PÚBLICA" ou "PROPRIEDADE PRIVADA"
    public String estadoAdjudicacao;   // "ADJUDICADO" ou "NÃO ADJUDICADO"

    // Atributos Opcionais / Condicionais (se ADJUDICADO)
    public Long dataAdjudicacao;      // Data como timestamp Unix em milissegundos
    public Long dataInicioPrevista;   // Data como timestamp Unix em milissegundos
    public Long dataFimPrevista;      // Data como timestamp Unix em milissegundos
    public String contaEntidade;       // Username do PARTNER
    public String nomeEmpresa;
    public String nifEmpresa;
    // estadoObra não é definido aqui, é atualizado depois pelo PARTNER
    public String observacoes;         // Observações gerais

    // Conjuntos para validação (copiados das constantes do Resource)
    private static final Set<String> VALID_ADJUDICATION_STATES = Set.of("ADJUDICADO", "NÃO ADJUDICADO");
    private static final Set<String> VALID_TARGET_TYPES = Set.of("PROPRIEDADE PÚBLICA", "PROPRIEDADE PRIVADA");


    public WorkSheetData() {}

    /**
     * Valida os dados da folha de obra.
     * @return true se os dados são válidos, false caso contrário.
     */
    public boolean isValid() {
        // 1. Validar campos obrigatórios base
        if (referencia == null || referencia.isBlank() ||
                descricao == null || descricao.isBlank() ||
                tipoAlvo == null || tipoAlvo.isBlank() ||
                estadoAdjudicacao == null || estadoAdjudicacao.isBlank()) {
            LOG.warning("WorkSheet validation failed: Missing mandatory fields (referencia, descricao, tipoAlvo, estadoAdjudicacao).");
            return false;
        }

        // 2. Validar valores de tipoAlvo e estadoAdjudicacao
        String tipoAlvoUpper = tipoAlvo.toUpperCase();
        String estadoAdjUpper = estadoAdjudicacao.toUpperCase();

        if (!VALID_TARGET_TYPES.contains(tipoAlvoUpper)) {
            LOG.warning("WorkSheet validation failed: Invalid tipoAlvo: " + tipoAlvo);
            return false;
        }
        if (!VALID_ADJUDICATION_STATES.contains(estadoAdjUpper)) {
            LOG.warning("WorkSheet validation failed: Invalid estadoAdjudicacao: " + estadoAdjudicacao);
            return false;
        }

        // 3. Validar campos condicionais se ADJUDICADO
        if (estadoAdjUpper.equals("ADJUDICADO")) {
            if (dataAdjudicacao == null || dataInicioPrevista == null || dataFimPrevista == null ||
                    contaEntidade == null || contaEntidade.isBlank() ||
                    nomeEmpresa == null || nomeEmpresa.isBlank() ||
                    nifEmpresa == null || nifEmpresa.isBlank())
            {
                LOG.warning("WorkSheet validation failed: Missing adjudication details for ADJUDICADO state.");
                return false;
            }
            // Poderia adicionar validação de datas (inicio < fim), formato NIF, etc.
        }

        // Se chegou aqui, é válido
        return true;
    }
}
