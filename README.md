# Documentação do Projeto ShoppingCart

## Visão Geral

Este documento apresenta a arquitetura e implementação do projeto **ShoppingCart**, desenvolvido com Spring Boot seguindo os princípios da **Arquitetura em Camadas (Layered Architecture)**. A estrutura adotada prioriza a separação de responsabilidades, facilitando a manutenção, testes e reutilização de código em aplicações corporativas Java.

## Arquitetura do Sistema

### Estrutura de Pastas

O projeto está organizado em `src/main/java/com/valderson/shoppingcart` com as seguintes divisões:

**config**
Classes de configuração da aplicação, incluindo beans personalizados, configuração de CORS, Swagger e segurança.

**controller**
Responsável pelo recebimento de requisições HTTP e encaminhamento para os serviços apropriados, implementando a API pública da aplicação.

**dto**
Objetos de transferência de dados (DTOs) utilizados para comunicação entre camadas, evitando exposição direta das entidades.

**entity**
Entidades JPA que representam as tabelas e relacionamentos do banco de dados.

**enums**
Tipos enumerados do domínio da aplicação, como papéis de usuários e status de pedidos.

**repository**
Interfaces de acesso a dados que estendem `JpaRepository`, `CrudRepository` ou outras abstrações do Spring Data.

**security**
Classes relacionadas à segurança da aplicação, incluindo autenticação JWT, filtros de autorização e configurações do Spring Security.

**service**
Implementação da lógica de negócio da aplicação, utilizada pelos controllers para realizar operações principais.

**util**
Classes auxiliares e funções utilitárias reutilizáveis entre diferentes camadas.

### Estrutura de Testes

Os testes estão organizados em `src/test/java/com/valderson/shoppingcart`, mantendo a hierarquia do código principal:

**Testes Unitários**
Localizados em `controller/unit` e `service/unit`, validam o comportamento isolado de classes e métodos utilizando mocks para dependências externas.

**Testes de Integração**
Localizados em `controller/integration` e `service/integration`, validam o comportamento real da aplicação com contexto Spring carregado, incluindo acesso ao banco de dados.

**Parametrização**
Implementação de testes parametrizados utilizando `@ParameterizedTest` e `@CsvSource` para validar múltiplos cenários de entrada, aumentando a cobertura e reduzindo duplicação de código.

## Princípios de Clean Code Aplicados

### Polimorfismo

Implementação do padrão Strategy através da interface `HandlerMethodArgumentResolver` do Spring Framework:

```java
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        return request.getAttribute("userId");
    }
}
```

**Benefícios:**
- Responsabilidade única na resolução do usuário atual
- Extensibilidade para novas implementações de `HandlerMethodArgumentResolver`
- Desacoplamento entre código cliente e implementação específica
- Testabilidade isolada de cada implementação

### Extração de Método

Refatoração aplicada para eliminar duplicação de código e melhorar legibilidade:

```java
public class ProductService {

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
```

**Benefícios:**
- Reutilização do método `mapToResponse` em diferentes pontos do serviço
- Legibilidade aprimorada com foco na lógica de negócio
- Manutenibilidade centralizada para mudanças de mapeamento
- Testabilidade isolada do método extraído
- Aplicação do princípio DRY (Don't Repeat Yourself)

### Separação de Responsabilidades

A arquitetura em camadas demonstra a aplicação do princípio da responsabilidade única:

**Controllers**: Recebimento de requisições HTTP e delegação para serviços, sem lógica de negócio.

**Services**: Concentração da lógica de negócio, independente da camada de apresentação.

**Repositories**: Foco exclusivo no acesso e manipulação de dados, abstraindo complexidade de persistência.

**DTOs**: Contratos de dados entre camadas, evitando vazamento de detalhes de implementação das entidades.

### Nomenclatura Expressiva

Convenções de nomenclatura que tornam o código autodocumentado:

- Classes de serviço: sufixo "Service" (ex: `ProductService`)
- Classes de controle: sufixo "Controller" (ex: `ProductController`)
- DTOs de resposta: sufixo "Response" (ex: `ProductResponse`)
- Métodos com nomes que expressam claramente sua intenção (ex: `mapToResponse`)

## Estratégias de Teste

### Teste Parametrizado

Validação de múltiplos cenários com diferentes entradas em uma única implementação:

```java
@ParameterizedTest
@CsvSource({
        "João Silva, joao@email.com, senha123",
        "Maria Santos, maria@email.com, senha456",
        "Pedro Oliveira, pedro@email.com, senha789",
        "Ana Costa, ana@email.com, senhaABC"
})
@DisplayName("Deve registrar diferentes usuários com sucesso")
void shouldRegisterUsersWithDifferentValidData(String name, String email, String password) {
    RegisterRequest request = RegisterRequest.builder()
            .name(name)
            .email(email)
            .password(password)
            .build();

    User user = User.builder()
            .id(1L)
            .name(name)
            .email(email)
            .passwordHash("hashedPassword")
            .createdAt(LocalDateTime.now())
            .build();

    when(userRepository.existsByEmail(email)).thenReturn(false);
    when(passwordEncoder.encode(password)).thenReturn("hashedPassword");
    when(userRepository.save(any(User.class))).thenReturn(user);

    UserResponse response = authService.register(request);

    assertThat(response).isNotNull();
    assertThat(response.getName()).isEqualTo(name);
    assertThat(response.getEmail()).isEqualTo(email);

    verify(userRepository).existsByEmail(email);
    verify(passwordEncoder).encode(password);
}
```

**Benefícios da parametrização:**
- Cobertura ampla com múltiplas combinações de dados
- Redução de duplicação de lógica de teste
- Facilidade para adicionar novos casos de teste
- Clareza na visualização dos dados de teste

### Teste Unitário

Validação de componentes isoladamente utilizando mocks para dependências:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Testes Unitários")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve registrar usuário com sucesso")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .name("João Silva")
                .email("joao@email.com")
                .password("senha123")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("João Silva");
        assertThat(response.getEmail()).isEqualTo("joao@email.com");

        verify(userRepository).existsByEmail("joao@email.com");
        verify(passwordEncoder).encode("senha123");
        verify(userRepository).save(any(User.class));
    }
}
```

**Características dos testes unitários:**
- Isolamento da unidade específica sem dependências externas
- Execução rápida devido ao uso de mocks
- Controle para simulação de diferentes cenários
- Verificação de comportamento das dependências

### Teste de Integração

Validação do comportamento completo com contexto Spring e banco de dados real:

```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@Transactional
@DisplayName("AuthService - Testes de Integração")
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @ParameterizedTest
    @CsvSource({
            "João Integration, joao.integration@email.com, senha123",
            "Maria Silva, maria@email.com, abc123456",
            "Ana Teste, ana@email.com, securePass",
            "Carlos Example, carlos@email.com, senhaSegura!"
    })
    @DisplayName("Deve registrar, logar e buscar usuário com sucesso usando diferentes dados")
    void shouldRegisterLoginAndSearchWithDifferentUsers(String name, String email, String password) {
        // Registro
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name(name)
                .email(email)
                .password(password)
                .build();

        UserResponse registeredUser = authService.register(registerRequest);

        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getName()).isEqualTo(name);
        assertThat(registeredUser.getEmail()).isEqualTo(email);

        // Validação no banco
        User savedUser = userRepository.findById(registeredUser.getId()).orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(passwordEncoder.matches(password, savedUser.getPasswordHash())).isTrue();

        // Login
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        UserResponse loggedUser = authService.login(loginRequest);
        assertThat(loggedUser.getId()).isEqualTo(registeredUser.getId());
    }
}
```

**Características dos testes de integração:**
- Carregamento completo do contexto Spring
- Utilização de banco de dados H2 em memória
- Teste de cenários end-to-end incluindo persistência
- Validação realística da integração entre camadas

## Modelo de Dados

O projeto utiliza PostgreSQL como sistema de gerenciamento de banco de dados, seguindo boas práticas de modelagem relacional e otimização de performance.

### Tabela users

Armazena informações dos usuários do sistema:

```sql
CREATE TABLE public.users (
    id SERIAL NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_email_key UNIQUE (email)
);

