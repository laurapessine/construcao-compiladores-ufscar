package br.ufscar.dc.compiladores;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabelaDeSimbolos {
    // Representa os tipos básicos da linguagem LA e um tipo INVALIDO para erros
    public enum TipoLA {
        INTEIRO, REAL, LITERAL, LOGICO, REGISTRO, ENDERECO, INVALIDO
    }

    // Representa o que o identificador é no código
    public enum EstruturaLA {
        VARIAVEL, CONSTANTE, TIPO, PROCEDIMENTO, FUNCAO
    }

    // A classe interna que guarda os dados de cada identificador
    public static class EntradaTabelaDeSimbolos {
        public String nome;
        public TipoLA tipo;
        public EstruturaLA estrutura;
        // Se a variável for de um tipo estendido (como um registro), guarda o nome dele
        public String nomeTipoEstendido;
        // Para funções/procedimentos, guarda a assinatura (tipos dos parâmetros)
        public List<TipoLA> tiposParametros;
        // Para registros, guarda as variáveis internas
        public TabelaDeSimbolos camposRegistro;
        public EntradaTabelaDeSimbolos(String nome, TipoLA tipo, EstruturaLA estrutura) {
            this.nome = nome;
            this.tipo = tipo;
            this.estrutura = estrutura;
            this.tiposParametros = new ArrayList<>();
            this.camposRegistro = new TabelaDeSimbolos();
        }

        public EntradaTabelaDeSimbolos(String nome, TipoLA tipo, EstruturaLA estrutura, String nomeTipoEstendido) {
            this.nome = nome;
            this.tipo = tipo;
            this.estrutura = estrutura;
            this.nomeTipoEstendido = nomeTipoEstendido;
            this.tiposParametros = new ArrayList<>();
            this.camposRegistro = new TabelaDeSimbolos();
        }
    }

    // "Memória" (dicionário que liga o nome do identificador aos seus dados)
    private final Map<String, EntradaTabelaDeSimbolos> tabela;

    public TabelaDeSimbolos() {
        this.tabela = new HashMap<>();
    }

    // Métodos para adicionar um novo identificador na tabela
    public void adicionar(String nome, TipoLA tipo, EstruturaLA estrutura) {
        tabela.put(nome, new EntradaTabelaDeSimbolos(nome, tipo, estrutura));
    }

    public void adicionar(String nome, TipoLA tipo, EstruturaLA estrutura, String nomeTipoEstendido) {
        tabela.put(nome, new EntradaTabelaDeSimbolos(nome, tipo, estrutura, nomeTipoEstendido));
    }

    // Novo método para adicionar uma entrada já montada (útil para funções)
    public void adicionar(EntradaTabelaDeSimbolos entrada) {
        tabela.put(entrada.nome, entrada);
    }

    // Verifica se um identificador já existe na tabela
    public boolean existe(String nome) {
        return tabela.containsKey(nome);
    }

    public EntradaTabelaDeSimbolos verificar(String nome) {
        return tabela.get(nome);
    }
}