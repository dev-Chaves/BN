# Guia Completo de Programação Reativa com Mutiny

> **Autor:** Um Sênior Java Developer  
> **Audiência:** Desenvolvedores Java que querem dominar programação reativa  
> **Projeto:** Sistema de Benefícios (Quarkus + Mutiny)  
> **Nível:** Intermediário → Avançado

---

## 📚 Índice

1. [Introdução à Programação Reativa](#1-introdução-à-programação-reativa)
2. [Mutiny 101: Fundamentos](#2-mutiny-101-fundamentos)
3. [Code Smells em Código Reativo](#3-code-smells-em-código-reativo)
4. [Deep Dive: Análise de requestPartnership()](#4-deep-dive-análise-de-requestpartnership)
5. [Patterns & Best Practices](#5-patterns--best-practices)
6. [Refactoring Techniques](#6-refactoring-techniques)
7. [Testing Reactive Code](#7-testing-reactive-code)
8. [Exercícios Práticos](#8-exercícios-práticos)
9. [Cheatsheet de Referência](#9-cheatsheet-de-referência)
10. [Referências e Recursos](#10-referências-e-recursos)
11. [FAQ - Perguntas Frequentes](#faq---perguntas-frequentes)

---

## 1. Introdução à Programação Reativa

### 1.1 O Que É Programação Reativa?

Programação reativa é um **paradigma de programação** orientado a fluxos de dados assíncronos e propagação de mudanças. Em termos simples:

> **"Programação reativa trata de responder a eventos quando eles acontecem, não de esperar por eles."**

#### Analogia do Mundo Real

**Imperativo (Bloqueante):**
```
Você vai a uma cafeteria.
Faz o pedido.
Fica esperando na fila sem fazer nada. ⏳
Recebe o café.
Volta ao trabalho.
```

**Reativo (Não-bloqueante):**
```
Você faz o pedido.
Recebe um dispositivo que vibra quando o café estiver pronto. 📳
Volta para sua mesa e continua trabalhando.
Quando vibra, você busca o café.
```

### 1.2 Por Que Usar Programação Reativa?

#### ✅ Vantagens

1. **Escalabilidade**: Threads não ficam bloqueadas esperando I/O
2. **Eficiência de Recursos**: Menos threads = menos memória
3. **Responsividade**: Sistema responde mais rápido sob carga
4. **Backpressure**: Controle de fluxo quando o consumidor é mais lento
5. **Composição Elegante**: Operadores funcionais para transformar dados

#### ❌ Desafios

1. **Curva de Aprendizado**: Mindset diferente do imperativo
2. **Debugging**: Stack traces podem ser confusas
3. **Complexidade**: Código pode ficar difícil de ler se mal escrito
4. **Testing**: Requer abordagens específicas

### 1.3 Imperativo vs Reativo

#### Exemplo: Buscar Usuário do Banco

**❌ Estilo Imperativo (Bloqueante)**
```java
public User findUser(Long id) {
    // Thread BLOQUEIA aqui até o DB responder
    User user = database.findById(id);  // 🚫 BLOCKING!
    
    if (user == null) {
        throw new NotFoundException("User not found");
    }
    
    return user;
}

// Thread fica PARADA esperando I/O
// 1000 requisições = 1000 threads bloqueadas = 💥 Out of Memory
```

**✅ Estilo Reativo (Não-bloqueante)**
```java
public Uni<User> findUser(Long id) {
    // Retorna IMEDIATAMENTE uma "promessa" de User
    return database.findById(id)  // ✅ NON-BLOCKING!
        .onItem().ifNull().failWith(() -> 
            new NotFoundException("User not found")
        );
}

// Thread libera imediatamente
// 1000 requisições = ~10-50 threads = 🚀 Eficiente
```

#### 💡 Onde Está o "Bloqueio" Real?

```java
// Anatomia de uma requisição reativa:
public Uni<UserDTO> getUser(Long id) {
    return repository.findById(id)        // ← 100ms (I/O ASSÍNCRONO)
        .map(entity -> toDTO(entity));    // ← 0.001ms (CPU SÍNCRONO)
}

// Tempo total: ~100ms
// Tempo bloqueante: 0ms ✅
// Por quê? O I/O é não-bloqueante! A thread não espera.

// Comparação com código BLOQUEANTE:
public UserDTO getUserBlocking(Long id) {
    User entity = repository.findByIdBlocking(id);  // ← Thread PARA aqui! 🚫
    return toDTO(entity);  // ← Também síncrono, mas irrelevante
}
// Tempo total: ~100ms
// Tempo bloqueante: 100ms ❌
// A thread ficou PARADA esperando o banco responder!
```

**O vilão é o I/O, não o código síncrono!**
- Entity → DTO (0.001ms) não importa se é síncrono
- Database query (100ms) PRECISA ser assíncrona para não bloquear

### 1.4 Conceitos Fundamentais

#### Publisher (Produtor)
Emite dados ou eventos ao longo do tempo.

```java
// Uni = Publisher que emite 0 ou 1 item
Uni<String> publisher = Uni.createFrom().item("Hello");
```

#### Subscriber (Consumidor)
Recebe e processa os dados emitidos.

```java
publisher.subscribe().with(
    item -> System.out.println(item),     // onItem
    failure -> System.err.println(failure) // onFailure
);
```

#### Backpressure
Mecanismo que permite o consumidor controlar a velocidade de emissão de dados quando está sobrecarregado.

```
Publisher (rápido) --[100 items/s]--> Subscriber (lento) [10 items/s]
                                      ⬆ "Slow down!" (Backpressure)
```

#### Non-blocking I/O
Operações de I/O não bloqueiam a thread:
- Database queries
- HTTP requests
- File operations
- Message queue operations

---

## 2. Mutiny 101: Fundamentos

### 2.1 Tipos Principais

Mutiny tem dois tipos principais:

#### `Uni<T>` - Zero ou Um Item
Representa um stream que emite **0 ou 1 item**.

```java
// ✅ Sucesso: emite 1 item
Uni<String> success = Uni.createFrom().item("value");

// ❌ Falha: emite 0 items (error)
Uni<String> failure = Uni.createFrom().failure(new Exception());

// ⏸ Null: emite 0 items (completion)
Uni<String> empty = Uni.createFrom().nullItem();
```

**Use quando:**
- Buscar uma entidade do banco
- Fazer uma HTTP request
- Executar uma operação que retorna um único resultado

#### `Multi<T>` - Zero ou Muitos Items
Representa um stream que emite **0, 1 ou N items**.

```java
Multi<Integer> numbers = Multi.createFrom().items(1, 2, 3, 4, 5);

// Infinite stream
Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(1));
```

**Use quando:**
- Buscar lista de entidades
- Processar streams de eventos
- Server-Sent Events (SSE)

### 2.2 Criando Unis

> **⚠️ IMPORTANTE:** No Quarkus, você raramente precisa criar Unis manualmente. Repositories reativos já retornam `Uni<T>` automaticamente!

```java
// 1. De um valor
Uni<String> uni1 = Uni.createFrom().item("value");

// 2. De um supplier (lazy)
Uni<String> uni2 = Uni.createFrom().item(() -> "computed value");

// 3. De uma failure
Uni<String> uni3 = Uni.createFrom().failure(new RuntimeException("error"));

// 4. De null
Uni<String> uni4 = Uni.createFrom().nullItem();

// 5. De um CompletionStage (⚠️ APENAS para integração com código legado!)
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "async");
Uni<String> uni5 = Uni.createFrom().completionStage(future);
// ⚠️ NÃO USE CompletableFuture em código novo! Você JÁ tem Uni!

// 6. De um Uni existente
Uni<String> uni6 = Uni.createFrom().uni(uni1);
```

#### ⚠️ CompletableFuture vs Uni: Quando Usar Cada Um?

**Regra de Ouro no Quarkus:**
> Se você já tem Uni disponível (repository reativo, HTTP client reativo), **NUNCA** crie CompletableFuture!

```java
// ❌ ANTI-PATTERN: Criar CompletableFuture desnecessariamente
public Uni<User> findUser(Long id) {
    CompletableFuture<User> future = CompletableFuture.supplyAsync(() -> {
        // operação qualquer
        return user;
    });
    return Uni.createFrom().completionStage(future);  // ❌ POR QUÊ?!
}

// ✅ CORRETO: Usar Uni diretamente
public Uni<User> findUser(Long id) {
    return repository.findById(id);  // ✅ Já é reativo!
}
```

**Único caso válido:** Integração com bibliotecas legadas que retornam `CompletableFuture`:

```java
// Biblioteca legada que você NÃO controla:
public interface LegacyService {
    CompletableFuture<String> getData();
}

// Sua camada de adaptação:
public Uni<String> getDataReactive() {
    // ✅ OK: Convertendo legado para moderno
    return Uni.createFrom().completionStage(legacyService.getData());
}
```

**Por que Uni é superior:**

| Característica | CompletableFuture | Uni (Mutiny) |
|----------------|-------------------|--------------|
| **Execução** | Eager (imediata) | Lazy (sob demanda) |
| **Composição** | `.thenApply()`, `.thenCompose()` | `.map()`, `.flatMap()` (mais intuitivo) |
| **Backpressure** | ❌ Não suporta | ✅ Suporta |
| **Null Safety** | ❌ Aceita null | ✅ `.onItem().ifNull()` |
| **Cancelamento** | Limitado | Completo |
| **Stack Traces** | Confusas | Mais claras |

### 2.3 Operadores Essenciais

#### `map` - Transformar Item

Transforma o item emitido **sincronicamente**.

```java
Uni<Integer> number = Uni.createFrom().item(5);

Uni<String> text = number
    .map(n -> "Number: " + n);  // 5 -> "Number: 5"
    
// ⚠️ Use map para transformações SÍNCRONAS (sem I/O)
```

> **❓ "Mas síncrono não vai bloquear a thread?"**  
> **NÃO!** Síncrono ≠ Bloqueante. `map()` executa operações **rápidas em memória** (nanossegundos), não faz I/O. A thread não fica "esperando" nada - ela só executa o código e continua.

**Quando usar:**
- Converter tipos (Entity → DTO) - ✅ Apenas copia campos, sem I/O
- Aplicar cálculos simples - ✅ CPU pura, muito rápido
- Formatar strings - ✅ Manipulação de memória
- Modificar o valor sem operações assíncronas

**Exemplo típico:**
```java
return repository.findById(id)  // ← Assíncrono (I/O do banco)
    .map(entity -> new UserDTO(    // ← Síncrono (copia campos)
        entity.getName(),
        entity.getEmail()
    ));
// Total: ~100ms (99.99ms = query, 0.01ms = DTO)
// Thread SÓ BLOQUEIA se houver I/O, não por causa do map!
```

#### `flatMap` - Transformar Item Assincronamente

Transforma o item em outro `Uni` e "achata" o resultado.

```java
Uni<User> user = findUser(1L);

Uni<Address> address = user
    .flatMap(u -> findAddress(u.getAddressId()));  // Uni<User> → Uni<Address>
    
// ⚠️ Use flatMap quando a transformação retorna Uni<T>
```

**Quando usar:**
- Operações que dependem do resultado anterior
- Chamadas ao banco de dados encadeadas
- HTTP requests em sequência

#### `call` - Executar Side-Effect

Executa uma operação reativa mas **descarta o resultado**, mantendo o valor original.

```java
Uni<User> user = findUser(1L)
    .call(u -> auditLog.save(new Log("User accessed")))  // Salva log
    .call(u -> cache.put(u.id, u));                      // Atualiza cache
    
// Resultado final: Uni<User> (não Uni<Log> ou Uni<Void>)
```

**Quando usar:**
- Persistir entidade no banco
- Enviar notificações
- Atualizar cache
- Qualquer operação assíncrona que não afeta o valor final

#### `onItem().transform()` - Alias para `map`

```java
// Essas duas formas são EQUIVALENTES:
uni.map(item -> item.toUpperCase())
uni.onItem().transform(item -> item.toUpperCase())

// onItem() é mais verboso mas pode ser mais legível
```

#### `onItem().ifNull()` - Lidar com Nulls

```java
Uni<User> user = repository.findById(id)
    .onItem().ifNull().failWith(() -> 
        new NotFoundException("User not found")
    );
    
// Se repository retornar null, emite NotFoundException
```

#### `onFailure()` - Tratamento de Erros

```java
Uni<User> user = repository.findById(id)
    .onFailure().retry().atMost(3)                    // Retry 3 vezes
    .onFailure().recoverWithItem(User.GUEST_USER)     // Fallback
    .onFailure().invoke(err -> log.error(err));       // Log erro
```

### 2.4 Combinando Múltiplos Unis

#### `Uni.combine()` - Executar em Paralelo

```java
Uni<User> user = findUser(1L);
Uni<Company> company = findCompany(2L);
Uni<Product> product = findProduct(3L);

// ✅ Executa os 3 em PARALELO
Uni<MyData> combined = Uni.combine().all()
    .unis(user, company, product)
    .asTuple()
    .map(tuple -> new MyData(
        tuple.getItem1(),  // User
        tuple.getItem2(),  // Company
        tuple.getItem3()   // Product
    ));
```

**⚠️ IMPORTANTE:** Os Unis devem ser **independentes** (não dependem uns dos outros).

#### Quando NÃO usar `combine()`

```java
// ❌ ERRADO: company depende de user
Uni.combine().all()
    .unis(
        findUser(1L),
        findCompany(user.companyId)  // 🚫 user ainda não existe!
    )
    .asTuple();
    
// ✅ CORRETO: usar flatMap para dependências
findUser(1L)
    .flatMap(user -> findCompany(user.companyId));
```

### 2.5 Lifecycle de um Uni

```
┌─────────────┐
│   Created   │  Uni.createFrom().item(value)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Subscribed │  .subscribe().with(...)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Emitted   │  onItem, onFailure, or onCompletion
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Completed  │
└─────────────┘
```

**⚠️ IMPORTANTE:** Unis são **lazy**! Nada acontece até `.subscribe()` ser chamado.

```java
// ❌ Esse código NÃO executa nada!
Uni<String> uni = database.findById(1L);
// Nada aconteceu ainda...

// ✅ Isso executa a query
uni.subscribe().with(
    item -> System.out.println(item),
    failure -> System.err.println(failure)
);
```

**No Quarkus REST:** O framework chama `.subscribe()` automaticamente quando você retorna `Uni<Response>`.

---

## 3. Code Smells em Código Reativo

### 3.1 Nested Chains (O Problema Mais Comum)

#### ❌ Anti-pattern: Callback Hell

```java
// 🚫 RUIM: 5 níveis de aninhamento
return repository1.find(id)
    .call(item1 -> repository2.find(item1.getId())
        .call(item2 -> repository3.find(item2.getId())
            .call(item3 -> repository4.find(item3.getId())
                .call(item4 -> repository5.save(item4)))));
                
// Problemas:
// 1. Difícil de ler (indentação cresce horizontalmente)
// 2. Difícil de testar (tudo acoplado)
// 3. Difícil de debugar (stack trace confusa)
// 4. Difícil de modificar (adicionar validações, logs, etc)
```

#### ✅ Solução: Flatten com Métodos Privados

```java
// ✅ BOM: Flat e legível
return getItem1(id)
    .flatMap(this::getItem2)
    .flatMap(this::getItem3)
    .flatMap(this::getItem4)
    .call(repository5::save);
    
// Métodos privados extraídos:
private Uni<Item2> getItem2(Item1 item1) {
    return repository2.find(item1.getId());
}
// ... etc
```

### 3.2 Uso Incorreto de `call()` vs `flatMap()`

#### ❌ Usar `call()` quando deveria ser `flatMap()`

```java
// 🚫 RUIM: call() descarta o resultado!
return findUser(id)
    .call(user -> findAddress(user.addressId));  // ❌ Address é descartado!
    // Resultado: Uni<User>, não Uni<Address>!
```

#### ✅ Usar `flatMap()` para transformação

```java
// ✅ BOM: flatMap() transforma o resultado
return findUser(id)
    .flatMap(user -> findAddress(user.addressId));  // ✅ 
    // Resultado: Uni<Address>
```

#### ✅ Usar `call()` para side-effects

```java
// ✅ BOM: call() para persistir mas manter o valor
return buildEntity(request)
    .call(repository::persist)  // Salva no banco
    .map(this::toResponse);     // Converte para DTO
    // Resultado: Uni<Response>
```

#### ❌ Usar `flatMap()` para operações síncronas

```java
// 🚫 ANTI-PATTERN: flatMap para criar DTO
return repository.findById(id)
    .flatMap(entity -> Uni.createFrom().item(toDTO(entity)));
    
// Problemas:
// 1. Verboso (Uni.createFrom().item desnecessário)
// 2. Performance (overhead de criar Uni extra)
// 3. Semântica errada (flatMap = assíncrono)
```

#### ✅ Usar `map()` para operações síncronas

```java
// ✅ BOM: map para transformações diretas
return repository.findById(id)
    .map(entity -> toDTO(entity));
    
// Mais limpo, mais rápido, semântica correta
```

**Regra simples:**
- Se o método retorna `Uni<T>` → use `flatMap`
- Se o método retorna `T` diretamente → use `map`

### 3.3 Bloqueio Acidental de Threads

#### ❌ Chamar `.await()` em código de serviço

```java
// 🚫 NUNCA FAÇA ISSO EM UM SERVICE!
@ApplicationScoped
public class MyService {
    public Response doSomething(Long id) {
        // ❌ BLOQUEANTE! Destrói todo o benefício reativo
        User user = repository.findById(id).await().indefinitely();
        return Response.ok(user).build();
    }
}
```

#### ✅ Retornar o Uni

```java
// ✅ BOM: Deixa o framework gerenciar
@ApplicationScoped
public class MyService {
    public Uni<Response> doSomething(Long id) {
        return repository.findById(id)
            .map(user -> Response.ok(user).build());
    }
}
```

**⚠️ Único lugar onde `.await()` é aceitável:** Testes unitários.

```java
@Test
void testFindUser() {
    // ✅ OK em testes
    User user = service.findUser(1L).await().indefinitely();
    assertNotNull(user);
}
```

### 3.4 Uso Desnecessário de `subscribe()`

#### ❌ Chamar `subscribe()` em código de serviço

```java
// 🚫 RUIM: subscribe() em service
@ApplicationScoped
public class MyService {
    public void doSomething(Long id) {
        repository.findById(id)
            .subscribe().with(
                user -> System.out.println(user),  // ❌
                err -> System.err.println(err)
            );
        // O método retorna void ANTES do callback executar!
    }
}
```

#### ✅ Retornar o Uni e deixar o framework fazer subscribe

```java
// ✅ BOM: Retorna Uni
@ApplicationScoped
public class MyService {
    public Uni<User> doSomething(Long id) {
        return repository.findById(id);  // Framework chama subscribe()
    }
}
```

### 3.5 Criar Unis Desnecessariamente

#### ❌ Wrap valores simples em Uni

```java
// 🚫 DESNECESSÁRIO
private Uni<Partnership> createPartnership(Company c, Benefit b) {
    return Uni.createFrom().item(
        Partnership.builder(c, b).build()
    );
}
```

#### ✅ Criar diretamente se não há I/O

```java
// ✅ MELHOR: Só criar Uni se necessário
private Partnership createPartnership(Company c, Benefit b) {
    return Partnership.builder(c, b).build();
}

// Uso:
return validatePartnership()
    .map(() -> createPartnership(company, benefit));  // Wrap aqui
```

**Exceção:** Se você precisa manter a assinatura reativa por consistência da API.

### 3.6 Ignorar Tratamento de Erros

#### ❌ Não tratar possíveis falhas

```java
// 🚫 E se findById retornar null?
return repository.findById(id)
    .map(user -> user.getName());  // ❌ NullPointerException!
```

#### ✅ Sempre validar null

```java
// ✅ Tratar null explicitamente
return repository.findById(id)
    .onItem().ifNull().failWith(() -> 
        new NotFoundException("User not found")
    )
    .map(user -> user.getName());
```

### 3.7 Variáveis Não-Finais em Lambdas

#### ❌ Tentar modificar variável externa

```java
// 🚫 ERRO DE COMPILAÇÃO
int counter = 0;
return repository.findAll()
    .map(item -> {
        counter++;  // ❌ Variable used in lambda should be final
        return item;
    });
```

#### ✅ Usar estruturas funcionais

```java
// ✅ Contar funcionalmente
return repository.findAll()
    .collect().asList()
    .map(List::size);
```

---

## 4. Deep Dive: Análise de requestPartnership()

Agora vamos analisar **o código real** do seu projeto e identificar cada problema.

### 4.1 O Código Original

```java
public Uni<PartnershipResponse> requestPartnership(String managerEmail, Long benefitId){

    return managerRepository.findByEmail(managerEmail).onItem().ifNull().failWith(()-> new NotFoundException("Manager not found"))
    .call(manager -> companyRepository.findByManagerEmail(managerEmail).onItem().ifNull().failWith(()-> new NotFoundException("Requested Company not found"))
            .call(requestCompany -> benefitRepository.findById(benefitId).onItem().ifNull().failWith(()-> new NotFoundException("Requested Benefit not found"))
                    .call(benefit -> validatePartnership(requestCompany.id, benefit.id)
                            .call(()-> verifyClientCompany(requestCompany, benefit.getProvider()))
                            .call(clientCompany -> createPartnership(requestCompany, benefit)))
                    .onItem().transform(partnership -> new PartnershipResponse(
                            partnership.id,
                            partnership.getClientCompany().id,
                            partnership.getBenefit().id,
                            partnership.getStatus(),
                            partnership.getCreatedAt()
                    ))));
}
```

### 4.2 Análise Visual da Estrutura

```
Level 0: managerRepository.findByEmail()
         │
         └─> call (Level 1: companyRepository.findByManagerEmail())
                   │
                   └─> call (Level 2: benefitRepository.findById())
                             │
                             └─> call (Level 3: validatePartnership())
                                       │
                                       └─> call (Level 4: verifyClientCompany())
                                                 │
                                                 └─> call (Level 5: createPartnership())
                                                           │
                                                           └─> transform (Response)
```

**Problema:** 5 níveis de aninhamento! Cada nível adiciona:
- 1 indentação adicional
- 1 contexto adicional para manter na cabeça
- 1 callback adicional
- Mais complexidade para debugar

### 4.3 Identificando os Code Smells

#### 🚫 Smell #1: Nested Calls

```java
.call(manager -> companyRepository.findByManagerEmail(managerEmail)
    .call(requestCompany -> benefitRepository.findById(benefitId)
        .call(benefit -> validatePartnership(requestCompany.id, benefit.id)
            // ... ainda mais aninhamento
```

**Por que é ruim:**
- Cresce horizontalmente (pyramid of doom)
- Variáveis de escopo externo ficam "presas" (manager, requestCompany, benefit)
- Difícil adicionar logs ou validações intermediárias

#### 🚫 Smell #2: Variável Não Utilizada

```java
.call(manager -> companyRepository.findByManagerEmail(managerEmail)
//    ^^^^^^^ Variável "manager" nunca é usada!
```

**Por que é ruim:**
- `manager` é buscado mas nunca usado
- Apenas valida que existe (com `.onItem().ifNull().failWith()`)
- Poderia ser uma validação separada

#### 🚫 Smell #3: Duplicação de Parâmetro

```java
managerRepository.findByEmail(managerEmail)
    .call(manager -> companyRepository.findByManagerEmail(managerEmail))
    //                                                     ^^^^^^^^^^^^
    //                                       Repetindo o mesmo parâmetro!
```

**Por que é ruim:**
- `managerEmail` é passado duas vezes
- Se `manager` foi encontrado, a company poderia vir de `manager.getCompany()`
- Ou a busca de company poderia incluir validação do manager

#### 🚫 Smell #4: Uso Incorreto de `call()`

```java
.call(benefit -> validatePartnership(requestCompany.id, benefit.id)
    .call(()-> verifyClientCompany(requestCompany, benefit.getProvider()))
    .call(clientCompany -> createPartnership(requestCompany, benefit)))
    //    ^^^^^^^^^^^^^ Esta variável NÃO VEM de verifyClientCompany!
```

**Análise:**
- `verifyClientCompany()` retorna `Uni<Company>`
- Mas `call()` **descarta** o resultado
- `clientCompany` na lambda seguinte é `requestCompany`, não o retorno de `verifyClientCompany`!

**Código equivalente (mais claro):**
```java
return validatePartnership(requestCompany.id, benefit.id)
    .flatMap(() -> verifyClientCompany(requestCompany, benefit.getProvider()))
    .map(ignoredResult -> requestCompany)  // verifyClientCompany retorna, mas é ignorado
    .flatMap(comp -> createPartnership(comp, benefit));
```

#### 🚫 Smell #5: Criação Desnecessária de Uni

```java
private Uni<Partnership> createPartnership(Company company, Benefit benefit){
    return Uni.createFrom().item(Partnership.builder(company, benefit).build());
}
```

**Por que é ruim:**
- Não há operação assíncrona aqui
- Criar o builder é instantâneo (sem I/O)
- Poderia retornar `Partnership` direto e wrap com `map()`

#### 🚫 Smell #6: Validação Retorna Boolean Não Usado

```java
private Uni<Boolean> validatePartnership(Long company, Long benefit){
    return partnershipRepository.findExistingPartnership(company, benefit).call(exists -> {
        if(exists){
            return Uni.createFrom().failure(new IllegalStateException("Partnership already exists"));
        }
        return Uni.createFrom().voidItem();
    });
}
```

**Problemas:**
- Retorna `Uni<Boolean>` mas ninguém usa o Boolean
- `call()` deveria ser `flatMap()` já que retorna outro Uni
- Pattern confuso: retorna Boolean mas emite failure ou void

### 4.4 Rastreando o Fluxo de Dados

Vamos traçar o que cada operação faz:

```java
1. managerRepository.findByEmail(email)           → Uni<Manager>
   └─ onItem().ifNull().failWith()                → Falha se null
   
2. .call(manager -> ...)                          → DESCARTA Manager
   ├─ companyRepository.findByManagerEmail()      → Uni<Company>
   └─ onItem().ifNull().failWith()                → Falha se null
   
3. .call(requestCompany -> ...)                   → DESCARTA Company? (não!)
   ├─ benefitRepository.findById()                → Uni<Benefit>
   └─ onItem().ifNull().failWith()                → Falha se null
   
4. .call(benefit -> ...)                          → Mantém Benefit
   ├─ validatePartnership()                       → Uni<Boolean>
   └─ .call(...)                                  → DESCARTA Boolean
   
5. .call(() -> ...)                               → Mantém Company
   ├─ verifyClientCompany()                       → Uni<Company>
   └─ .call(...)                                  → DESCARTA Company!
   
6. .call(clientCompany -> ...)                    → clientCompany = requestCompany
   └─ createPartnership()                         → Uni<Partnership>
   
7. .onItem().transform(partnership -> ...)        → Uni<PartnershipResponse>
```

**Problema:** É difícil rastrear qual valor está disponível em cada ponto!

### 4.5 O Que o Código REALMENTE Faz

Traduzindo para pseudo-código imperativo:

```java
// 1. Buscar manager (apenas validar que existe)
Manager manager = managerRepository.findByEmail(email);
if (manager == null) throw new NotFoundException("Manager not found");

// 2. Buscar company do manager
Company requestCompany = companyRepository.findByManagerEmail(email);
if (requestCompany == null) throw new NotFoundException("Requested Company not found");

// 3. Buscar benefit
Benefit benefit = benefitRepository.findById(benefitId);
if (benefit == null) throw new NotFoundException("Requested Benefit not found");

// 4. Validar se partnership já existe
boolean exists = partnershipRepository.findExistingPartnership(requestCompany.id, benefit.id);
if (exists) throw new IllegalStateException("Partnership already exists");

// 5. Validar que company não é seu próprio provider
Company providerCompany = benefit.getProvider();
if (requestCompany.equals(providerCompany)) {
    throw new IllegalArgumentException("Company cannot be its own provider");
}

// 6. Criar partnership
Partnership partnership = Partnership.builder(requestCompany, benefit).build();

// 7. Converter para response
return new PartnershipResponse(
    partnership.id,
    partnership.getClientCompany().id,
    partnership.getBenefit().id,
    partnership.getStatus(),
    partnership.getCreatedAt()
);
```

**Insight:** O código imperativo é MUITO mais fácil de entender!

### 4.6 Por Que Ficou Assim?

**Razões comuns:**

1. **Falta de conhecimento de operadores:** Não saber quando usar `flatMap` vs `call` vs `combine`
2. **Desenvolvimento incremental:** Adicionar validações uma a uma sem refatorar
3. **Copy-paste de patterns:** Repetir estruturas vistas em outros lugares
4. **Falta de code review:** Ninguém apontou o problema antes de crescer
5. **Pressão de tempo:** "Funciona, entrego depois arrumo"

**Mas o principal motivo:**
> Código reativo MAL ESCRITO é PIOR que código imperativo. A verbosidade dos operadores esconde a lógica de negócio.

---

## 5. Patterns & Best Practices

### 5.1 Pattern: Extract and Name

#### Princípio
> **"Se você precisa de um comentário, você precisa de um método."**

#### ❌ Antes

```java
return repository.findById(id)
    .onItem().ifNull().failWith(() -> new NotFoundException("Not found"))
    .flatMap(item -> otherRepo.find(item.relatedId))
    .onItem().ifNull().failWith(() -> new NotFoundException("Related not found"));
```

#### ✅ Depois

```java
return getItemById(id)
    .flatMap(this::getRelatedItem);
    
// Métodos privados com nomes descritivos:
private Uni<Item> getItemById(Long id) {
    return repository.findById(id)
        .onItem().ifNull().failWith(() -> 
            new NotFoundException("Item not found")
        );
}

private Uni<RelatedItem> getRelatedItem(Item item) {
    return otherRepo.find(item.relatedId)
        .onItem().ifNull().failWith(() -> 
            new NotFoundException("Related item not found")
        );
}
```

**Benefícios:**
- Leitura linear (como história)
- Testável isoladamente
- Reutilizável
- Auto-documentado

### 5.2 Pattern: Combine Independent Operations

#### Quando Usar
Quando você precisa buscar **múltiplos dados independentes**.

#### ✅ Exemplo

```java
// ❌ SEQUENCIAL (lento): ~300ms total
return getUser(userId)                    // 100ms
    .flatMap(user -> getCompany(companyId)  // +100ms
        .flatMap(company -> getProduct(productId)  // +100ms
            .map(product -> combine(user, company, product))));

// ✅ PARALELO (rápido): ~100ms total
return Uni.combine().all()
    .unis(
        getUser(userId),        // |
        getCompany(companyId),  // | → Executam ao mesmo tempo
        getProduct(productId)   // |
    )
    .asTuple()
    .map(t -> combine(t.getItem1(), t.getItem2(), t.getItem3()));
```

**⚠️ Regra de Ouro:**
> Use `combine()` quando as operações **NÃO dependem umas das outras**.

#### Exemplos de Independência

```java
// ✅ INDEPENDENTES: Podem ser paralelos
getUser(1L)       // Não precisa de Company
getCompany(2L)    // Não precisa de User
getSettings()     // Não precisa de ninguém

// ❌ DEPENDENTES: DEVEM ser sequenciais
getUser(1L)                           // Primeiro buscar user
    .flatMap(u -> getCompany(u.companyId))  // Depois buscar company do user
```

### 5.3 Pattern: Separate Validation from Transformation

#### Princípio
> **"Valide primeiro, transforme depois."**

#### ❌ Misturado

```java
return repository.findById(id)
    .onItem().ifNull().failWith(() -> new NotFoundException())
    .flatMap(item -> {
        if (item.isDisabled()) {
            return Uni.createFrom().failure(new IllegalStateException());
        }
        if (item.getStatus() != Status.ACTIVE) {
            return Uni.createFrom().failure(new IllegalStateException());
        }
        return otherRepo.find(item.relatedId);
    });
```

#### ✅ Separado

```java
return getItemById(id)
    .call(this::validateItemIsEnabled)
    .call(this::validateItemIsActive)
    .flatMap(this::getRelatedItem);
    
// Validações claras e reutilizáveis
private Uni<Void> validateItemIsEnabled(Item item) {
    if (item.isDisabled()) {
        return Uni.createFrom().failure(
            new IllegalStateException("Item is disabled")
        );
    }
    return Uni.createFrom().voidItem();
}

private Uni<Void> validateItemIsActive(Item item) {
    if (item.getStatus() != Status.ACTIVE) {
        return Uni.createFrom().failure(
            new IllegalStateException("Item is not active")
        );
    }
    return Uni.createFrom().voidItem();
}
```

### 5.4 Pattern: Use `Uni<Void>` for Validations

#### Por Que?

Validações não produzem valores, apenas **passam ou falham**.

#### ✅ Exemplo

```java
// ❌ RUIM: Retorna Boolean não usado
private Uni<Boolean> validateUnique(String email) {
    return repository.existsByEmail(email)
        .flatMap(exists -> exists 
            ? Uni.createFrom().failure(new ConflictException())
            : Uni.createFrom().item(true)  // Boolean não usado!
        );
}

// ✅ BOM: Retorna Void
private Uni<Void> validateUnique(String email) {
    return repository.existsByEmail(email)
        .flatMap(exists -> exists 
            ? Uni.createFrom().failure(new ConflictException())
            : Uni.createFrom().voidItem()  // Void = "passou na validação"
        );
}

// Uso:
return validateUnique(email)
    .flatMap(() -> createUser(email));  // Se chegou aqui, validação passou
```

### 5.5 Pattern: Builder with Persistence

#### Quando Persistir?

```java
// ❌ RUIM: Persistir antes de validações
return buildEntity(request)
    .call(repository::persist)      // Salva ANTES de validar!
    .call(this::validateBusiness)   // ❌ Se falhar, entidade já foi salva
    .map(this::toResponse);

// ✅ BOM: Validar antes de persistir
return buildEntity(request)
    .call(this::validateBusiness)   // Valida ANTES de salvar
    .call(repository::persist)      // ✅ Só salva se validações passaram
    .map(this::toResponse);
```

**Regra:**
> **"Validate early, persist late."**

### 5.6 Pattern: Consistent Error Messages

```java
// ✅ Mensagens consistentes
private Uni<Manager> getManagerByEmail(String email) {
    return managerRepository.findByEmail(email)
        .onItem().ifNull().failWith(() -> 
            new NotFoundException("Manager not found with email: " + email)
        );
}

private Uni<Company> getCompanyByManager(String managerEmail) {
    return companyRepository.findByManagerEmail(managerEmail)
        .onItem().ifNull().failWith(() -> 
            new NotFoundException("Company not found for manager: " + managerEmail)
        );
}

// Todas seguem o padrão: "{Entity} not found [with/for] {identifier}: {value}"
```

### 5.7 Pattern: Avoid Premature Uni Creation

```java
// ❌ DESNECESSÁRIO
private Uni<Entity> buildEntity(Request req) {
    return Uni.createFrom().item(
        Entity.builder(req.name).build()
    );
}

// ✅ MELHOR (sem I/O)
private Entity buildEntity(Request req) {
    return Entity.builder(req.name).build();
}

// Uso:
return Uni.createFrom().item(buildEntity(req))
    .call(repository::persist);
```

**Exceção:** Se você quer consistência de API (todos métodos retornam Uni).

---

## 6. Refactoring Techniques

### 6.1 Técnica: Flatten Nested Calls

#### Passo 1: Identificar as Operações

```java
// Original (nested)
return op1()
    .call(r1 -> op2()
        .call(r2 -> op3()
            .call(r3 -> op4())));
```

**Operações identificadas:**
1. `op1()` → `r1`
2. `op2()` → `r2` (não depende de `r1`)
3. `op3()` → `r3` (não depende de `r2`)
4. `op4()` (não depende de `r3`)

#### Passo 2: Verificar Dependências

```
op1 → op2: Depende? NÃO
op2 → op3: Depende? NÃO
op3 → op4: Depende? NÃO
```

**Conclusão:** Podem ser paralelas!

#### Passo 3: Usar `combine()`

```java
// Refatorado (parallel)
return Uni.combine().all()
    .unis(op1(), op2(), op3())
    .asTuple()
    .flatMap(tuple -> op4());
```

#### Passo 4 (se houver dependências): Usar `flatMap()`

```java
// Se op2 depende de op1:
return op1()
    .flatMap(r1 -> op2(r1.getId()))
    .flatMap(r2 -> op3(r2.getData()))
    .flatMap(r3 -> op4(r3.getValue()));
```

### 6.2 Técnica: Extract to Private Methods

#### Passo 1: Identificar Blocos Lógicos

```java
// Original
return repository.findByEmail(email)
    .onItem().ifNull().failWith(() -> new NotFoundException("User not found"))
    .flatMap(user -> /* lógica complexa aqui */);
```

**Bloco identificado:** Buscar e validar usuário.

#### Passo 2: Extrair para Método Nomeado

```java
// Método extraído
private Uni<User> getUserByEmail(String email) {
    return repository.findByEmail(email)
        .onItem().ifNull().failWith(() -> 
            new NotFoundException("User not found with email: " + email)
        );
}

// Uso simplificado
return getUserByEmail(email)
    .flatMap(user -> /* lógica aqui */);
```

#### Passo 3: Nomear Descritivamente

**Bons nomes:**
- `getUserByEmail(email)` ✅
- `getCompanyByManager(managerEmail)` ✅
- `validateUserIsActive(user)` ✅
- `enrichWithPermissions(user)` ✅

**Nomes ruins:**
- `doSomething()` ❌
- `process()` ❌
- `handle()` ❌
- `getData()` ❌

### 6.3 Técnica: Refatorar `requestPartnership()`

Vamos refatorar o código real passo a passo.

#### Passo 1: Extrair Buscas

```java
// Métodos privados para cada busca
private Uni<Manager> getManagerByEmail(String email) {
    return managerRepository.findByEmail(email)
        .onItem().ifNull().failWith(() -> 
            new NotFoundException("Manager not found with email: " + email)
        );
}

private Uni<Company> getCompanyByManagerEmail(String email) {
    return companyRepository.findByManagerEmail(email)
        .onItem().ifNull().failWith(() -> 
            new NotFoundException("Company not found for manager: " + email)
        );
}

private Uni<Benefit> getBenefitById(Long id) {
    return benefitRepository.findById(id)
        .onItem().ifNull().failWith(() -> 
            new NotFoundException("Benefit not found with id: " + id)
        );
}
```

#### Passo 2: Refatorar Validações

```java
// De: Uni<Boolean> para: Uni<Void>
private Uni<Void> validatePartnershipDoesNotExist(Long companyId, Long benefitId) {
    return partnershipRepository.findExistingPartnership(companyId, benefitId)
        .flatMap(exists -> exists
            ? Uni.createFrom().failure(
                new IllegalStateException("Partnership already exists")
            )
            : Uni.createFrom().voidItem()
        );
}

// Simplificar verifyClientCompany
private Uni<Void> validateCompanyIsNotOwnProvider(Company client, Benefit benefit) {
    if (client.equals(benefit.getProvider())) {
        return Uni.createFrom().failure(
            new IllegalArgumentException("Company cannot be its own provider")
        );
    }
    return Uni.createFrom().voidItem();
}
```

#### Passo 3: Identificar Operações Paralelas

```
getManagerByEmail(email)        → Manager (apenas validação)
getCompanyByManagerEmail(email) → Company (precisa para resultado)
getBenefitById(id)              → Benefit (precisa para resultado)
```

**Análise:**
- `Manager` não é usado depois (apenas valida existência)
- `Company` e `Benefit` precisam estar disponíveis para validações e criação
- `Company` e `Benefit` são **independentes** → podem ser paralelos!

**Mas:** Faz sentido validar Manager primeiro (fast-fail).

#### Passo 4: Refatorar Método Principal

```java
@WithTransaction
public Uni<PartnershipResponse> requestPartnership(String managerEmail, Long benefitId) {
    // Passo 1: Validar que manager existe (fast-fail)
    return getManagerByEmail(managerEmail)
        
        // Passo 2: Buscar company e benefit em PARALELO
        .flatMap(manager -> Uni.combine().all()
            .unis(
                getCompanyByManagerEmail(managerEmail),
                getBenefitById(benefitId)
            )
            .asTuple()
        )
        
        // Passo 3: Validar regras de negócio
        .call(tuple -> {
            Company company = tuple.getItem1();
            Benefit benefit = tuple.getItem2();
            
            return validatePartnershipDoesNotExist(company.id, benefit.id)
                .call(() -> validateCompanyIsNotOwnProvider(company, benefit));
        })
        
        // Passo 4: Criar e persistir partnership
        .flatMap(tuple -> {
            Company company = tuple.getItem1();
            Benefit benefit = tuple.getItem2();
            
            Partnership partnership = Partnership.builder(company, benefit).build();
            
            return partnershipRepository.persist(partnership);
        })
        
        // Passo 5: Converter para response
        .map(this::toPartnershipResponse);
}

// Helper method
private PartnershipResponse toPartnershipResponse(Partnership partnership) {
    return new PartnershipResponse(
        partnership.id,
        partnership.getClientCompany().id,
        partnership.getBenefit().id,
        partnership.getStatus(),
        partnership.getCreatedAt()
    );
}
```

#### Resultado Final

**Comparação:**

| Métrica | Antes | Depois |
|---------|-------|--------|
| Níveis de aninhamento | 5+ | 2-3 |
| Linhas no método principal | 15 | 35 (mas muito mais legível) |
| Métodos privados | 3 | 6 |
| Testabilidade | Baixa | Alta |
| Legibilidade | Ruim | Excelente |
| Paralelização | Não | Sim (Company + Benefit) |
| Transação | Não | Sim (@WithTransaction) |

### 6.4 Técnica: Use Method References

```java
// ❌ VERBOSO
.map(item -> convertToDto(item))
.flatMap(id -> findById(id))
.call(entity -> repository.persist(entity))

// ✅ CONCISO
.map(this::convertToDto)
.flatMap(this::findById)
.call(repository::persist)
```

### 6.5 Técnica: Chain Validations

```java
// ✅ Validações em cadeia linear
return getEntity(id)
    .call(this::validateIsActive)
    .call(this::validateHasPermission)
    .call(this::validateNotExpired)
    .flatMap(this::doOperation);
    
// Cada validação é Uni<Void>:
// - Se passar: continua
// - Se falhar: interrompe a cadeia
```

---

## 6.6 🎯 EXEMPLO PRÁTICO COMPLETO: requestPartnership() Refatorado

> **Este é o código REAL e FUNCIONAL do método refatorado com TODOS os métodos privados necessários.**

### Código Completo do Service

```java
package org.acme.domains.partnership;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.partnership.dto.PartnershipResponse;
import org.jboss.resteasy.reactive.common.NotImplementedYet;

import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class PartnershipService {

    private final PartnershipRepository partnershipRepository;
    private final ManagerRepository managerRepository;
    private final CompanyRepository companyRepository;
    private final BenefitRepository benefitRepository;

    // Constructor injection
    public PartnershipService(
            PartnershipRepository partnershipRepository,
            ManagerRepository managerRepository,
            CompanyRepository companyRepository,
            BenefitRepository benefitRepository) {
        this.partnershipRepository = partnershipRepository;
        this.managerRepository = managerRepository;
        this.companyRepository = companyRepository;
        this.benefitRepository = benefitRepository;
    }

    // ============================================================================
    // MÉTODO PRINCIPAL - Linear, legível, sem aninhamento!
    // ============================================================================
    
    /**
     * Solicita uma parceria entre uma empresa e um benefício.
     * 
     * Fluxo:
     * 1. Valida que o manager existe (fast-fail)
     * 2. Busca company e benefit em PARALELO (performance)
     * 3. Valida regras de negócio (partnership não existe + não é self-provider)
     * 4. Cria e persiste a partnership
     * 5. Converte para DTO de resposta
     */
    @WithTransaction
    public Uni<PartnershipResponse> requestPartnership(String managerEmail, Long benefitId) {
        return validateManagerExists(managerEmail)
            .flatMap(ignored -> fetchCompanyAndBenefit(managerEmail, benefitId))
            .call(this::validateBusinessRules)
            .flatMap(this::createAndPersistPartnership)
            .map(this::toPartnershipResponse);
    }
    
    // ============================================================================
    // MÉTODOS PRIVADOS - Cada um faz UMA COISA
    // ============================================================================
    
    /**
     * Step 1: Valida que o manager existe.
     * 
     * Fast-fail pattern: Se manager não existe, falha ANTES de buscar outras coisas.
     * 
     * @return Uni<Void> - Não retorna o Manager porque ele não é usado depois,
     *                     apenas validamos que existe.
     */
    private Uni<Void> validateManagerExists(String email) {
        return managerRepository.findByEmail(email)
            .onItem().ifNull().failWith(() -> 
                new NotFoundException("Manager not found with email: " + email)
            )
            .replaceWithVoid();  // Descarta o Manager, retorna Uni<Void>
    }
    
    /**
     * Step 2: Busca Company e Benefit em PARALELO.
     * 
     * Performance: Executa as duas queries ao mesmo tempo (~100ms) em vez de 
     * sequencial (~200ms).
     * 
     * @return Uni<Tuple2<Company, Benefit>> - Tupla com os dois objetos
     */
    private Uni<Tuple2<Company, Benefit>> fetchCompanyAndBenefit(
            String managerEmail, 
            Long benefitId) {
        
        return Uni.combine().all()
            .unis(
                getCompanyByManagerEmail(managerEmail),
                getBenefitById(benefitId)
            )
            .asTuple();
    }
    
    /**
     * Step 3: Valida todas as regras de negócio.
     * 
     * Executa validações em sequência:
     * - Partnership não pode existir já
     * - Company não pode ser seu próprio provider
     * 
     * Se qualquer validação falhar, a cadeia é interrompida.
     * 
     * @return Uni<Void> - Se chegou aqui, todas validações passaram
     */
    private Uni<Void> validateBusinessRules(Tuple2<Company, Benefit> tuple) {
        Company company = tuple.getItem1();
        Benefit benefit = tuple.getItem2();
        
        return validatePartnershipDoesNotExist(company.id, benefit.id)
            .call(() -> validateCompanyIsNotOwnProvider(company, benefit));
    }
    
    /**
     * Step 4: Cria a Partnership e persiste no banco.
     * 
     * Builder pattern: Usa o builder da entidade para criar a instância.
     * 
     * @return Uni<Partnership> - Entidade persistida com ID gerado
     */
    private Uni<Partnership> createAndPersistPartnership(Tuple2<Company, Benefit> tuple) {
        Company company = tuple.getItem1();
        Benefit benefit = tuple.getItem2();
        
        // Cria a entidade usando builder pattern
        Partnership partnership = Partnership.builder(company, benefit).build();
        
        // Persiste e retorna a entidade com ID gerado
        return partnershipRepository.persist(partnership);
    }
    
    // ============================================================================
    // HELPERS DE BUSCA - Encapsulam queries + validação de null
    // ============================================================================
    
    /**
     * Busca Company por email do Manager.
     * 
     * Pattern: Busca + validação de null encapsulada.
     * Mensagem de erro clara e específica.
     */
    private Uni<Company> getCompanyByManagerEmail(String managerEmail) {
        return companyRepository.findByManagerEmail(managerEmail)
            .onItem().ifNull().failWith(() -> 
                new NotFoundException("Company not found for manager: " + managerEmail)
            );
    }
    
    /**
     * Busca Benefit por ID.
     * 
     * Pattern: Busca + validação de null encapsulada.
     * Mensagem de erro clara e específica.
     */
    private Uni<Benefit> getBenefitById(Long benefitId) {
        return benefitRepository.findById(benefitId)
            .onItem().ifNull().failWith(() -> 
                new NotFoundException("Benefit not found with id: " + benefitId)
            );
    }
    
    // ============================================================================
    // VALIDAÇÕES - Retornam Uni<Void> (passa ou falha, sem valor)
    // ============================================================================
    
    /**
     * Valida que a partnership NÃO existe ainda.
     * 
     * Pattern: Uni<Void> para validações.
     * - Se existir: emite IllegalStateException
     * - Se não existir: retorna Uni.voidItem() (validação passou)
     */
    private Uni<Void> validatePartnershipDoesNotExist(Long companyId, Long benefitId) {
        return partnershipRepository.findExistingPartnership(companyId, benefitId)
            .flatMap(exists -> {
                if (exists) {
                    return Uni.createFrom().failure(
                        new IllegalStateException(
                            "Partnership already exists between company " + 
                            companyId + " and benefit " + benefitId
                        )
                    );
                }
                return Uni.createFrom().voidItem();
            });
    }
    
    /**
     * Valida que a Company NÃO está tentando ser seu próprio provider.
     * 
     * Pattern: Validação síncrona (sem I/O).
     * - Se for self-provider: emite IllegalArgumentException
     * - Se for válido: retorna Uni.voidItem()
     */
    private Uni<Void> validateCompanyIsNotOwnProvider(Company client, Benefit benefit) {
        // Validação síncrona - apenas comparação em memória
        if (client.id.equals(benefit.getProvider().id)) {
            return Uni.createFrom().failure(
                new IllegalArgumentException(
                    "Company " + client.getName() + 
                    " cannot request a benefit from itself"
                )
            );
        }
        return Uni.createFrom().voidItem();
    }
    
    // ============================================================================
    // CONVERSÕES - Transformações síncronas (Entity → DTO)
    // ============================================================================
    
    /**
     * Converte Partnership entity para DTO de resposta.
     * 
     * Pattern: Transformação síncrona - usa map() no método principal.
     * Apenas copia campos, sem I/O.
     */
    private PartnershipResponse toPartnershipResponse(Partnership partnership) {
        return new PartnershipResponse(
            partnership.id,
            partnership.getClientCompany().id,
            partnership.getBenefit().id,
            partnership.getStatus(),
            partnership.getCreatedAt()
        );
    }
}
```

### 📊 Análise do Código Refatorado

#### ✅ **Benefícios Conquistados**

| Aspecto | Antes | Depois |
|---------|-------|--------|
| **Aninhamento** | 5 níveis | 0 níveis |
| **Legibilidade do método principal** | ⭐ | ⭐⭐⭐⭐⭐ |
| **Testabilidade** | Difícil | Fácil (cada método privado pode ser testado) |
| **Performance** | Sequencial | Paralelo (Company + Benefit) |
| **Manutenibilidade** | Difícil adicionar validações | Fácil (adiciona um `.call()`) |
| **Debugging** | Stack trace confusa | Stack trace clara |
| **Reutilização** | Impossível | Métodos podem ser reutilizados |

#### 🎯 **Como Ler o Código**

**O método principal conta uma HISTÓRIA:**

```java
public Uni<PartnershipResponse> requestPartnership(String managerEmail, Long benefitId) {
    return validateManagerExists(managerEmail)           // 1. Manager existe?
        .flatMap(ignored -> fetchCompanyAndBenefit(...)) // 2. Busca dados (paralelo!)
        .call(this::validateBusinessRules)               // 3. Valida regras de negócio
        .flatMap(this::createAndPersistPartnership)      // 4. Cria e salva
        .map(this::toPartnershipResponse);               // 5. Converte para DTO
}
```

**Cada linha é AUTO-EXPLICATIVA!** Você NÃO precisa ler os métodos privados para entender o fluxo.

#### 🔍 **Detalhes de Implementação**

**1. Por que `replaceWithVoid()`?**

```java
private Uni<Void> validateManagerExists(String email) {
    return managerRepository.findByEmail(email)
        .onItem().ifNull().failWith(...)
        .replaceWithVoid();  // ← Converte Uni<Manager> → Uni<Void>
}
```

- Manager não é usado depois, apenas validamos que existe
- `replaceWithVoid()` descarta o valor mas mantém a chain reativa
- Semântica clara: "Esta operação valida, não retorna dados"

**2. Por que `Uni.combine().all()`?**

```java
private Uni<Tuple2<Company, Benefit>> fetchCompanyAndBenefit(...) {
    return Uni.combine().all()
        .unis(
            getCompanyByManagerEmail(managerEmail),  // Query 1
            getBenefitById(benefitId)                // Query 2
        )
        .asTuple();  // Executa SIMULTANEAMENTE!
}
```

- Company e Benefit são **independentes**
- Execução paralela: 100ms em vez de 200ms
- `asTuple()` retorna `Tuple2<Company, Benefit>` para acessar depois

**3. Por que `.call()` para validações?**

```java
.call(this::validateBusinessRules)  // ← call(), não flatMap()
```

- `call()` executa o Uni mas **MANTÉM o valor original** (Tuple2)
- Se validação passar: continua com a Tuple2
- Se validação falhar: chain é interrompida
- Perfeito para side-effects que não mudam o tipo de retorno

**4. Por que `Uni<Void>` nas validações?**

```java
private Uni<Void> validatePartnershipDoesNotExist(...) {
    return partnershipRepository.findExistingPartnership(...)
        .flatMap(exists -> exists
            ? Uni.createFrom().failure(new IllegalStateException())
            : Uni.createFrom().voidItem()  // ← Sucesso = void
        );
}
```

- Validações não produzem valores, apenas **passam ou falham**
- `Uni<Void>` deixa isso explícito na assinatura
- Semântica clara: "Ou emite erro, ou não faz nada"

#### 📝 **Quando Usar Cada Operador**

| Operador | Quando Usar | Exemplo no Código |
|----------|-------------|-------------------|
| `.flatMap()` | Operação assíncrona que **muda o tipo** | `fetchCompanyAndBenefit()` → muda de Void para Tuple2 |
| `.call()` | Operação assíncrona que **mantém o tipo** | `validateBusinessRules()` → mantém Tuple2 |
| `.map()` | Transformação síncrona (sem I/O) | `toPartnershipResponse()` → Entity para DTO |
| `Uni.combine()` | Múltiplas operações **independentes** | Buscar Company e Benefit ao mesmo tempo |

#### 🧪 **Como Testar**

```java
@QuarkusTest
class PartnershipServiceTest {
    
    @Inject
    PartnershipService service;
    
    @Test
    void shouldCreatePartnership() {
        // ✅ Código limpo é fácil de testar
        PartnershipResponse response = service
            .requestPartnership("manager@company.com", 1L)
            .await().indefinitely();
            
        assertNotNull(response);
        assertEquals(PartnershipStatus.PENDING, response.status());
    }
    
    @Test
    void shouldFailWhenManagerNotFound() {
        // ✅ Testa validação específica
        assertThrows(NotFoundException.class, () -> {
            service.requestPartnership("invalid@email.com", 1L)
                .await().indefinitely();
        });
    }
    
    @Test
    void shouldFailWhenPartnershipAlreadyExists() {
        // ✅ Testa regra de negócio específica
        assertThrows(IllegalStateException.class, () -> {
            service.requestPartnership("manager@company.com", 1L)
                .await().indefinitely();
        });
    }
}
```

#### 💡 **Dicas para Aplicar no Seu Código**

1. **Comece pelo método principal:** Escreva como uma história
2. **Extraia métodos privados:** Cada flatMap/call vira um método
3. **Nomeie descritivamente:** `getUserById()`, não `getUser()`
4. **Use Uni<Void> para validações:** Deixa semântica clara
5. **Identifique operações paralelas:** Use `Uni.combine()` quando possível
6. **Teste cada método isoladamente:** Facilita debugging

---

## 7. Testing Reactive Code

### 7.1 Testando com `await()`

```java
@QuarkusTest
class UserServiceTest {
    
    @Inject
    UserService service;
    
    @Test
    void shouldFindUserById() {
        // ✅ await() em testes é OK
        User user = service.findUser(1L)
            .await().indefinitely();
            
        assertNotNull(user);
        assertEquals("John", user.getName());
    }
    
    @Test
    void shouldFailWhenUserNotFound() {
        // Testar falhas
        assertThrows(NotFoundException.class, () -> {
            service.findUser(999L).await().indefinitely();
        });
    }
}
```

### 7.2 Testando Timeouts

```java
@Test
void shouldTimeoutAfter5Seconds() {
    // Simular operação lenta
    Uni<String> slow = Uni.createFrom().item("result")
        .onItem().delayIt().by(Duration.ofSeconds(10));
        
    // Testar timeout
    assertThrows(TimeoutException.class, () -> {
        slow.await().atMost(Duration.ofSeconds(5));
    });
}
```

### 7.3 Testando com AssertSubscriber

```java
@Test
void shouldEmitThreeItems() {
    Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
    
    AssertSubscriber<Integer> subscriber = multi
        .subscribe().withSubscriber(AssertSubscriber.create(3));
        
    subscriber
        .assertCompleted()
        .assertItems(1, 2, 3);
}
```

### 7.4 Mocking Repositories Reativos

```java
@QuarkusTest
class PartnershipServiceTest {
    
    @InjectMock
    PartnershipRepository repository;
    
    @InjectMock
    ManagerRepository managerRepository;
    
    @Inject
    PartnershipService service;
    
    @Test
    void shouldCreatePartnership() {
        // Arrange
        Manager manager = new Manager("john@company.com");
        Company company = new Company("ACME Corp");
        Benefit benefit = new Benefit("Health Insurance");
        
        when(managerRepository.findByEmail("john@company.com"))
            .thenReturn(Uni.createFrom().item(manager));
            
        when(repository.findExistingPartnership(any(), any()))
            .thenReturn(Uni.createFrom().item(false));
            
        // Act
        PartnershipResponse response = service
            .requestPartnership("john@company.com", 1L)
            .await().indefinitely();
            
        // Assert
        assertNotNull(response);
        verify(repository).persist(any(Partnership.class));
    }
}
```

### 7.5 Testando Métodos Privados (Indiretamente)

```java
// ❌ NÃO tente tornar métodos privados públicos só para testar
// ❌ NÃO use reflection para testar métodos privados

// ✅ Teste através do método público
@Test
void shouldValidateManagerExists() {
    when(managerRepository.findByEmail("invalid@email.com"))
        .thenReturn(Uni.createFrom().nullItem());
        
    // Valida indiretamente que getManagerByEmail() falha corretamente
    assertThrows(NotFoundException.class, () -> {
        service.requestPartnership("invalid@email.com", 1L)
            .await().indefinitely();
    });
}
```

---

## 8. Exercícios Práticos

### Exercício 1: Flatten Nested Calls ⭐

**Código Original:**
```java
public Uni<OrderResponse> createOrder(CreateOrderRequest request, String userEmail) {
    return userRepository.findByEmail(userEmail)
        .onItem().ifNull().failWith(() -> new NotFoundException("User not found"))
        .call(user -> productRepository.findById(request.getProductId())
            .onItem().ifNull().failWith(() -> new NotFoundException("Product not found"))
            .call(product -> inventoryRepository.checkStock(product.id, request.getQuantity())
                .call(hasStock -> {
                    if (!hasStock) {
                        return Uni.createFrom().failure(new OutOfStockException());
                    }
                    return Uni.createFrom().voidItem();
                })
                .call(() -> createOrder(user, product, request.getQuantity())));
}
```

**Tarefa:** Refatore este código para:
1. Extrair métodos privados para cada busca
2. Achatar os níveis de aninhamento
3. Usar nomes descritivos
4. Adicionar `@WithTransaction`

<details>
<summary><strong>💡 Dica</strong></summary>

- User e Product são independentes → podem ser paralelos?
- Não! Product depende de request, mas user não depende de product
- Mas vale a pena paralelizar só 2 itens? Considere legibilidade vs performance
- Validações devem vir ANTES de createOrder
</details>

---

### Exercício 2: Fix Incorrect Operators ⭐⭐

**Código Original:**
```java
public Uni<InvoiceResponse> generateInvoice(Long orderId) {
    return orderRepository.findById(orderId)
        .call(order -> calculateTotal(order))  // ❌ Problema aqui!
        .call(total -> applyDiscount(total))   // ❌ E aqui!
        .flatMap(discountedTotal -> createInvoice(orderId, discountedTotal));
}

private Uni<BigDecimal> calculateTotal(Order order) { /*...*/ }
private Uni<BigDecimal> applyDiscount(BigDecimal total) { /*...*/ }
```

**Tarefa:** 
- Identifique os operadores incorretos
- Corrija para usar `flatMap` onde apropriado
- Explique por que estava errado

<details>
<summary><strong>💡 Dica</strong></summary>

- `call()` descarta o resultado do Uni
- Se você precisa do resultado para a próxima operação, use `flatMap()`
- Qual o tipo de cada operação? `Uni<Order>` → `Uni<BigDecimal>` → `Uni<BigDecimal>` → `Uni<Invoice>`
</details>

---

### Exercício 3: Add Proper Validation ⭐⭐

**Código Original:**
```java
public Uni<SubscriptionResponse> subscribe(Long employeeId, Long benefitId) {
    return employeeRepository.findById(employeeId)
        .flatMap(employee -> benefitRepository.findById(benefitId)
            .flatMap(benefit -> {
                Subscription sub = Subscription.builder(employee, benefit).build();
                return subscriptionRepository.persist(sub);
            }))
        .map(this::toResponse);
}
```

**Problemas:**
- Não valida se employee ou benefit são null
- Não valida se subscription já existe
- Não valida se employee está ativo

**Tarefa:**
Adicione as validações apropriadas usando métodos privados e `Uni<Void>`.

---

### Exercício 4: Parallelize Independent Operations ⭐⭐⭐

**Código Original:**
```java
public Uni<DashboardData> getDashboard(String userEmail) {
    return userRepository.findByEmail(userEmail)
        .flatMap(user -> companyRepository.findById(user.getCompanyId())
            .flatMap(company -> statisticsService.getCompanyStats(company.id)
                .flatMap(stats -> notificationService.getUnreadCount(user.id)
                    .map(unread -> new DashboardData(user, company, stats, unread)))));
}
```

**Tarefa:**
- Identifique quais operações são independentes
- Refatore usando `Uni.combine()` onde apropriado
- Reduza o tempo total de execução

<details>
<summary><strong>💡 Dica</strong></summary>

```
User → Company (depende de user.companyId)
      ↓
      ├─> Stats (depende de company.id)
      └─> Unread (depende de user.id)
      
Stats e Unread são independentes entre si!
```
</details>

---

### Exercício 5: Refactor Real Code ⭐⭐⭐

Pegue um dos seus próprios métodos do projeto e refatore aplicando TODAS as técnicas aprendidas:

**Checklist:**
- [ ] Extrair métodos privados
- [ ] Achatar chains aninhados
- [ ] Usar operadores corretos (`map`, `flatMap`, `call`)
- [ ] Validações retornam `Uni<Void>`
- [ ] Operações independentes são paralelas
- [ ] Nomes descritivos
- [ ] Adicionar `@WithTransaction` se necessário
- [ ] Mensagens de erro consistentes

---

## 9. Cheatsheet de Referência

### 9.1 Quando Usar Cada Operador

| Situação | Operador | Exemplo |
|----------|----------|---------|
| Transformar valor (síncrono) | `map` | `.map(entity -> dto)` |
| Transformar valor (assíncrono) | `flatMap` | `.flatMap(id -> findById(id))` |
| Side-effect (salvar, log, cache) | `call` | `.call(repo::persist)` |
| Validar e continuar | `call` | `.call(this::validate)` |
| Combinar múltiplos Unis independentes | `combine` | `Uni.combine().all().unis(...)` |
| Tratar null | `onItem().ifNull()` | `.onItem().ifNull().failWith(...)` |
| Tratar erro | `onFailure()` | `.onFailure().recoverWith(...)` |
| Transformar em lista | `collect()` | `.collect().asList()` |
| Filtrar items | `select().where()` | `.select().where(x -> x > 10)` |

### 9.2 Fluxograma de Decisão

```
Preciso transformar o valor?
├─ Sim
│  ├─ A transformação retorna Uni<T>?
│  │  ├─ Sim → use flatMap
│  │  └─ Não → use map (⚠️ NUNCA flatMap com Uni.createFrom().item!)
│  └─ Não
│     ├─ Preciso fazer uma operação assíncrona mas manter o valor original?
│     │  └─ Sim → use call
│     └─ Preciso combinar múltiplos Unis?
│        └─ Sim → use combine
```

**⚠️ ANTI-PATTERN COMUM:**
```java
// ❌ ERRADO: Usar flatMap quando map seria suficiente
.flatMap(entity -> Uni.createFrom().item(toDTO(entity)))

// ✅ CORRETO: Usar map diretamente
.map(entity -> toDTO(entity))

// Regra: Se você precisa de Uni.createFrom().item(), use map!
```

### 9.3 Common Patterns

#### Pattern: Get or Fail
```java
private Uni<Entity> getEntityById(Long id) {
    return repository.findById(id)
        .onItem().ifNull().failWith(() -> 
            new NotFoundException("Entity not found: " + id)
        );
}
```

#### Pattern: Validate or Fail
```java
private Uni<Void> validateCondition(Entity entity) {
    if (!entity.isValid()) {
        return Uni.createFrom().failure(
            new ValidationException("Invalid entity")
        );
    }
    return Uni.createFrom().voidItem();
}
```

#### Pattern: Parallel Fetch
```java
Uni.combine().all()
    .unis(getUser(id), getCompany(id), getSettings())
    .asTuple()
    .map(tuple -> combine(
        tuple.getItem1(),
        tuple.getItem2(),
        tuple.getItem3()
    ));
```

#### Pattern: Sequential with Dependency
```java
getUser(id)
    .flatMap(user -> getCompany(user.companyId))
    .flatMap(company -> getProducts(company.id));
```

#### Pattern: Build, Validate, Persist
```java
return buildEntity(request)
    .call(this::validateBusinessRules)
    .call(repository::persist)
    .map(this::toResponse);
```

### 9.4 Error Handling Patterns

#### Retry on Failure
```java
repository.findById(id)
    .onFailure().retry().atMost(3)
    .onFailure().recoverWithItem(DEFAULT_VALUE);
```

#### Fallback Value
```java
service.getData()
    .onFailure().recoverWithItem(FALLBACK_DATA);
```

#### Transform Error
```java
externalService.call()
    .onFailure(IOException.class)
    .transform(err -> new ServiceUnavailableException(err));
```

### 9.5 Operações Síncronas vs Assíncronas

Entenda a diferença crucial entre operações que bloqueiam e operações rápidas:

| Operação | Tipo | Tempo | Operador | Bloqueia? |
|----------|------|-------|----------|-----------|
| Entity → DTO | Síncrono (memória) | ~0.001ms | `map` | ❌ Não |
| Cálculo matemático | Síncrono (CPU) | ~0.0001ms | `map` | ❌ Não |
| String.toUpperCase() | Síncrono (memória) | ~0.0001ms | `map` | ❌ Não |
| new Object() | Síncrono (memória) | ~0.001ms | `map` | ❌ Não |
| Query no banco | **Assíncrono (I/O)** | ~50-200ms | `flatMap` | ✅ Se bloqueante! |
| HTTP request | **Assíncrono (I/O)** | ~100-500ms | `flatMap` | ✅ Se bloqueante! |
| Ler arquivo | **Assíncrono (I/O)** | ~10-100ms | `flatMap` | ✅ Se bloqueante! |
| Cache lookup (Redis) | **Assíncrono (I/O)** | ~1-5ms | `flatMap` | ✅ Se bloqueante! |

**Regra de Ouro:**
> Use `map` para operações que fazem apenas **computação/memória** (rápidas).  
> Use `flatMap` para operações que fazem **I/O** (retornam `Uni`).

**Por que `map` não bloqueia?**
```java
// ✅ map() executa código simples instantaneamente
.map(user -> new UserDTO(user.name, user.email))  // ~0.001ms

// Mesmo sendo "síncrono", não há espera!
// A thread executa o código e continua imediatamente.
```

**O que realmente bloqueia?**
```java
// 🚫 ISTO bloqueia (I/O síncrono):
String result = httpClient.syncGet("https://...");  // Thread ESPERA resposta

// ✅ ISTO não bloqueia (I/O assíncrono):
Uni<String> result = httpClient.get("https://...");  // Thread continua
```

### 9.6 Code Review Checklist

Use esta checklist ao revisar código reativo:

**Estrutura:**
- [ ] Níveis de aninhamento ≤ 3?
- [ ] Métodos privados têm nomes descritivos?
- [ ] Lógica de negócio é clara?

**Operadores:**
- [ ] Usando `flatMap` quando retorna `Uni`?
- [ ] Usando `call` apenas para side-effects?
- [ ] Usando `map` apenas para transformações síncronas?

**Validações:**
- [ ] Null checks com `.onItem().ifNull().failWith()`?
- [ ] Validações retornam `Uni<Void>`?
- [ ] Validações antes de persistência?

**Performance:**
- [ ] Operações independentes são paralelas (`combine`)?
- [ ] Sem bloqueios acidentais (`.await()` em service)?

**Transações:**
- [ ] `@WithTransaction` em operações de escrita?

**Testes:**
- [ ] Casos de sucesso cobertos?
- [ ] Casos de falha cobertos?
- [ ] Timeouts testados?

**Mensagens:**
- [ ] Erros são descritivos?
- [ ] Incluem contexto (IDs, emails, etc)?

---

## 10. Referências e Recursos

### 10.1 Documentação Oficial

**Mutiny:**
- 📘 [Mutiny Reference Guide](https://smallrye.io/smallrye-mutiny/)
- 📘 [Mutiny Javadoc](https://javadoc.io/doc/io.smallrye.reactive/mutiny/latest/index.html)
- 📺 [Mutiny Tutorials](https://smallrye.io/smallrye-mutiny/tutorials)

**Quarkus:**
- 📘 [Quarkus Reactive Programming Guide](https://quarkus.io/guides/getting-started-reactive)
- 📘 [Hibernate Reactive with Panache](https://quarkus.io/guides/hibernate-reactive-panache)
- 📘 [Quarkus REST Reactive](https://quarkus.io/guides/resteasy-reactive)

### 10.2 Conceitos de Programação Reativa

**Reactive Manifesto:**
- 📄 [The Reactive Manifesto](https://www.reactivemanifesto.org/)
- Conceitos: Responsive, Resilient, Elastic, Message-Driven

**Reactive Streams:**
- 📄 [Reactive Streams Specification](http://www.reactive-streams.org/)
- 📄 [Understanding Backpressure](https://www.baeldung.com/spring-webflux-backpressure)

### 10.3 Livros Recomendados

**📕 "Reactive Programming with RxJava"** - Tomasz Nurkiewicz, Ben Christensen
- Embora seja RxJava, os conceitos se aplicam ao Mutiny
- Excelente para entender operadores e padrões

**📕 "Reactive Design Patterns"** - Roland Kuhn, Brian Hanafee, Jamie Allen
- Padrões arquiteturais para sistemas reativos
- Como estruturar aplicações reativas

### 10.4 Artigos e Blog Posts

- 📝 [Mutiny - An Intuitive Reactive Programming Library](https://quarkus.io/blog/mutiny-intro/)
- 📝 [From Callback Hell to Uni Heaven](https://quarkus.io/blog/uni-and-multi/)
- 📝 [Testing Reactive Applications](https://quarkus.io/guides/getting-started-testing#testing-reactive-endpoints)

### 10.5 Comunidade

- 💬 [Quarkus Zulip Chat](https://quarkusio.zulipchat.com/)
- 💬 [Stack Overflow - tag: mutiny](https://stackoverflow.com/questions/tagged/mutiny)
- 🐙 [Mutiny GitHub](https://github.com/smallrye/smallrye-mutiny)

### 10.6 Ferramentas

**Debugging:**
- Use `.log()` para traçar o fluxo: `uni.log().map(...)`
- IntelliJ IDEA tem suporte para reactive streams
- Quarkus Dev UI para visualizar métricas

**Testing:**
- `AssertSubscriber` para testar Multi
- `.await().indefinitely()` para testes
- `@QuarkusTest` para integration tests

### 10.7 Comparação com Outras Bibliotecas

| Biblioteca | `Uni<T>` equivalente | `Multi<T>` equivalente |
|------------|---------------------|------------------------|
| **Project Reactor** | `Mono<T>` | `Flux<T>` |
| **RxJava** | `Single<T>` / `Maybe<T>` | `Observable<T>` / `Flowable<T>` |
| **Java 9+** | `CompletableFuture<T>` | `Flow.Publisher<T>` |

**Por que Mutiny?**
- Projetado especificamente para Quarkus
- API mais intuitiva que Reactor/RxJava
- Melhor integração com Hibernate Reactive
- Mensagens de erro mais claras
- Menos overhead

---

## Conclusão

### O Que Você Aprendeu

1. **Fundamentos**: O que é programação reativa e por que usar
2. **Mutiny API**: `Uni`, `Multi`, operadores essenciais
3. **Code Smells**: Identificar problemas em código reativo
4. **Análise Real**: Dissecar o método `requestPartnership()`
5. **Patterns**: Boas práticas e padrões profissionais
6. **Refactoring**: Técnicas para melhorar código existente
7. **Testing**: Como testar código reativo
8. **Exercícios**: Prática hands-on

### Próximos Passos

1. **Revisite seu código** usando o checklist da Seção 9.5
2. **Refatore um método** aplicando as técnicas aprendidas
3. **Compartilhe com o time** este guia como referência
4. **Pratique os exercícios** da Seção 8
5. **Leia a documentação oficial** da Seção 10

### Pensamento Final

> **"Código reativo bem escrito é como uma história bem contada: linear, clara e fácil de seguir. Código reativo mal escrito é como inception: você precisa de um diagrama para entender em qual nível de sonho você está."**

Programação reativa não é difícil - é apenas **diferente**. Com as técnicas certas, você escreve código mais:
- ✅ Performático (non-blocking)
- ✅ Escalável (menos recursos)
- ✅ Legível (quando bem estruturado)
- ✅ Testável (quando bem decomposto)
- ✅ Manutenível (quando bem documentado)

**Continue praticando!** 🚀

---

## FAQ - Perguntas Frequentes

### ❓ Por que usar Uni sendo que já tenho CompletableFuture?

**Resposta:** NÃO use CompletableFuture se você já tem Uni!

- **CompletableFuture é EAGER** - executa imediatamente, mesmo se ninguém vai usar o resultado
- **Uni é LAZY** - só executa quando alguém faz subscribe (mais eficiente)
- **Uni tem API melhor** - `map/flatMap` ao invés de `thenApply/thenCompose`
- **Uni suporta backpressure** - essencial para sistemas reativos
- **Uni é primeira classe no Quarkus** - melhor integração com framework

**Use CompletableFuture APENAS quando:**
- Integrar com biblioteca legada que retorna CF
- Interoperabilidade com código Java 8+ não-reativo

Caso contrário, sempre prefira Uni!

### ❓ Quando usar `map` vs `flatMap`?

**map:** Quando a transformação retorna um valor direto (síncrono)
```java
.map(user -> user.getName())  // String getName()
```

**flatMap:** Quando a transformação retorna outro Uni (assíncrono)
```java
.flatMap(user -> findAddress(user.addressId))  // Uni<Address> findAddress()
```

**Regra simples:** Se o método retorna `Uni<T>`, use `flatMap`. Senão, use `map`.

### ❓ Por que meu código tem 5+ níveis de aninhamento?

Você está usando `.call()` para operações que deveriam ser `flatMap()` ou usando `flatMap` para operações que poderiam ser paralelas com `combine()`.

**Solução:**
1. Extraia métodos privados para cada operação
2. Use `Uni.combine()` para operações independentes
3. Use `flatMap` linear para operações dependentes

Veja a Seção 6.3 para exemplo completo de refatoração.

### ❓ `map()` não vai bloquear a thread sendo que é síncrono?

**NÃO!** Essa é uma confusão comum entre **síncrono vs bloqueante**.

**Bloqueante** = Thread fica **ESPERANDO** por I/O (rede, disco, etc):
```java
// 🚫 BLOQUEANTE - Thread para e ESPERA
String result = httpClient.get("https://api.com");  // ESPERANDO rede
User user = database.query("SELECT...");             // ESPERANDO banco
```

**Síncrono (map)** = Operação **RÁPIDA** em memória (nanossegundos):
```java
// ✅ SÍNCRONO mas NÃO bloqueante - Execução instantânea
UserDTO dto = new UserDTO(entity.getName(), entity.getEmail());  // ~0.001ms
String upper = name.toUpperCase();                                // ~0.0001ms
int total = price * quantity;                                     // ~0.00001ms
```

**Por que usar `map` para Entity → DTO?**
```java
return repository.findById(id)     // 100ms (I/O do banco) ← AQUI está o I/O
    .map(entity -> toDTO(entity)); // 0.001ms (cópia de campos) ← Irrelevante!
```

**E se eu usar `flatMap`?**
```java
// ❌ FUNCIONA mas é MÁ PRÁTICA
return repository.findById(id)
    .flatMap(entity -> Uni.createFrom().item(toDTO(entity)));
    
// Problemas:
// 1. Verbosidade desnecessária (criar Uni manualmente)
// 2. Overhead de performance (objeto Uni extra)
// 3. Má semântica (flatMap sugere operação assíncrona)
// 4. Menos legível (obscurece a intenção)

// ✅ CORRETO - map expressa a intenção correta
return repository.findById(id)
    .map(entity -> toDTO(entity));
```

**Regra:**
- **I/O (banco, rede, arquivo)?** → Use `flatMap` (retorna `Uni`)
- **Memória/CPU pura?** → Use `map` (retorna valor direto)

### ❓ Qual o problema de usar `flatMap` para mapear DTO?

**Resposta:** Tecnicamente funciona, mas é um **anti-pattern**!

```java
// ❌ ANTI-PATTERN: flatMap para operação síncrona
.flatMap(user -> Uni.createFrom().item(new UserDTO(user)))

// ✅ CORRETO: map para operação síncrona
.map(user -> new UserDTO(user))
```

**Por que é ruim?**

1. **Overhead desnecessário**
   ```java
   // flatMap cria objeto Uni extra internamente
   // map executa diretamente (mais rápido)
   ```

2. **Verbosidade**
   ```java
   // 10 caracteres
   .map(u -> dto)
   
   // 45 caracteres
   .flatMap(u -> Uni.createFrom().item(dto))
   ```

3. **Semântica errada**
   - `map`: "Vou transformar este valor"
   - `flatMap`: "Vou fazer uma operação assíncrona que retorna Uni"
   - Criar DTO não é assíncrono!

4. **Code review**
   - Outro dev vai pensar "por que usou flatMap? há I/O aqui?"
   - Gera confusão no time

**Quando `flatMap` é necessário:**
```java
// ✅ Método retorna Uni (I/O)
.flatMap(user -> repository.findAddress(user.addressId))

// ❌ Método retorna valor direto (sem I/O)
.map(user -> buildDTO(user))
```

**Resumo:** Se não retorna `Uni`, não use `flatMap`!

### ❓ Posso usar `.await()` em código de produção?

**NÃO!** `.await()` bloqueia a thread, destruindo todo o benefício reativo.

**❌ NUNCA faça:**
```java
@ApplicationScoped
public class MyService {
    public User getUser(Long id) {
        return repository.findById(id).await().indefinitely();  // 🚫 BLOQUEANTE!
    }
}
```

**✅ SEMPRE retorne o Uni:**
```java
@ApplicationScoped
public class MyService {
    public Uni<User> getUser(Long id) {
        return repository.findById(id);  // ✅ NON-BLOCKING
    }
}
```

**Exceção:** Testes unitários - `.await()` é OK para simplificar asserções.

### ❓ Como debugar código reativo?

1. **Use `.log()`** - adiciona logging em cada etapa:
   ```java
   return repository.findById(id)
       .log()  // Loga item, completion, failure
       .map(this::transform)
       .log();  // Loga resultado transformado
   ```

2. **Use breakpoints condicionais** - IDEs modernas suportam reactive debugging

3. **Extraia métodos** - mais fácil testar isoladamente

4. **Teste cada parte separadamente** - valide entradas/saídas de cada operação

### ❓ `call()` vs `flatMap()` - qual usar?

**call()** - Quando você quer executar uma operação mas **MANTER o valor original**:
```java
return buildEntity()
    .call(repository::persist)  // Salva, mas mantém entity no stream
    .map(this::toDTO);          // Converte entity (não void) para DTO
```

**flatMap()** - Quando você quer **TRANSFORMAR** o valor:
```java
return findUser(id)
    .flatMap(user -> findCompany(user.companyId));  // Transforma User → Company
```

### ❓ Devo adicionar `@WithTransaction` em todos os métodos?

Apenas em métodos que fazem **escrita no banco**:

```java
// ✅ Precisa de @WithTransaction (persist)
@WithTransaction
public Uni<User> createUser(UserRequest req) {
    return repository.persist(buildUser(req));
}

// ❌ NÃO precisa (apenas leitura)
public Uni<User> findUser(Long id) {
    return repository.findById(id);
}
```

---

**Criado por:** Copilot Senior Engineer  
**Data:** Março 2026  
**Versão:** 1.1  
**Licença:** Use livremente no seu projeto

---

## Apêndice A: Glossário

- **Assíncrono**: Operação que pode demorar (I/O). Retorna `Uni<T>` no Mutiny
- **Backpressure**: Mecanismo de controle de fluxo
- **Bloqueante (Blocking)**: Operação que faz a thread ESPERAR por I/O. ❌ Evite em código reativo!
- **Cold Stream**: Stream que só emite quando tem subscriber
- **CompletableFuture**: API Java para programação assíncrona (legado). ⚠️ No Quarkus, prefira Uni
- **Eager Evaluation**: Computação executada imediatamente (ex: CompletableFuture)
- **Hot Stream**: Stream que emite independente de subscribers
- **I/O (Input/Output)**: Operações de rede, disco, banco de dados (lentas)
- **Lazy Evaluation**: Computação só acontece quando necessário (ex: Uni)
- **Non-blocking I/O**: I/O que não bloqueia a thread (thread pode fazer outras coisas)
- **Publisher**: Emissor de eventos/dados
- **Reactive Streams**: Especificação para processamento assíncrono
- **Side-effect**: Operação que não afeta o valor (ex: log, cache)
- **Subscriber**: Consumidor de eventos/dados
- **Síncrono**: Operação rápida (memória/CPU). ✅ Não bloqueia! Usa `map()` no Mutiny
- **Uni**: Stream que emite 0 ou 1 item (preferido no Quarkus)

## Apêndice B: Atalhos do IntelliJ

- `Ctrl+Alt+M` - Extract Method
- `Ctrl+Alt+V` - Extract Variable
- `Ctrl+Alt+Shift+T` - Refactor This (menu)
- `Alt+Enter` - Quick Fix / Intention Actions
- `Ctrl+Shift+Space` - Smart Completion (sugere `.flatMap` vs `.map`)

## Apêndice C: Template para Novos Métodos

```java
/**
 * [Descrição do que o método faz]
 * 
 * @param [parâmetro] [descrição]
 * @return Uni contendo [tipo] com [descrição]
 * @throws NotFoundException se [condição]
 * @throws IllegalStateException se [condição]
 */
@WithTransaction  // Se faz escrita no banco
public Uni<ResponseDTO> myMethod(RequestDTO request, String userEmail) {
    // 1. Buscar dados necessários
    return getUserByEmail(userEmail)
        
        // 2. Buscar dados dependentes ou combinar independentes
        .flatMap(user -> Uni.combine().all()
            .unis(
                getEntity1(request.getId()),
                getEntity2(user.getCompanyId())
            )
            .asTuple()
        )
        
        // 3. Validações de regras de negócio
        .call(tuple -> validateBusinessRules(tuple.getItem1(), tuple.getItem2()))
        
        // 4. Criar/Modificar entidade
        .map(tuple -> buildEntity(tuple.getItem1(), tuple.getItem2()))
        
        // 5. Persistir (se necessário)
        .call(repository::persist)
        
        // 6. Converter para DTO
        .map(this::toResponseDTO);
}

// Métodos privados auxiliares abaixo...
```

---

**Fim do Guia** 🎓
