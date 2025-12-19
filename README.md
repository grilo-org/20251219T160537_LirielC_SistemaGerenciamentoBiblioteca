# Sistema de Gerenciamento de Biblioteca

## Guia Rápido de Avaliação

Credenciais iniciais (inseridas por DataInitializer):

| Papel | Login | Senha |
|-------|-------|-------|
| Admin | admin | admin |
| Gerente | gerente | gerente |
| Funcionário | func | func |
| Cliente | cliente | cliente |

Inicie com:
```bash
mvn spring-boot:run
```
Acesse http://localhost:8080 e faça login com as credenciais acima.

Principais rotas:
* /livros – catálogo (Gerente pode cadastrar/editar)
* /emprestimos – balcão (Funcionário)
* /carrinho – compras/aluguel (Cliente)
* /dashboard – métricas (Gerente/Admin)
* /minha-conta – perfil do usuário (agora com endereço/telefone)

### Alertas de estoque baixo
Job agendado (hora em hora) envia e-mail mock para gerentes quando algum livro ficar com estoque < 5.

---

# Sistema de Biblioteca - Versão 4.2 Web

Este é um sistema completo de gerenciamento de biblioteca que permite vendas de livros, empréstimos e controle de clientes. O sistema foi migrado para **Java 17 + Spring Boot 3.2 + Vaadin 24.2 LTS**, mantendo total compatibilidade com a versão console existente.

## 🚀 Versões Disponíveis

### **Versão Web (Nova)** - Java 17
- **Frontend**: Vaadin 24.2 LTS (interface web moderna)
- **Backend**: Spring Boot 3.2.0
- **Acesso**: http://localhost:8080
- **Execução**: `mvn spring-boot:run`


## Funcionalidades Principais

### 1. Gerenciamento de Vendas
- Carrinho de compras com múltiplos itens
- Processamento de pagamentos via Stripe (cartão de crédito e boleto)
- Geração automática de documentos fiscais
- Dashboard com métricas de vendas e gráficos

### 2. Sistema de Empréstimos
- Empréstimo de livros por 7 dias
- Taxa de empréstimo: 10% do valor do livro
- Multa por atraso: 10% do valor do empréstimo por dia
- Limite de 3 empréstimos ativos por usuário
- Verificação de situação regular do usuário

### 3. Gestão de Clientes
- Cadastro completo de clientes (nome, CPF, email, endereço)
- Validação de CPF
- Histórico de compras e empréstimos
- Atualização de dados cadastrais

### 4. Controle de Estoque
- Cadastro de livros com título e valor
- Controle de disponibilidade para empréstimo
- Gestão de fornecedores

## Tecnologias Utilizadas

- Java
- JPA/Hibernate
- MySQL
- Stripe API
- Vaadin 

## Estrutura do Projeto

```
src/main/java/com/biblioteca/
├── Main.java                 # Ponto de entrada e interface principal
├── DashboardUI.java         # Interface gráfica do dashboard
├── model/                   # Entidades do sistema
│   ├── Livro.java
│   ├── Usuario.java
│   ├── Cliente.java
│   ├── Venda.java
│   ├── Emprestimo.java
│   └── ...
├── service/                 # Lógica de negócio
│   ├── PagamentoService.java
│   ├── EmprestimoService.java
│   ├── ClienteService.java
│   └── DocumentoFiscalService.java
└── util/                   # Classes utilitárias
    ├── JPAUtil.java
    └── CpfValidator.java
```

## Funcionalidades Detalhadas

### Sistema de Vendas

1. **Processo de Venda:**
   - Criação do carrinho
   - Adição de livros
   - Coleta de dados do cliente
   - Processamento do pagamento via Stripe
   - Geração de documentos fiscais
   - Registro no histórico

2. **Dashboard de Vendas:**
   - Total de vendas
   - Ticket médio
   - Gráfico de vendas por data
   - Distribuição por método de pagamento
   - Status das vendas (PAGO/PENDENTE)

### Sistema de Empréstimos

1. **Regras de Empréstimo:**
   - Verificação de elegibilidade do usuário
   - Cálculo de taxas e multas
   - Controle de devolução
   - Geração de comprovantes

2. **Controles:**
   - Limite de empréstimos ativos
   - Prazo de devolução
   - Cálculo automático de multas
   - Bloqueio de novos empréstimos para usuários irregulares

## Configuração do Ambiente

1. **Requisitos:**
   - Java 8 ou superior (aqui estou usando o 11)
   - MySQL 5.7 ou superior
   - Maven

2. **Configuração do Banco de Dados:**
   ```sql
   CREATE DATABASE biblioteca;
   USE biblioteca;
   ```

3. **Configuração do Stripe:**
   - Criar conta no Stripe
   - Configurar chave API no `PagamentoService.java`

4. **Execução:**
   ```bash
   mvn clean install
   java -jar target/biblioteca.jar
   ```