CREATE INDEX idx_users_email ON public.users USING btree (email);
```

### Tabela products

Catálogo de produtos disponíveis para compra:

```sql
CREATE TABLE public.products (
    id SERIAL NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT products_pkey PRIMARY KEY (id),
    CONSTRAINT products_price_check CHECK (price >= 0::NUMERIC)
);
```

### Tabela shopping_carts

Representa o carrinho de compras de cada usuário:

```sql
CREATE TABLE public.shopping_carts (
    id SERIAL NOT NULL,
    user_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT shopping_carts_pkey PRIMARY KEY (id),
    CONSTRAINT shopping_carts_user_id_key UNIQUE (user_id),
    CONSTRAINT shopping_carts_user_id_fkey 
        FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_shopping_carts_user_id ON public.shopping_carts USING btree (user_id);
```

### Tabela cart_items

Itens individuais dentro de cada carrinho de compras:

```sql
CREATE TABLE public.cart_items (
    id SERIAL NOT NULL,
    shopping_cart_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    quantity INTEGER DEFAULT 1 NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT cart_items_pkey PRIMARY KEY (id),
    CONSTRAINT cart_items_quantity_check CHECK (quantity > 0),
    CONSTRAINT cart_items_shopping_cart_id_product_id_key 
        UNIQUE (shopping_cart_id, product_id),
    CONSTRAINT cart_items_shopping_cart_id_fkey 
        FOREIGN KEY (shopping_cart_id) REFERENCES public.shopping_carts(id) ON DELETE CASCADE,
    CONSTRAINT cart_items_product_id_fkey 
        FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE CASCADE
);

CREATE INDEX idx_cart_items_cart_id ON public.cart_items USING btree (shopping_cart_id);
```

### Tabela orders

Registra os pedidos realizados pelos usuários:

```sql
CREATE TABLE public.orders (
    id SERIAL NOT NULL,
    user_id INTEGER NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'pending'::CHARACTER VARYING,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT orders_pkey PRIMARY KEY (id),
    CONSTRAINT orders_total_amount_check CHECK (total_amount >= 0::NUMERIC),
    CONSTRAINT orders_status_check CHECK (
        status::TEXT = ANY (ARRAY['pending'::CHARACTER VARYING, 
                                 'confirmed'::CHARACTER VARYING, 
                                 'cancelled'::CHARACTER VARYING]::TEXT[])
    ),
    CONSTRAINT orders_user_id_fkey 
        FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_orders_user_id ON public.orders USING btree (user_id);
```

### Tabela order_items

Itens específicos de cada pedido com dados históricos:

```sql
CREATE TABLE public.order_items (
    id SERIAL NOT NULL,
    order_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_price NUMERIC(10, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    
    CONSTRAINT order_items_pkey PRIMARY KEY (id),
    CONSTRAINT order_items_quantity_check CHECK (quantity > 0),
    CONSTRAINT order_items_order_id_fkey 
        FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE,
    CONSTRAINT order_items_product_id_fkey 
        FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_items_order_id ON public.order_items USING btree (order_id);
```

### Diagrama do Banco de Dados

<img width="774" alt="image" src="https://github.com/user-attachments/assets/e2d5fa73-8236-4f80-a203-e834a9889a9b" />