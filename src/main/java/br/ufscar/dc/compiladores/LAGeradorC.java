package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public class LAGeradorC extends LAParserBaseVisitor<Void> {
    StringBuilder saida;
    Escopos escoposAninhados; // Usado para descobrir tipos de variáveis

    public LAGeradorC() {
        saida = new StringBuilder();
        this.escoposAninhados = new Escopos();
    }

    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx) {
        // Escreve o cabeçalho padrão da linguagem C
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("#include <string.h>\n"); // Necessário para strcpy (atribuição de strings)
        saida.append("\n");
        if (ctx.declaracoes() != null) {
            visitDeclaracoes(ctx.declaracoes());
        }
        saida.append("int main() {\n");
        if (ctx.corpo() != null) {
            visitCorpo(ctx.corpo());
        }
        saida.append("    return 0;\n");
        saida.append("}\n");
        return null;
    }

    // --- 1. DECLARAÇÕES LOCAIS ---
    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        if (ctx.DECLARE() != null) {
            String tipoLA = ctx.variavel().tipo().getText();
            String tipoC = mapearTipoC(tipoLA);
            TabelaDeSimbolos escopoAtual = escoposAninhados.obterEscopoAtual();
            for (LAParser.IdentificadorContext idCtx : ctx.variavel().identificador()) {
                String nomeVar = idCtx.getText();
                // Em C, strings (literal) são arrays de char
                if (tipoLA.equals("literal")) {
                    saida.append("    ").append(tipoC).append(" ").append(nomeVar).append("[80];\n");
                } else {
                    saida.append("    ").append(tipoC).append(" ").append(nomeVar).append(";\n");
                }
                // Guarda no escopo para saber o tipo na hora de fazer scanf/printf
                escopoAtual.adicionar(nomeVar, determinarTipo(tipoLA), TabelaDeSimbolos.EstruturaLA.VARIAVEL);
            }
        } else if (ctx.CONSTANTE() != null) {
            String tipoC = mapearTipoC(ctx.tipo_basico().getText());
            String nome = ctx.IDENT().getText();
            String valor = getExpressaoC(ctx.valor_constante());
            saida.append("    const ").append(tipoC).append(" ").append(nome).append(" = ").append(valor).append(";\n");
        }
        return super.visitDeclaracao_local(ctx);
    }

    // --- 2. COMANDOS BÁSICOS (LEIA, ESCREVA, ATRIBUIÇÃO) ---
    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        for (LAParser.IdentificadorContext idCtx : ctx.identificador()) {
            String nomeVar = idCtx.getText();
            TabelaDeSimbolos.EntradaTabelaDeSimbolos entrada = LASemanticoUtils.buscarSimbolo(escoposAninhados, nomeVar);
            if (entrada != null) {
                if (entrada.tipo == TabelaDeSimbolos.TipoLA.INTEIRO) {
                    saida.append("    scanf(\"%d\", &").append(nomeVar).append(");\n");
                } else if (entrada.tipo == TabelaDeSimbolos.TipoLA.REAL) {
                    saida.append("    scanf(\"%f\", &").append(nomeVar).append(");\n");
                } else if (entrada.tipo == TabelaDeSimbolos.TipoLA.LITERAL) {
                    saida.append("    gets(").append(nomeVar).append(");\n");
                }
            }
        }
        return null;
    }

    // 3. COMANDO ESCREVA
    @Override
    public Void visitCmdEscreva(LAParser.CmdEscrevaContext ctx) {
        for (LAParser.ExpressaoContext expCtx : ctx.expressao()) {
            String expressaoStr = expCtx.getText();
            // Se for apenas uma string (ex: "Olá, mundo")
            if (expressaoStr.startsWith("\"") && expressaoStr.endsWith("\"")) {
                saida.append("    printf(").append(expressaoStr).append(");\n");
            } else {
                // Se for uma variável ou expressão matemática, chama o SemanticoUtils
                TabelaDeSimbolos.TipoLA tipo = LASemanticoUtils.verificarTipo(escoposAninhados, expCtx);
                String expC = getExpressaoC(expCtx);
                if (tipo == TabelaDeSimbolos.TipoLA.INTEIRO) {
                    saida.append("    printf(\"%d\", ").append(expC).append(");\n");
                } else if (tipo == TabelaDeSimbolos.TipoLA.REAL) {
                    saida.append("    printf(\"%f\", ").append(expC).append(");\n");
                } else if (tipo == TabelaDeSimbolos.TipoLA.LITERAL) {
                    saida.append("    printf(\"%s\", ").append(expC).append(");\n");
                } else {
                    saida.append("    printf(\"%d\", ").append(expC).append(");\n"); // Fallback
                }
            }
        }
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        String ponteiro = ctx.PONTEIRO() != null ? "*" : "";
        String id = ctx.identificador().getText();
        String exp = getExpressaoC(ctx.expressao());
        TabelaDeSimbolos.EntradaTabelaDeSimbolos entrada = LASemanticoUtils.buscarSimbolo(escoposAninhados, id);
        // C não suporta 'string = string', usa strcpy
        if (entrada != null && entrada.tipo == TabelaDeSimbolos.TipoLA.LITERAL) {
            saida.append("    strcpy(").append(id).append(", ").append(exp).append(");\n");
        } else {
            saida.append("    ").append(ponteiro).append(id).append(" = ").append(exp).append(";\n");
        }
        return null;
    }

    // --- 3. CONTROLE DE FLUXO E LAÇOS ---
    @Override
    public Void visitCmdSe(LAParser.CmdSeContext ctx) {
        String exp = getExpressaoC(ctx.expressao());
        saida.append("    if (").append(exp).append(") {\n");
        boolean inSenao = false;
        for (ParseTree child : ctx.children) {
            if (child.getText().equals("senao")) {
                saida.append("    } else {\n");
                inSenao = true;
            } else if (child instanceof LAParser.CmdContext) {
                visit(child);
            }
        }
        saida.append("    }\n");
        return null;
    }

    @Override
    public Void visitCmdEnquanto(LAParser.CmdEnquantoContext ctx) {
        String exp = getExpressaoC(ctx.expressao());
        saida.append("    while (").append(exp).append(") {\n");
        for (LAParser.CmdContext cmd : ctx.cmd()) {
            visit(cmd);
        }
        saida.append("    }\n");
        return null;
    }

    @Override
    public Void visitCmdPara(LAParser.CmdParaContext ctx) {
        String id = ctx.IDENT().getText();
        String expInicio = getExpressaoC(ctx.exp_aritmetica(0));
        String expFim = getExpressaoC(ctx.exp_aritmetica(1));
        saida.append("    for (").append(id).append(" = ").append(expInicio).append("; ").append(id).append(" <= ").append(expFim).append("; ").append(id).append("++) {\n");
        for (LAParser.CmdContext cmd : ctx.cmd()) {
            visit(cmd);
        }
        saida.append("    }\n");
        return null;
    }

    @Override
    public Void visitCmdFaca(LAParser.CmdFacaContext ctx) {
        saida.append("    do {\n");
        for (LAParser.CmdContext cmd : ctx.cmd()) {
            visit(cmd);
        }
        String exp = getExpressaoC(ctx.expressao());
        saida.append("    } while (").append(exp).append(");\n");
        return null;
    }

    @Override
    public Void visitCmdCaso(LAParser.CmdCasoContext ctx) {
        String exp = getExpressaoC(ctx.exp_aritmetica());
        saida.append("    switch (").append(exp).append(") {\n");
        visit(ctx.selecao());
        boolean inSenao = false;
        for (ParseTree child : ctx.children) {
            if (child.getText().equals("senao")) {
                saida.append("    default:\n");
                inSenao = true;
            } else if (inSenao && child instanceof LAParser.CmdContext) {
                visit(child);
            }
        }
        saida.append("    }\n");
        return null;
    }

    @Override
    public Void visitItem_selecao(LAParser.Item_selecaoContext ctx) {
        String[] constantes = ctx.constantes().getText().split(",");
        for (String constante : constantes) {
            if (constante.contains("..")) {
                String[] range = constante.split("\\.\\.");
                // Extensão do GCC para ranges no case (case 1 ... 3:)
                saida.append("    case ").append(range[0]).append(" ... ").append(range[1]).append(":\n");
            } else {
                saida.append("    case ").append(constante).append(":\n");
            }
        }
        for (LAParser.CmdContext cmd : ctx.cmd()) {
            visit(cmd);
        }
        saida.append("    break;\n");
        return null;
    }

    // --- 4. FUNÇÕES UTILITÁRIAS ---
    // Desce pela árvore da expressão traduzindo operadores LA para C
    private String getExpressaoC(ParseTree ctx) {
        if (ctx == null) return "";
        if (ctx instanceof TerminalNode) {
            String text = ctx.getText();
            return switch (text) {
                case "=" -> "==";
                case "<>" -> "!=";
                case "e" -> "&&";
                case "ou" -> "||";
                case "nao" -> "!";
                case "^" -> "*"; // Derreferenciação de ponteiros
                default -> text;
            };
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            sb.append(getExpressaoC(ctx.getChild(i)));
        }
        return sb.toString();
    }

    private String mapearTipoC(String tipoLA) {
        return switch (tipoLA) {
            case "inteiro" -> "int";
            case "real" -> "float";
            case "literal" -> "char";
            default -> "int";
        };
    }

    private TabelaDeSimbolos.TipoLA determinarTipo(String tipoTexto) {
        return switch (tipoTexto) {
            case "inteiro" -> TabelaDeSimbolos.TipoLA.INTEIRO;
            case "real" -> TabelaDeSimbolos.TipoLA.REAL;
            case "literal" -> TabelaDeSimbolos.TipoLA.LITERAL;
            case "logico" -> TabelaDeSimbolos.TipoLA.LOGICO;
            default -> TabelaDeSimbolos.TipoLA.INVALIDO;
        };
    }
}