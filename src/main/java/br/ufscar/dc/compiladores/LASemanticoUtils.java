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

    // Verifica se o símbolo existe em qualquer um dos escopos abertos
    public static boolean verificarSimbolo(Escopos escopos, String nome) {
        for (TabelaDeSimbolos tabela : escopos.percorrerEscoposAninhados()) {
            if (tabela.existe(nome)) {
                return true;
            }
        }
        return false;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, String nomeVar) {
        for (TabelaDeSimbolos tabela : escopos.percorrerEscoposAninhados()) {
            if (tabela.existe(nomeVar)) {
                return tabela.verificar(nomeVar).tipo;
            }
        }
        return TabelaDeSimbolos.TipoLA.INVALIDO;
    }

    // Regra de compatibilidade de atribuição (REAL e INTEIRO)
    public static boolean verificarCompatibilidade(TabelaDeSimbolos.TipoLA tipo1, TabelaDeSimbolos.TipoLA tipo2) {
        if (tipo1 == tipo2) return true;
        if (tipo1 == TabelaDeSimbolos.TipoLA.INVALIDO || tipo2 == TabelaDeSimbolos.TipoLA.INVALIDO) return false;
        return (tipo1 == TabelaDeSimbolos.TipoLA.INTEIRO || tipo1 == TabelaDeSimbolos.TipoLA.REAL) && (tipo2 == TabelaDeSimbolos.TipoLA.INTEIRO || tipo2 == TabelaDeSimbolos.TipoLA.REAL);
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
                if ((ret == TabelaDeSimbolos.TipoLA.LITERAL && (aux == TabelaDeSimbolos.TipoLA.INTEIRO || aux == TabelaDeSimbolos.TipoLA.REAL)) ||
                        (aux == TabelaDeSimbolos.TipoLA.LITERAL && (ret == TabelaDeSimbolos.TipoLA.INTEIRO || ret == TabelaDeSimbolos.TipoLA.REAL))) {
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
                if ((ret == TabelaDeSimbolos.TipoLA.LITERAL && (aux == TabelaDeSimbolos.TipoLA.INTEIRO || aux == TabelaDeSimbolos.TipoLA.REAL)) ||
                        (aux == TabelaDeSimbolos.TipoLA.LITERAL && (ret == TabelaDeSimbolos.TipoLA.INTEIRO || ret == TabelaDeSimbolos.TipoLA.REAL))) {
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
                if ((ret == TabelaDeSimbolos.TipoLA.LITERAL && (aux == TabelaDeSimbolos.TipoLA.INTEIRO || aux == TabelaDeSimbolos.TipoLA.REAL)) ||
                        (aux == TabelaDeSimbolos.TipoLA.LITERAL && (ret == TabelaDeSimbolos.TipoLA.INTEIRO || ret == TabelaDeSimbolos.TipoLA.REAL))) {
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
        if (ctx.identificador() != null) return verificarTipo(escopos, ctx.identificador().getText());
        if (ctx.IDENT() != null) return verificarTipo(escopos, ctx.IDENT().getText()); // Chamada de função
        // Pega a primeira expressão da lista
        if (ctx.expressao() != null && !ctx.expressao().isEmpty()) {
            return verificarTipo(escopos, ctx.expressao(0));
        }
        return TabelaDeSimbolos.TipoLA.INVALIDO;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(Escopos escopos, LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.identificador() != null) {
            String nomeVar = ctx.identificador().getText();
            if (!verificarSimbolo(escopos, nomeVar)) {
                adicionarErroSemantico(ctx.identificador().start, "identificador " + nomeVar + " nao declarado");
            }
            return verificarTipo(escopos, nomeVar);
        }
        return TabelaDeSimbolos.TipoLA.LITERAL; // Se não for endereço, é CADEIA
    }
}