5. **Inicialização do Admin:**
   Ao iniciar a aplicação, o sistema criará/atualizará automaticamente um usuário administrador com os seguintes dados:
   - Nome (login): `admin`
   - Senha: `admin`
   - Email: `admin@biblioteca.com`
   - CPF: `00000000000`
   - Status: `ATIVO`
   - Endereço: *(vazio)*
   Para recriar manualmente, execute no banco:
   ```sql
   DELETE FROM usuarios WHERE nome = 'admin';
   ```

## Uso do Sistema

1. **Menu Principal:**
   - Gerenciar Carrinho
   - Realizar Compra
   - Listar Livros
   - Dashboard de Vendas
   - Gerenciar Empréstimos

2. **Realizando uma Venda:**
   - Selecionar opção "Gerenciar Carrinho"
   - Adicionar livros ao carrinho
   - Informar dados do cliente
   - Escolher método de pagamento
   - Confirmar pagamento
   - Receber documentos fiscais

3. **Realizando um Empréstimo:**
   - Selecionar opção "Gerenciar Empréstimos"
   - Escolher "Realizar Empréstimo"
   - Selecionar usuário e livro
   - Processar pagamento da taxa
   - Confirmar empréstimo

### Verificando Multas de Empréstimo

1. **Listando Empréstimos Ativos e Multas:**
   ```
   1. Selecione "5" no menu principal (Gerenciar Empréstimos)
   2. Selecione "3" (Listar Empréstimos Ativos)
   3. O sistema mostrará:
      - ID do empréstimo
      - Nome do usuário
      - Livro emprestado
      - Data do empréstimo
      - Data prevista de devolução
      - Valor do empréstimo
      - Multa atual (se houver atraso)
   ```

2. **Verificando Multa na Devolução:**
   ```
   1. Selecione "5" no menu principal (Gerenciar Empréstimos)
   2. Selecione "2" (Devolver Livro)
   3. Digite o ID do empréstimo
   4. O sistema mostrará:
      - Confirmação da devolução
      - Valor da multa (se houver atraso)
   ```

3. **Cálculo da Multa:**
   - A multa é calculada como 10% do valor do empréstimo por dia de atraso
   - Exemplo: 
     * Valor do empréstimo: R$ 10,00
     * Dias de atraso: 2
     * Multa: R$ 2,00 (R$ 1,00 por dia)

## Segurança e Validações

- Validação de CPF
- Controle de transações no banco de dados
- Tratamento de exceções
- Logs de operações
- Proteção contra duplicidade de registros

## Manutenção

O sistema inclui:
- Logs detalhados de operações
- Tratamento de erros
- Backup automático de dados
- Interface para monitoramento de operações

## Contribuição

Para contribuir com o projeto:
1. Faça um fork do repositório
2. Crie uma branch para sua feature
3. Faça commit das alterações
4. Push para a branch
5. Abra um Pull Request
6. Sinta-se livre para fazer alterações necessárias.


## 🛒 Sistema de Pedidos e Carrinho

### Como Funciona para Diferentes Tipos de Usuários

#### **👤 CLIENTES**
- Acesso direto ao próprio carrinho
- Fazem pedidos apenas para si mesmos
- CPF é automaticamente preenchido (se cadastrado)

#### **👨‍💼 FUNCIONÁRIOS, GERENTES e ADMINISTRADORES**
O sistema oferece dois modos de operação:

##### **🔸 Modo Pedido Próprio**
- Funcionário faz pedido para si mesmo
- Funciona igual ao cliente normal
- Útil quando o funcionário quer comprar/alugar livros

##### **👤 Modo Pedido para Cliente**
- Funcionário pode buscar qualquer cliente por CPF
- Faz o pedido em nome do cliente selecionado
- CPF é validado (deve ter exatamente 11 dígitos)
- Sistema mostra informações do cliente encontrado
- Útil para atendimento presencial na biblioteca

### **Fluxo de Trabalho**

1. **Funcionário acessa a página Carrinho**
2. **Seleciona o modo de operação:**
   - "Pedido Próprio": Compra para si mesmo
   - "Pedido para Cliente": Busca cliente por CPF
3. **Se modo cliente:**
   - Digite o CPF do cliente (com ou sem formatação)
   - Clica em "Buscar"
   - Sistema mostra dados do cliente encontrado
4. **Adiciona produtos ao carrinho do cliente selecionado**
5. **Finaliza compra:**
   - Mostra quem é o operador (funcionário logado)
   - Mostra para quem é o pedido (cliente)
   - Permite ajustar o CPF se necessário
   - Processa pagamento normalmente

### **Segurança e Auditoria**

- ✅ Todas as operações são registradas na auditoria
- ✅ Sistema identifica quem fez o pedido (operador)
- ✅ Sistema identifica para quem foi feito (cliente)
- ✅ CPF é validado antes de prosseguir
- ✅ Apenas funcionários autorizados podem fazer pedidos para outros

### **Interface Intuitiva**

- 🎨 **Ícones visuais** para facilitar identificação
- 🔍 **Busca rápida** por CPF
- ✅ **Validação em tempo real** dos dados
- 📋 **Informações claras** sobre o pedido
- 🎯 **Feedback visual** para todas as ações 
