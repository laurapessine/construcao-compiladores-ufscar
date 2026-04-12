parser grammar LAParser;

/* Importa o vocabulário de tokens gerado pelo analisador léxico (T1) */
options { tokenVocab=LALexer; }

/* ---------------------------------------------------------
 * 1. ESTRUTURA PRINCIPAL DO PROGRAMA
 * --------------------------------------------------------- */
programa : declaracoes ALGORITMO corpo FIM_ALGO EOF ;

corpo : declaracao_local* cmd* ;

/* ---------------------------------------------------------
 * 2. DECLARAÇÕES (Globais e Locais)
 * Regras para declaração de variáveis, constantes, tipos,
 * procedimentos e funções.
 * --------------------------------------------------------- */
declaracoes : decl_local_global* ;

decl_local_global : declaracao_local | declaracao_global ;

declaracao_local : DECLARE variavel
                 | CONSTANTE IDENT DOIS_PONTOS tipo_basico IGUAL valor_constante
                 | TIPO IDENT DOIS_PONTOS tipo
                 ;

declaracao_global : PROCEDIMENTO IDENT ABRE_PAR parametros? FECHA_PAR declaracao_local* cmd* FIM_PROCEDIMENTO
                  | FUNCAO IDENT ABRE_PAR parametros? FECHA_PAR DOIS_PONTOS tipo_estendido declaracao_local* cmd* FIM_FUNCAO
                  ;

variavel : identificador (VIRGULA identificador)* DOIS_PONTOS tipo ;

identificador : IDENT (PONTO IDENT)* dimensao ;

dimensao : (ABRE_COL exp_aritmetica FECHA_COL)* ;

parametro : VAR? identificador (VIRGULA identificador)* DOIS_PONTOS tipo_estendido ;

parametros : parametro (VIRGULA parametro)* ;

/* ---------------------------------------------------------
 * 3. TIPOS E VALORES
 * --------------------------------------------------------- */
tipo : registro | tipo_estendido ;

tipo_basico : LITERAL | INTEIRO | REAL | LOGICO ;

tipo_basico_ident : tipo_basico | IDENT ;

tipo_estendido : PONTEIRO? tipo_basico_ident ;

registro : REGISTRO variavel* FIM_REGISTRO ;

valor_constante : CADEIA | NUM_INT | NUM_REAL | VERDADEIRO | FALSO ;

/* ---------------------------------------------------------
 * 4. COMANDOS
 * Regras para comandos de fluxo de controle, atribuição,
 * leitura/escrita e retorno.
 * --------------------------------------------------------- */
cmd : cmdLeia
    | cmdEscreva
    | cmdSe
    | cmdCaso
    | cmdPara
    | cmdEnquanto
    | cmdFaca
    | cmdAtribuicao
    | cmdChamada
    | cmdRetorne
    ;

cmdLeia : LEIA ABRE_PAR PONTEIRO? identificador (VIRGULA PONTEIRO? identificador)* FECHA_PAR ;

cmdEscreva : ESCREVA ABRE_PAR expressao (VIRGULA expressao)* FECHA_PAR ;

cmdSe : SE expressao ENTAO cmd* (SENAO cmd*)? FIM_SE ;

cmdCaso : CASO exp_aritmetica SEJA selecao (SENAO cmd*)? FIM_CASO ;

cmdPara : PARA IDENT ATRIBUICAO exp_aritmetica ATE exp_aritmetica FACA cmd* FIM_PARA ;

cmdEnquanto : ENQUANTO expressao FACA cmd* FIM_ENQUANTO ;

cmdFaca : FACA cmd* ATE expressao ;

cmdAtribuicao : PONTEIRO? identificador ATRIBUICAO expressao ;

cmdChamada : IDENT ABRE_PAR expressao (VIRGULA expressao)* FECHA_PAR ;

cmdRetorne : RETORNE expressao ;

selecao : item_selecao* ;

item_selecao : constantes DOIS_PONTOS cmd* ;

constantes : numero_intervalo (VIRGULA numero_intervalo)* ;

numero_intervalo : op_unario? NUM_INT (PONTO_PONTO op_unario? NUM_INT)? ;

/* ---------------------------------------------------------
 * 5. EXPRESSÕES
 * Regras para expressões aritméticas, relacionais e lógicas.
 * A precedência é garantida pela estrutura aninhada das regras.
 * --------------------------------------------------------- */
expressao : termo_logico (op_logico_1 termo_logico)* ;

termo_logico : fator_logico (op_logico_2 fator_logico)* ;

fator_logico : NAO? parcela_logica ;

parcela_logica : VERDADEIRO | FALSO | exp_relacional ;

exp_relacional : exp_aritmetica (op_relacional exp_aritmetica)? ;

exp_aritmetica : termo (op1 termo)* ;

termo : fator (op2 fator)* ;

fator : parcela (op3 parcela)* ;

parcela : op_unario? parcela_unario | parcela_nao_unario ;

parcela_unario : PONTEIRO? identificador
               | IDENT ABRE_PAR expressao (VIRGULA expressao)* FECHA_PAR
               | NUM_INT
               | NUM_REAL
               | ABRE_PAR expressao FECHA_PAR
               ;

parcela_nao_unario : ENDERECO identificador | CADEIA ;

/* ---------------------------------------------------------
 * 6. OPERADORES
 * --------------------------------------------------------- */
op_unario : MENOS ;
op1 : MAIS | MENOS ;
op2 : VEZES | DIVISAO ;
op3 : CENTO ;
op_relacional : IGUAL | DIFERENTE | MAIOR_IGUAL | MENOR_IGUAL | MAIOR | MENOR ;
op_logico_1 : OU ;
op_logico_2 : E ;