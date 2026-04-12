package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Main {
    public static void main(String[] args) {
        try {
            String arquivoEntrada = args[0];
            String arquivoSaida = args[1];
            CharStream cs = CharStreams.fromFileName(arquivoEntrada);
            LALexer lexer = new LALexer(cs);
            try (PrintWriter pw = new PrintWriter(new FileWriter(arquivoSaida))) {
                boolean isT1 = arquivoEntrada.contains("t1");
                boolean erroLexico = false;
                Token t;
                // Análise Léxica: Unificada para T1 e T2
                while ((t = lexer.nextToken()).getType() != Token.EOF) {
                    if (t.getType() == LALexer.ERRO) {
                        pw.println("Linha " + t.getLine() + ": " + t.getText() + " - simbolo nao identificado");
                        erroLexico = true;
                        break;
                    } else if (t.getType() == LALexer.COMENTARIO_NAO_FECHADO) {
                        pw.println("Linha " + t.getLine() + ": comentario nao fechado");
                        erroLexico = true;
                        break;
                    } else if (t.getType() == LALexer.CADEIA_NAO_FECHADA) {
                        pw.println("Linha " + t.getLine() + ": cadeia literal nao fechada");
                        erroLexico = true;
                        break;
                    } else if (isT1) {
                        // Só imprime a listagem de tokens se for o T1
                        String nomeToken = LALexer.VOCABULARY.getDisplayName(t.getType());
                        pw.println("<'" + t.getText() + "'," + nomeToken + ">");
                    }
                }
                // Análise Sintática: Executa apenas do T2 em diante, se não houver erro léxico
                if (!isT1) {
                    if (!erroLexico) {
                        lexer.reset(); // Rebobina os tokens para o parser poder consumir desde o início
                        CommonTokenStream tokens = new CommonTokenStream(lexer);
                        LAParser parser = new LAParser(tokens);
                        parser.removeErrorListeners();
                        CustomErrorListener cel = new CustomErrorListener(pw);
                        parser.addErrorListener(cel);
                        try {
                            parser.programa();
                        } catch (ParseCancellationException e) {
                            // A exceção interrompe a execução no primeiro erro sintático.
                            // A mensagem já foi impressa corretamente pelo CustomErrorListener.
                        }
                    }
                    // Exigência do T2 e T3
                    pw.println("Fim da compilacao");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}