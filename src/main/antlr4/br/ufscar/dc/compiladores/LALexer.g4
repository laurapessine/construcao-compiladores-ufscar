lexer grammar LALexer;

/* ---------------------------------------------------------
 * 1. PALAVRAS-CHAVE
 * Palavras reservadas da linguagem LA.
 * --------------------------------------------------------- */
ALGORITMO : 'algoritmo';
FIM_ALGO  : 'fim_algoritmo';
DECLARE   : 'declare';
LEIA      : 'leia';
ESCREVA   : 'escreva';
INTEIRO   : 'inteiro';
REAL      : 'real';
LITERAL   : 'literal';
LOGICO    : 'logico';
CARACTER  : 'caracter';
TIPO      : 'tipo';
REGISTRO  : 'registro';
FIM_REGISTRO : 'fim_registro';
CONSTANTE : 'constante';
SE        : 'se';
ENTAO     : 'entao';
SENAO     : 'senao';
FIM_SE    : 'fim_se';
CASO      : 'caso';
SEJA      : 'seja';
FIM_CASO  : 'fim_caso';
PARA      : 'para';
ATE       : 'ate';
FACA      : 'faca';
FIM_PARA  : 'fim_para';
ENQUANTO  : 'enquanto';
FIM_ENQUANTO : 'fim_enquanto';
PROCEDIMENTO : 'procedimento';
FIM_PROCEDIMENTO : 'fim_procedimento';
FUNCAO    : 'funcao';
FIM_FUNCAO : 'fim_funcao';
RETORNE   : 'retorne';
VERDADEIRO : 'verdadeiro';
FALSO     : 'falso';
NAO       : 'nao';
E         : 'e';
OU        : 'ou';
VAR       : 'var';

/* ---------------------------------------------------------
 * 2. PONTUAÇÃO E OPERADORES
 * Atribuições, operadores aritméticos, relacionais e delimitadores.
 * --------------------------------------------------------- */
ATRIBUICAO : '<-';
MAIOR_IGUAL: '>=';
MENOR_IGUAL: '<=';
DIFERENTE  : '<>';
MAIS       : '+';
MENOS      : '-';
VEZES      : '*';
DIVISAO    : '/';
IGUAL      : '=';
MAIOR      : '>';
MENOR      : '<';
PONTEIRO   : '^';
ENDERECO   : '&';
CENTO      : '%';
DOIS_PONTOS: ':';
PONTO_PONTO: '..';
PONTO      : '.';
ABRE_PAR   : '(';
FECHA_PAR  : ')';
ABRE_COL   : '[';
FECHA_COL  : ']';
VIRGULA    : ',';

/* ---------------------------------------------------------
 * 3. IDENTIFICADORES E LITERAIS BÁSICOS
 * --------------------------------------------------------- */
NUM_INT  : [0-9]+;
NUM_REAL : [0-9]+ '.' [0-9]+;
IDENT    : [a-zA-Z] [a-zA-Z0-9_]*;
CADEIA   : '"' ~('"'|'\n'|'\r')* '"'; // Strings delimitadas por aspas

/* ---------------------------------------------------------
 * 4. ESPAÇOS E COMENTÁRIOS
 * Regras para ignorar formatação e comentários bem formados.
 * --------------------------------------------------------- */
WS         : [ \t\r\n]+ -> skip;
COMENTARIO : '{' ~[{}]* '}' -> skip; // Ignora comentários no padrão { ... }

/* ---------------------------------------------------------
 * 5. TRATAMENTO DE ERROS LÉXICOS
 * Regras específicas para capturar erros. O ANTLR as identificará como tokens
 * anômalos para que a classe Main as trate e interrompa a compilação.
 * --------------------------------------------------------- */
COMENTARIO_NAO_FECHADO : '{' ~[{}]* ; // Chave aberta sem correspondente de fechamento
CADEIA_NAO_FECHADA     : '"' ~('"'|'\n'|'\r')*; // Aspas abertas que não fecham na mesma linha
ERRO                   : . ; // Regra genérica (fallback) para capturar símbolos desconhecidos