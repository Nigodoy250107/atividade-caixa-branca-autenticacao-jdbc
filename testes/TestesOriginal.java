public class TestesOriginal {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Não executado: informe usuario válido, senha válida, usuário inexistente e senha incorreta.");
            System.out.println("Pré-requisitos: MySQL em localhost:3306, schema loja, tabela usuarios e driver JDBC.");
            return;
        }

        AutenticacaoLegada mesmoObjeto = new AutenticacaoLegada();
        boolean loginValido = mesmoObjeto.autenticar(args[0], args[1]);
        System.out.println("Login fornecido aceito: " + loginValido);
        System.out.println("Último usuário: " + mesmoObjeto.getUltimoUsuario());

        boolean senhaInvalidaAposSucesso = mesmoObjeto.autenticar(args[0], args[3]);
        System.out.println("Senha incorreta após login válido (esperado false): " + senhaInvalidaAposSucesso);

        AutenticacaoLegada usuarioInexistente = new AutenticacaoLegada();
        boolean usuarioNaoEncontrado = usuarioInexistente.autenticar(args[2], "senha-de-teste");
        System.out.println("Usuário inexistente em instância nova (esperado false): " + usuarioNaoEncontrado);

        String[] entradasBorda = { null, "", "   " };
        for (String entrada : entradasBorda) {
            AutenticacaoLegada casoBorda = new AutenticacaoLegada();
            System.out.println("Entrada de usuário de borda [" + entrada + "]: "
                    + casoBorda.autenticar(entrada, "senha-de-teste"));
        }
        AutenticacaoLegada senhaNula = new AutenticacaoLegada();
        System.out.println("Senha null: " + senhaNula.autenticar("usuario-de-teste", null));

        String[] payloads = { "' OR '1'='1", "admin' -- " };
        for (String payload : payloads) {
            AutenticacaoLegada casoInjection = new AutenticacaoLegada();
            boolean injectionAceita = casoInjection.autenticar(payload, "x");
            System.out.println("Payload de injection [" + payload + "] aceito (observar resultado): "
                + injectionAceita);
        }
    }
}