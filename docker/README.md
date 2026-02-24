# 🐳 Docker Compose Setup for mod-entities-links

Local development environment for mod-entities-links using Docker Compose.

## 📋 Prerequisites

- Docker and Docker Compose V2+
- Java 21+ (for local development mode)
- Maven 3.8+ (for building the module)

## 🏗️ Architecture

Two compose files provide flexible development workflows:

- **`infra-docker-compose.yml`**: Infrastructure services only (PostgreSQL, Kafka, WireMock, etc.)
- **`app-docker-compose.yml`**: Full stack including the module (uses `include` to incorporate infra services)

## ⚙️ Configuration

Configuration is managed via the `.env` file in this directory.

### Key Environment Variables

| Variable                 | Description                   | Default                |
|--------------------------|-------------------------------|------------------------|
| `ENV`                    | FOLIO environment name        | `dev`                  |
| `MODULE_REPLICAS`        | Number of module instances    | `1`                    |
| `MODULE_PORT`            | Module host port              | `8081`                 |
| `DEBUG_PORT`             | Remote debugging port         | `5005`                 |
| `DB_HOST`                | PostgreSQL hostname           | `postgres`             |
| `DB_PORT`                | PostgreSQL port               | `5432`                 |
| `DB_DATABASE`            | Database name                 | `okapi_modules`        |
| `DB_USERNAME`            | Database user                 | `folio_admin`          |
| `DB_PASSWORD`            | Database password             | `folio_admin`          |
| `KAFKA_HOST`             | Kafka hostname                | `kafka`                |
| `KAFKA_PORT`             | Kafka port (Docker internal)  | `9093`                 |
| `KAFKA_UI_PORT`          | Kafka UI port                 | `8080`                 |
| `KAFKA_TOPIC_PARTITIONS` | Default partitions for topics | `10`                   |
| `WIREMOCK_PORT`          | WireMock (Okapi mock) port    | `9130`                 |
| `OKAPI_URL`              | Okapi URL for the module      | `http://wiremock:8080` |
| `PGADMIN_PORT`           | PgAdmin port                  | `5050`                 |
| `SYSTEM_USER_ENABLED`    | Enable system user            | `false`                |

## 🚀 Services

### PostgreSQL
- **Purpose**: Primary database for module data
- **Version**: PostgreSQL 16 Alpine
- **Access**: localhost:5432 (configurable via `DB_PORT`)
- **Credentials**: See `DB_USERNAME` and `DB_PASSWORD` in `.env`
- **Database**: See `DB_DATABASE` in `.env`

### pgAdmin
- **Purpose**: Database administration interface
- **Access**: http://localhost:5050 (configurable via `PGADMIN_PORT`)
- **Login**: Use `PGADMIN_DEFAULT_EMAIL` and `PGADMIN_DEFAULT_PASSWORD` from `.env`

### Apache Kafka
- **Purpose**: Message broker for event-driven architecture
- **Mode**: KRaft mode (no Zookeeper required)
- **Listeners**:
  - Docker internal: `kafka:9093`
  - Host: `localhost:29092`

### Kafka UI
- **Purpose**: Web interface for Kafka management
- **Access**: http://localhost:8080 (configurable via `KAFKA_UI_PORT`)
- **Features**: Topic browsing, message viewing/producing, consumer group monitoring

### Kafka Topic Initialization
- **Purpose**: Automatically creates required Kafka topics on startup
- **Script**: `kafka-init.sh` - Creates topics for inventory, authorities, and linking
- **Configuration**: Edit `KAFKA_TOPIC_PARTITIONS` in `.env` to adjust partition count
- **Topics Created**:
  - `{ENV}.Default.inventory.instance`
  - `{ENV}.Default.inventory.holdings-record`
  - `{ENV}.Default.inventory.item`
  - `{ENV}.Default.inventory.bound-with`
  - `{ENV}.Default.authorities.authority`
  - `{ENV}.Default.authority.authority-source-file`
  - `{ENV}.Default.links.instance-authority`
  - `{ENV}.Default.links.instance-authority-stats`
  - `{ENV}.Default.DI_INVENTORY_AUTHORITY_UPDATED`
  - `{ENV}.Default.DI_INVENTORY_AUTHORITY_CREATED_READY_FOR_POST_PROCESSING`
  - `{ENV}.Default.DI_SRS_MARC_AUTHORITY_RECORD_CREATED`
  - `{ENV}.Default.DI_SRS_MARC_AUTHORITY_RECORD_MODIFIED_READY_FOR_POST_PROCESSING`
  - `{ENV}.Default.DI_SRS_MARC_AUTHORITY_RECORD_NOT_MATCHED`
  - `{ENV}.Default.DI_SRS_MARC_AUTHORITY_RECORD_DELETED`
  - `{ENV}.Default.DI_COMPLETED`
  - `{ENV}.Default.DI_ERROR`

