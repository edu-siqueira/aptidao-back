# Inscrições da Corrida Solidária

API REST para cadastrar, listar, consultar, editar e cancelar as inscrições da 5ª Corrida Solidária da Vila Esperança, com o total de inscritos por percurso.

Java 21, Spring Boot 4.1.2, Spring Data JPA, H2 (padrão) ou PostgreSQL (opcional).

## Como executar

Requisito: JDK 21. O Maven não precisa estar instalado, o projeto traz o Maven Wrapper.

```bash
./mvnw spring-boot:run
```

No Windows (cmd ou PowerShell) também funciona `mvnw.cmd spring-boot:run`.

A API sobe em `http://localhost:8080`. Por padrão os dados ficam em memória (H2) e começam vazios; ao reiniciar o app, as inscrições são perdidas.

Para rodar os testes:

```bash
./mvnw test
```

## API

Prefixo `/api`, corpo e respostas em JSON.

| Operação | Rota | Corpo | Sucesso | Recusas |
|---|---|---|---|---|
| Cadastrar | `POST /api/inscricoes` | `nome`, `email`, `dataNascimento`, `percurso` | 201 | 400, 409 |
| Listar | `GET /api/inscricoes` | | 200 | |
| Consultar | `GET /api/inscricoes/{email}` | | 200 | 404 |
| Editar | `PUT /api/inscricoes/{email}` | `nome`, `dataNascimento`, `percurso` | 200 | 400, 404 |
| Cancelar | `DELETE /api/inscricoes/{email}` | | 204 | 404 |
| Resumo | `GET /api/inscricoes/resumo` | | 200 | |

`percurso` aceita `KM_5` ou `KM_10`, e `dataNascimento` usa o formato `aaaa-mm-dd`.

Exemplo:

```bash
curl -X POST http://localhost:8080/api/inscricoes \
  -H "Content-Type: application/json" \
  -d '{"nome":"Ana Souza","email":"ana.souza@email.com","dataNascimento":"1990-05-20","percurso":"KM_10"}'
```

```json
{
  "nome": "Ana Souza",
  "email": "ana.souza@email.com",
  "dataNascimento": "1990-05-20",
  "percurso": "KM_10",
  "idadeNaProva": 36,
  "categoria": "ADULTO"
}
```

O resumo devolve `{ "total": 3, "km5": 2, "km10": 1 }`. Toda recusa devolve uma mensagem em português, sem detalhes internos:

```json
{ "mensagem": "A idade mínima para participar é 14 anos." }
```

## Regras implementadas

- **Data da prova:** 14/03/2027 (Art. 5º do regulamento), definida em um único lugar, `corrida.data-prova`, em `src/main/resources/application.properties`.
- **Idade:** a idade completa na data da prova. Fazer aniversário no dia da prova já conta.
- **Idade mínima:** 14 anos. Ela é verificada antes da regra dos 10 km.
- **10 km:** somente a partir de 18 anos.
- **Categorias:** Juvenil (14 a 17), Adulto (18 a 39), Master (40 a 59) e Sênior (60 ou mais). Idade e categoria são sempre calculadas pelo sistema; se o cliente enviá-las, são ignoradas.
- **Nome:** de 3 a 100 caracteres, depois de remover espaços nas pontas e reduzir os repetidos.
- **E-mail:** é a chave da inscrição. Espaços nas pontas e maiúsculas são removidos, então `" Ana@Email.COM "` e `ana@email.com` são a mesma inscrição. Um e-mail, uma inscrição ativa. Ele não pode ser alterado na edição.
- **Cancelar:** remove a inscrição e libera o e-mail para um novo cadastro, que entra no fim da lista.
- **Editar:** só nome, nascimento e percurso. Se a edição for recusada, nada é alterado.

## PostgreSQL (opcional)

O perfil `postgres` usa um banco PostgreSQL e mantém as inscrições entre reinícios. As credenciais vêm de variáveis de ambiente, e não há nenhuma no código. Se alguma faltar, o app não sobe.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/<banco>
export DB_USER=<usuario>
export DB_PASSWORD=<senha>
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

No PowerShell, use `$env:DB_URL = "..."` no lugar de `export`.

## Premissas

- Onde o áudio do cliente diverge do regulamento (ele cita o dia 13 de março, o regulamento diz 14), vale o regulamento.
- Pontos no e-mail (`joao.silva@gmail.com` e `joaosilva@gmail.com`) não são tratados como equivalentes: é uma regra do Gmail e não está no regulamento.
- Fora do escopo, conforme o pacote do cliente: pagamento (Pix), envio de e-mail, login, autorização de responsáveis e bloqueio por prazo.

## Estrutura

```
controller/   rotas HTTP
service/      regras de negócio (InscricaoService, ValidadorInscricao, RegulamentoService)
repository/   acesso ao banco
model/        entidade InscricaoEntity
dto/          formatos do JSON
enums/        EnumPercurso, EnumCategoria
exceptions/   erros do domínio e tratamento global
config/       data da prova e beans
```
