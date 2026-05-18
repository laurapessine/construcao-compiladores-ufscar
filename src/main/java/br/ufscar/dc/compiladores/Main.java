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
        if (args.length < 2) {
            System.err.println("Uso: java -jar <programa> <arquivo_entrada> <arquivo_saida>");
            return;
        }
        String arquivoEntrada = args[0];
        String arquivoSaida = args[1];
        String entradaLower = arquivoEntrada.toLowerCase();
        String saidaLower = arquivoSaida.toLowerCase();
        boolean isT1 = entradaLower.contains("t1") || saidaLower.contains("t1");
        boolean isT2 = entradaLower.contains("t2") || saidaLower.contains("t2");
        boolean isT5 = entradaLower.contains("t5") || saidaLower.contains("t5") || saidaLower.endsWith(".c");
        try (PrintWriter pw = new PrintWriter(new FileWriter(arquivoSaida))) {
            CharStream cs = CharStreams.fromFileName(arquivoEntrada);
            LALexer lexer = new LALexer(cs);
            // Passo 1: análise léxica
            boolean erroLexico = executarAnaliseLexica(lexer, pw, isT1);
            boolean codigoGerado = false;
            // Passo 2: fases avançadas
            if (!isT1) {
                if (!erroLexico) {
                    // Só executa o sintático/semântico se o léxico passou
                    codigoGerado = executarFasesAvancadas(lexer, pw, isT2, isT5);
                }
                // Passo 3: finalização padrão
                // Se não gerou código C (ou seja, deu erro léxico, sintático, semântico ou é T2/T3/T4)
                if (!codigoGerado) {
                    pw.println("Fim da compilacao");
                }
            }
        } catch (IOException ex) {
            System.err.println("Erro de E/S: " + ex.getMessage());
        }
    }

    // --- Método para a análise léxica (T1) ---
    private static boolean executarAnaliseLexica(LALexer lexer, PrintWriter pw, boolean isT1) {
        Token t;
        while ((t = lexer.nextToken()).getType() != Token.EOF) {
            if (t.getType() == LALexer.ERRO) {
                pw.println("Linha " + t.getLine() + ": " + t.getText() + " - simbolo nao identificado");
                return true;
            } else if (t.getType() == LALexer.COMENTARIO_NAO_FECHADO) {
                pw.println("Linha " + t.getLine() + ": comentario nao fechado");
                return true;
            } else if (t.getType() == LALexer.CADEIA_NAO_FECHADA) {
                pw.println("Linha " + t.getLine() + ": cadeia literal nao fechada");
                return true;
            } else if (isT1) {
                String nomeToken = LALexer.VOCABULARY.getDisplayName(t.getType());
                pw.println("<'" + t.getText() + "'," + nomeToken + ">");
            }
        }
        return false;
    }

    // --- Método para as análises sintática, semântica e geração (T2 ao T5) ---
    // Retorna TRUE se gerou código C, FALSE caso contrário
    private static boolean executarFasesAvancadas(LALexer lexer, PrintWriter pw, boolean isT2, boolean isT5) {
        lexer.reset();
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LAParser parser = new LAParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new CustomErrorListener(pw));
        try {
            LAParser.ProgramaContext arvore = parser.programa();
            if (isT2) return false;
            // Análise semântica (T3 e T4)
            LASemanticoUtils.errosSemanticos.clear();
            LASemantico as = new LASemantico();
            as.visitPrograma(arvore);
            if (!LASemanticoUtils.errosSemanticos.isEmpty()) {
                for (String erro : LASemanticoUtils.errosSemanticos) {
                    pw.println(erro);
                }
                return false;
            } else if (isT5) {
                // Geração de código (T5)
                LAGeradorC gerador = new LAGeradorC();
                gerador.visitPrograma(arvore);
                pw.print(gerador.saida.toString());
                return true;
            }
        } catch (ParseCancellationException e) {
            // Erro sintático (a mensagem já foi impressa pelo listener)
        }
        return false;
    }
}