import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutenticacaoSegura {
    private static final Logger LOGGER = Logger.getLogger(AutenticacaoSegura.class.getName());
    private static final String SQL_AUTENTICAR =
            "SELECT nome FROM usuarios WHERE usuario = ? AND senha = ?";

    private final String url;
    private final String usuarioBanco;
    private final String senhaBanco;
    private final ConnectionFactory connectionFactory;

    public AutenticacaoSegura(Properties configuracao) {
        this(configuracao, DriverManager::getConnection);
    }

    AutenticacaoSegura(Properties configuracao, ConnectionFactory connectionFactory) {
        Objects.requireNonNull(configuracao, "configuracao não pode ser nula");
        this.connectionFactory = Objects.requireNonNull(connectionFactory,
                "connectionFactory não pode ser nula");
        this.url = obterConfiguracao(configuracao, "DB_URL");
        this.usuarioBanco = obterConfiguracao(configuracao, "DB_USER");
        this.senhaBanco = obterConfiguracao(configuracao, "DB_PASSWORD");
    }

    public Optional<String> autenticar(String usuario, String senha) {
        if (entradaInvalida(usuario, senha)) {
            return Optional.empty();
        }

        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_AUTENTICAR)) {
            comando.setString(1, usuario);
            comando.setString(2, senha);

            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return Optional.ofNullable(resultado.getString("nome"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Falha ao consultar as credenciais de autenticação.", e);
        }

        return Optional.empty();
    }

    private static boolean entradaInvalida(String usuario, String senha) {
        return usuario == null || senha == null
                || usuario.isBlank() || senha.isBlank();
    }

    private Connection abrirConexao() throws SQLException {
        if (url == null || usuarioBanco == null || senhaBanco == null
                || url.isBlank() || usuarioBanco.isBlank() || senhaBanco.isBlank()) {
            throw new SQLException("Configuração do banco de dados incompleta.");
        }
        return connectionFactory.getConnection(url, usuarioBanco, senhaBanco);
    }

    private static String obterConfiguracao(Properties configuracao, String chave) {
        String valor = configuracao.getProperty(chave);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(chave);
        }
        return valor;
    }

    @FunctionalInterface
    interface ConnectionFactory {
        Connection getConnection(String url, String usuario, String senha) throws SQLException;
    }
}