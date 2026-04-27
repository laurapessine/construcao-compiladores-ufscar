package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class LASemanticoUtils {
    // Lista para guardar os erros semânticos encontrados
    public static List<String> errosSemanticos = new ArrayList<>();

    public static void adicionarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        errosSemanticos.add("Linha " + linha + ": " + mensagem);
    }

    // Verifica se o símbolo existe em qualquer um dos escopos abertos
    public static boolean verificarSimbolo(Escopos escopos, String nome) {
        for (TabelaDeSimbolos tabela : escopos.percorrerEscoposAninhados()) {
            if (tabela.existe(nome)) {
                return true;
            }
        }
        return false;
    }
}