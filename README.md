# TaciaDocs API Server (Java/Spring Boot)

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

High-performance RESTful API server for TaciaDocs, built with Spring Boot. This is the Java backend implementation that powers the TaciaDocs documentation portal, providing robust content management and delivery.

## ✨ Features

- **Content Management**
  - Markdown content processing and delivery
  - Hierarchical document structure navigation
  - Full-text search capabilities
  - File upload and management

- **Performance**
  - Multi-level caching (in-memory, Redis)
  - Asynchronous processing
  - Efficient content delivery

- **API**
  - RESTful JSON API
  - Comprehensive API documentation with Swagger/OpenAPI
  - Internationalization support
  - Rate limiting and security

- **Operational**
  - Health checks and metrics
  - Container and cloud-native ready
  - Comprehensive logging and monitoring

## 🚀 Prerequisites

- Java 17 or higher
- Maven 3.8+ or Gradle 7.6+
- PostgreSQL 13+ (or embedded H2 for development)
- Redis (optional, for distributed caching)
- Docker (optional, for containerized deployment)

## 🚀 Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/tacia-docs.git
   cd tacia-docs/backend-java
   ```

2. **Build the application**
   ```bash
   # Using Maven
   mvn clean install
   
   # Or using Gradle
   ./gradlew build
   ```

3. **Run the application**
   ```bash
   # Using Maven
   mvn spring-boot:run
   
   # Or using the JAR
   java -jar target/taciadocs-backend-*.jar
   ```

4. **Access the application**
   - API Documentation: http://localhost:8080/swagger-ui.html
   - Health Check: http://localhost:8080/actuator/health
   - Metrics: http://localhost:8080/actuator/metrics
   - API Base URL: http://localhost:8080/api

## 🔄 Working with Frontend

This backend is designed to work with the TaciaDocs frontend. To run both together:

1. Start the Java backend:
   ```bash
   cd backend-java
   mvn spring-boot:run
   ```

2. In a separate terminal, start the frontend:
   ```bash
   cd ../frontend
   npm start
   ```

3. Access the application at `http://localhost:4200`

## ⚙️ Configuration

Configuration can be modified in `src/main/resources/application.yml` or via environment variables.

### Key Configuration Properties

#### Server Configuration
```yaml
server:
  port: 8080
  servlet:
    context-path: /api
```

#### Content Management
```yaml
app:
  content:
    root-directory: ./content  # Root directory for markdown content
    cache-ttl: 3600           # Cache TTL in seconds
    max-cache-size: 1000      # Maximum cache entries
    allowed-file-types: ["md", "markdown", "mdx"]
```

#### Database
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taciadocs
    username: user
    password: password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

#### Cache (Redis)
```yaml
spring:
  cache:
    type: redis
  redis:
    host: localhost
    port: 6379
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profiles | `dev` |
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:h2:mem:taciadocs` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `CONTENT_DIR` | Content directory | `./content` |

## 📚 API Documentation

For detailed API documentation, visit the Swagger UI at `http://localhost:8080/swagger-ui.html` when running locally.

### Core Endpoints

#### Content Management
- `GET /api/content` - List content at path
- `GET /api/content/{id}` - Get content by ID
- `GET /api/content/path` - Get content by path
- `POST /api/content` - Create/update content
- `DELETE /api/content/{id}` - Delete content

#### Document Structure
- `GET /api/structure` - Get document hierarchy
- `GET /api/search` - Full-text search

#### Related Content
- `GET /api/related` - Find related documents
- `GET /api/related/tags` - Find by common tags

#### System
- `GET /api/health` - Health status
- `GET /api/info` - Application info
- `GET /api/metrics` - Performance metrics

## 🐳 Docker Deployment

### Build the Image
```bash
docker build -t taciadocs/backend-java .
```

### Run with Docker Compose
```yaml
version: '3.8'
services:
  backend:
    image: taciadocs/backend-java
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/taciadocs
      - REDIS_HOST=redis
    depends_on:
      - db
      - redis

  db:
    image: postgres:13
    environment:
      - POSTGRES_DB=taciadocs
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=password
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:alpine
    ports:
      - "6379:6379"

volumes:
  postgres_data:
```

## 🧪 Testing

Run the test suite:
```bash
# Unit tests
mvn test

# Integration tests
mvn verify -Pintegration-tests

# With coverage report
mvn jacoco:report
```

## 🛠 Development

### Project Structure

```
backend-java/
├── src/
│   ├── main/
│   │   ├── java/com/example/backend/
│   │   │   ├── api/               # API controllers
│   │   │   ├── config/            # Spring configuration
│   │   │   ├── model/             # Domain models
│   │   │   ├── repository/        # Data access layer
│   │   │   ├── service/           # Business logic
│   │   │   └── util/              # Utility classes
│   │   └── resources/
│   │       ├── static/            # Static resources
│   │       ├── templates/         # Server-side templates
│   │       └── application.yml    # Configuration
│   └── test/                      # Test code
└── pom.xml                        # Maven build file
```

### Building

```bash
# Build and package
mvn clean package

# Skip tests
mvn clean package -DskipTests
```

### Code Style

This project uses Google Java Format. Format your code with:

```bash
mvn spotless:apply
```

### Database Migrations

We use Flyway for database migrations. To create a new migration:

1. Create a new SQL file in `src/main/resources/db/migration/`
2. Name it following the pattern: `V{version}__{description}.sql`
3. The migration will run automatically on startup

## 🚀 Production Deployment

### JAR Deployment

```bash
# Build with production profile
mvn clean package -Pprod

# Run with production profile
java -jar target/taciadocs-backend-*.jar --spring.profiles.active=prod
```

### Kubernetes

Example deployment for Kubernetes:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: taciadocs-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: taciadocs-backend
  template:
    metadata:
      labels:
        app: taciadocs-backend
    spec:
      containers:
      - name: taciadocs-backend
        image: taciadocs/backend-java:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
```

## 🤝 Contributing

Contributions are welcome! Please read our [Contribution Guidelines](.github/CONTRIBUTING.md) to get started.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  Made with ❤️ by the TaciaNet Team
</div>
