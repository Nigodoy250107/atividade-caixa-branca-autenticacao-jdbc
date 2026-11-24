# Atividade de Caixa Branca: Autenticação JDBC

**Aluno:** Nicolas Godoy  
**Curso/turma:** AS406TSN3 - UX/UI e Testes de Software
**Instituição:** UniFacens
**Docente:** Daniel Ohata
**Data:** 24/11/2026

## Estrutura da Entrega

```text
atividade-caixa-branca/
├── original/
│   └── AutenticacaoLegada.java
├── revisado/
│   └── AutenticacaoSegura.java
├── testes/
│   ├── TestesOriginal.java
│   ├── TestesRevisado.java
│   └── MatrizDeTestes.md
├── diagramas/
│   ├── abrirConexao.puml
│   ├── autenticar.puml
│   └── Grafos.md
├── evidencias/
│   └── Evidencias.md
├── PLANO_DE_TESTE.xlsx
└── README.md
```

A planilha contém o preenchimento da aba `CAIXA BRANCA (ESTÁTICO)`; as demais abas foram preservadas sem alteração.

## 1. Introdução

Este trabalho analisa por inspeção estática e estrutural uma classe Java de autenticação baseada em JDBC, calcula a complexidade ciclomática dos métodos fornecidos, propõe caminhos independentes e apresenta casos de teste. Em seguida, compara o código-base com uma implementação revisada que usa consulta parametrizada, configuração externa, validação, registro de falhas e fechamento determinístico dos recursos.

## 2. Descrição do Cenário 1 (Autenticação JDBC)

A classe original, `AutenticacaoLegada`, conecta-se ao MySQL local no schema `loja`, consulta `usuarios` por `usuario` e `senha` e guarda o nome encontrado. A consulta é construída por concatenação. A conexão usa credenciais embutidas na URL, e a classe mantém os campos `autenticado` e `ultimoUsuario` entre chamadas. O cenário revisado, `AutenticacaoSegura`, retorna `Optional<String>` por tentativa, não armazena o resultado da autenticação no objeto e recebe `DB_URL`, `DB_USER` e `DB_PASSWORD` em `Properties` ou no ambiente.

## 3. Análise Estática

O código original não possui JavaDoc; os blocos `catch` estão vazios e não documentam o descarte das falhas nem contratos, pré-condições ou resultados. Os nomes são parcialmente compreensíveis, mas `comando`, `resultado` e o parâmetro `e` são genéricos. A classe mistura configuração de conexão, acesso JDBC, autenticação e estado de sessão, com acoplamento direto a `DriverManager` e MySQL.

| Localização | Descrição | Categoria | Severidade | Impacto | Recomendação |
|---|---|---|---|---|---|
| `abrirConexao`, URL JDBC | Usuário e senha fixos (`root`/`123`) | Segurança / configuração | Crítica | Segredo exposto no fonte e difícil rotação; privilégios excessivos possíveis | Remover do código e injetar configuração protegida via ambiente/secret manager; usar conta de privilégio mínimo |
| `autenticar`, construção de `sql` | Valores concatenados diretamente na consulta | Segurança / injeção | Crítica | Entrada pode alterar a lógica SQL e contornar a autenticação | Usar `PreparedStatement` e parâmetros vinculados |
| `abrirConexao`, URL | Credencial aparece em texto legível na URL | Segurança / segredo | Alta | Pode ser exposta em logs, diagnóstico, dumps ou inspeção do processo | Manter segredo fora da URL e restringir seu armazenamento e acesso |
| `autenticar`, comparação de `senha` | Código compara diretamente a senha recebida com coluna `senha`; não há hash ou verificação de hash | Segurança / armazenamento | Crítica se a coluna guardar senha em claro; não verificável pelo trecho | Comprometimento do banco pode revelar senhas reutilizáveis | Armazenar hashes adaptativos com salt, como Argon2id, bcrypt ou PBKDF2; verificar com biblioteca apropriada |
| `autenticar`, parâmetros | Não valida `null`, vazio ou branco | Validação | Média | Consultas inúteis, erros e comportamento ambíguo | Rejeitar entradas inválidas antes de abrir conexão |
| `abrirConexao` e `autenticar` | Captura `Exception` genérica e ignora | Tratamento de erros | Alta | Falhas tornam-se indistinguíveis de credenciais inválidas; diagnóstico é perdido | Capturar exceções esperadas, registrar contexto não sensível e devolver resultado explícito |
| ambos os métodos | Ausência de logs | Observabilidade | Média | Falhas de conectividade/consulta não podem ser investigadas | Registrar falhas sem incluir senha, SQL sensível ou dados pessoais desnecessários |
| `autenticar` | `Connection`, `Statement` e `ResultSet` não são fechados | Recursos | Alta | Vazamento de conexões/cursores e esgotamento do pool/servidor | Usar `try-with-resources` para os três recursos |
| `autenticar` | Se `abrirConexao()` retorna `null`, `createStatement()` falha; falha é ignorada | Confiabilidade | Alta | Falha de infraestrutura parece um resultado de login e pode retornar estado antigo | Propagar/registrar falha de conexão e retornar resultado sem autenticar |
| campos `autenticado` e `ultimoUsuario` | Estado persiste entre autenticações; o ramo sem linha e o ramo de exceção não redefinem os campos | Estado / correção | Alta | Uma tentativa inválida subsequente pode retornar `true` e exibir o nome anterior | Tornar resultado local à chamada; não manter sessão implícita na classe |
| classe inteira | Falta JavaDoc e contrato explícito; responsabilidades acopladas | Documentação / manutenção | Baixa | Uso incorreto e manutenção mais arriscada | Documentar entradas, retorno e falhas; separar configuração, persistência e regra de autenticação quando o sistema crescer |

