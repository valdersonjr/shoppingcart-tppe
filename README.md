# Ponto de Controle 2

Este documento descreve a arquitetura utilizada no projeto **ShoppingCart**, implementado com Spring Boot. O projeto segue a abordagem de **Arquitetura em Camadas (Layered Architecture)**, que organiza o sistema com base em responsabilidades bem definidas.

Essa estrutura facilita a manutenção, testes, reutilização de código e separação de interesses, sendo uma escolha comum para aplicações corporativas baseadas em Java e Spring.

---

## Estrutura de Pastas

A estrutura principal do projeto está localizada em `src/main/java/com/valderson/shoppingcart`, com as seguintes pastas:

### config
Contém classes de configuração da aplicação, como beans personalizados, configuração de CORS, Swagger e segurança.

### controller
Responsável por receber as requisições HTTP e encaminhá-las para os serviços apropriados. Implementa a API pública da aplicação.

### dto
Define os objetos de transferência de dados (DTOs), utilizados para trafegar informações entre camadas, evitando o uso direto das entidades.

### entity
Agrupa as entidades JPA que representam as tabelas e relacionamentos no banco de dados.

### enums
Contém os tipos enumerados utilizados no domínio da aplicação (por exemplo, papéis de usuários, status de pedidos etc.).

### repository
Define interfaces de acesso a dados estendendo `JpaRepository`, `CrudRepository` ou outras abstrações do Spring Data.

### security
Reúne as classes relacionadas à segurança da aplicação, incluindo autenticação via JWT, filtros de autorização e configurações do Spring Security.

### service
Implementa a lógica de negócio da aplicação. Os controllers dependem dos serviços para realizar as operações principais.

### util
Contém classes auxiliares e funções utilitárias reutilizáveis em diferentes camadas da aplicação.

---

## Estrutura de Testes

Os testes estão organizados em `src/test/java/com/valderson/shoppingcart`, seguindo a mesma hierarquia do código principal. A estrutura foi dividida em dois tipos principais:

### Testes Unitários
Estão localizados nas pastas `controller/unit` e `service/unit`. Esses testes têm como objetivo validar o comportamento isolado de classes e métodos, utilizando mocks para dependências externas.

### Testes de Integração
Estão localizados nas pastas `controller/integration` e `service/integration`. Esses testes validam o comportamento real da aplicação com contexto Spring carregado, incluindo acesso ao banco de dados e interações entre camadas reais.

### Parametrização
Vários testes foram implementados de forma **parametrizada**, utilizando anotações como `@ParameterizedTest` e `@CsvSource`, com o objetivo de validar múltiplos cenários de entrada de forma automatizada e limpa, aumentando a cobertura e reduzindo a duplicação de código.

---

## Exemplos de Clean Code Aplicados

### Polimorfismo

O projeto implementa polimorfismo através do padrão Strategy com a interface `HandlerMethodArgumentResolver` do Spring Framework. Esta abordagem permite diferentes implementações de resolução de argumentos, mantendo o código flexível e extensível.

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

**Benefícios desta implementação:**
- **Responsabilidade única**: A classe tem apenas uma responsabilidade - resolver o usuário atual
- **Extensibilidade**: Novas implementações de `HandlerMethodArgumentResolver` podem ser facilmente adicionadas
- **Desacoplamento**: O código cliente não precisa conhecer a implementação específica
- **Testabilidade**: Cada implementação pode ser testada isoladamente

### Extração de Método

A refatoração através da extração de métodos foi aplicada para eliminar duplicação de código e melhorar a legibilidade. Um exemplo claro pode ser visto na classe `ProductService`:

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

**Benefícios da extração do método:**
- **Reutilização**: O método `mapToResponse` pode ser utilizado em diferentes pontos do serviço
- **Legibilidade**: O código principal fica mais limpo e focado na lógica de negócio
- **Manutenibilidade**: Mudanças na estrutura de mapeamento precisam ser feitas em apenas um local
- **Testabilidade**: O método extraído pode ser testado isoladamente
- **Princípio DRY**: Elimina a duplicação de código de mapeamento

### Separação de Responsabilidades

A arquitetura em camadas demonstra claramente a aplicação do princípio da responsabilidade única:

**Controllers**: Responsáveis apenas por receber requisições HTTP e delegar para os serviços apropriados, sem conter lógica de negócio.

**Services**: Concentram toda a lógica de negócio da aplicação, mantendo-se independentes da camada de apresentação.

**Repositories**: Focam exclusivamente no acesso e manipulação de dados, abstraindo a complexidade de persistência.

**DTOs**: Servem como contratos de dados entre camadas, evitando o vazamento de detalhes de implementação das entidades.

### Nomenclatura Expressiva

O projeto adota convenções de nomenclatura que tornam o código auto-documentado:

- Classes de serviço terminam com "Service" (ex: `ProductService`)
- Classes de controle terminam com "Controller" (ex: `ProductController`)
- DTOs de resposta terminam com "Response" (ex: `ProductResponse`)
- Métodos possuem nomes que expressam claramente sua intenção (ex: `mapToResponse`)

Esta abordagem reduz a necessidade de comentários excessivos e torna o código mais intuitivo para novos desenvolvedores.

---

## Exemplos de Testes Aplicados

### Teste Parametrizado

Os testes parametrizados permitem validar múltiplos cenários com diferentes entradas usando uma única implementação de teste. Um exemplo pode ser visto no teste unitário da classe `AuthService`:

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
- **Cobertura ampla**: Testa múltiplas combinações de dados com uma única implementação
- **Redução de duplicação**: Evita repetir a mesma lógica de teste para diferentes entradas
- **Manutenibilidade**: Facilita a adição de novos casos de teste apenas incluindo novas linhas no `@CsvSource`
- **Legibilidade**: Os dados de teste ficam claramente visíveis na anotação

### Teste Unitário

Os testes unitários validam componentes isoladamente, utilizando mocks para suas dependências. Exemplo da classe `AuthServiceTest`:

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
- **Isolamento**: Testa apenas a unidade específica sem dependências externas
- **Velocidade**: Execução rápida devido ao uso de mocks
- **Controle**: Permite simular diferentes cenários através dos mocks
- **Verificação de comportamento**: Valida se as dependências são chamadas corretamente

### Teste de Integração

Os testes de integração validam o comportamento completo da aplicação com contexto Spring e banco de dados real. Exemplo da classe `AuthServiceIntegrationTest`:

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
- **Contexto completo**: Carrega todo o contexto Spring da aplicação
- **Banco real**: Utiliza banco de dados H2 em memória para testes
- **Fluxo completo**: Testa cenários end-to-end incluindo persistência
- **Validação realística**: Verifica se diferentes camadas funcionam corretamente em conjunto