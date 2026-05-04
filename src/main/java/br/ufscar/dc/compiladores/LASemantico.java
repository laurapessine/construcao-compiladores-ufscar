package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.Token;

public class LASemantico extends LAParserBaseVisitor<Void> {
    Escopos escoposAninhados;

    // Método auxiliar para inserir na tabela
    private void inserirNaTabela(String nomeId, LAParser.TipoContext tipoCtx, Token nomeToken) {
        TabelaDeSimbolos escopoAtual = escoposAninhados.obterEscopoAtual();
        // Verifica se já não existe no mesmo escopo (Erro 1)
        if (escopoAtual.existe(nomeId)) {
            LASemanticoUtils.adicionarErroSemantico(nomeToken, "identificador " + nomeId + " ja declarado anteriormente");
            return;
        }
        // É um ponteiro?
        boolean ehPonteiro = tipoCtx.tipo_estendido() != null && tipoCtx.tipo_estendido().PONTEIRO() != null;
        TabelaDeSimbolos.TipoLA tipo = TabelaDeSimbolos.TipoLA.INVALIDO;
        String nomeTipoEstendido = null;
        if (tipoCtx.tipo_estendido() != null && tipoCtx.tipo_estendido().tipo_basico_ident() != null) {
            // Pode ser um tipo básico (inteiro, real...) ou um tipo definido pelo usuário (registro)
            if (tipoCtx.tipo_estendido().tipo_basico_ident().tipo_basico() != null) {
                // É um tipo básico
                String strTipo = tipoCtx.tipo_estendido().tipo_basico_ident().tipo_basico().getText();
                tipo = switch (strTipo) {
                    case "inteiro" -> TabelaDeSimbolos.TipoLA.INTEIRO;
                    case "real" -> TabelaDeSimbolos.TipoLA.REAL;
                    case "literal" -> TabelaDeSimbolos.TipoLA.LITERAL;
                    case "logico" -> TabelaDeSimbolos.TipoLA.LOGICO;
                    default -> TabelaDeSimbolos.TipoLA.INVALIDO;
                };
            } else {
                nomeTipoEstendido = tipoCtx.tipo_estendido().tipo_basico_ident().IDENT().getText();
                TabelaDeSimbolos.EntradaTabelaDeSimbolos entrada = LASemanticoUtils.buscarSimbolo(escoposAninhados, nomeTipoEstendido);
                if (entrada != null && entrada.estrutura == TabelaDeSimbolos.EstruturaLA.TIPO) {
                    tipo = TabelaDeSimbolos.TipoLA.REGISTRO;
                } else {
                    LASemanticoUtils.adicionarErroSemantico(tipoCtx.tipo_estendido().tipo_basico_ident().IDENT().getSymbol(), "tipo " + nomeTipoEstendido + " nao declarado");
                    return;
                }
            }
        } else if (tipoCtx.registro() != null) {
            tipo = TabelaDeSimbolos.TipoLA.REGISTRO;
        }
        if (ehPonteiro) {
            tipo = TabelaDeSimbolos.TipoLA.ENDERECO;
        }
        escopoAtual.adicionar(nomeId, tipo, TabelaDeSimbolos.EstruturaLA.VARIAVEL, nomeTipoEstendido);
        if (tipoCtx.registro() != null) {
            popularRegistro(escopoAtual.verificar(nomeId).camposRegistro, tipoCtx.registro());
        } else if (nomeTipoEstendido != null) {
            TabelaDeSimbolos.EntradaTabelaDeSimbolos entradaTipo = LASemanticoUtils.buscarSimbolo(escoposAninhados, nomeTipoEstendido);
            if (entradaTipo != null && entradaTipo.camposRegistro != null) {
                escopoAtual.verificar(nomeId).camposRegistro = entradaTipo.camposRegistro;
            }
        }
    }

