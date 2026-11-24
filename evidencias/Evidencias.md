# Evidências

## Escopo e ambiente

Os quatro fontes Java da entrega foram encontrados. Não há `pom.xml`, configuração Gradle, dependência JUnit ou driver MySQL no projeto. `JAVA_HOME` estava vazio e `java`/`javac` não estavam no `PATH`. A extensão Java do VS Code contém um JDK Temurin utilizável em `C:\Users\jsant324\.vscode\extensions\redhat.java-1.58.0-win32-x64\jre\25.0.4.1-win32-x86_64`; a variável foi configurada somente na sessão de terminal usada para esta execução.

`Test-NetConnection localhost -Port 3306` retornou `False`. Por isso, não havia servidor MySQL local disponível para os casos de integração do código original.

## Execução dos testes revisados

Os testes foram implementados como programas Java executáveis, sem JUnit. A tentativa inicial por `PATH` falhou porque `JAVA_HOME` estava vazio e os comandos não eram encontrados; em seguida, foi localizado e usado o JDK 25 da extensão Java. A compilação e os testes revisados foram executados efetivamente. Para reproduzir na mesma instalação, na pasta `atividade-caixa-branca`:

```powershell
$jdkHome = "$env:USERPROFILE\.vscode\extensions\redhat.java-1.58.0-win32-x64\jre\25.0.4.1-win32-x86_64"
$env:JAVA_HOME = $jdkHome
$env:PATH = "$jdkHome\bin;$env:PATH"
$output = Join-Path $env:TEMP 'atividade-caixa-branca-classes'
New-Item -ItemType Directory -Force $output | Out-Null
javac -d $output original/AutenticacaoLegada.java revisado/AutenticacaoSegura.java testes/TestesOriginal.java testes/TestesRevisado.java
java -cp $output TestesRevisado
```

## Versões identificadas

```text
openjdk version "25.0.4.1" 2026-08-18 LTS
OpenJDK Runtime Environment Temurin-25.0.4.1+1 (build 25.0.4.1+1-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.4.1+1 (build 25.0.4.1+1-LTS, mixed mode, sharing)
javac 25.0.4.1
```

## Saída observada

Os quatro arquivos `.java` foram compilados juntos. A compilação não produziu mensagens de erro e terminou com `compile exit=0`. Foram gerados `AutenticacaoLegada.class`, `AutenticacaoSegura.class`, `AutenticacaoSegura$ConnectionFactory.class`, `TestesOriginal.class`, `TestesRevisado.class` e `TestesRevisado$ContextoJdbc.class` em `%TEMP%\atividade-caixa-branca-classes`.

A execução de `TestesRevisado` produziu a saída completa:

```text
Todos os testes da versão revisada passaram.
TestesRevisado exit=0
```

Foram executadas cinco rotinas de teste, cobrindo doze cenários e vinte e três asserções: autenticação válida, usuário inexistente, senha incorreta, os payloads `' OR '1'='1` e `admin' -- ` como parâmetros, entradas nulas/vazias/em branco, falha de conexão e fechamento de recursos. Todas as asserções passaram.

## Execução exploratória da versão original

A classe `TestesOriginal` foi invocada sem argumentos apenas para verificar os pré-requisitos. A saída completa foi:

```text
Não executado: informe usuario válido, senha válida, usuário inexistente e senha incorreta.
Pré-requisitos: MySQL em localhost:3306, schema loja, tabela usuarios e driver JDBC.
TestesOriginal exit=0
```

Nenhum caso funcional original foi executado. Além de não haver driver MySQL no projeto, o teste de porta local confirmou que não há serviço escutando em `localhost:3306`. A execução requer o driver JDBC, schema `loja`, tabela `usuarios`, credenciais de banco codificadas na versão original e quatro argumentos de teste válidos.

Com o driver disponível no diretório atual e o banco de teste preparado, o comando é:

```text
java -cp "out;mysql-connector-j.jar" TestesOriginal usuarioValido senhaValida usuarioInexistente senhaIncorreta
```

## Evidência do fluxo e limitações

Os arquivos PlantUML representam as arestas enumeradas em `diagramas/Grafos.md`. Para renderização local, abra os arquivos `.puml` em uma extensão PlantUML compatível. Não foram geradas capturas de tela nem relatório de ferramenta de análise estática; a evidência executável desta rodada é a compilação Java e a saída do harness revisado acima.