## 4. Grafos de Fluxo

Os diagramas PlantUML e as listas de nós/arestas estão em [`diagramas/Grafos.md`](diagramas/Grafos.md), [`diagramas/abrirConexao.puml`](diagramas/abrirConexao.puml) e [`diagramas/autenticar.puml`](diagramas/autenticar.puml). O fluxo normal, consulta sem resultado e exceções capturadas foram modelados.

## 5. Cálculo da Complexidade Ciclomática

Adota-se a convenção didática de contar a alternativa do `catch` como bifurcação de exceção. A fórmula é `V(G) = E - N + 2P`, com `P = 1`.

| Método | N | E | P | Cálculo | Decisões | V(G) |
|---|---:|---:|---:|---|---:|---:|
| `abrirConexao()` original | 4 | 4 | 1 | `4 - 4 + 2(1)` | 1 (exceção) | 2 |
| `autenticar()` original | 6 | 7 | 1 | `7 - 6 + 2(1)` | 2 (`catch` e `if`) | 3 |

O resultado também corresponde a `1 + número de decisões` na convenção declarada. Ferramentas podem adotar tratamento diferente para exceções e operadores booleanos; por isso, os números devem ser comparados apenas usando a mesma convenção. No método original `autenticar`, `abrirConexao()` captura internamente suas próprias exceções; uma conexão nula pode causar exceção na chamada `createStatement`, capturada pelo `catch` externo.

## 6. Caminhos Independentes

Em `abrirConexao()`: (1) conexão obtida; (2) falha de conexão capturada e retorno nulo. Em `autenticar()`: (1) consulta encontra linha e atualiza os campos; (2) consulta não encontra linha; (3) exceção durante a operação JDBC e retorno. A descrição completa de cada sequência de nós e sua justificativa consta em `diagramas/Grafos.md`.

## 7. Casos de Teste

A matriz completa, com pré-condições, entradas, resultados e status, está em [`testes/MatrizDeTestes.md`](testes/MatrizDeTestes.md). Há casos para login válido, usuário inexistente, senha incorreta, entradas vazias/nulas/em branco, indisponibilidade do banco, estado persistente e os payloads SQL Injection `' OR '1'='1` e `admin' -- `. Os resultados da original são identificados como análise estática ou pendentes de ambiente, não como execução observada. Os testes da versão revisada estão implementados em programa Java executável, sem JUnit, e não exigem banco externo.

## 8. Procedimento de Execução dos Testes

Os testes foram implementados e executados. No início, `JAVA_HOME` estava vazio e `java`/`javac` não eram encontrados no `PATH`; a busca localizou o JDK Temurin 25.0.4.1 incluído no runtime da extensão Java do VS Code. Configurei `JAVA_HOME` e `PATH` somente na sessão do terminal integrado para compilar e executar. A compilação dos quatro arquivos Java terminou com código 0, e os testes da versão revisada passaram. A execução dos casos de integração da versão original continua pendente por falta de servidor MySQL e driver no projeto.

A forma geral `javac *.java` seguida de `java NomeDaClasseTeste` é aplicável quando os arquivos-fonte estão no mesmo diretório. Nesta entrega, os fontes estão organizados em subdiretórios. Para reproduzir no PowerShell do Windows, primeiro ajuste o terminal ao caminho do JDK localizado nesta máquina:

```powershell
$jdkHome = "$env:USERPROFILE\.vscode\extensions\redhat.java-1.58.0-win32-x64\jre\25.0.4.1-win32-x86_64"
$env:JAVA_HOME = $jdkHome
$env:PATH = "$jdkHome\bin;$env:PATH"
```

Depois, na pasta `atividade-caixa-branca`, compile todos os fontes e execute o programa de testes revisados:

