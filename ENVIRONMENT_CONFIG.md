# Environment Configuration Guide

This application supports multiple environments (sandbox and production) with different collection IDs and API tokens.

## Available Profiles

### Sandbox (Default)
- Collection IDs: 451, 452, 453, 454, 455, 456
- Uses sandbox Cashmere API token

### Production
- Collection IDs: 405, 408, 409, 410, 411, 412
- Uses production Cashmere API token (must be configured)

## How to Run

### Running with Sandbox (Default)
```bash
./gradlew bootRun
```
or
```bash
java -jar build/libs/transfer_service-0.0.1-SNAPSHOT.jar
```

### Running with Production
```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```
or
```bash
java -jar build/libs/transfer_service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Setting Profile via Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=prod
./gradlew bootRun
```

### Running in IntelliJ IDEA

There are **three ways** to set the profile in IntelliJ:

#### Option 1: Program Arguments (Recommended)
1. Open **Run** → **Edit Configurations...**
2. Select your Spring Boot application configuration
3. In the **Program arguments** field, add:
   ```
   --spring.profiles.active=prod
   ```
4. Click **Apply** and **OK**

#### Option 2: VM Options
1. Open **Run** → **Edit Configurations...**
2. Select your Spring Boot application configuration
3. In the **VM options** field, add:
   ```
   -Dspring.profiles.active=prod
   ```
4. Click **Apply** and **OK**

#### Option 3: Environment Variables
1. Open **Run** → **Edit Configurations...**
2. Select your Spring Boot application configuration
3. In the **Environment variables** field, add:
   ```
   SPRING_PROFILES_ACTIVE=prod
   ```
4. Click **Apply** and **OK**

#### Option 4: Active Profiles (Spring Boot Configuration)
1. Open **Run** → **Edit Configurations...**
2. Select your Spring Boot application configuration
3. Look for **Active profiles** field (if available in your IntelliJ version)
4. Enter: `prod`
5. Click **Apply** and **OK**

**Note**: Option 1 (Program Arguments) is recommended as it's most explicit and portable.

## Configuration Files

- `application.properties` - Default configuration (sandbox)
- `application-sandbox.properties` - Sandbox-specific configuration
- `application-prod.properties` - Production-specific configuration

## Important Notes

1. **Production Token**: Before deploying to production, update the `cashmere.api.token` in `application-prod.properties` with the actual production token.

2. **Collection ID Mapping**:
   - CL-Articles-Base: Sandbox=451, Prod=405
   - CL-Articles-Inclusive: Sandbox=452, Prod=408
   - CL-Videos-Base: Sandbox=453, Prod=409
   - CL-Videos-Inclusive: Sandbox=454, Prod=410
   - CL-Podcasts-Base: Sandbox=455, Prod=411
   - CL-Podcasts-Inclusive: Sandbox=456, Prod=412

3. **Testing**: Always test with sandbox profile before deploying to production.

## Docker

When running in Docker, set the profile via environment variable:
```bash
docker run -e SPRING_PROFILES_ACTIVE=prod <image-name>
```

Or update the `compose.yaml` file:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
```

