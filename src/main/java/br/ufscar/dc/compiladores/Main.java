package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Classe principal responsável por inicializar e executar o analisador léxico.
 */
public class Main {
    public static void main(String[] args) {
        try {
            // Inicializa a leitura do arquivo de entrada (fornecido via args[0])
            CharStream cs = CharStreams.fromFileName(args[0]);
            LALexer lexer = new LALexer(cs);

            // Bloco try-with-resources para garantir a correta escrita e fechamento do arquivo de saída (args[1])
            try (PrintWriter pw = new PrintWriter(new FileWriter(args[1]))) {
                Token t = null;

                // Laço para solicitar tokens ao analisador até o final do arquivo (EOF)
                while ((t = lexer.nextToken()).getType() != Token.EOF) {

                    // Tratamento de erros: caso a regra de erro dispare, imprime a mensagem específica e encerra o laço.
                    if (t.getType() == LALexer.ERRO) {
                        pw.println("Linha " + t.getLine() + ": " + t.getText() + " - simbolo nao identificado");
                        break;
                    } else if (t.getType() == LALexer.COMENTARIO_NAO_FECHADO) {
                        pw.println("Linha " + t.getLine() + ": comentario nao fechado");
                        break;
                    } else if (t.getType() == LALexer.CADEIA_NAO_FECHADA) {
                        pw.println("Linha " + t.getLine() + ": cadeia literal nao fechada");
                        break;
                    } else {
                        // Caminho feliz: formata e imprime os tokens lexicais válidos
                        String nomeToken = LALexer.VOCABULARY.getDisplayName(t.getType());
                        pw.println("<'" + t.getText() + "'," + nomeToken + ">");
                    }
                }
            }
        } catch (IOException ex) {
            // Exceção de manipulação dos arquivos (entrada não encontrada ou erro de permissão na saída)
            ex.printStackTrace();
        }
    }
}