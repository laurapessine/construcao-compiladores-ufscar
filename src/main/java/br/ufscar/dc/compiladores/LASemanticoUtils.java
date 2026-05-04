package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class LASemanticoUtils {
    // Lista para guardar os erros semânticos encontrados
    public static List<String> errosSemanticos = new ArrayList<>();

    public static void adicionarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        errosSemanticos.add("Linha " + linha + ": " + mensagem);
    }

    // Usa a busca com pontos e colchetes
    public static boolean verificarSimbolo(Escopos escopos, String nomeVar) {
        return buscarSimbolo(escopos, nomeVar) != null;
    }

    // Garante que os identificadores com "." peguem o tipo certo do último pedaço
    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, String nomeVar) {
        TabelaDeSimbolos.EntradaTabelaDeSimbolos entrada = buscarSimbolo(escopos, nomeVar);
        if (entrada != null) {
            return entrada.tipo;
        }
        return TabelaDeSimbolos.TipoLA.INVALIDO;
    }

    // Busca o símbolo, mas ignora os colchetes de vetores
    public static TabelaDeSimbolos.EntradaTabelaDeSimbolos buscarSimbolo(Escopos escopos, String nomeVar) {
        // Ignora os colchetes para procurar a raiz da variável (ex: valor[0] vira valor)
        if (nomeVar.contains("[")) {
            nomeVar = nomeVar.split("\\[")[0];
        }
        if (!nomeVar.contains(".")) {
            for (TabelaDeSimbolos tabela : escopos.percorrerEscoposAninhados()) {
                if (tabela.existe(nomeVar)) {
                    return tabela.verificar(nomeVar);
                }
            }
            return null;
        }
        // Se for uma busca composta (ex: ponto1.x), quebra o nome
        String[] partes = nomeVar.split("\\.");
        TabelaDeSimbolos tabelaAtual = null;
        // Acha a primeira parte (ex: ponto1) nos escopos globais/locais
        for (TabelaDeSimbolos tabela : escopos.percorrerEscoposAninhados()) {
            if (tabela.existe(partes[0])) {
                tabelaAtual = tabela.verificar(partes[0]).camposRegistro;
                break;
            }
        }
        if (tabelaAtual == null) return null;
        // Desce nos registros (ex: aluno.endereco.rua)
        for (int i = 1; i < partes.length - 1; i++) {
            if (tabelaAtual.existe(partes[i])) {
                tabelaAtual = tabelaAtual.verificar(partes[i]).camposRegistro;
                if (tabelaAtual == null) return null;
            } else {
                return null;
            }
        }
        // Retorna a última parte (ex: x, ou rua)
        String ultimaParte = partes[partes.length - 1];
        if (tabelaAtual.existe(ultimaParte)) {
            return tabelaAtual.verificar(ultimaParte);
        }
        return null;
    }

    // Regra de compatibilidade de atribuição
    public static boolean verificarCompatibilidade(TabelaDeSimbolos.TipoLA tipo1, TabelaDeSimbolos.TipoLA tipo2) {
        if (tipo1 == tipo2) return true;
        if (tipo1 == TabelaDeSimbolos.TipoLA.INVALIDO || tipo2 == TabelaDeSimbolos.TipoLA.INVALIDO) return false;
        // Inteiro e Real
        if ((tipo1 == TabelaDeSimbolos.TipoLA.INTEIRO || tipo1 == TabelaDeSimbolos.TipoLA.REAL) && (tipo2 == TabelaDeSimbolos.TipoLA.INTEIRO || tipo2 == TabelaDeSimbolos.TipoLA.REAL)) {
            return true;
        }
        // Ponteiros só aceitam endereços de memória, ou dados numéricos (se for dereferênciação)
        if (tipo1 == TabelaDeSimbolos.TipoLA.ENDERECO && tipo2 == TabelaDeSimbolos.TipoLA.ENDERECO) return true;
        return tipo1 == TabelaDeSimbolos.TipoLA.ENDERECO && (tipo2 == TabelaDeSimbolos.TipoLA.INTEIRO || tipo2 == TabelaDeSimbolos.TipoLA.REAL);
    }

    // ==============================================================
    // MOTOR DE INFERÊNCIA DE TIPOS (desce a árvore da Expressão)
    // ==============================================================
    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.ExpressaoContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (LAParser.Termo_logicoContext tl : ctx.termo_logico()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(escopos, tl);
            if (ret == null) ret = aux;
            else if (ret != aux && aux != TabelaDeSimbolos.TipoLA.INVALIDO) ret = TabelaDeSimbolos.TipoLA.INVALIDO;
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Termo_logicoContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (LAParser.Fator_logicoContext fl : ctx.fator_logico()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(escopos, fl);
            if (ret == null) ret = aux;
            else if (ret != aux && aux != TabelaDeSimbolos.TipoLA.INVALIDO) ret = TabelaDeSimbolos.TipoLA.INVALIDO;
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Fator_logicoContext ctx) {
        return verificarTipo(escopos, ctx.parcela_logica());
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null) {
            return verificarTipo(escopos, ctx.exp_relacional());
        } else {
            return TabelaDeSimbolos.TipoLA.LOGICO; // "verdadeiro" ou "falso"
        }
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Exp_relacionalContext ctx) {
        if (ctx.exp_aritmetica().size() == 1) {
            return verificarTipo(escopos, ctx.exp_aritmetica(0));
        } else {
            // Se tem duas expressões aritméticas (ex: 2 > 1), o resultado da comparação é LOGICO
            TabelaDeSimbolos.TipoLA aux1 = verificarTipo(escopos, ctx.exp_aritmetica(0));
            TabelaDeSimbolos.TipoLA aux2 = verificarTipo(escopos, ctx.exp_aritmetica(1));
            if (verificarCompatibilidade(aux1, aux2)) {
                return TabelaDeSimbolos.TipoLA.LOGICO;
            }
            return TabelaDeSimbolos.TipoLA.INVALIDO;
        }
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Exp_aritmeticaContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (LAParser.TermoContext te : ctx.termo()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(escopos, te);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoLA.INVALIDO) {
                // Se misturou Literal com qualquer número (Inteiro ou Real), a conta quebra
                if ((ret == TabelaDeSimbolos.TipoLA.LITERAL && (aux == TabelaDeSimbolos.TipoLA.INTEIRO || aux == TabelaDeSimbolos.TipoLA.REAL)) || (aux == TabelaDeSimbolos.TipoLA.LITERAL && (ret == TabelaDeSimbolos.TipoLA.INTEIRO || ret == TabelaDeSimbolos.TipoLA.REAL))) {
                    ret = TabelaDeSimbolos.TipoLA.INVALIDO;
                } else if (verificarCompatibilidade(ret, aux)) {
                    if (ret == TabelaDeSimbolos.TipoLA.INTEIRO && aux == TabelaDeSimbolos.TipoLA.REAL) {
                        ret = TabelaDeSimbolos.TipoLA.REAL;
                    }
                } else {
                    ret = TabelaDeSimbolos.TipoLA.INVALIDO;
                }
            }
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.TermoContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (LAParser.FatorContext fa : ctx.fator()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(escopos, fa);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoLA.INVALIDO) {
                if ((ret == TabelaDeSimbolos.TipoLA.LITERAL && (aux == TabelaDeSimbolos.TipoLA.INTEIRO || aux == TabelaDeSimbolos.TipoLA.REAL)) || (aux == TabelaDeSimbolos.TipoLA.LITERAL && (ret == TabelaDeSimbolos.TipoLA.INTEIRO || ret == TabelaDeSimbolos.TipoLA.REAL))) {
                    ret = TabelaDeSimbolos.TipoLA.INVALIDO;
                } else if (verificarCompatibilidade(ret, aux)) {
                    if (ret == TabelaDeSimbolos.TipoLA.INTEIRO && aux == TabelaDeSimbolos.TipoLA.REAL) {
                        ret = TabelaDeSimbolos.TipoLA.REAL;
                    }
                } else {
                    ret = TabelaDeSimbolos.TipoLA.INVALIDO;
                }
            }
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.FatorContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (LAParser.ParcelaContext pa : ctx.parcela()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(escopos, pa);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoLA.INVALIDO) {
                if ((ret == TabelaDeSimbolos.TipoLA.LITERAL && (aux == TabelaDeSimbolos.TipoLA.INTEIRO || aux == TabelaDeSimbolos.TipoLA.REAL)) || (aux == TabelaDeSimbolos.TipoLA.LITERAL && (ret == TabelaDeSimbolos.TipoLA.INTEIRO || ret == TabelaDeSimbolos.TipoLA.REAL))) {
                    ret = TabelaDeSimbolos.TipoLA.INVALIDO;
                } else if (verificarCompatibilidade(ret, aux)) {
                    if (ret == TabelaDeSimbolos.TipoLA.INTEIRO && aux == TabelaDeSimbolos.TipoLA.REAL) {
                        ret = TabelaDeSimbolos.TipoLA.REAL;
                    }
                } else {
                    ret = TabelaDeSimbolos.TipoLA.INVALIDO;
                }
            }
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null) return verificarTipo(escopos, ctx.parcela_unario());
        return verificarTipo(escopos, ctx.parcela_nao_unario());
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Parcela_unarioContext ctx) {
        if (ctx.NUM_INT() != null) return TabelaDeSimbolos.TipoLA.INTEIRO;
        if (ctx.NUM_REAL() != null) return TabelaDeSimbolos.TipoLA.REAL;
        if (ctx.identificador() != null) {
            String nomeVar = ctx.identificador().getText();
            TabelaDeSimbolos.EntradaTabelaDeSimbolos entrada = buscarSimbolo(escopos, nomeVar);
            if (entrada != null) {
                // Se ^x (ponteiro), o tipo não é ENDERECO, é o tipo do dado apontado
                if (ctx.PONTEIRO() != null) {
                    return entrada.tipo;
                }
                // Se é variável normal, retorna o tipo dela
                return entrada.tipo;
            }
        }
        // Funções (identificador com parâmetros entre parênteses)
        if (ctx.IDENT() != null) {
            TabelaDeSimbolos.EntradaTabelaDeSimbolos func = buscarSimbolo(escopos, ctx.IDENT().getText());
            if (func != null) return func.tipo;
        }
        if (ctx.expressao() != null && !ctx.expressao().isEmpty()) {
            return verificarTipo(escopos, ctx.expressao(0));
        }
        return TabelaDeSimbolos.TipoLA.INVALIDO;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.identificador() != null) {
            String nomeVar = ctx.identificador().getText();
            if (buscarSimbolo(escopos, nomeVar) == null) {
                adicionarErroSemantico(ctx.identificador().start, "identificador " + nomeVar + " nao declarado");
            }
            // Se usou & (endereço), o tipo dessa parcela é ENDERECO
            if (ctx.ENDERECO() != null) {
                return TabelaDeSimbolos.TipoLA.ENDERECO;
            }
            TabelaDeSimbolos.EntradaTabelaDeSimbolos ent = buscarSimbolo(escopos, nomeVar);
            return ent != null ? ent.tipo : TabelaDeSimbolos.TipoLA.INVALIDO;
        }
        return TabelaDeSimbolos.TipoLA.LITERAL; // CADEIA
    }
}