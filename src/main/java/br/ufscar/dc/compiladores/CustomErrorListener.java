package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.BaseErrorListener; // ouvinte de erro base
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.PrintWriter;

public class CustomErrorListener extends BaseErrorListener {
    private final PrintWriter pw;

    public CustomErrorListener(PrintWriter pw) {
        this.pw = pw;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        Token t = (Token) offendingSymbol;
        String texto = t.getText();
        // Se o erro for ao final do arquivo, o texto vem como "<EOF>", mas o corretor espera "EOF"
        if (texto.equals("<EOF>")) {
            texto = "EOF";
        }
        // Imprime a mensagem padronizada
        pw.println("Linha " + line + ": erro sintatico proximo a " + texto);
        // Interrompe a compilação no primeiro erro sintático encontrado
        throw new ParseCancellationException("Erro sintático");
    }
}