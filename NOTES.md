# Project Notes — Inventory & Order Management System

This file is a personal learning journal. Every concept is documented with:

- **What** it is
- **Why** it exists / why we use it
- **How** it works in practice

---

## 1. Project Overview

### What are we building?

A backend REST API for managing inventory — products, categories, suppliers, customers, and orders.
Real-world use case: think of the backend system powering a warehouse or e-commerce store's inventory.

### Why this project?

- Covers the most common backend patterns interviewers ask about
- Introduces databases, relationships, transactions, validation, and error handling
- Gives a strong "I built something real" story in interviews
- Bridges the gap between SAP ABAP (enterprise domain knowledge) and SDE (engineering skills)

### Tech Stack Decisions

| Technology    | What             | Why                                                                   |
| ------------- | ---------------- | --------------------------------------------------------------------- |
| Java 21       | Language         | Most in-demand for enterprise backend roles in India                  |
| Spring Boot 4 | Framework        | Industry standard; reduces boilerplate; embeds Tomcat                 |
| PostgreSQL    | Database         | Production-grade relational DB; better than MySQL for complex queries |
| Docker        | Containerization | Run PostgreSQL without installing it; mirrors real dev environments   |
| Maven         | Build tool       | Dependency management and build lifecycle                             |
| Lombok        | Code generator   | Eliminates repetitive Java boilerplate                                |

---

## 2. Core Spring Concepts

### What is Spring Boot?

Spring Boot is a framework built on top of Spring Framework that removes most of the manual configuration.
Before Spring Boot, you had to configure everything manually — XML files, servlet config, external Tomcat server.
Spring Boot auto-configures everything based on what's on the classpath.

### Inversion of Control (IoC)

**What:** A design principle where the control of object creation is inverted — instead of your code creating objects, a framework (Spring) creates and manages them.

**Why:** Without IoC, every class has to manually create its dependencies:

```java
// Without IoC — tightly coupled, hard to test
public class CategoryService {
    private CategoryRepository repo = new CategoryRepository(); // manual creation
}
```

With IoC, Spring creates and injects everything:

```java
// With IoC — loosely coupled, easy to test
public class CategoryService {
    private final CategoryRepository repo; // Spring provides this
}
```

**How:** Spring maintains an "Application Context" — a registry of all managed objects (called beans). At startup it scans all classes, finds annotated ones, creates instances, and wires them together.

### Dependency Injection (DI)

**What:** The mechanism Spring uses to implement IoC — it injects required dependencies into a class automatically.

**Why:** Makes code loosely coupled and testable. You can swap implementations without changing the class that uses them.

**How — Three types:**

```java
// 1. Constructor Injection (RECOMMENDED)
@RequiredArgsConstructor  // Lombok generates the constructor
public class CategoryService {
    private final CategoryRepository repo; // injected via constructor
}

// 2. Field Injection (NOT recommended — harder to test)
@Autowired
private CategoryRepository repo;

// 3. Setter Injection (rarely used)
@Autowired
public void setRepo(CategoryRepository repo) { this.repo = repo; }
```

Constructor injection is preferred because dependencies are explicit and the class can't be created without them — making bugs visible at startup, not runtime.

### Component Scan

**What:** At startup, Spring scans all classes under your base package and registers annotated classes as beans.

**Why:** This is why you never manually instantiate services, repositories, or controllers — Spring finds and manages them.

**How:** These annotations tell Spring to register the class:

- `@Component` — generic bean
- `@Service` — business logic layer (same as @Component, but semantic)
- `@Repository` — data access layer (adds DB exception translation)
- `@RestController` — HTTP handler
- `@RestControllerAdvice` — global exception handler

---

## 3. Layered Architecture

### What is it?

A pattern where your codebase is split into distinct layers, each with one responsibility.

### Why use it?

- Each layer can be changed independently
- Easier to test each layer in isolation
- Mirrors how real enterprise systems are structured
- Interviewers specifically ask about this — it shows you understand separation of concerns

