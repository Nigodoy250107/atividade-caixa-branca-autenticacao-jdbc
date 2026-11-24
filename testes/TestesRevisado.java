import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestesRevisado {
    public static void main(String[] args) {
        Logger.getLogger(AutenticacaoSegura.class.getName()).setLevel(Level.OFF);
        testarLoginValidoFechaRecursos();
        testarUsuarioInexistenteESenhaInvalida();
        testarInjectionTratadaComoDado();
        testarEntradasInvalidasSemConectar();
        testarFalhaDeConexao();
        System.out.println("Todos os testes da versão revisada passaram.");
    }

    private static void testarLoginValidoFechaRecursos() {
        ContextoJdbc contexto = new ContextoJdbc(true);
        AutenticacaoSegura autenticacao = novaAutenticacao(contexto);

        exigir(autenticacao.autenticar("ana", "segredo").orElse("").equals("Ana"),
                "O login válido deve retornar o nome associado.");
        exigir(contexto.conexaoFechada && contexto.comandoFechado && contexto.resultadoFechado,
                "Connection, PreparedStatement e ResultSet devem ser fechados.");
        exigir("ana".equals(contexto.parametros[1]) && "segredo".equals(contexto.parametros[2]),
                "Os valores devem ser vinculados como parâmetros.");
    }

    private static void testarUsuarioInexistenteESenhaInvalida() {
        testarSemCorrespondencia("usuario-inexistente", "segredo",
                "Usuário inexistente não deve autenticar.");
        testarSemCorrespondencia("ana", "senha-incorreta",
                "Senha incorreta não deve autenticar.");
    }

    private static void testarSemCorrespondencia(String usuario, String senha, String mensagem) {
        ContextoJdbc contexto = new ContextoJdbc(false);
        AutenticacaoSegura autenticacao = novaAutenticacao(contexto);

        exigir(!autenticacao.autenticar(usuario, senha).isPresent(), mensagem);
        exigir(contexto.conexaoFechada && contexto.comandoFechado && contexto.resultadoFechado,
                "Os recursos devem ser fechados quando não há correspondência.");
    }

    private static void testarInjectionTratadaComoDado() {
        String[] payloads = { "' OR '1'='1", "admin' -- " };
        for (String payload : payloads) {
            ContextoJdbc contexto = new ContextoJdbc(false);
            AutenticacaoSegura autenticacao = novaAutenticacao(contexto);

            exigir(!autenticacao.autenticar(payload, "x").isPresent(),
                "A entrada de injection não deve autenticar sem correspondência.");
            exigir(contexto.sqlPreparado.contains("usuario = ? AND senha = ?"),
                "A consulta deve conter placeholders, não os valores recebidos.");
            exigir(payload.equals(contexto.parametros[1]),
                "O payload deve ser enviado somente como valor do parâmetro.");
            exigir(contexto.conexaoFechada && contexto.comandoFechado && contexto.resultadoFechado,
                "Os recursos devem ser fechados também quando não há correspondência.");
        }
    }

    private static void testarEntradasInvalidasSemConectar() {
        ContextoJdbc contexto = new ContextoJdbc(true);
        AutenticacaoSegura autenticacao = novaAutenticacao(contexto);

        exigir(!autenticacao.autenticar(null, "x").isPresent(), "Usuário null deve ser rejeitado.");
        exigir(!autenticacao.autenticar("ana", null).isPresent(), "Senha null deve ser rejeitada.");
        exigir(!autenticacao.autenticar("", "x").isPresent(), "Usuário vazio deve ser rejeitado.");
        exigir(!autenticacao.autenticar("ana", "").isPresent(), "Senha vazia deve ser rejeitada.");
        exigir(!autenticacao.autenticar("   ", "x").isPresent(), "Usuário em branco deve ser rejeitado.");
        exigir(!autenticacao.autenticar("ana", "   ").isPresent(), "Senha em branco deve ser rejeitada.");
        exigir(contexto.conexoesAbertas == 0, "Entrada inválida não deve abrir conexão.");
    }

    private static void testarFalhaDeConexao() {
        Properties propriedades = propriedades();
        AutenticacaoSegura autenticacao = new AutenticacaoSegura(propriedades,
                (url, usuario, senha) -> { throw new SQLException("falha simulada"); });
        exigir(!autenticacao.autenticar("ana", "segredo").isPresent(),
                "Falha de conexão deve resultar em ausência de autenticação.");
    }

    private static AutenticacaoSegura novaAutenticacao(ContextoJdbc contexto) {
        return new AutenticacaoSegura(propriedades(), contexto::abrirConexao);
    }

    private static Properties propriedades() {
        Properties propriedades = new Properties();
        propriedades.setProperty("DB_URL", "jdbc:teste:memoria");
        propriedades.setProperty("DB_USER", "teste");
        propriedades.setProperty("DB_PASSWORD", "teste");
        return propriedades;
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }

    private static final class ContextoJdbc {
        private final boolean possuiLinha;
        private final Object[] parametros = new Object[3];
        private boolean conexaoFechada;
        private boolean comandoFechado;
        private boolean resultadoFechado;
        private int conexoesAbertas;
        private String sqlPreparado;

        private ContextoJdbc(boolean possuiLinha) {
            this.possuiLinha = possuiLinha;
        }

        private Connection abrirConexao(String url, String usuario, String senha) {
            conexoesAbertas++;
            return proxy(Connection.class, (objeto, metodo, argumentos) -> {
                if (metodo.getName().equals("prepareStatement")) {
                    sqlPreparado = (String) argumentos[0];
                    return criarComando();
                }
                if (metodo.getName().equals("close")) {
                    conexaoFechada = true;
                    return null;
                }
                return valorPadrao(metodo.getReturnType());
            });
        }

        private PreparedStatement criarComando() {
            return proxy(PreparedStatement.class, (objeto, metodo, argumentos) -> {
                if (metodo.getName().equals("setString")) {
                    parametros[(Integer) argumentos[0]] = argumentos[1];
                    return null;
                }
                if (metodo.getName().equals("executeQuery")) {
                    return criarResultado();
                }
                if (metodo.getName().equals("close")) {
                    comandoFechado = true;
                    return null;
                }
                return valorPadrao(metodo.getReturnType());
            });
        }

        private ResultSet criarResultado() {
            return proxy(ResultSet.class, (objeto, metodo, argumentos) -> {
                if (metodo.getName().equals("next")) {
                    return possuiLinha;
                }
                if (metodo.getName().equals("getString")) {
                    return "Ana";
                }
                if (metodo.getName().equals("close")) {
                    resultadoFechado = true;
                    return null;
                }
                return valorPadrao(metodo.getReturnType());
            });
        }
    }

    private static <T> T proxy(Class<T> tipo, InvocationHandler handler) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[] { tipo }, handler));
    }

    private static Object valorPadrao(Class<?> tipo) {
        if (tipo == boolean.class) return false;
        if (tipo == int.class) return 0;
        if (tipo == long.class) return 0L;
        if (tipo == double.class) return 0.0;
        if (tipo == float.class) return 0.0f;
        if (tipo == short.class) return (short) 0;
        if (tipo == byte.class) return (byte) 0;
        if (tipo == char.class) return '\0';
        return null;
    }
}