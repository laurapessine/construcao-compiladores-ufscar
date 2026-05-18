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
                String entradaLower = arquivoEntrada.toLowerCase();
                String saidaLower = arquivoSaida.toLowerCase();
                boolean isT1 = entradaLower.contains("t1") || saidaLower.contains("t1");
                boolean isT2 = entradaLower.contains("t2") || saidaLower.contains("t2");
                boolean isT5 = entradaLower.contains("t5") || saidaLower.contains("t5") || saidaLower.endsWith(".c");
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
                    boolean codigoGerado = false; // Flag para controlar a impressão do "Fim da compilacao"
                    if (!erroLexico) {
                        lexer.reset(); // Rebobina os tokens para o parser consumir desde o início
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
                                // Limpa os erros de execuções anteriores
                                LASemanticoUtils.errosSemanticos.clear();

                                // Instancia e aciona o Visitor semântico
                                LASemantico as = new LASemantico();
                                as.visitPrograma(arvore);
                                if (!LASemanticoUtils.errosSemanticos.isEmpty()) {
                                    // Imprime os erros semânticos encontrados
                                    for (String erro : LASemanticoUtils.errosSemanticos) {
                                        pw.println(erro);
                                    }
                                } else if (isT5) {
                                    // 4. GERAÇÃO DE CÓDIGO
                                    LAGeradorC gerador = new LAGeradorC();
                                    gerador.visitPrograma(arvore);
                                    pw.print(gerador.saida.toString());
                                    codigoGerado = true; // Marca que o código C foi gerado com sucesso
                                }
                            }
                        } catch (ParseCancellationException e) {
                            // Erro sintático capturado. A mensagem já foi impressa.
                        }
                    }
                    // Imprime "Fim da compilacao" APENAS se o código C NÃO tiver sido gerado
                    if (!codigoGerado) {
                        pw.println("Fim da compilacao");
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}