```text
New-Item -ItemType Directory -Force out | Out-Null
javac -d out original/AutenticacaoLegada.java revisado/AutenticacaoSegura.java testes/TestesOriginal.java testes/TestesRevisado.java
java -cp out TestesRevisado
```

`TestesRevisado` usa proxies JDBC em memória. No ambiente verificado, foram cobertos doze cenários com vinte e três asserções; a saída foi `Todos os testes da versão revisada passaram.` Para executar os casos funcionais de `TestesOriginal`, adicione o driver MySQL ao classpath, prepare servidor/schema/dados e forneça quatro argumentos: usuário válido, senha válida, usuário inexistente e senha incorreta. Use somente uma base e contas de teste locais. O harness original imprime resultados para inspeção e não converte as falhas conhecidas em asserções automáticas.

Exemplo de execução do harness original no Windows, supondo `mysql-connector-j.jar` no diretório atual:

```text
java -cp "out;mysql-connector-j.jar" TestesOriginal usuarioValido senhaValida usuarioInexistente senhaIncorreta
```

## 9. Evidências

As instruções de compilação, execução, pré-requisitos para a versão original e saída coletada estão em [`evidencias/Evidencias.md`](evidencias/Evidencias.md). A versão revisada foi compilada e executada com sucesso usando o JDK da extensão; a versão original continua dependente de driver, servidor MySQL e dados locais.

## 10. Refatoração

`revisado/AutenticacaoSegura.java` substitui `Statement` por `PreparedStatement`, retira valores recebidos do texto SQL, usa configuração externa, rejeita entradas nulas/vazias/em branco, registra `SQLException` sem registrar credenciais e usa `try-with-resources`. A API retorna `Optional<String>`: resultado presente indica autenticação e nome recuperado; vazio indica entrada inválida, ausência de linha ou falha SQL. A falha SQL é registrada, mas, para sistemas que precisem distinguir indisponibilidade de credenciais inválidas, recomenda-se evoluir o contrato para um resultado tipado ou exceção de aplicação.

Esta refatoração não implementa verificação de hash porque o esquema e o formato de armazenamento existentes não foram fornecidos. A comparação direta pressupõe que a consulta legada compara a senha como texto; o armazenamento seguro requer migração e política de hash adaptativo, além de verificação específica no código.

## 11. Comparação Original x Revisado

| Aspecto | Original | Revisado |
|---|---|---|
| Segurança | SQL concatenado e credenciais fixas; senha comparada diretamente | Parâmetros preparados; credenciais externas; sem senha em logs. Hash de senha exige migração não incluída |
| Tratamento de erros | Captura genérica e silenciosa | Captura `SQLException`, registra falha sem dados de autenticação e retorna vazio |
| Legibilidade | Concatenação inline e responsabilidades misturadas | Consulta constante, validação e configuração em métodos separados |
| Manutenibilidade | Conectado ao ambiente local e dependente de estado mutável | Configuração injetável e resultado por chamada; fábrica de conexão isolável em testes |
| Confiabilidade | Vazamentos possíveis; tentativa inválida pode herdar sucesso anterior | Recursos fechados em sucesso/falha e estado não persiste entre chamadas |
| Complexidade ciclomática | `abrirConexao`: 2; `autenticar`: 3, pela convenção documentada | `autenticar`: 3 pela mesma convenção, considerando validação, retorno da consulta e exceção SQL |

A complexidade ciclomática de `autenticar` permanece no mesmo valor porque a redução de riscos decorre principalmente de parametrização, validação, escopo de recursos e remoção de estado, não de eliminar os caminhos essenciais de decisão. A conexão privada revisada não captura exceção localmente; a falha propaga para o tratamento específico de `SQLException` em `autenticar`.

## 12. Limitações

- Não há configuração de schema, DDL, dados de teste nem versão do servidor original; o teste original não pode ser executado de forma reproduzível sem preparar o ambiente.
- No momento da execução, não havia servidor escutando em `localhost:3306` e nenhum driver MySQL no projeto; portanto, os casos de integração do harness original não foram executados.
- `original/AutenticacaoLegada.java` preserva literalmente `root/123` do exemplo acadêmico fornecido para análise de vulnerabilidades. Esses valores são demonstrativos no contexto da atividade, não credenciais operacionais. Se coincidirem com credenciais reais, rotacione-as e publique uma cópia sanitizada, preservando o original acadêmico em local não público.
- Os testes revisados usam JDBC simulado, não um servidor MySQL real; eles verificam o contrato de interação, não compatibilidade com o driver/esquema de produção.
- A aplicação não valida TLS, pool de conexões, política de rotação de segredos, limitação de tentativas ou autenticação multifator, pois esses aspectos excedem o código fornecido.
- A implementação ainda consulta senha diretamente; adotar hash exige alteração do esquema e fluxo de cadastro/autenticação.
- A captura de `SQLException` e retorno vazio não diferencia falha técnica de credencial inválida para o chamador.