### WireMock
- **Purpose**: Mock Okapi and other FOLIO modules for testing
- **Access**: http://localhost:9130 (configurable via `WIREMOCK_PORT`)
- **Mappings**: Located in `docker/mappings`

## 📖 Usage

> **Note**: All commands in this guide assume you are in the `docker/` directory. If you're at the project root, run `cd docker` first.

### Starting the Environment

```bash
# Build the module first
mvn -f ../pom.xml clean package -DskipTests

# Start all services (infrastructure + module)
docker compose -f app-docker-compose.yml up -d
```

```bash
# Start only infrastructure services (for local development)
docker compose -f infra-docker-compose.yml up -d
```

```bash
# Start with build (if module code changed)
docker compose -f app-docker-compose.yml up -d --build
```

```bash
# Start specific service
docker compose -f infra-docker-compose.yml up -d postgres
```

### Stopping the Environment

```bash
# Stop all services
docker compose -f app-docker-compose.yml down
```

```bash
# Stop infra services only
docker compose -f infra-docker-compose.yml down
```

```bash
# Stop and remove volumes (clean slate)
docker compose -f app-docker-compose.yml down -v
```

### Viewing Logs

```bash
# All services
docker compose -f app-docker-compose.yml logs
```

```bash
# Specific service
docker compose -f app-docker-compose.yml logs mod-entities-links
```

```bash
# Follow logs in real-time
docker compose -f app-docker-compose.yml logs -f mod-entities-links
```

```bash
# Last 100 lines
docker compose -f app-docker-compose.yml logs --tail=100 mod-entities-links
```

### Scaling the Module

The module is configured with resource limits and deployment policies for production-like scaling:

- **CPU Limits**: 1.0 CPU (max), 0.5 CPU (reserved)
- **Memory Limits**: 1024M (max), 512M (reserved)
- **Restart Policy**: Automatic restart on failure
- **Update Strategy**: Rolling updates with 1 instance at a time, 10s delay

```bash
# Scale to 3 instances
docker compose -f app-docker-compose.yml up -d --scale mod-entities-links=3
```

```bash
# Or modify MODULE_REPLICAS in .env and restart
echo "MODULE_REPLICAS=3" >> .env
docker compose -f app-docker-compose.yml up -d
```

### Cleanup and Reset

```bash
# Complete cleanup (stops containers, removes volumes)
docker compose -f app-docker-compose.yml down -v
```

```bash
# Remove all Docker resources
docker compose -f app-docker-compose.yml down -v
docker volume prune -f
docker network prune -f
```

## 🔧 Development Workflows

### Workflow 1: Full Docker Stack
Run everything in Docker, including the module.

```bash
# Build the module
mvn -f ../pom.xml clean package -DskipTests

# Start all services
docker compose -f app-docker-compose.yml up -d

# View logs
docker compose -f app-docker-compose.yml logs -f mod-entities-links
```

**Use Case**: Testing the full deployment, simulating production environment, scaling tests.

### Workflow 2: Infrastructure Only + IDE
Run infrastructure in Docker, develop the module in your IDE.

```bash
# Start infrastructure
docker compose -f infra-docker-compose.yml up -d

# Run module from IDE or command line
mvn -f ../pom.xml spring-boot:run
```

**Use Case**: Active development with hot reload, debugging in IDE, faster iteration cycles.

### Workflow 3: Spring Boot Docker Compose Integration
Let Spring Boot manage Docker Compose automatically (recommended for rapid development).

```bash
# Run with dev profile (starts infrastructure automatically)
mvn -f ../pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile is configured to:
- Start services from `docker/infra-docker-compose.yml`
- Connect to services via localhost ports (Kafka: 29092, PostgreSQL: 5432)
- Keep containers running after the application stops for faster subsequent startups

**Use Case**: Quickest way to start development, automatic infrastructure management, no manual Docker commands needed.

### Workflow 4: Spring Boot DevTools
For rapid development with automatic restart on code changes.

```bash
# Start infrastructure
docker compose -f infra-docker-compose.yml up -d

