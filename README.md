# MXC Backend

Backend service for the MXC application, built with Spring Boot.

## Features

- Content management (CRUD operations for files and directories)
- Document structure navigation
- Related documents discovery
- Health checks and monitoring
- API documentation with Swagger UI
- Caching for improved performance

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/mxc-backend.git
   cd mxc-backend
   ```

2. **Build the application**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   java -jar target/mxc-backend-0.0.1-SNAPSHOT.jar
   ```
   Or using Maven:
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - API Documentation: http://localhost:8080/swagger-ui.html
   - Health Check: http://localhost:8080/actuator/health
   - Application Info: http://localhost:8080/actuator/info

## Configuration

Configuration can be modified in `src/main/resources/application.yml` or by setting environment variables.

### Important Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `app.content.root-directory` | Root directory for content storage | `./content` |
| `app.cache.ttl` | Cache time-to-live in seconds | `3600` (1 hour) |
| `app.cache.max-size` | Maximum number of cache entries | `1000` |

## API Endpoints

### Content Management

- `GET /api/content` - List content at the specified path
- `GET /api/content/item?path={path}&recursive={boolean}` - Get content item details
- `POST /api/content?path={path}` - Create or update content
- `DELETE /api/content?path={path}` - Delete content

### Document Structure

- `GET /api/structure?path={path}` - Get document structure

### Related Documents

- `GET /api/related?path={path}&limit={number}&skipCache={boolean}` - Get related documents

### First Document

- `GET /api/first-document?directory={path}` - Find the first document in a directory

### Health Checks

- `GET /api/health` - Basic health check
- `GET /api/health/liveness` - Liveness probe
- `GET /api/health/readiness` - Readiness probe

## Development

### Building

```bash
mvn clean package
```

### Running Tests

```bash
mvn test
```

### Code Style

This project uses the Google Java Format style. You can format your code using:

```bash
mvn spotless:apply
```

## Deployment

The application can be deployed as a standalone JAR file or as a Docker container.

### Docker

Build the Docker image:

```bash
docker build -t mxc-backend .
```

Run the container:

```bash
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod mxc-backend
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