### How it works in this project:

HTTP Request
↓
Controller → "What did the client ask for?"
Handles HTTP only. Calls Service.
↓
Service → "How do we fulfill this?"
All business rules and logic live here.
↓
Repository → "Get or save the data."
Talks to DB only. No logic.
↓
Database → Stores the data
↑
Entity → Java class mapped to a DB table
↑
DTO → Controls what data enters/exits the API

### Why DTOs instead of exposing Entities directly?

**Problem with direct entity exposure:**

- Your entity might have sensitive fields (internal flags, passwords)
- The shape of data a client sends is often different from what you return
- Any DB schema change would break your API contract

**Solution — separate DTOs:**

- `RequestDTO` — defines what the client sends
- `ResponseDTO` — defines what you return
- Entity changes don't affect the API contract
- You control exactly what gets serialized to JSON

---

## 4. JPA & Hibernate

### What is JPA?

Java Persistence API — a **specification** (a set of rules/interfaces) that defines how Java objects should map to database tables.

### What is Hibernate?

The most popular **implementation** of JPA. It's the actual engine that generates and runs SQL.

### What is Spring Data JPA?

An abstraction layer on top of Hibernate that makes it even simpler. Instead of writing Hibernate code, you just define interfaces.

**Interview angle:** Know the difference — JPA is the spec, Hibernate is the implementation, Spring Data JPA is the convenience layer on top.

### How entities map to tables:

```java
@Entity                        // tells JPA: this class = a DB table
@Table(name = "categories")    // explicit table name (else defaults to class name)
public class Category {

    @Id                                              // primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // auto-increment (1,2,3...)
    private Long id;

    @Column(nullable = false, unique = true)    // NOT NULL + UNIQUE constraint in DB
    private String name;
}
```

### Relationships

**@ManyToOne** — many products belong to one category

```java
@ManyToOne(fetch = FetchType.LAZY)   // don't load Category unless accessed
@JoinColumn(name = "category_id")    // foreign key column name in products table
private Category category;
```

**LAZY vs EAGER fetching:**

- `LAZY` — related entity loaded only when you access it (recommended)
- `EAGER` — related entity loaded immediately with the parent

Why LAZY is almost always correct:
Imagine loading 1000 products. With EAGER, every product immediately loads its full Category and Supplier objects — that's 3000 DB queries. With LAZY, you only load what you actually need.

**@JoinColumn name convention:**
Always use `snake_case` matching the actual DB column name.
`category_id` ✅ — `category.id` ❌ — `Category.id` ❌

### ddl-auto settings

```yaml
ddl-auto: update    # creates/alters tables to match entities — used in development
ddl-auto: create    # drops and recreates ALL tables on every start — dangerous
ddl-auto: validate  # only validates schema, never changes it — used in production
ddl-auto: none      # does nothing — used in production with Flyway/Liquibase
```

### Why IDs don't reset after deletion

PostgreSQL uses sequences — counters that only ever go forward.
If you insert id=1, delete it, and insert again, you get id=2.
This is intentional:

- Prevents ambiguity in logs and audit trails
- Prevents foreign key conflicts in distributed systems
- IDs are "surrogate keys" — they have no business meaning, just uniqueness

---

## 5. Spring Data JPA — Repository Layer

### What is the Repository pattern?

A pattern where all database access is centralized in one layer (repository), hiding the SQL details from the rest of the app.

### How Spring Data JPA implements it:

You define an interface extending `JpaRepository<Entity, IDType>`.
Spring generates the full implementation at runtime — you never write a single SQL query.

**Free methods from JpaRepository:**

```java
save(entity)          // INSERT or UPDATE
findById(id)          // SELECT WHERE id = ? → returns Optional
findAll()             // SELECT *
existsById(id)        // SELECT COUNT(*) WHERE id = ?
deleteById(id)        // DELETE WHERE id = ?
count()               // SELECT COUNT(*)
```

