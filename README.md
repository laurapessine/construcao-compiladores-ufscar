# Compilador - Linguagem Algorítmica (LA)

Este repositório contém os trabalhos práticos da disciplina de Construção de Compiladores (DC/UFSCar). O projeto consiste na implementação de um compilador completo para a linguagem LA (Linguagem Algorítmica), desenvolvida pelo Prof. Jander, abrangendo desde a análise léxica até a geração de código.

## Desenvolvedora
* Laura Pessine Teixeira - 824388

## Requisitos
Para compilar e executar este projeto, você precisará ter na sua máquina:
* **Java 21** (ou superior)
* **Maven** (gerenciador de dependências e build)

*(O reconhecimento de padrões é feito utilizando o **ANTLR 4**, que já é baixado e gerenciado automaticamente pelo plugin do Maven durante a build).*

## Como compilar
Para compilar o código-fonte e gerar o executável (arquivo `.jar` encapsulado com as dependências), abra o terminal na raiz do projeto e execute:

```bash
mvn clean package
```
*Dica: Utilizamos `package` no lugar de `install` para que o Maven apenas gere o `.jar` na pasta `target/` do projeto, sem precisar instalá-lo no seu repositório Maven local.*

## Executando o Compilador

O compilador recebe dois argumentos obrigatórios: o caminho do arquivo-fonte (entrada) e o caminho do arquivo de destino (saída). O comportamento da compilação adapta-se à fase de testes baseada no nome do diretório/arquivo de entrada.

### T1 - Analisador Léxico
Se o caminho do arquivo de entrada contiver a string `"t1"` (ex: rodando no corretor automático dentro da pasta `1.casos_teste_t1`), o compilador funcionará em modo exclusivamente léxico, imprimindo todos os tokens identificados ou interrompendo no primeiro erro léxico encontrado.

**Exemplo de execução forçando o T1:**
```bash
java -jar target/trabalho-compiladores-1.0-SNAPSHOT-jar-with-dependencies.jar entrada_t1.txt saida.txt
```

### T2 - Analisador Sintático
Para arquivos de entrada comuns ou rodando nos testes do T2 em diante, o compilador efetua a verificação léxica de forma silenciosa. Caso não haja erros léxicos, ele prossegue para a montagem da árvore sintática. Ao encontrar o primeiro erro de sintaxe, a compilação é interrompida apontando a linha e a palavra causadora do erro. Caso o código esteja correto, exibe `Fim da compilacao`.

**Exemplo de execução do T2:**
```bash
java -jar target/trabalho-compiladores-1.0-SNAPSHOT-jar-with-dependencies.jar entrada.txt saida.txt
```