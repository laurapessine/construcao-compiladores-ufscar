package br.ufscar.dc.compiladores;

public class LASemantico extends LAParserBaseVisitor<Void> {
    TabelaDeSimbolos tabela;

    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx) {
        // Quando o programa começa, cria uma tabela de símbolos
        tabela = new TabelaDeSimbolos();
        // Continua a visitação para os "filhos" do nó programa (declarações e corpo)
        return super.visitPrograma(ctx);
    }

    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        // 1. O caminho do "declare" (variáveis)
        if (ctx.DECLARE() != null) {
            String tipoTexto = ctx.variavel().tipo().getText();
            TabelaDeSimbolos.TipoLA tipoVariavel = determinarTipo(tipoTexto);
            // Uma mesma linha de "declare" pode ter várias variáveis separadas por vírgula
            for (LAParser.IdentificadorContext idCtx : ctx.variavel().identificador()) {
                String nomeVar = idCtx.getText();
                // Erro 1: Identificador já declarado
                if (tabela.existe(nomeVar)) {
                    LASemanticoUtils.adicionarErroSemantico(idCtx.start, "identificador " + nomeVar + " ja declarado anteriormente");
                } else {
                    tabela.adicionar(nomeVar, tipoVariavel, TabelaDeSimbolos.EstruturaLA.VARIAVEL, tipoTexto);
                }
            }
        }
        // 2. O caminho da "constante"
        else if (ctx.CONSTANTE() != null) {
            String nomeConst = ctx.IDENT().getText();
            if (tabela.existe(nomeConst)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nomeConst + " ja declarado anteriormente");
            } else {
                TabelaDeSimbolos.TipoLA tipoConst = determinarTipo(ctx.tipo_basico().getText());
                tabela.adicionar(nomeConst, tipoConst, TabelaDeSimbolos.EstruturaLA.CONSTANTE);
            }
        }
        // 3. O caminho do "tipo" (tipos criados pelo usuário)
        else if (ctx.TIPO() != null) {
            String nomeTipo = ctx.IDENT().getText();
            if (tabela.existe(nomeTipo)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nomeTipo + " ja declarado anteriormente");
            } else {
                // Tipos criados pelo usuário entram como REGISTRO ou outro tipo estendido
                tabela.adicionar(nomeTipo, TabelaDeSimbolos.TipoLA.REGISTRO, TabelaDeSimbolos.EstruturaLA.TIPO);
            }
        }
        return super.visitDeclaracao_local(ctx);
    }

    // Converte a string do código-fonte para o Enum interno
    private TabelaDeSimbolos.TipoLA determinarTipo(String tipoTexto) {
        // Remove marcadores de ponteiro se existirem para pegar o tipo base
        tipoTexto = tipoTexto.replace("^", "");
        return switch (tipoTexto) {
            case "inteiro" -> TabelaDeSimbolos.TipoLA.INTEIRO;
            case "real" -> TabelaDeSimbolos.TipoLA.REAL;
            case "literal" -> TabelaDeSimbolos.TipoLA.LITERAL;
            case "logico" -> TabelaDeSimbolos.TipoLA.LOGICO;
            default -> TabelaDeSimbolos.TipoLA.INVALIDO; // Para tipos customizados ou registros
        };
    }
}