# Run with devtools (automatic restart on code changes)
mvn -f ../pom.xml spring-boot:run

# Make code changes - application will automatically restart
```

**Use Case**: Continuous development with automatic reload, live code updates, rapid feedback loop.

## 🛠️ Common Tasks

### Building the Module

```bash
# Clean build (skip tests)
mvn -f ../pom.xml clean package -DskipTests
```

```bash
# Build with tests
mvn -f ../pom.xml clean package
```

### Accessing Services

```bash
# PostgreSQL CLI
docker compose -f infra-docker-compose.yml exec postgres psql -U folio_admin -d okapi_modules
```

```bash
# View database tables
docker compose -f infra-docker-compose.yml exec postgres psql -U folio_admin -d okapi_modules -c "\dt"
```

```bash
# Check PostgreSQL health
docker compose -f infra-docker-compose.yml exec postgres pg_isready -U folio_admin
```

```bash
# List Kafka topics
docker compose -f infra-docker-compose.yml exec kafka kafka-topics.sh --bootstrap-server localhost:9093 --list
```

```bash
# Create a Kafka topic
docker compose -f infra-docker-compose.yml exec kafka kafka-topics.sh --bootstrap-server localhost:9093 --create --topic test-topic --partitions 3 --replication-factor 1
```

```bash
# Consume messages from a topic
docker compose -f infra-docker-compose.yml exec kafka kafka-console-consumer.sh --bootstrap-server localhost:9093 --topic dev.Default.authorities.authority --from-beginning
```

### Adding New Kafka Topics

Edit `kafka-init.sh` and add topics to the `TOPICS` array:

```bash
TOPICS=(
  "${ENV}.Default.inventory.instance"
  "${ENV}.Default.your-new-topic"  # Add your new topic here
)
```

After editing, restart the kafka-topic-init service:

```bash
docker compose -f infra-docker-compose.yml up -d kafka-topic-init
```

### Rebuilding the Module

```bash
# Rebuild and restart the module
mvn -f ../pom.xml clean package -DskipTests
docker compose -f app-docker-compose.yml up -d --build mod-entities-links
```

```bash
# Force rebuild without cache
docker compose -f app-docker-compose.yml build --no-cache mod-entities-links
docker compose -f app-docker-compose.yml up -d mod-entities-links
```

## 🐛 Troubleshooting

### Port Conflicts

If you encounter port conflicts, modify the ports in `.env`:

```bash
# Example: Change module port to 8082
echo "MODULE_PORT=8082" >> .env
docker compose -f app-docker-compose.yml up -d
```

### Container Health Issues

```bash
# Check container status
docker compose -f app-docker-compose.yml ps

# Check specific container logs
docker compose -f app-docker-compose.yml logs mod-entities-links

# Restart a specific service
docker compose -f app-docker-compose.yml restart mod-entities-links
```

### Database Connection Issues

```bash
# Verify PostgreSQL is running
docker compose -f infra-docker-compose.yml ps postgres

# Check PostgreSQL logs
docker compose -f infra-docker-compose.yml logs postgres

# Test database connection
docker compose -f infra-docker-compose.yml exec postgres psql -U folio_admin -d okapi_modules -c "SELECT 1"
```

### Kafka Issues

```bash
# Check Kafka logs
docker compose -f infra-docker-compose.yml logs kafka

# Verify Kafka is ready
docker compose -f infra-docker-compose.yml exec kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9093
```

### Clean Start

If you need to completely reset the environment:

```bash
# Stop and remove everything
docker compose -f app-docker-compose.yml down -v

# Remove any orphaned containers
docker container prune -f

# Remove unused networks
docker network prune -f

# Start fresh
mvn -f ../pom.xml clean package -DskipTests
docker compose -f app-docker-compose.yml up -d --build
```

## 📚 Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Compose Support](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.docker-compose)
- [mod-entities-links Documentation](../README.md)

## 💡 Tips

- Use **Workflow 3** (Spring Boot Docker Compose) for the fastest development experience
- Keep infrastructure running between development sessions to save startup time
- Use **Workflow 1** (Full Docker Stack) when testing deployment or scaling scenarios
- Use `docker compose -f infra-docker-compose.yml logs -f` to monitor all infrastructure services
- PgAdmin and Kafka UI provide helpful web interfaces for inspecting database and Kafka state

