package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.Set;
import java.util.logging.Logger;

public class WorkSheetData {

    private static final Logger LOG = Logger.getLogger(WorkSheetData.class.getName());

    public String referencia;
    public String descricao;
    public String tipoAlvo;
    public String estadoAdjudicacao;

    public Long dataAdjudicacao;
    public Long dataInicioPrevista;
    public Long dataFimPrevista;
    public String contaEntidade;
    public String nomeEmpresa;
    public String nifEmpresa;
    public String observacoes;

    private static final Set<String> VALID_ADJUDICATION_STATES = Set.of("ADJUDICADO", "NÃO ADJUDICADO");
    private static final Set<String> VALID_TARGET_TYPES = Set.of("PROPRIEDADE PÚBLICA", "PROPRIEDADE PRIVADA");


    public WorkSheetData() {}

    public boolean isValid() {
        if (referencia == null || referencia.isBlank() ||
                descricao == null || descricao.isBlank() ||
                tipoAlvo == null || tipoAlvo.isBlank() ||
                estadoAdjudicacao == null || estadoAdjudicacao.isBlank()) {
            LOG.warning("WorkSheet validation failed: Missing mandatory fields (referencia, descricao, tipoAlvo, estadoAdjudicacao).");
            return false;
        }

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

        if (estadoAdjUpper.equals("ADJUDICADO")) {
            if (dataAdjudicacao == null || dataInicioPrevista == null || dataFimPrevista == null ||
                    contaEntidade == null || contaEntidade.isBlank() ||
                    nomeEmpresa == null || nomeEmpresa.isBlank() ||
                    nifEmpresa == null || nifEmpresa.isBlank())
            {
                LOG.warning("WorkSheet validation failed: Missing adjudication details for ADJUDICADO state.");
                return false;
            }
        }

        return true;
    }
}