package br.ufscar.dc.compiladores;

public class LAGeradorC extends LAParserBaseVisitor<Void> {

    StringBuilder saida;
    TabelaDeSimbolos tabela; // Usado para descobrir tipos de variáveis

    public LAGeradorC() {
        saida = new StringBuilder();
        this.tabela = new TabelaDeSimbolos();
    }

    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx) {
        // Escreve o cabeçalho padrão da linguagem C
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("\n");

        // Visita declarações globais antes do main, se houver
        // ...

        saida.append("int main() {\n");

        // Visita o corpo do programa (declarações locais e comandos)
        visitDeclaracoes(ctx.declaracoes());
        visitCorpo(ctx.corpo());

        saida.append("    return 0;\n");
        saida.append("}\n");

        return null; // O código gerado fica guardado no StringBuilder 'saida'
    }

    // Repassa a visita para os filhos para não travar a compilação
    @Override
    public Void visitDeclaracoes(LAParser.DeclaracoesContext ctx) {
        return super.visitDeclaracoes(ctx);
    }

    @Override
    public Void visitCorpo(LAParser.CorpoContext ctx) {
        return super.visitCorpo(ctx);
    }
}