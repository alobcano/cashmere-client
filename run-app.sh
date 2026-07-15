#!/bin/bash

# Script to build and run the Spring Boot application in Docker
#
# AWS Configuration:
# This script automatically mounts your AWS credentials from ~/.aws into the container.
#
# To use a specific AWS profile, set the AWS_PROFILE environment variable:
#   AWS_PROFILE=myprofile ./run-app.sh
#
# To use a specific AWS region, set the AWS_REGION environment variable:
#   AWS_REGION=us-west-2 ./run-app.sh
#
# You can also set both:
#   AWS_PROFILE=myprofile AWS_REGION=us-west-2 ./run-app.sh

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="transfer-service"
CONTAINER_NAME="transfer-service-container"
PORT=8080

echo -e "${GREEN}Starting Docker build and run process...${NC}"
echo -e "${YELLOW}AWS Configuration:${NC}"
echo -e "  - Profile: ${AWS_PROFILE:-default}"
echo -e "  - Region: ${AWS_REGION:-us-east-1}"
echo -e "  - Credentials: $HOME/.aws"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running. Please start Docker and try again.${NC}"
    exit 1
fi

# Stop and remove existing container if it exists
if docker ps -a | grep -q $CONTAINER_NAME; then
    echo -e "${YELLOW}Stopping and removing existing container...${NC}"
    docker stop $CONTAINER_NAME 2>/dev/null || true
    docker rm $CONTAINER_NAME 2>/dev/null || true
fi

# Build the Docker image
echo -e "${GREEN}Building Docker image...${NC}"
docker build -t $IMAGE_NAME .

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Docker image built successfully!${NC}"
else
    echo -e "${RED}Docker image build failed!${NC}"
    exit 1
fi

# Run the container
echo -e "${GREEN}Starting container...${NC}"

# Check if AWS credentials exist
if [ ! -d "$HOME/.aws" ]; then
    echo -e "${YELLOW}Warning: AWS credentials directory not found at $HOME/.aws${NC}"
    echo -e "${YELLOW}The application may not be able to access AWS services.${NC}"
fi

docker run -d \
    --name $CONTAINER_NAME \
    -p $PORT:8080 \
    -v "$(pwd)/logs:/app/logs" \
    -v "$HOME/.aws:/root/.aws:ro" \
    -e AWS_PROFILE="${AWS_PROFILE:-default}" \
    -e AWS_REGION="${AWS_REGION:-us-east-1}" \
    $IMAGE_NAME

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Container started successfully!${NC}"
    echo -e "${GREEN}Application is running at: http://localhost:$PORT${NC}"
    echo -e "${YELLOW}View logs with: docker logs -f $CONTAINER_NAME${NC}"
    echo -e "${YELLOW}Stop container with: docker stop $CONTAINER_NAME${NC}"

    # Wait a few seconds and show logs
    echo -e "\n${GREEN}Showing application logs (press Ctrl+C to exit log view):${NC}"
    sleep 3
    docker logs -f $CONTAINER_NAME
else
    echo -e "${RED}Failed to start container!${NC}"
    exit 1
fi

