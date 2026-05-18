package br.ufscar.dc.compiladores;

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
        saida.append("\n");
        // Visita declarações globais antes do main, se houver
        saida.append("int main() {\n");
        // Visita o corpo do programa (declarações locais e comandos)
        visitDeclaracoes(ctx.declaracoes());
        visitCorpo(ctx.corpo());
        saida.append("    return 0;\n");
        saida.append("}\n");
        return null; // O código gerado fica guardado no StringBuilder 'saida'
    }

    // 1. DECLARAÇÃO DE VARIÁVEIS
    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        if (ctx.DECLARE() != null) {
            String tipoLA = ctx.variavel().tipo().getText();
            String tipoC = "";
            // Traduz o tipo de LA para C
            switch (tipoLA) {
                case "inteiro":
                    tipoC = "int";
                    break;
                case "real":
                    tipoC = "float";
                    break;
                case "literal":
                    tipoC = "char";
                    break;
                default:
                    tipoC = "int";
                    break;
            }
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
        }
        return super.visitDeclaracao_local(ctx);
    }

    // 2. COMANDO LEIA
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
                if (tipo == TabelaDeSimbolos.TipoLA.INTEIRO) {
                    saida.append("    printf(\"%d\", ").append(expressaoStr).append(");\n");
                } else if (tipo == TabelaDeSimbolos.TipoLA.REAL) {
                    saida.append("    printf(\"%f\", ").append(expressaoStr).append(");\n");
                } else if (tipo == TabelaDeSimbolos.TipoLA.LITERAL) {
                    saida.append("    printf(\"%s\", ").append(expressaoStr).append(");\n");
                } else {
                    saida.append("    printf(\"%d\", ").append(expressaoStr).append(");\n"); // Fallback
                }
            }
        }
        return null;
    }

    // Método utilitário
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