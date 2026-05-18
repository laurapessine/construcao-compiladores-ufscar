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
        saida.append("#include <string.h>\n"); // Necessário para strcpy em strings
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

    // --- 1. DECLARAÇÕES E ESTRUTURAS GLOBAIS ---
    @Override
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        String nome = ctx.IDENT().getText();
        boolean isFuncao = ctx.FUNCAO() != null;
        if (isFuncao) {
            String tipoLA = ctx.tipo_estendido().getText();
            String tipoC = mapearTipoC(tipoLA);
            if (tipoLA.equals("literal")) tipoC = "char*";
            saida.append(tipoC).append(" ").append(nome).append("(");
        } else {
            saida.append("void ").append(nome).append("(");
        }
        escoposAninhados.criarNovoEscopo();
        TabelaDeSimbolos escopoAtual = escoposAninhados.obterEscopoAtual();
        if (ctx.parametros() != null) {
            for (int i = 0; i < ctx.parametros().parametro().size(); i++) {
                LAParser.ParametroContext paramCtx = ctx.parametros().parametro(i);
                String tipoLA = paramCtx.tipo_estendido().getText();
                String tipoC = mapearTipoC(tipoLA);
                if (tipoLA.equals("literal")) tipoC = "char*";
                for (int j = 0; j < paramCtx.identificador().size(); j++) {
                    String nomeParam = paramCtx.identificador(j).getText();
                    saida.append(tipoC).append(" ");
                    if (paramCtx.getText().startsWith("var")) saida.append("*"); // Parâmetro por referência
                    saida.append(nomeParam);
                    if (j < paramCtx.identificador().size() - 1) saida.append(", ");
                    // Adiciona os parâmetros na tabela para saber os tipos depois
                    registrarVariavelNaTabela(escopoAtual, nomeParam, tipoLA);
                }
                if (i < ctx.parametros().parametro().size() - 1) saida.append(", ");
            }
        }
        saida.append(") {\n");
        for (LAParser.Declaracao_localContext decl : ctx.declaracao_local()) visit(decl);
        for (LAParser.CmdContext cmd : ctx.cmd()) visit(cmd);
        saida.append("}\n\n");
        escoposAninhados.abandonarEscopo();
        return null;
    }

    // --- 2. DECLARAÇÕES LOCAIS (variáveis, constantes, tipos e registros) ---
    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        TabelaDeSimbolos escopoAtual = escoposAninhados.obterEscopoAtual();
        if (ctx.DECLARE() != null) {
            if (ctx.variavel().tipo().registro() != null) {
                // Registro declarado na hora (inline)
                saida.append("    struct {\n");
                for (LAParser.VariavelContext varCtx : ctx.variavel().tipo().registro().variavel()) {
                    String tipoCampoLA = varCtx.tipo().getText();
                    String tipoCampoC = mapearTipoC(tipoCampoLA);
                    for (LAParser.IdentificadorContext idCtx : varCtx.identificador()) {
                        String nomeCampo = idCtx.getText();
                        if (tipoCampoLA.equals("literal")) {
                            saida.append("        ").append(tipoCampoC).append(" ").append(nomeCampo).append("[80];\n");
                        } else {
                            saida.append("        ").append(tipoCampoC).append(" ").append(nomeCampo).append(";\n");
                        }
                    }
                }
                saida.append("    } ");
                for (int i = 0; i < ctx.variavel().identificador().size(); i++) {
                    String nomeVar = ctx.variavel().identificador(i).getText();
                    saida.append(nomeVar);
                    if (i < ctx.variavel().identificador().size() - 1) saida.append(", ");
                    // Adiciona o registro e popula os campos para o gerador lembrar
                    escopoAtual.adicionar(nomeVar, TabelaDeSimbolos.TipoLA.REGISTRO, TabelaDeSimbolos.EstruturaLA.VARIAVEL);
                    LASemanticoUtils.popularRegistro(escopoAtual.verificar(nomeVar).camposRegistro, ctx.variavel().tipo().registro(), escoposAninhados);
                }
                saida.append(";\n");
            } else {
                // Declaração normal ou de um tipo já criado
                String tipoLA = ctx.variavel().tipo().getText();
                String tipoC = mapearTipoC(tipoLA);
                for (LAParser.IdentificadorContext idCtx : ctx.variavel().identificador()) {
                    String nomeVar = idCtx.getText();
                    if (tipoLA.replace("^", "").equals("literal")) {
                        saida.append("    ").append(tipoC).append(" ").append(nomeVar).append("[80];\n");
                    } else {
                        saida.append("    ").append(tipoC).append(" ").append(nomeVar).append(";\n");
                    }
                    // Ensina para a tabela qual é o tipo (mesmo se for registro customizado)
                    registrarVariavelNaTabela(escopoAtual, nomeVar, tipoLA);
                }
            }
        } else if (ctx.CONSTANTE() != null) {
            String tipoC = mapearTipoC(ctx.tipo_basico().getText());
            String nome = ctx.IDENT().getText();
            String valor = getExpressaoC(ctx.valor_constante());
            saida.append("    const ").append(tipoC).append(" ").append(nome).append(" = ").append(valor).append(";\n");
            escopoAtual.adicionar(nome, LASemanticoUtils.determinarTipo(ctx.tipo_basico().getText()), TabelaDeSimbolos.EstruturaLA.CONSTANTE);
        } else if (ctx.TIPO() != null) {
            // Definição de tipo struct
            String nomeTipo = ctx.IDENT().getText();
            if (ctx.tipo().registro() != null) {
                saida.append("typedef struct {\n");
                for (LAParser.VariavelContext varCtx : ctx.tipo().registro().variavel()) {
                    String tipoCampoLA = varCtx.tipo().getText();
                    String tipoCampoC = mapearTipoC(tipoCampoLA);
                    for (LAParser.IdentificadorContext idCtx : varCtx.identificador()) {
                        String nomeCampo = idCtx.getText();
                        if (tipoCampoLA.equals("literal")) {
                            saida.append("    ").append(tipoCampoC).append(" ").append(nomeCampo).append("[80];\n");
                        } else {
                            saida.append("    ").append(tipoCampoC).append(" ").append(nomeCampo).append(";\n");
                        }
                    }
                }
                saida.append("} ").append(nomeTipo).append(";\n");
                // Grava o "molde" da struct na tabela
                escopoAtual.adicionar(nomeTipo, TabelaDeSimbolos.TipoLA.REGISTRO, TabelaDeSimbolos.EstruturaLA.TIPO);
                LASemanticoUtils.popularRegistro(escopoAtual.verificar(nomeTipo).camposRegistro, ctx.tipo().registro(), escoposAninhados);
            }
        }
        return null;
    }

    // --- 3. COMANDOS BÁSICOS (LEIA, ESCREVA, ATRIBUIÇÃO, RETORNO) ---
    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        for (LAParser.IdentificadorContext idCtx : ctx.identificador()) {
            String nomeVarLA = idCtx.getText();
            String nomeVarC = nomeVarLA.replace("^", "*");
            String nomeBusca = nomeVarLA.replace("^", "").split("\\[")[0];
            TabelaDeSimbolos.TipoLA tipo = LASemanticoUtils.verificarTipo(escoposAninhados, nomeBusca);
            if (tipo == TabelaDeSimbolos.TipoLA.INTEIRO) {
                saida.append("    scanf(\"%d\", &").append(nomeVarC).append(");\n");
            } else if (tipo == TabelaDeSimbolos.TipoLA.REAL) {
                saida.append("    scanf(\"%f\", &").append(nomeVarC).append(");\n");
            } else if (tipo == TabelaDeSimbolos.TipoLA.LITERAL) {
                saida.append("    gets(").append(nomeVarC).append(");\n");
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
                if (tipo == TabelaDeSimbolos.TipoLA.INTEIRO)
                    saida.append("    printf(\"%d\", ").append(expC).append(");\n");
                else if (tipo == TabelaDeSimbolos.TipoLA.REAL)
                    saida.append("    printf(\"%f\", ").append(expC).append(");\n");
                else if (tipo == TabelaDeSimbolos.TipoLA.LITERAL)
                    saida.append("    printf(\"%s\", ").append(expC).append(");\n");
                else saida.append("    printf(\"%d\", ").append(expC).append(");\n"); // Fallback
            }
        }
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        String ponteiro = ctx.PONTEIRO() != null ? "*" : "";
        String idLA = ctx.identificador().getText();
        String idC = idLA.replace("^", "*");
        String expC = getExpressaoC(ctx.expressao());
        String nomeBusca = idLA.replace("^", "").split("\\[")[0];
        TabelaDeSimbolos.TipoLA tipo = LASemanticoUtils.verificarTipo(escoposAninhados, nomeBusca);
        // C não suporta 'string = string', usa strcpy
        if (tipo == TabelaDeSimbolos.TipoLA.LITERAL) {
            saida.append("    strcpy(").append(ponteiro).append(idC).append(", ").append(expC).append(");\n");
        } else {
            saida.append("    ").append(ponteiro).append(idC).append(" = ").append(expC).append(";\n");
        }
        return null;
    }

    @Override
    public Void visitCmdRetorne(LAParser.CmdRetorneContext ctx) {
        saida.append("    return ").append(getExpressaoC(ctx.expressao())).append(";\n");
        return null;
    }

    @Override
    public Void visitCmdChamada(LAParser.CmdChamadaContext ctx) {
        saida.append("    ").append(ctx.IDENT().getText()).append("(");
        for (int i = 0; i < ctx.expressao().size(); i++) {
            saida.append(getExpressaoC(ctx.expressao(i)));
            if (i < ctx.expressao().size() - 1) saida.append(", ");
        }
        saida.append(");\n");
        return null;
    }

    // --- 4. CONTROLE DE FLUXO E LAÇOS ---
    @Override
    public Void visitCmdSe(LAParser.CmdSeContext ctx) {
        String exp = getExpressaoC(ctx.expressao());
        saida.append("    if (").append(exp).append(") {\n");
        for (ParseTree child : ctx.children) {
            if (child.getText().equals("senao")) {
                saida.append("    } else {\n");
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
        for (LAParser.CmdContext cmd : ctx.cmd()) visit(cmd);
        saida.append("    }\n");
        return null;
    }

    @Override
    public Void visitCmdPara(LAParser.CmdParaContext ctx) {
        String id = ctx.IDENT().getText();
        String expInicio = getExpressaoC(ctx.exp_aritmetica(0));
        String expFim = getExpressaoC(ctx.exp_aritmetica(1));
        saida.append("    for (").append(id).append(" = ").append(expInicio).append("; ").append(id).append(" <= ").append(expFim).append("; ").append(id).append("++) {\n");
        for (LAParser.CmdContext cmd : ctx.cmd()) visit(cmd);
        saida.append("    }\n");
        return null;
    }

    @Override
    public Void visitCmdFaca(LAParser.CmdFacaContext ctx) {
        saida.append("    do {\n");
        for (LAParser.CmdContext cmd : ctx.cmd()) visit(cmd);
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
                // O GCC suporta cases em intervalo nativamente: case X ... Y:
                saida.append("    case ").append(range[0]).append(" ... ").append(range[1]).append(":\n");
            } else {
                saida.append("    case ").append(constante).append(":\n");
            }
        }
        for (LAParser.CmdContext cmd : ctx.cmd()) visit(cmd);
        saida.append("    break;\n");
        return null;
    }

    // --- 5. FUNÇÕES UTILITÁRIAS ---
    // Desce pela árvore traduzindo operadores do LA para operadores C
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
                case "^" -> "*"; // Desreferenciação
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
        String base = tipoLA.replace("^", "");
        String tipoC = switch (base) {
            case "inteiro", "logico" -> "int"; // Agrupados aqui
            case "real" -> "float";
            case "literal" -> "char";
            default -> base;
        };
        // Se for um ponteiro, insere o "*" do C
        if (tipoLA.startsWith("^")) {
            tipoC += "*";
        }
        return tipoC;
    }

    // Método auxiliar para evitar código duplicado ao registrar variáveis
    private void registrarVariavelNaTabela(TabelaDeSimbolos escopo, String nome, String tipoLA) {
        TabelaDeSimbolos.TipoLA tipoAux = LASemanticoUtils.determinarTipo(tipoLA);
        if (tipoAux == TabelaDeSimbolos.TipoLA.INVALIDO) {
            TabelaDeSimbolos.EntradaTabelaDeSimbolos entradaTipo = LASemanticoUtils.buscarSimbolo(escoposAninhados, tipoLA);
            if (entradaTipo != null) {
                escopo.adicionar(nome, TabelaDeSimbolos.TipoLA.REGISTRO, TabelaDeSimbolos.EstruturaLA.VARIAVEL);
                escopo.verificar(nome).camposRegistro = entradaTipo.camposRegistro;
            } else {
                escopo.adicionar(nome, tipoAux, TabelaDeSimbolos.EstruturaLA.VARIAVEL);
            }
        } else {
            escopo.adicionar(nome, tipoAux, TabelaDeSimbolos.EstruturaLA.VARIAVEL);
        }
    }
}