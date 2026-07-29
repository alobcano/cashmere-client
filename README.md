# Cashmere Transfer Service

A Spring Boot application for transferring and managing Omnipub content between different systems and Cashmere collections.

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Gradle 9.3+
- AWS credentials configured (for S3 access)
- Access to Cashmere API

### Running the Application

**Default (Sandbox environment)**:
```bash
./gradlew bootRun
```

**Production environment**:
```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

**IntelliJ IDEA**: See [INTELLIJ_SETUP.md](INTELLIJ_SETUP.md) for detailed instructions

## 📚 Documentation

- **[ENVIRONMENT_CONFIG.md](ENVIRONMENT_CONFIG.md)** - Complete guide for running with different profiles
- **[INTELLIJ_SETUP.md](INTELLIJ_SETUP.md)** - IntelliJ IDEA specific setup instructions
- **[REFACTORING_SUMMARY.md](REFACTORING_SUMMARY.md)** - Recent refactoring changes and architecture
- **[DOCKER_README.md](DOCKER_README.md)** - Docker deployment instructions
- **[SERVICE_TESTS_README.md](SERVICE_TESTS_README.md)** - Testing documentation
- **[XML_CLEANUP_DOCUMENTATION.md](XML_CLEANUP_DOCUMENTATION.md)** - XML processing documentation

## 🔧 Configuration

### Environment Profiles

The application supports two environments with different configurations:

| Environment | Profile | Collection IDs | API Token |
|-------------|---------|----------------|-----------|
| **Sandbox** (default) | `sandbox` | 451-456 | Sandbox token |
| **Production** | `prod` | 405, 408-412 | Production token |

### Collection ID Mappings

| Collection Name | Sandbox ID | Production ID |
|-----------------|------------|---------------|
| CL-Articles-Base | 451 | 405 |
| CL-Articles-Inclusive | 452 | 408 |
| CL-Videos-Base | 453 | 409 |
| CL-Videos-Inclusive | 454 | 410 |
| CL-Podcasts-Base | 455 | 411 |
| CL-Podcasts-Inclusive | 456 | 412 |

## 🏗️ Building

### Build JAR
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test
```

### Build Docker Image
```bash
docker build -t cashmere-transfer-service .
```

## 🐳 Docker Deployment

### Using docker-compose (Sandbox)
```bash
docker-compose up
```

### Using docker-compose (Production)
Edit `compose.yaml` and add:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
```

Then run:
```bash
docker-compose up
```

### Using docker run
```bash
# Sandbox
docker run -p 8080:8080 cashmere-transfer-service

# Production
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod cashmere-transfer-service
```

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/hbr/cashmere/transfer_service/
│   │   ├── configuration/       # Spring configuration classes
│   │   ├── constants/           # Application constants
│   │   ├── controller/          # REST controllers
│   │   ├── model/              # Data models
│   │   ├── service/            # Business logic
│   │   └── util/               # Utility classes
│   └── resources/
│       ├── application.properties              # Default configuration
│       ├── application-sandbox.properties      # Sandbox environment
│       └── application-prod.properties         # Production environment
└── test/                       # Test classes
```

## 🔐 Security Notes

1. **API Tokens**: Never commit production tokens to version control
2. **Environment Variables**: Use environment variables for sensitive data in production
3. **AWS Credentials**: Ensure AWS credentials are properly configured and secured

## 🧪 Testing

The project includes unit tests for core functionality. To run tests:

```bash
./gradlew test
```

Test reports are generated in `build/reports/tests/test/index.html`

## 📝 Key Features

- **CSV Processing**: Import content from CSV files
- **Video Content Management**: Handle video-specific XML processing
- **S3 Integration**: Download and process XML files from S3
- **GitHub Integration**: Fetch content from GitHub repositories
- **Collection Management**: Manage Omnipubs across different collections
- **Multi-Environment Support**: Separate configurations for sandbox and production

## 🔄 Recent Changes

See [REFACTORING_SUMMARY.md](REFACTORING_SUMMARY.md) for detailed information about the recent environment-based configuration refactoring.

## 🛠️ Troubleshooting

### Wrong Collection IDs Being Used?
Check which profile is active in the console output:
```
The following 1 profile is active: "prod"
```

### Build Failures?
```bash
./gradlew clean build
```

### IntelliJ Not Picking Up Changes?
- File → Invalidate Caches / Restart
- Reimport the Gradle project

## 📧 Support

For issues or questions, please refer to the documentation files or contact the development team.

## 📄 License

[Your License Here]

