# Inventory & Order Management System

A production-ready RESTful API built with Java 21 and Spring Boot 4, designed to manage inventory, suppliers, customers, and orders with real-world business logic.

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 4.1
- **Database:** PostgreSQL 16 (via Docker)
- **ORM:** Spring Data JPA + Hibernate 7
- **Validation:** Jakarta Validation
- **Boilerplate Reduction:** Lombok
- **Build Tool:** Maven

## Features

- Full CRUD for Products, Categories, Suppliers and Customers
- Order placement with automatic stock deduction (transactional)
- Order cancellation with automatic stock restoration
- Low stock detection with configurable reorder levels
- Global exception handling with clean JSON error responses
- Input validation on all endpoints
- Layered architecture (Controller → Service → Repository)

## Project Structure

src/main/java/com/first_project/inventory_management/
├── controller/ # HTTP request handlers
├── service/ # Business logic
├── repository/ # Database access layer
├── entity/ # JPA entities (DB tables)
├── dto/ # Request and Response data shapes
└── exception/ # Global exception handler

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.6+
- Docker Desktop

### Run Locally

1. Start the database:

```bash
docker compose up -d
```

2. Run the application:

```bash
.\mvnw.cmd spring-boot:run
```

3. API is available at `http://localhost:8080`

## API Endpoints

### Categories

| Method | Endpoint             | Description        |
| ------ | -------------------- | ------------------ |
| POST   | /api/categories      | Create category    |
| GET    | /api/categories      | Get all categories |
| GET    | /api/categories/{id} | Get category by ID |
| PUT    | /api/categories/{id} | Update category    |
| DELETE | /api/categories/{id} | Delete category    |

### Suppliers

| Method | Endpoint            | Description        |
| ------ | ------------------- | ------------------ |
| POST   | /api/suppliers      | Create supplier    |
| GET    | /api/suppliers      | Get all suppliers  |
| GET    | /api/suppliers/{id} | Get supplier by ID |
| PUT    | /api/suppliers/{id} | Update supplier    |
| DELETE | /api/suppliers/{id} | Delete supplier    |

### Products

| Method | Endpoint                | Description            |
| ------ | ----------------------- | ---------------------- |
| POST   | /api/products           | Create product         |
| GET    | /api/products           | Get all products       |
| GET    | /api/products/{id}      | Get product by ID      |
| PUT    | /api/products/{id}      | Update product         |
| DELETE | /api/products/{id}      | Delete product         |
| GET    | /api/products/low-stock | Get low stock products |

### Customers

| Method | Endpoint            | Description        |
| ------ | ------------------- | ------------------ |
| POST   | /api/customers      | Create customer    |
| GET    | /api/customers      | Get all customers  |
| GET    | /api/customers/{id} | Get customer by ID |
| PUT    | /api/customers/{id} | Update customer    |
| DELETE | /api/customers/{id} | Delete customer    |

### Orders (coming soon)

| Method | Endpoint                | Description     |
| ------ | ----------------------- | --------------- |
| POST   | /api/orders             | Place an order  |
| GET    | /api/orders             | Get all orders  |
| GET    | /api/orders/{id}        | Get order by ID |
| PATCH  | /api/orders/{id}/cancel | Cancel an order |
