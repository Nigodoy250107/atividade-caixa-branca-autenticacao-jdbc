# Grafos de Fluxo de Controle

## Convenção

Os grafos incluem o fluxo normal e a saída do bloco `catch`. Para o cálculo didático, cada bloco protegido por `try/catch` é representado por uma bifurcação agregada: exceções de diferentes chamadas JDBC convergem no mesmo nó `catch` e são abstraídas como um caminho independente. Esse modelo de alto nível evita contar cada ponto de lançamento como uma decisão separada. `P` é o número de componentes conexos, igual a 1 em cada método.

## `abrirConexao()`

### Nós

| Nó | Ação |
|---|---|
| N1 | Entrada; inicializa `conexao` com `null` |
| N2 | Monta URL com usuário/senha fixos e tenta `getConnection` |
| N3 | Trata qualquer `Exception` sem ação |
| N4 | Retorna `conexao` |

### Arestas

| Aresta | Transição |
|---|---|
| A12 | N1 → N2 |
| A24 | N2 → N4, conexão obtida |
| A23 | N2 → N3, exceção |
| A34 | N3 → N4, retorna `null` |

### Complexidade e caminhos

`N = 4`, `E = 4`, `P = 1`.

`V(G) = E - N + 2P = 4 - 4 + 2(1) = 2`.

Validação por decisões: uma bifurcação entre conclusão normal da tentativa e exceção capturada, portanto `V(G) = 1 + 1 = 2` (termo base mais uma decisão).

| Caminho | Sequência | Justificativa |
|---|---|---|
| C1 | N1 → N2 → N4 | Conexão obtida e retornada |
| C2 | N1 → N2 → N3 → N4 | Exceção capturada; conexão nula retornada |

## `autenticar()`

### Nós

| Nó | Ação |
|---|---|
| N1 | Abre conexão e concatena as entradas ao SQL |
| N2 | Executa as operações JDBC protegidas pelo `try`; exceções são agregadas na saída para o `catch` |
| N3 | Avalia `resultado.next()` |
| N4 | Atualiza `ultimoUsuario` e `autenticado` |
| N5 | Captura e ignora uma `Exception` |
| N6 | Retorna o campo `autenticado` |

### Arestas

| Aresta | Transição |
|---|---|
| A12 | N1 → N2 |
| A23 | N2 → N3, consulta concluída |
| A25 | N2 → N5, exceção em criação/execução/acesso JDBC |
| A34 | N3 → N4, há linha |
| A36 | N3 → N6, não há linha |
| A46 | N4 → N6 |
| A56 | N5 → N6 |

Exceções durante `abrirConexao()` são capturadas dentro desse método e levam a `null`; a falha subsequente em `conexao.createStatement()` é capturada pelo `catch` de `autenticar()`.

### Complexidade e caminhos

`N = 6`, `E = 7`, `P = 1`.

`V(G) = E - N + 2P = 7 - 6 + 2(1) = 3`.

Validação por decisões: bifurcação de exceção mais a decisão `resultado.next()`, logo `V(G) = 1 + 2 = 3`.

| Caminho | Sequência | Justificativa |
|---|---|---|
| C1 | N1 → N2 → N3 → N4 → N6 | Consulta concluída com linha; autenticação passa a `true` |
| C2 | N1 → N2 → N3 → N6 | Consulta sem linha; estado não é redefinido |
| C3 | N1 → N2 → N5 → N6 | Exceção capturada; estado não é redefinido |

Os caminhos C2 e C3 evidenciam o problema de estado: em instância reutilizada, ambos podem devolver `true` de uma autenticação anterior.
