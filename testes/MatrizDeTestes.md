# Matriz de Testes

## Critérios

Os resultados observados para a versão original abaixo são conclusões estáticas sobre o fluxo, não resultados de execução com banco. A confirmação dinâmica depende do MySQL local, do esquema e dos dados. Na versão revisada, os testes executáveis são os métodos de `TestesRevisado.java`.

| ID | Método | Caminho coberto | Entrada | Pré-condição | Resultado esperado | Resultado observado | Status |
|---|---|---|---|---|---|---|---|
| O-01 | `autenticar` original | consulta retorna linha | usuário e senha válidos | MySQL `loja` disponível e credencial existente | `true`; nome atualizado | Dependente da base; não executado | Pendente de ambiente |
| O-02 | `autenticar` original | consulta sem linha após sucesso | senha incorreta na mesma instância após login válido | Base acessível e usuário válido | `false` para esta tentativa | Retorna `true` se o login anterior definiu `autenticado`; cenário está no harness | Falha de projeto |
| O-03 | `autenticar` original | consulta sem linha em instância nova | usuário inexistente | Base acessível e usuário realmente inexistente | `false` | Previsto no harness; depende dos dados e não foi executado | Pendente de ambiente |
| O-04 | `autenticar` original | consulta sem linha em instância nova | senha incorreta | Base acessível e usuário existente | `false` | Previsto no harness; depende dos dados e não foi executado | Pendente de ambiente |
| O-05 | `autenticar` original | concatenação de entrada | usuário ou senha `null` (testados separadamente) | Base acessível | rejeitar antes da consulta | Concatenação gera o texto `null`; não há rejeição; ambos os casos estão no harness | Falha de requisito |
| O-06 | `autenticar` original | concatenação de entrada | usuário vazio | Base acessível | rejeitar antes da consulta | Não há validação; consulta é executada | Falha de requisito |
| O-07 | `autenticar` original | concatenação de entrada | usuário com espaços | Base acessível | rejeitar entrada em branco | Espaços são concatenados e consultados | Falha de requisito |
| O-08 | `autenticar` original | entrada maliciosa | `' OR '1'='1` | Base acessível | não autenticar | A concatenação altera a expressão SQL; eficácia depende do contexto e dos dados | Falha de segurança |
| O-09 | `autenticar` original | entrada maliciosa com comentário | usuário `admin' -- ` | MySQL acessível e usuário `admin` existente | não autenticar | O comentário pode eliminar a verificação de senha; não executado | Falha de segurança |
| O-10 | `abrirConexao` original | exceção de conexão | servidor indisponível | MySQL inacessível | erro observável e controlado | Exceção ignorada; retorna `null` | Falha de tratamento |
| O-11 | `autenticar` original | conexão nula / exceção | banco indisponível | `abrirConexao` retorna `null` | falha previsível e registrada | `NullPointerException` é capturada e ignorada; retorna estado anterior | Falha de confiabilidade |
| R-01 | `autenticar` revisado | linha encontrada e retorno | `ana` / `segredo` | JDBC simulado com linha | `Optional` com `Ana`; recursos fechados | Asserções passaram com JDK 25.0.4.1 | Aprovado |
| R-02 | `autenticar` revisado | consulta sem linha | usuário inexistente | JDBC simulado sem linha | `Optional.empty()`; recursos fechados | Asserções passaram com JDK 25.0.4.1 | Aprovado |
| R-03 | `autenticar` revisado | consulta sem linha | usuário existente e senha incorreta | JDBC simulado sem linha | `Optional.empty()`; recursos fechados | Asserções passaram com JDK 25.0.4.1 | Aprovado |
| R-04 | `autenticar` revisado | validação de nulos | usuário ou senha `null` | Nenhuma | `Optional.empty()` sem conexão | Asserções passaram com JDK 25.0.4.1 | Aprovado |
| R-05 | `autenticar` revisado | validação de vazio | usuário ou senha `""` | Nenhuma | `Optional.empty()` sem conexão | Asserções passaram com JDK 25.0.4.1 | Aprovado |
| R-06 | `autenticar` revisado | validação de branco | usuário ou senha com espaços | Nenhuma | `Optional.empty()` sem conexão | Asserções passaram com JDK 25.0.4.1 | Aprovado |
| R-07 | `autenticar` revisado | parâmetros SQL | payloads `' OR '1'='1` e `admin' -- ` como usuário | JDBC simulado sem linha | payloads tratados como valores; sem autenticação | Asserções de placeholders, parâmetros e liberação passaram com JDK 25.0.4.1 | Aprovado |
| R-08 | `autenticar` revisado | falha de conexão | fábrica de conexão lança `SQLException` | Configuração de teste válida | `Optional.empty()` e falha registrada sem credenciais | Retornou `Optional.empty()`; teste não verificou conteúdo do log | Aprovado com ressalva |
| R-09 | `autenticar` revisado | liberação normal/sem resultado | consulta simulada | JDBC simulado | fechar `ResultSet`, `PreparedStatement` e `Connection` | Asserções de fechamento passaram com JDK 25.0.4.1 | Aprovado |

## Interpretação dos casos de segurança

`' OR '1'='1` é um fragmento de teste cuja eficácia depende da expressão completa e dos dados; não se afirma que essa entrada isolada sempre autentique. `admin' -- ` testa comentário da condição de senha quando existe uma conta `admin`. Na versão revisada, ambos são vinculados como valores e não alteram a estrutura SQL. O harness original imprime os resultados; eles dependem do esquema e dos dados locais.