### Derived Query Methods

**What:** Spring reads the method name and auto-generates SQL from it.

**Why:** You express your query intent in plain English and Spring handles the SQL.

**How — naming rules:**

```java
findBy + FieldName                     → WHERE field = ?
existsBy + FieldName                   → SELECT COUNT(*) WHERE field = ?
findBy + FieldName + LessThanEqual     → WHERE field <= ?
findBy + FieldName + Containing        → WHERE field LIKE '%value%'
findBy + Field1 + And + Field2         → WHERE field1 = ? AND field2 = ?
```

**Critical:** All keywords are case-sensitive. `findBy` not `findby`. `existsBy` not `existBy`.

---

## 6. Validation

### What is it?

Jakarta Validation lets you declare rules on your DTO fields using annotations.
Spring automatically checks these rules when a request comes in.

### Why use it?

Without validation, invalid data reaches your database.
With validation, bad requests are rejected before any business logic runs.

### How it works:

```java
public class ProductRequestDTO {
    @NotBlank(message = "Name is required")     // not null, not empty, not just spaces
    private String name;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Cannot be negative")
    private Double price;

    @Email(message = "Invalid email")           // must match email format
    private String email;
}
```

`@Valid` on the controller method parameter triggers validation:

```java
public ResponseEntity<?> createProduct(@Valid @RequestBody ProductRequestDTO request)
```

If validation fails, `MethodArgumentNotValidException` is thrown → caught by `GlobalExceptionHandler` → returns clean 400 error.

---

## 7. Global Exception Handling

### What is it?

A single centralized class that handles all exceptions thrown anywhere in the application.

### Why?

Without it:

- Every controller needs try-catch blocks
- Error responses are inconsistent
- Stack traces leak to the client (security risk)

With it:

- One place to handle all errors
- Consistent JSON error format across the entire API
- Internal details never exposed

### How — @RestControllerAdvice:

```java
@RestControllerAdvice   // Spring registers this as a global interceptor
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)   // catches this specific exception
    public ResponseEntity<ErrorResponseDTO> handleNotFound(EntityNotFoundException ex) {
        // return clean JSON error
    }
}
```

**Why you never import or call it manually:**
`@RestControllerAdvice` is detected by component scan at startup.
Spring registers it as an interceptor in the request pipeline.
When any exception is thrown, Spring checks this class before sending a response.
This is AOP (Aspect Oriented Programming) — cross-cutting concerns like error handling are separated from business logic.

### HTTP Status Codes

200 OK → successful GET, PUT
201 CREATED → successful POST
204 NO CONTENT → successful DELETE (no body returned)
400 BAD REQUEST → validation failure, duplicate entry, bad input
404 NOT FOUND → requested entity doesn't exist
500 INTERNAL ERROR → unexpected/unhandled error

---

## 8. Lombok

### What is it?

A Java annotation processor that generates boilerplate code at compile time.
The generated code exists in the compiled `.class` files — it's not visible in your source code.

### Why use it?

Without Lombok, a class with 5 fields needs 30+ lines of getters, setters, constructors, toString, equals, hashCode.
With Lombok, one annotation replaces all of it.

### Key annotations:

```java
@Data               // getters + setters + toString + equals + hashCode
@Builder            // enables Product.builder().name("x").price(100).build()
@NoArgsConstructor  // empty constructor — required by JPA
@AllArgsConstructor // constructor with all fields — required by @Builder
@RequiredArgsConstructor // constructor for all 'final' fields — used for DI
```

### How @Builder works:

```java
// Instead of:
Product p = new Product();
p.setName("iPhone");
p.setPrice(79999.0);

// You write:
Product p = Product.builder()
                   .name("iPhone")
                   .price(79999.0)
                   .build();
```

Builder pattern is safer — you can't accidentally forget a required field.

### Important setup:

Lombok must be configured as an annotation processor in `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

Without this, `javac` doesn't invoke Lombok during compilation and `builder()` methods won't be generated.

---

## 9. Docker

### What is Docker?

A tool that packages software (like PostgreSQL) into isolated containers.
A container is like a lightweight virtual machine — it has everything the software needs to run.

### Why use Docker for this project?

- No need to install PostgreSQL on your machine
- Everyone on the team gets the same DB version and config
- Easy to start/stop/reset the database
- Mirrors how databases are managed in real production environments

### How docker-compose.yml works:

```yaml
services:
  postgres:
    image: postgres:16 # use PostgreSQL version 16
    container_name: inventory_postgres
    environment:
      POSTGRES_DB: inventory_db # creates this database
      POSTGRES_USER: postgres # username
      POSTGRES_PASSWORD: postgres # password
    ports:
      - "5432:5432" # host:container port mapping
    volumes:
      - postgres_data:/var/lib/postgresql/data # persist data between restarts
```

### Key commands:

```bash
docker compose up -d      # start containers in background (-d = detached)
docker compose down       # stop and remove containers (data preserved in volume)
docker compose stop       # pause without removing
docker ps                 # list running containers
```

### Why data persists after `docker compose down`:

The `volumes` section creates a named volume (`postgres_data`).
Docker stores this volume separately from the container.
Even when the container is removed, the volume (and your data) remains.

---

## 10. Common Errors & Root Causes

| Error                            | Root Cause                                                   | Fix                                                       |
| -------------------------------- | ------------------------------------------------------------ | --------------------------------------------------------- |
| `Asia/Calcutta` timezone error   | PostgreSQL 16 dropped old timezone name                      | Add `-Duser.timezone=Asia/Kolkata` to JVM args in pom.xml |
| `Not a managed type`             | Invalid `@JoinColumn` name caused Hibernate to reject entity | Use `category_id` not `category.id` in @JoinColumn        |
| `No property 'existBySku'`       | Typo in derived query method name                            | `existsBySku` not `existBySku`                            |
| `No property 'findbyX'`          | Wrong capitalisation in derived query                        | `findBy` not `findby` — case sensitive                    |
| `builder() not found`            | Wrong content saved in DTO file                              | Verify file content with `cat filename.java`              |
| `Failed to configure DataSource` | application.yml not being read                               | Check file is in `src/main/resources/`, no tabs in YAML   |

---

## 11. Git Workflow

### Why version control from day one?

- Safety net — you can always roll back to a working state
- Shows progression on GitHub (portfolio value)
- Required in every professional team

### Commands used:

```bash
git init                           # initialise local repo
git add .                          # stage all changes
git commit -m "descriptive message" # snapshot with message
git remote add origin <url>        # link to GitHub
git push -u origin main            # first push
git push                           # subsequent pushes
```

### Good commit message format:

Add Customer entity with CRUD APIs
Fix timezone error in application.yml
Add global exception handler

One line, imperative tense, describes what the commit does.

## 12. API Design Conventions (REST)

### URL naming:

/api/categories # collection resource — plural noun
/api/categories/{id} # single resource — ID in path
/api/products/low-stock # sub-resource or filtered collection

### HTTP method conventions:

POST /api/products # create new resource
GET /api/products # read all
GET /api/products/{id} # read one
PUT /api/products/{id} # full update (replace entire resource)
PATCH /api/products/{id} # partial update (change specific fields)
DELETE /api/products/{id} # delete

### Response conventions:

- Always return `ResponseEntity` for full control over status code
- POST → 201 CREATED with the created resource in body
- DELETE → 204 NO CONTENT with empty body
- Errors → consistent JSON with status, message, timestamp

---

## 13. Transactions & @Transactional

### What is a Transaction?

A transaction is a group of database operations that must ALL succeed or ALL fail together.
There is no partial success — either everything is committed or everything is rolled back.

### Why do we need it?

Without transactions, a failure midway through a multi-step operation corrupts your data:
Deduct stock for Product A ✅
Deduct stock for Product B ✅
Save Order to DB ❌ (exception thrown here)
Result: Stock deducted but no order created — data is now corrupted

With `@Transactional`:
Deduct stock for Product A ✅
Deduct stock for Product B ✅
Save Order to DB ❌ (exception thrown here)
Result: Steps 1 and 2 are automatically ROLLED BACK — data stays clean

### How @Transactional works:

Method starts → Spring opens a transaction
All DB operations → run inside that transaction (not committed yet)
Method returns → Spring COMMITS (changes saved permanently)
Exception thrown → Spring ROLLS BACK (all changes undone automatically)

Spring handles all of this — you just annotate the method.

### ACID Properties (interview must-know)

Every database transaction guarantees four properties:

| Property        | What it means                                           |
| --------------- | ------------------------------------------------------- |
| **Atomicity**   | All operations succeed or none do — no partial results  |
| **Consistency** | Data always moves from one valid state to another       |
| **Isolation**   | Concurrent transactions don't interfere with each other |
| **Durability**  | Once committed, data survives crashes and restarts      |

`@Transactional` gives you all four automatically.

### Read vs Write transactions

```java
@Transactional                          // read-write (default)
public OrderResponseDTO placeOrder() {}