## 13. Conclusão

A análise identificou injeção SQL e credenciais fixas como riscos críticos, além de exceções silenciosas, recursos não liberados e estado de autenticação persistente. Os grafos resultaram em V(G)=2 para `abrirConexao()` e V(G)=3 para `autenticar()`, com a mesma quantidade de caminhos independentes em cada método. A versão revisada substitui a concatenação por parâmetros JDBC, remove o estado compartilhado e fecha os recursos; a compilação com JDK 25.0.4.1 e os 12 cenários simulados passaram.

Esse resultado valida o contrato testado em memória, não a integração com MySQL. Os casos funcionais originais continuam sem execução por indisponibilidade de driver e servidor local. A versão revisada também não migra senhas para hashes adaptativos: isso exige conhecer e alterar o esquema de dados. Portanto, a atividade documenta uma refatoração segura para o escopo fornecido, mas não representa por si só uma autenticação pronta para produção.

## 14. Referências

- McCabe, T. J. (1976). *A Complexity Measure*. IEEE Transactions on Software Engineering, SE-2(4), 308–320. https://doi.org/10.1109/TSE.1976.233837
- Oracle. *Java SE Documentation: JDBC Basics*. https://docs.oracle.com/javase/tutorial/jdbc/
- Oracle. *Java SE Documentation: The try-with-resources Statement*. https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
- Oracle. *Java SE API: `PreparedStatement`*. https://docs.oracle.com/en/java/javase/11/docs/api/java.sql/java/sql/PreparedStatement.html
- OWASP. *SQL Injection Prevention Cheat Sheet*. https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html
- OWASP. *Password Storage Cheat Sheet*. https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html

## 15. Checklist Final da Entrega

| Requisito | Status | Justificativa |
|---|---|---|
| Estrutura de pastas e arquivos solicitados | Atendido | A entrega contém `original`, `revisado`, `testes`, `diagramas`, `evidencias` e este README com os arquivos correspondentes. |
| Análise estática e tabela de achados | Atendido | Localização, descrição, categoria, severidade, impacto e recomendação estão documentados; incluem segurança, documentação, exceções, recursos e estado. |
| Grafos de `abrirConexao()` e `autenticar()` | Atendido | Nós, arestas, fluxos normais e exceções estão em Markdown e PlantUML; os diagramas representam exceções como bifurcações. |
| Cálculo de N, E, P e V(G), validado por decisões | Atendido | `abrirConexao()`: N=4, E=4, P=1, V(G)=2; `autenticar()`: N=6, E=7, P=1, V(G)=3. A contagem por decisões foi apresentada. |
| Caminhos independentes e igualdade com V(G) | Atendido | Foram listados 2 caminhos para `abrirConexao()` e 3 para `autenticar()`, respectivamente iguais a V(G). |
| Matriz de casos normais, inválidos, borda, segurança e falha | Atendido | A matriz cobre login válido, usuário inexistente, senha incorreta, nulos, vazios, brancos, SQL Injection e falha de conexão, com resultados originais identificados como estáticos/pendentes. |
| Harness de teste da versão original | Parcialmente Atendido | Está implementado para execução exploratória, mas requer MySQL, driver, esquema e dados locais; o servidor local não respondeu na porta 3306 e os casos funcionais permanecem pendentes. |
| Refatoração com `PreparedStatement`, configuração, logs, validação e try-with-resources | Atendido | Implementada em `AutenticacaoSegura`; as entradas são vinculadas como parâmetros e os três recursos são fechados por `try-with-resources`. |
| Hash/verificação criptográfica de senha | Parcialmente Atendido | A ausência de hash é analisada e a limitação está explicada, mas não foi implementada migração porque o esquema e o formato de armazenamento não foram fornecidos. |
| Testes implementados para versão revisada | Atendido | Programa executável verifica login, ausência de linha por usuário/senha, injection, validações, falha de conexão e fechamento de recursos; não usa JUnit. |
| Compilação e execução prática dos testes revisados | Atendido | Os quatro fontes foram compilados com `javac 25.0.4.1`; `TestesRevisado` executou com saída de sucesso e código 0. |
| Comparação entre versões, limitações, conclusão e referências | Atendido | Seções correspondentes estão presentes; as limitações de execução, integração JDBC e hash estão declaradas. |
| Evidências de execução real e integração com MySQL | Parcialmente Atendido | Há saída real da compilação e dos testes revisados; a integração original não foi validada porque não havia servidor MySQL/driver disponível. |
| Dados acadêmicos | Atendido | Os campos disponíveis foram preenchidos conforme os dados já informados no README; não foram inventadas informações. |