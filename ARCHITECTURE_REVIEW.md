# Revisão Arquitetural e de Código: API BN

Esta revisão foca exclusivamente em apontar defeitos, anti-padrões e áreas de melhoria crítica na base de código atual, fundamentando-se em boas práticas de engenharia de software, no livro *Effective Java* (Joshua Bloch), e nas documentações oficiais do Quarkus e Mutiny.

## 1. Anti-padrões Quarkus e Mutiny

### 1.1 Parsing Manual e Frágil de JWT Claims
Em `BenefitResource.java`, o método `claimCompanyId()` realiza um parsing manual e extremamente verboso de claims JWT, utilizando múltiplos blocos `try-catch` e `instanceof`.
*   **Problema:** Este código procedural é muito propenso a erros e quebra a injeção de dependência declarativa que o Quarkus oferece.
*   **Solução:** Utilize a anotação nativa `@Claim` do MicroProfile JWT para injetar diretamente o valor tipado (`@Inject @Claim("companyId") Long companyId`).

### 1.2 Mapeamento de Exceções Perigoso (Vazamento de Detalhes Internos)
O `GlobalExceptionMapper` está mapeando cegamente as exceções padrão do Java (`IllegalArgumentException` e `IllegalStateException`) para um HTTP `400 Bad Request`.
*   **Problema:** Isso é uma prática severamente desencorajada. Exceções de estado ilegal podem ser lançadas por falhas internas do framework (ex: Mutiny, falha de injeção, etc.), e retornar um HTTP 400 expõe essas falhas estruturais para o cliente web. No `AuthService`, uma falha de "Invalid credentials" lança `IllegalStateException`, que resulta em 400 ao invés do HTTP `401 Unauthorized` correto.
*   **Solução:** Crie uma hierarquia customizada de exceções de negócio (ex: `BusinessValidationException`, `AuthenticationFailedException`). Nunca utilize exceções nativas da JVM para controle de fluxo de negócio. *(Baseado no Effective Java - Item 69: Use exceptions only for exceptional conditions / Item 72: Favor the use of standard exceptions, mas proteja seu domínio).*

## 2. Violações do Effective Java e Design de Domínio

### 2.1 Abuso e Má Implementação do Padrão Builder (Item 2)
A classe `Employee` implementa um padrão Builder parcial que não faz sentido arquitetural.
*   **Problema:** O `Employee.Builder` exige todos os campos (`name`, `company`, `account`) em seu construtor principal e não provê nenhum encadeamento semântico (fluent API) para parâmetros opcionais. Ele age meramente como uma versão mais complexa e desnecessária de um construtor comum, quebrando o propósito de facilitar a inicialização.
*   **Solução:** Considere usar métodos fábrica estáticos (`Employee.create(...)`) para poucos argumentos obrigatórios, ou implementar um Builder fluente real se a classe for crescer e demandar parâmetros opcionais.

### 2.2 Mistura de Padrões Active Record e Repository (Item 16)
A entidade `Employee` estende o `PanacheEntity` (que incentiva o uso de campos públicos como o `.id`), mas simultaneamente declara propriedades privadas com getters/setters (`getName()`, `getCompany()`).
*   **Problema:** Misturar Active Record e acesso via Repository quebra o encapsulamento. O código que chama essa classe fica inconsistente: ora acessa `.id` publicamente, ora chama `getName()`. *(Effective Java - Item 16: Em classes públicas, use métodos de acesso, não campos públicos).*
*   **Solução:** Ou adote o padrão Active Record puramente (removendo os getters/setters explícitos se não agregarem valor), ou altere para herdar de `PanacheEntityBase` configurando seu próprio ID e mantendo tudo privado de fato.

### 2.3 Assinaturas de Método Confusas no Domínio (Item 51)
Os métodos de transição de estado na entidade `Employee` têm um design ruim:
```java
public void activeEmployee(EmployeeStatus val) {
    if (val == EmployeeStatus.DISABLED) throw new IllegalArgumentException("This employee is disabled");
    this.active = val;
}
```
*   **Problema:** O método se chama "activeEmployee", mas exige um parâmetro `val`. Se você passar `DISABLED` para ele, uma exceção confusa é lançada. O comportamento interno é ambíguo.
*   **Solução:** Os métodos devem encapsular a mutação de estado. Mude para `activate()` e `disable()` sem parâmetros, fazendo com que a própria classe decida o status, reduzindo a carga cognitiva da API pública da entidade *(Effective Java - Item 51: Design method signatures carefully)*.

### 2.4 Risco Silencioso de NullPointerException em Fluxos Reativos
Em `EmployeeService`, validações como `if(employee.getActive().equals(DISABLED))` assumem que `getActive()` nunca é nulo.
*   **Problema:** Como o atributo `active` só é inicializado no método `@PrePersist` (`onCreate`), instâncias recém-criadas em memória que ainda não sofreram flush no banco através do Panache terão esse valor como `null`. Isso gerará `NullPointerException` intermitentes nas cadeias Mutiny.
*   **Solução:** Inicialize valores padrão na declaração do campo da entidade (`private EmployeeStatus active = EmployeeStatus.DISABLED;`) ou force a inicialização garantida dentro do Construtor/Fábrica, abandonando a dependência excessiva de ganchos do ciclo de vida do JPA para integridade em memória.
