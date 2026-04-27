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
                boolean isT2 = arquivoEntrada.contains("t2");
                boolean erroLexico = false;
                Token t;
                // 1. ANÁLISE LÉXICA
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
                        String nomeToken = LALexer.VOCABULARY.getDisplayName(t.getType());
                        pw.println("<'" + t.getText() + "'," + nomeToken + ">");
                    }
                }
                // 2. ANÁLISE SINTÁTICA E SEMÂNTICA
                if (!isT1) {
                    if (!erroLexico) {
                        lexer.reset(); // Rebobina os tokens para o parser poder consumir desde o início
                        CommonTokenStream tokens = new CommonTokenStream(lexer);
                        LAParser parser = new LAParser(tokens);
                        parser.removeErrorListeners();
                        CustomErrorListener cel = new CustomErrorListener(pw);
                        parser.addErrorListener(cel);
                        try {
                            // O parser gera a árvore sintática (AST)
                            LAParser.ProgramaContext arvore = parser.programa();
                            // 3. ANÁLISE SEMÂNTICA (T3 em diante)
                            if (!isT2) {
                                // Limpa os erros de execuções anteriores (importante para o corretor)
                                LASemanticoUtils.errosSemanticos.clear();

                                // Instancia e aciona o Visitor semântico
                                LASemantico as = new LASemantico();
                                as.visitPrograma(arvore);

                                // Imprime todos os erros semânticos encontrados
                                for (String erro : LASemanticoUtils.errosSemanticos) {
                                    pw.println(erro);
                                }
                            }
                        } catch (ParseCancellationException e) {
                            // Erro sintático capturado. A mensagem já foi impressa.
                        }
                    }
                    pw.println("Fim da compilacao");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}