# TaciaDocs API Server (Java/Spring Boot)

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

RESTful API server for TaciaDocs, built with Spring Boot. This is the Java backend implementation that serves content for the TaciaDocs documentation portal.

## ✨ Features

- **Content Management**
  - Serve Markdown content
  - Hierarchical document structure
  - Basic content search

- **API**
  - RESTful JSON API
  - Basic health checks
  - Request/response logging

## 🚀 Prerequisites

- Java 17 or higher
- Maven 3.8+
- (Optional) Docker for containerized deployment

## 🚀 Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/tacia-docs.git
   cd tacia-docs/backend-java
   ```

2. **Build the application**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - API Base URL: http://localhost:8080/api
   - Health Check: http://localhost:8080/actuator/health

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

### Server Configuration

```yaml
server:
  port: 8080
  servlet:
    context-path: /
```

### Logging Configuration

```yaml
logging:
  level:
    root: INFO
    com.example.backend: DEBUG
```

## 📚 API Endpoints

### Content Management
- `GET /api/content` - List content at the specified path
- `GET /api/content/item` - Get content item details
- `GET /api/content/raw` - Get raw content

### Document Structure
- `GET /api/structure` - Get document structure
- `GET /api/first-document` - Find the first document in a directory

### Related Content
- `GET /api/related` - Find related documents

### System
- `GET /actuator/health` - Application health check

## 🐳 Docker Support

### Build and Run

```bash
# Build the application
mvn clean package

# Build Docker image
docker build -t taciadocs/backend-java .

# Run the container
docker run -p 8080:8080 taciadocs/backend-java
```

## 🧪 Testing

Run the tests:
```bash
mvn test
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
│   │   │   └── repository/        # Data access layer
│   │   └── resources/
│   │       └── application.yml    # Configuration
│   └── test/                      # Test code
└── pom.xml                        # Maven build file
```

### Building

```bash
# Build and package
mvn clean package
```

## 🚀 Production Deployment

### JAR Deployment

```bash
# Build the application
mvn clean package

# Run the application
java -jar target/backend-java-*.jar
```

### Configuration for Production

For production, you may want to:
1. Set up proper logging
2. Configure monitoring
3. Set appropriate security headers
4. Configure CORS if needed

## 🤝 Contributing

Contributions are welcome! To contribute:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  Made with ❤️ by the TaciaNet Team
</div>