    // Método auxiliar para popular registros
    private void popularRegistro(TabelaDeSimbolos tabelaRegistro, LAParser.RegistroContext ctx) {
        for (LAParser.VariavelContext varCtx : ctx.variavel()) {
            for (LAParser.IdentificadorContext identCtx : varCtx.identificador()) {
                String nomeCampo = identCtx.getText();
                TabelaDeSimbolos.TipoLA tipoCampo = TabelaDeSimbolos.TipoLA.INVALIDO;
                String nomeTipoEstendido = null;
                if (varCtx.tipo().tipo_estendido() != null && varCtx.tipo().tipo_estendido().tipo_basico_ident() != null) {
                    if (varCtx.tipo().tipo_estendido().tipo_basico_ident().tipo_basico() != null) {
                        String strTipo = varCtx.tipo().tipo_estendido().tipo_basico_ident().tipo_basico().getText();
                        tipoCampo = switch (strTipo) {
                            case "inteiro" -> TabelaDeSimbolos.TipoLA.INTEIRO;
                            case "real" -> TabelaDeSimbolos.TipoLA.REAL;
                            case "literal" -> TabelaDeSimbolos.TipoLA.LITERAL;
                            case "logico" -> TabelaDeSimbolos.TipoLA.LOGICO;
                            default -> TabelaDeSimbolos.TipoLA.INVALIDO;
                        };
                    } else {
                        nomeTipoEstendido = varCtx.tipo().tipo_estendido().tipo_basico_ident().IDENT().getText();
                        tipoCampo = TabelaDeSimbolos.TipoLA.REGISTRO;
                    }
                }
                tabelaRegistro.adicionar(nomeCampo, tipoCampo, TabelaDeSimbolos.EstruturaLA.VARIAVEL, nomeTipoEstendido);
                if (nomeTipoEstendido != null) {
                    TabelaDeSimbolos.EntradaTabelaDeSimbolos entradaTipo = LASemanticoUtils.buscarSimbolo(escoposAninhados, nomeTipoEstendido);
                    if (entradaTipo != null && entradaTipo.camposRegistro != null) {
                        tabelaRegistro.verificar(nomeCampo).camposRegistro = entradaTipo.camposRegistro;
                    }
                }
            }
        }
    }

    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx) {
        escoposAninhados = new Escopos();
        // Continua a visitação para os "filhos" do nó programa (declarações e corpo)
        return super.visitPrograma(ctx);
    }

    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        TabelaDeSimbolos escopoAtual = escoposAninhados.obterEscopoAtual();
        // 1. O caminho do "declare" (variáveis)
        if (ctx.DECLARE() != null) {
            String tipoTexto = ctx.variavel().tipo().getText();
            TabelaDeSimbolos.TipoLA tipoVariavel = determinarTipo(tipoTexto);
            // ERRO 2: Tipo não declarado
            // Se o tipo for INVALIDO, significa que não é um tipo básico. Checa se o usuário criou esse tipo.
            if (tipoVariavel == TabelaDeSimbolos.TipoLA.INVALIDO) {
                String nomeTipo = tipoTexto.replace("^", "");
                if (!LASemanticoUtils.verificarSimbolo(escoposAninhados, nomeTipo)) {
                    LASemanticoUtils.adicionarErroSemantico(ctx.variavel().tipo().start, "tipo " + nomeTipo + " nao declarado");
                }
            }
            // Uma mesma linha de "declare" pode ter várias variáveis separadas por vírgula
            for (LAParser.IdentificadorContext idCtx : ctx.variavel().identificador()) {
                String nomeVar = idCtx.getText();
                // ERRO 1: Identificador já declarado (só olha para o escopo atual)
                if (escopoAtual.existe(nomeVar)) {
                    LASemanticoUtils.adicionarErroSemantico(idCtx.start, "identificador " + nomeVar + " ja declarado anteriormente");
                } else {
                    escopoAtual.adicionar(nomeVar, tipoVariavel, TabelaDeSimbolos.EstruturaLA.VARIAVEL, tipoTexto);
                }
            }
        // 2. O caminho da "constante"
        } else if (ctx.CONSTANTE() != null) {
            String nomeConst = ctx.IDENT().getText();
            if (escopoAtual.existe(nomeConst)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nomeConst + " ja declarado anteriormente");
            } else {
                TabelaDeSimbolos.TipoLA tipoConst = determinarTipo(ctx.tipo_basico().getText());
                escopoAtual.adicionar(nomeConst, tipoConst, TabelaDeSimbolos.EstruturaLA.CONSTANTE);
            }
        // 3. O caminho do "tipo" (tipos criados pelo usuário)
        } else if (ctx.TIPO() != null) {
            String nomeTipo = ctx.IDENT().getText();
            if (escopoAtual.existe(nomeTipo)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nomeTipo + " ja declarado anteriormente");
            } else {
                // Tipos criados pelo usuário entram como REGISTRO ou outro tipo estendido
                escopoAtual.adicionar(nomeTipo, TabelaDeSimbolos.TipoLA.REGISTRO, TabelaDeSimbolos.EstruturaLA.TIPO);
            }
        }
        return super.visitDeclaracao_local(ctx);
    }

    // ERRO 3: Identificador não declarado (exemplo no leia)
    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        for (LAParser.IdentificadorContext idCtx : ctx.identificador()) {
            String nomeVar = idCtx.getText();
            if (!LASemanticoUtils.verificarSimbolo(escoposAninhados, nomeVar)) {
                LASemanticoUtils.adicionarErroSemantico(idCtx.start, "identificador " + nomeVar + " nao declarado");
            }
        }
        return super.visitCmdLeia(ctx);
    }

    // ERRO 3 (não declarado) e ERRO 4 (incompatibilidade de tipos) na atribuição
    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        String nomeVar = ctx.identificador().getText();
        if (!LASemanticoUtils.verificarSimbolo(escoposAninhados, nomeVar)) {
            LASemanticoUtils.adicionarErroSemantico(ctx.identificador().start, "identificador " + nomeVar + " nao declarado");
        } else {
            // Descobre o tipo da variável e o tipo do lado direito da conta
            TabelaDeSimbolos.TipoLA tipoVariavel = LASemanticoUtils.verificarTipo(escoposAninhados, nomeVar);
            TabelaDeSimbolos.TipoLA tipoExpressao = LASemanticoUtils.verificarTipo(escoposAninhados, ctx.expressao());
            // Se for um ponteiro (ex: ^x <- 5), inclui o ^ na mensagem de erro
            String nomeVariavelCompleto = ctx.PONTEIRO() != null ? "^" + nomeVar : nomeVar;
            if (tipoVariavel != TabelaDeSimbolos.TipoLA.INVALIDO) {
                if (!LASemanticoUtils.verificarCompatibilidade(tipoVariavel, tipoExpressao)) {
                    LASemanticoUtils.adicionarErroSemantico(ctx.identificador().start, "atribuicao nao compativel para " + nomeVariavelCompleto);
                }
            }
        }
        return super.visitCmdAtribuicao(ctx);
    }

    // ERRO 3: Identificador não declarado (exemplo no uso dentro de expressões/contas matemáticas)
    @Override
    public Void visitParcela_unario(LAParser.Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            String nomeVar = ctx.identificador().getText();
            if (!LASemanticoUtils.verificarSimbolo(escoposAninhados, nomeVar)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.identificador().start, "identificador " + nomeVar + " nao declarado");
            }
        }
        return super.visitParcela_unario(ctx);
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