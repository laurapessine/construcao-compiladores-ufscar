package br.ufscar.dc.compiladores;

import java.util.HashMap;
import java.util.Map;

public class TabelaDeSimbolos {
    // Representa os tipos básicos da linguagem LA e um tipo INVALIDO para erros
    public enum TipoLA {
        INTEIRO, REAL, LITERAL, LOGICO, REGISTRO, INVALIDO
    }

    // Representa o que o identificador é no código
    public enum EstruturaLA {
        VARIAVEL, CONSTANTE, TIPO, PROCEDIMENTO, FUNCAO
    }

    // A classe interna que guarda os dados de cada identificador
    static class EntradaTabelaDeSimbolos {
        String nome;
        TipoLA tipo;
        EstruturaLA estrutura;

        // Se a variável for de um tipo estendido (como um registro), guarda o nome dele
        String nomeTipoEstendido;

        public EntradaTabelaDeSimbolos(String nome, TipoLA tipo, EstruturaLA estrutura) {
            this.nome = nome;
            this.tipo = tipo;
            this.estrutura = estrutura;
            this.nomeTipoEstendido = null;
        }

        public EntradaTabelaDeSimbolos(String nome, TipoLA tipo, EstruturaLA estrutura, String nomeTipoEstendido) {
            this.nome = nome;
            this.tipo = tipo;
            this.estrutura = estrutura;
            this.nomeTipoEstendido = nomeTipoEstendido;
        }
    }

    // "Memória" (dicionário que liga o nome do identificador aos seus dados)
    private final Map<String, EntradaTabelaDeSimbolos> tabela;

    public TabelaDeSimbolos() {
        this.tabela = new HashMap<>();
    }

    // Método para adicionar um novo identificador na tabela
    public void adicionar(String nome, TipoLA tipo, EstruturaLA estrutura) {
        tabela.put(nome, new EntradaTabelaDeSimbolos(nome, tipo, estrutura));
    }

    public void adicionar(String nome, TipoLA tipo, EstruturaLA estrutura, String nomeTipoEstendido) {
        tabela.put(nome, new EntradaTabelaDeSimbolos(nome, tipo, estrutura, nomeTipoEstendido));
    }

    // Verifica se um identificador já existe na tabela
    public boolean existe(String nome) {
        return tabela.containsKey(nome);
    }

    // Retorna o tipo de um identificador (útil para verificar compatibilidade de atribuição)
    public TipoLA verificar(String nome) {
        return tabela.get(nome).tipo;
    }
}