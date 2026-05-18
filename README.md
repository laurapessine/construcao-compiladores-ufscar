# Compilador - Linguagem Algorítmica (LA)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Maven](https://img.shields.io/badge/Maven-Build-blue.svg)
![ANTLR](https://img.shields.io/badge/ANTLR-v4-red.svg)
![Build Status](https://github.com/laurapessine/construcao-compiladores-ufscar/actions/workflows/maven.yml/badge.svg)

Este repositório contém os trabalhos práticos da disciplina de Construção de Compiladores (DC/UFSCar). O projeto consiste na implementação de um compilador completo para a linguagem LA (Linguagem Algorítmica), desenvolvida pelo Prof. Jander, abrangendo desde a análise léxica até a geração de código.

## Desenvolvedora
* Laura Pessine Teixeira - 824388

## Requisitos
Para compilar e executar este projeto, você precisará ter na sua máquina:
* **Java 21** (ou superior)
* **Maven** (gerenciador de dependências e build)
* **GCC (GNU Compiler Collection)** (necessário na fase T5 para compilar o código C gerado)

*(O reconhecimento de padrões é feito utilizando o **ANTLR 4**, que já é baixado e gerenciado automaticamente pelo plugin do Maven durante a build).*

## Arquitetura e estrutura do projeto

Este compilador foi desenvolvido seguindo a arquitetura padrão de projetos Maven e implementa as fases clássicas de construção de compiladores.

### Estrutura de diretórios
* `src/main/antlr4/`: Contém os arquivos de gramática (`.g4`). É aqui que as regras léxicas e sintáticas da linguagem LA são definidas. O plugin do ANTLR lê esses arquivos durante a *build* e gera os analisadores automaticamente.
* `src/main/java/.../compiladores/`: Contém o código-fonte Java estrutural:
    * `Main.java`: O ponto de entrada que orquestra todas as fases da compilação e o tratamento de erros.
    * `LASemantico.java` e `LASemanticoUtils.java`: O *Visitor* responsável por descer a árvore sintática aplicando regras de semântica, controle de escopos e validação de tipagem.
    * `LAGeradorC.java`: O *Visitor* responsável pela tradução da árvore validada para a linguagem de destino (C).
    * `TabelaDeSimbolos.java` e `Escopos.java`: Estruturas de dados que armazenam o contexto, propriedades e a visibilidade de variáveis e sub-rotinas.
* `target/`: Diretório gerado pelo Maven contendo os arquivos compilados e o executável encapsulado (`.jar`).

### Fluxo de dados (pipeline de compilação)
O processo de tradução de um programa na Linguagem Algorítmica (LA) para C ocorre em etapas estritamente sequenciais:

1. **Análise léxica (`LALexer`)**: Lê o arquivo-fonte caractere por caractere e os agrupa em blocos com significado, gerando um fluxo de **tokens** (ex: palavras reservadas, identificadores, operadores).
2. **Análise sintática (`LAParser`)**: Recebe o fluxo de tokens e verifica se eles formam instruções válidas segundo a gramática da linguagem, construindo em memória a **Árvore de Sintaxe Abstrata (AST)**.
3. **Análise semântica (`LASemantico`)**: Percorre a AST verificando a coerência do código (ex: validação de tipos em expressões, verificação de uso de variáveis não declaradas). Alimenta a tabela de símbolos.
4. **Geração de código (`LAGeradorC`)**: Se o código-fonte não contiver nenhum erro nas fases anteriores, a AST é percorrida uma última vez. Os nós da árvore são traduzidos progressivamente para comandos e blocos estruturais em linguagem **C**.

## Como compilar
Para compilar o código-fonte e gerar o executável (arquivo `.jar` encapsulado com as dependências), abra o terminal na raiz do projeto e execute:

```bash
mvn clean package
```
*Dica: Utilizamos `package` no lugar de `install` para que o Maven apenas gere o `.jar` na pasta `target/` do projeto, sem precisar instalá-lo no seu repositório Maven local.*

## Executando o compilador

O compilador recebe dois argumentos obrigatórios: o caminho do arquivo-fonte (entrada) e o caminho do arquivo de destino (saída). O comportamento da compilação adapta-se à fase de testes baseada no nome do diretório/arquivo de entrada.

### T1 - Analisador léxico
Se o caminho do arquivo de entrada contiver a string `"t1"` (ex: rodando no corretor automático dentro da pasta `1.casos_teste_t1`), o compilador funcionará em modo exclusivamente léxico, imprimindo todos os tokens identificados ou interrompendo no primeiro erro léxico encontrado.

**Exemplo de execução forçando o T1:**
```bash
java -jar target/trabalho-compiladores-1.0-SNAPSHOT-jar-with-dependencies.jar entrada_t1.txt saida.txt
```

### T2 - Analisador sintático
Para arquivos de entrada comuns ou rodando nos testes do T2 em diante, o compilador efetua a verificação léxica de forma silenciosa. Caso não haja erros léxicos, ele prossegue para a montagem da árvore sintática. Ao encontrar o primeiro erro de sintaxe, a compilação é interrompida apontando a linha e a palavra causadora do erro. Caso o código esteja correto, exibe `Fim da compilacao`.

**Exemplo de execução do T2:**
```bash
java -jar target/trabalho-compiladores-1.0-SNAPSHOT-jar-with-dependencies.jar entrada.txt saida.txt
```

### T3 - Analisador semântico
Para arquivos a partir do T3 (que não contenham "t1" ou "t2" no nome da pasta), o compilador também executa a verificação semântica utilizando o padrão Visitor, checando declarações duplicadas, variáveis não declaradas e compatibilidade de tipos, imprimindo todos os erros encontrados no arquivo de saída.

**Exemplo de execução do T3:**
```bash
java -jar target/trabalho-compiladores-1.0-SNAPSHOT-jar-with-dependencies.jar entrada_t3.txt saida.txt
```

### T4 - Analisador semântico (ponteiros e registros)
Na fase T4, o analisador semântico foi expandido para suportar o gerenciamento de escopos aninhados, garantindo o funcionamento de sub-rotinas (funções e procedimentos) e tipos estendidos (registros/structs e ponteiros). O compilador passa a checar a compatibilidade e a quantidade de parâmetros em chamadas de função, além de verificar comandos restritos, como o uso do `retorne` fora de um escopo válido.

**Exemplo de execução do T4:**
```bash
java -jar target/trabalho-compiladores-1.0-SNAPSHOT-jar-with-dependencies.jar entrada_t4.txt saida.txt
```

### T5 - Gerador de código (C)
Na fase final (T5), o compilador atinge o seu objetivo completo. Após passar por todas as validações léxicas, sintáticas e semânticas sem encontrar nenhum erro, o compilador utiliza o padrão Visitor para percorrer a árvore de sintaxe abstrata (AST) e traduzir o código da Linguagem Algorítmica (LA) para código **C** válido e executável. A tradução engloba estruturas de controle de fluxo, expressões lógicas/matemáticas, ponteiros, funções e registros (`structs`). Caso haja qualquer erro nas fases anteriores, o código C não é gerado e a compilação é abortada exibindo os erros.

**Exemplo de execução do T5:**
```bash
java -jar target/trabalho-compiladores-1.0-SNAPSHOT-jar-with-dependencies.jar entrada_t5.txt saida.c
```

---

## Validação com o corretor automático
Para testar o compilador em lote usando a ferramenta oficial da disciplina, certifique-se de ter o arquivo `.jar` do corretor na raiz do projeto e execute (substituindo `t5` pela fase desejada):
```bash
java -jar compiladores-corretor-automatico-1.0-SNAPSHOT-jar-with-dependencies.jar "java -jar target/trabalho-compiladores-1.0-SNAPSHOT-jar-with-dependencies.jar" gcc temp casos-de-teste "824388" t5
```

## Créditos e agradecimentos
Este projeto foi desenvolvido como parte da disciplina de Construção de Compiladores ministrada pelo Prof. Daniel Lucrédio no Departamento de Computação da UFSCar (DC/UFSCar).

A especificação da linguagem LA (Linguagem Algorítmica), o corretor automático e os materiais base da disciplina podem ser encontrados no repositório oficial do professor: [dlucredio/cursocompiladores](https://github.com/dlucredio/cursocompiladores).