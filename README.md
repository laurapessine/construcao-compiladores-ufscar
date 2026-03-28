# Compilador - Linguagem Algorítmica (LA)

Este repositório contém os trabalhos práticos da disciplina de Construção de Compiladores (DC/UFSCar). O projeto consiste na implementação de um compilador completo para a linguagem LA (Linguagem Algorítmica), desenvolvida pelo Prof. Jander, abrangendo desde a análise léxica até a geração de código.

## Desenvolvedora
* Laura Pessine Teixeira - 824388

## Requisitos
Para compilar e executar este projeto, você precisará ter em sua máquina:
* **Java 21** (ou superior)
* **Maven** (gerenciador de dependências e build)

*(O reconhecimento de padrões é feito utilizando o **ANTLR 4**, que já é baixado e gerenciado automaticamente pelo plugin do Maven durante o build).*

## Como compilar
Para compilar o código-fonte e gerar o executável (arquivo `.jar` encapsulado com as dependências), abra o terminal na raiz do projeto e execute:

```bash
mvn clean package
```
*Dica: Utilizamos `package` no lugar de `install` para que o Maven apenas gere o `.jar` na pasta `/target` do projeto, sem precisar instalá-lo no seu repositório Maven local.*

## Como executar (T1 - Analisador Léxico)
Após a compilação, o executável estará disponível na pasta `target`. Para executar a análise léxica em um arquivo de código LA, utilize o comando abaixo, passando o arquivo de entrada e o arquivo de destino para a saída:

```bash
java -jar target/trabalho-compiladores-1.0-SNAPSHOT-jar-with-dependencies.jar <caminho_arquivo_entrada> <caminho_arquivo_saida>
```

**Exemplo de uso:**
```bash
java -jar target/trabalho-compiladores-1.0-SNAPSHOT-jar-with-dependencies.jar entrada.txt saida.txt
```
