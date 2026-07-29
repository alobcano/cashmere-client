# Refactoring Summary: Environment-Based Collection IDs and API Tokens

## Overview
Successfully refactored the application to support different collection IDs and Cashmere API tokens for sandbox and production environments using Spring profiles.

## Changes Made

### 1. New Configuration Class
**File**: `CollectionIdProperties.java`
- Created a Spring `@Configuration` class with `@ConfigurationProperties`
- Binds collection IDs from application properties
- Provides methods to get collection name-to-ID and ID-to-name mappings

### 2. Updated Constants Class
**File**: `CollectionConstants.java`
- Converted from a utility class to a Spring `@Component`
- Injected `CollectionIdProperties` for dynamic collection ID retrieval
- Changed from static final constants to getter methods:
  - `getCLArticlesBaseId()`
  - `getCLArticlesDeiId()`
  - `getCLVideosBaseId()`
  - `getCLVideosDeiId()`
  - `getCLPodcastsBaseId()`
  - `getCLPodcastsDeiId()`
  - `getCollectionNameToId()`
  - `getCollectionIdToName()`

### 3. Updated Service Layer
**File**: `CsvService.java`
- Updated to use `CollectionConstants.getCollectionNameToId()` instead of `COLLECTION_NAME_TO_ID`
- Updated to use `CollectionConstants.getCollectionIdToName()` instead of `COLLECTION_ID_TO_NAME`

**File**: `CsvUtil.java`
- Updated `determineCollectionId()` method to use `CollectionConstants.getCollectionNameToId()`

### 4. Property Files

**File**: `application.properties` (Base configuration)
- Added `spring.profiles.active=sandbox` (default profile)
- Added collection ID properties for sandbox environment

**File**: `application-sandbox.properties` (New)
- Sandbox-specific configuration
- Collection IDs: 451, 452, 453, 454, 455, 456
- Sandbox Cashmere API token

**File**: `application-prod.properties` (New)
- Production-specific configuration
- Collection IDs: 405, 408, 409, 410, 411, 412
- Production Cashmere API token (placeholder - needs to be updated)

### 5. Test Updates
**File**: `CsvServiceTest.java`
- Added ObjectMapper mock to fix test compilation error

### 6. Documentation
**File**: `ENVIRONMENT_CONFIG.md` (New)
- Comprehensive guide on how to run the application with different profiles
- Examples for Gradle, Java, Docker, docker-compose, and IntelliJ IDEA
- Important notes about configuration

**File**: `INTELLIJ_SETUP.md` (New)
- Detailed IntelliJ IDEA-specific guide
- Step-by-step instructions with multiple methods
- Tips for creating multiple run configurations
- Troubleshooting section

## Collection ID Mappings

| Collection Name           | Sandbox ID | Production ID |
|---------------------------|------------|---------------|
| CL-Articles-Base          | 451        | 405           |
| CL-Articles-Inclusive     | 452        | 408           |
| CL-Videos-Base            | 453        | 409           |
| CL-Videos-Inclusive       | 454        | 410           |
| CL-Podcasts-Base          | 455        | 411           |
| CL-Podcasts-Inclusive     | 456        | 412           |

## How to Use

### Running in Sandbox (Default)
```bash
./gradlew bootRun
```

### Running in Production
```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### Setting Profile via Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=prod
./gradlew bootRun
```

## Next Steps

1. **Update Production Token**: Replace the placeholder in `application-prod.properties` with the actual production Cashmere API token
2. **Test in Sandbox**: Verify the application works correctly with sandbox profile
3. **Test in Production**: Verify the application works correctly with production profile
4. **Environment Variables**: Consider using environment variables for sensitive data like API tokens in production deployments

## Build Status
✅ All tests passing
✅ Build successful
✅ No compilation errors

