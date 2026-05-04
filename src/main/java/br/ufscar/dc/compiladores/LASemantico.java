package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.Token;

import java.util.List;

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
                // Corta o [ da declaração
                if (nomeCampo.contains("[")) nomeCampo = nomeCampo.split("\\[")[0];
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
        // O caminho do "declare" (variáveis)
        if (ctx.DECLARE() != null) {
            for (LAParser.IdentificadorContext idCtx : ctx.variavel().identificador()) {
                String nomeVar = idCtx.getText();
                // Corta o [ da declaração
                if (nomeVar.contains("[")) nomeVar = nomeVar.split("\\[")[0];
                inserirNaTabela(nomeVar, ctx.variavel().tipo(), idCtx.start);
            }
        } else if (ctx.CONSTANTE() != null) {
            String nomeConst = ctx.IDENT().getText();
            if (escopoAtual.existe(nomeConst)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nomeConst + " ja declarado anteriormente");
            } else {
                TabelaDeSimbolos.TipoLA tipoConst = determinarTipo(ctx.tipo_basico().getText());
                escopoAtual.adicionar(nomeConst, tipoConst, TabelaDeSimbolos.EstruturaLA.CONSTANTE);
            }
            // O caminho do "tipo" (tipos criados pelo usuário)
        } else if (ctx.TIPO() != null) {
            String nomeTipo = ctx.IDENT().getText();
            if (escopoAtual.existe(nomeTipo)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nomeTipo + " ja declarado anteriormente");
            } else {
                // Ao criar um tipo (ex: tipo aluno : registro...), insere no escopo atual e já popula os campos
                escopoAtual.adicionar(nomeTipo, TabelaDeSimbolos.TipoLA.REGISTRO, TabelaDeSimbolos.EstruturaLA.TIPO);
                if (ctx.tipo().registro() != null) {
                    popularRegistro(escopoAtual.verificar(nomeTipo).camposRegistro, ctx.tipo().registro());
                }
            }
        }
        return super.visitDeclaracao_local(ctx);
    }

    @Override
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        String nome = ctx.IDENT().getText();
        TabelaDeSimbolos escopoAtual = escoposAninhados.obterEscopoAtual();
        // Verifica se o nome da função/procedimento já foi usado
        if (escopoAtual.existe(nome)) {
            LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nome + " ja declarado anteriormente");
            return super.visitDeclaracao_global(ctx);
        }
        TabelaDeSimbolos.EstruturaLA estrutura = ctx.FUNCAO() != null ? TabelaDeSimbolos.EstruturaLA.FUNCAO : TabelaDeSimbolos.EstruturaLA.PROCEDIMENTO;
        TabelaDeSimbolos.TipoLA tipoRetorno = ctx.tipo_estendido() != null ? determinarTipo(ctx.tipo_estendido().getText()) : TabelaDeSimbolos.TipoLA.INVALIDO;
        TabelaDeSimbolos.EntradaTabelaDeSimbolos entrada = new TabelaDeSimbolos.EntradaTabelaDeSimbolos(nome, tipoRetorno, estrutura);
        escopoAtual.adicionar(entrada);
        // Abre um novo escopo DENTRO da função
        escoposAninhados.criarNovoEscopo();
        TabelaDeSimbolos escopoFuncao = escoposAninhados.obterEscopoAtual();
        // Adiciona os parâmetros na tabela da função (assinatura) E no escopo local como variáveis
        if (ctx.parametros() != null) {
            for (LAParser.ParametroContext paramCtx : ctx.parametros().parametro()) {
                for (LAParser.IdentificadorContext idCtx : paramCtx.identificador()) {
                    String nomeParam = idCtx.getText();
                    // Limpa qualquer sujeira da gramática e colchetes
                    nomeParam = nomeParam.replace("var", "").replace(":", "").trim();
                    if (nomeParam.contains("[")) nomeParam = nomeParam.split("\\[")[0];
                    TabelaDeSimbolos.TipoLA tipoParam = determinarTipo(paramCtx.tipo_estendido().getText());
                    // Acerta o tipo para REGISTRO primeiro
                    String nomeTipoEstendido = null;
                    if (tipoParam == TabelaDeSimbolos.TipoLA.INVALIDO || tipoParam == TabelaDeSimbolos.TipoLA.REGISTRO) {
                        nomeTipoEstendido = paramCtx.tipo_estendido().tipo_basico_ident().IDENT().getText();
                        tipoParam = TabelaDeSimbolos.TipoLA.REGISTRO;
                    }
                    // Com o tipo correto, salva na assinatura da função
                    entrada.tiposParametros.add(tipoParam);
                    escopoFuncao.adicionar(nomeParam, tipoParam, TabelaDeSimbolos.EstruturaLA.VARIAVEL, nomeTipoEstendido);
                    // Popula o parâmetro com os campos
                    if (nomeTipoEstendido != null) {
                        TabelaDeSimbolos.EntradaTabelaDeSimbolos entradaTipo = LASemanticoUtils.buscarSimbolo(escoposAninhados, nomeTipoEstendido);
                        if (entradaTipo != null && entradaTipo.camposRegistro != null) {
                            escopoFuncao.verificar(nomeParam).camposRegistro = entradaTipo.camposRegistro;
                        }
                    }
                }
            }
        }
        super.visitDeclaracao_global(ctx);
        // Fecha o escopo quando a função termina
        escoposAninhados.abandonarEscopo();
        return null; // O super já foi chamado antes de fechar o escopo
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

    // ERRO 3 e 2: Variáveis e Funções em expressões
    @Override
    public Void visitParcela_unario(LAParser.Parcela_unarioContext ctx) {
        // 1. Verifica se é uma variável normal
        if (ctx.identificador() != null) {
            String nomeVar = ctx.identificador().getText();
            if (LASemanticoUtils.buscarSimbolo(escoposAninhados, nomeVar) == null) {
                LASemanticoUtils.adicionarErroSemantico(ctx.identificador().start, "identificador " + nomeVar + " nao declarado");
            }
        }
        // 2. Verifica se é uma chamada de função
        if (ctx.IDENT() != null) {
            String nomeFunc = ctx.IDENT().getText();
            TabelaDeSimbolos.EntradaTabelaDeSimbolos entrada = LASemanticoUtils.buscarSimbolo(escoposAninhados, nomeFunc);
            if (entrada == null) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nomeFunc + " nao declarado");
            } else {
                List<TabelaDeSimbolos.TipoLA> tiposEsperados = entrada.tiposParametros;
                List<LAParser.ExpressaoContext> expressoes = ctx.expressao();
                if (tiposEsperados.size() != expressoes.size()) {
                    LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "incompatibilidade de parametros na chamada de " + nomeFunc);
                } else {
                    for (int i = 0; i < tiposEsperados.size(); i++) {
                        TabelaDeSimbolos.TipoLA tipoEsperado = tiposEsperados.get(i);
                        TabelaDeSimbolos.TipoLA tipoPassado = LASemanticoUtils.verificarTipo(escoposAninhados, expressoes.get(i));
                        // Na função, o tipo tem que ser exatamente o mesmo
                        if (tipoEsperado != tipoPassado) {
                            LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "incompatibilidade de parametros na chamada de " + nomeFunc);
                            break;
                        }
                    }
                }
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

    @Override
    public Void visitCmdRetorne(LAParser.CmdRetorneContext ctx) {
        // Checa se o comando "retorne" está solto no algoritmo principal ou procedimento
        boolean dentroDeFuncao = false;
        // Pula de pai em pai na árvore para tentar achar a declaração de função
        org.antlr.v4.runtime.RuleContext regraAtual = ctx;
        while (regraAtual != null) {
            if (regraAtual instanceof LAParser.Declaracao_globalContext && ((LAParser.Declaracao_globalContext) regraAtual).FUNCAO() != null) {
                dentroDeFuncao = true;
                break;
            }
            regraAtual = regraAtual.parent;
        }
        if (!dentroDeFuncao) {
            LASemanticoUtils.adicionarErroSemantico(ctx.start, "comando retorne nao permitido nesse escopo");
        }
        return super.visitCmdRetorne(ctx);
    }

    // ERRO 3 e 2: Chamada de procedimento (parâmetros errados e não declarado)
    @Override
    public Void visitCmdChamada(LAParser.CmdChamadaContext ctx) {
        String nome = ctx.IDENT().getText();
        TabelaDeSimbolos.EntradaTabelaDeSimbolos entrada = LASemanticoUtils.buscarSimbolo(escoposAninhados, nome);
        if (entrada == null) {
            LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nome + " nao declarado");
        } else {
            // Pega as listas de parâmetros esperados e argumentos passados
            List<TabelaDeSimbolos.TipoLA> tiposEsperados = entrada.tiposParametros;
            List<LAParser.ExpressaoContext> expressoes = ctx.expressao();
            // Checa a quantidade de parâmetros
            if (tiposEsperados.size() != expressoes.size()) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "incompatibilidade de parametros na chamada de " + nome);
            } else {
                // Checa o tipo de cada parâmetro
                for (int i = 0; i < tiposEsperados.size(); i++) {
                    TabelaDeSimbolos.TipoLA tipoEsperado = tiposEsperados.get(i);
                    TabelaDeSimbolos.TipoLA tipoPassado = LASemanticoUtils.verificarTipo(escoposAninhados, expressoes.get(i));
                    // Na chamada de procedimento, o tipo também deve ser exatamente igual
                    if (tipoEsperado != tipoPassado) {
                        LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "incompatibilidade de parametros na chamada de " + nome);
                        break; // Mostra o erro uma vez só para essa chamada
                    }
                }
            }
        }
        return super.visitCmdChamada(ctx);
    }
}