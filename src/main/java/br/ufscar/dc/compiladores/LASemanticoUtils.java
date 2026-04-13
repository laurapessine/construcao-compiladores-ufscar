package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class LASemanticoUtils {
    // Lista para guardar os erros semânticos encontrados para imprimir
    public static List<String> errosSemanticos = new ArrayList<>();

    public static void adicionarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        errosSemanticos.add("Linha " + linha + ": " + mensagem);
    }
}