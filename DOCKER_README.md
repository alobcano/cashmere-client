# Docker Deployment Guide

## Quick Start

To build and run the application in Docker with your AWS credentials:

```bash
./run-app.sh
```

The application will be available at http://localhost:8080

## AWS Credentials Configuration

The Docker container is configured to use your default AWS credentials from `~/.aws/`.

### Using Default Profile

By default, the container uses the `default` AWS profile:

```bash
./run-app.sh
```

### Using a Specific AWS Profile

To use a different AWS profile:

```bash
AWS_PROFILE=myprofile ./run-app.sh
```

### Setting AWS Region

To specify an AWS region (defaults to us-east-1):

```bash
AWS_REGION=us-west-2 ./run-app.sh
```

### Combining Profile and Region

```bash
AWS_PROFILE=production AWS_REGION=eu-west-1 ./run-app.sh
```

## Manual Docker Commands

If you prefer to run Docker commands manually:

### Build the Image

```bash
docker build -t transfer-service .
```

### Run the Container with AWS Credentials

```bash
docker run -d \
  --name transfer-service-container \
  -p 8080:8080 \
  -v "$(pwd)/logs:/app/logs" \
  -v "$HOME/.aws:/root/.aws:ro" \
  -e AWS_PROFILE=default \
  -e AWS_REGION=us-east-1 \
  transfer-service
```

### View Logs

```bash
docker logs -f transfer-service-container
```

### Stop the Container

```bash
docker stop transfer-service-container
```

### Remove the Container

```bash
docker rm transfer-service-container
```

## Troubleshooting

### AWS Credentials Not Working

1. Verify your AWS credentials are configured:
   ```bash
   cat ~/.aws/credentials
   ```

2. Check if the AWS CLI works:
   ```bash
   aws s3 ls
   ```

3. Verify the container has access to credentials:
   ```bash
   docker exec transfer-service-container ls -la /root/.aws
   ```

### Port Already in Use

If port 8080 is already in use, modify the PORT variable in `run-app.sh` or use:

```bash
docker run -d \
  --name transfer-service-container \
  -p 9090:8080 \
  -v "$(pwd)/logs:/app/logs" \
  -v "$HOME/.aws:/root/.aws:ro" \
  -e AWS_PROFILE=default \
  transfer-service
```

This maps the container's port 8080 to your host's port 9090.

## Environment Variables

The container supports the following environment variables:

- `AWS_PROFILE`: AWS profile to use (default: `default`)
- `AWS_REGION`: AWS region to use (default: `us-east-1`)
- `JAVA_OPTS`: JVM options (default: `-Xmx512m -Xms256m`)

### Custom Java Options

```bash
docker run -d \
  --name transfer-service-container \
  -p 8080:8080 \
  -v "$(pwd)/logs:/app/logs" \
  -v "$HOME/.aws:/root/.aws:ro" \
  -e JAVA_OPTS="-Xmx1024m -Xms512m" \
  transfer-service
```

## Security Notes

- The AWS credentials are mounted as read-only (`:ro`) to prevent the container from modifying them
- Logs are persisted to the host filesystem in the `logs/` directory
- The container runs as root by default; for production, consider using a non-root user

