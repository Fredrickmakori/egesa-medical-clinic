#!/bin/bash

# EGESA Medical Clinic - DigitalOcean Deployment Helper
# Usage: ./deploy.sh [production|staging]

set -e

ENVIRONMENT=${1:-production}
DROPLET_IP=${DROPLET_IP:-}
GITHUB_TOKEN=${GITHUB_TOKEN:-}

echo "🏥 EGESA Medical Clinic - Deployment Helper"
echo "Environment: $ENVIRONMENT"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Step 1: Check prerequisites
echo -e "${YELLOW}[1/5] Checking prerequisites...${NC}"
command -v git &> /dev/null || { echo "Git is required but not installed."; exit 1; }
command -v gradle &> /dev/null || command -v ./gradlew &> /dev/null || { echo "Gradle is required."; exit 1; }
echo -e "${GREEN}✓ Prerequisites OK${NC}\n"

# Step 2: Build the server
echo -e "${YELLOW}[2/5] Building server with Gradle...${NC}"
./gradlew :server:clean :server:build -x test --info
echo -e "${GREEN}✓ Build complete${NC}\n"

# Step 3: Prepare distribution
echo -e "${YELLOW}[3/5] Preparing distribution...${NC}"
cd server/build/distributions
TAR_FILE=$(ls server-*.tar.gz | head -1)
if [ -z "$TAR_FILE" ]; then
    echo -e "${RED}✗ No distribution tarball found${NC}"
    exit 1
fi
echo "Found: $TAR_FILE"
echo -e "${GREEN}✓ Distribution ready${NC}\n"

# Step 4: Deploy to Droplet (if IP provided)
if [ ! -z "$DROPLET_IP" ]; then
    echo -e "${YELLOW}[4/5] Deploying to Droplet ($DROPLET_IP)...${NC}"

    # Copy tarball to droplet
    scp "$TAR_FILE" "root@$DROPLET_IP:/tmp/"

    # Extract and restart
    ssh "root@$DROPLET_IP" bash << 'EOF'
        cd /opt/egesa-medical-clinic-mobile-app
        tar -xzf /tmp/$TAR_FILE
        systemctl restart egesa-clinic-server
        systemctl status egesa-clinic-server
EOF

    echo -e "${GREEN}✓ Deployment complete${NC}\n"
else
    echo -e "${YELLOW}[4/5] Skipping Droplet deployment (DROPLET_IP not set)${NC}\n"
fi

# Step 5: Summary
echo -e "${YELLOW}[5/5] Deployment Summary${NC}"
echo -e "${GREEN}✓ Server built successfully${NC}"
if [ ! -z "$DROPLET_IP" ]; then
    echo -e "${GREEN}✓ Deployed to $DROPLET_IP${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. SSH into droplet: ssh root@$DROPLET_IP"
    echo "  2. Check logs: journalctl -u egesa-clinic-server -f"
    echo "  3. Verify API: curl http://localhost:8080/health"
else
    echo "To deploy to DigitalOcean, set DROPLET_IP:"
    echo "  export DROPLET_IP=your.droplet.ip.address"
    echo "  ./deploy.sh $ENVIRONMENT"
fi

echo ""
echo -e "${GREEN}🚀 Deployment process complete!${NC}"