@Transactional(readOnly = true)         // read-only — slight performance gain
public List<OrderResponseDTO> getAllOrders() {}
```

`readOnly = true` tells the DB it can skip write-locking, which improves performance
for queries that don't modify data. Good practice to add on all GET methods.

### Why we validate ALL stock before deducting ANY

```java
// Step 1 — validate everything first
for (item : items) {
    if (product.stock < item.quantity) throw exception;
}

// Step 2 — only then deduct stock
for (item : items) {
    product.stock -= item.quantity;
}
```

If we validated and deducted in the same loop, a failure on item 3
would roll back items 1 and 2 anyway (thanks to @Transactional),
but it's cleaner and more readable to separate validation from mutation.
Also makes it easier to return a single clear error message listing ALL
insufficient products rather than stopping at the first one.

### Price Snapshotting

```java
private Double priceAtPurchase;   // stored on OrderItem, not Product
```

Why not just reference `product.getPrice()` later?
Because product prices change over time.
If iPhone was ₹79,999 when ordered but ₹74,999 now,
the order history must show what the customer actually paid.
Snapshotting the price at the time of purchase is standard practice
in any e-commerce or inventory system.

### CascadeType.ALL

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
private List<OrderItem> items;
```

Means any operation on Order automatically cascades to its OrderItems:

- Save Order → saves all OrderItems
- Delete Order → deletes all OrderItems

Without cascade, you'd have to manually save/delete each item.

### mappedBy — the most misunderstood JPA concept

```java
// Order side (inverse side — does NOT own the FK)
@OneToMany(mappedBy = "order")
private List<OrderItem> items;

// OrderItem side (owning side — HOLDS the FK column)
@ManyToOne
@JoinColumn(name = "order_id")
private Order order;
```

The side with `mappedBy` = inverse side = no FK column in its table
The side with `@JoinColumn` = owning side = has the FK column in its table
The FK (`order_id`) lives in the `order_items` table, not in `orders`.

### @Enumerated(EnumType.STRING)

```java
@Enumerated(EnumType.STRING)    // stores "PENDING", "CANCELLED" in DB
private OrderStatus status;
```

Never use the default `EnumType.ORDINAL` (stores 0, 1, 2...).
If you add a new enum value in the middle, all existing data shifts and breaks.
STRING is always safe — the stored value is human-readable and order-independent.

### PATCH vs PUT for cancel endpoint

PUT /api/orders/{id} → full update (replace entire resource)
PATCH /api/orders/{id}/cancel → partial update (change only status field)

Cancellation only changes the status field — not the entire order.
PATCH is the semantically correct HTTP method for partial updates.
