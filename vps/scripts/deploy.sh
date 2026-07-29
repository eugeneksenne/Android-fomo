#!/bin/bash
# Deploy script for FOMO Signaling Server to VPS
# Usage: ./scripts/deploy.sh [server_ip] [ssh_user]
# Example: ./scripts/deploy.sh 123.45.67.89 root

set -e

SERVER_IP=${1:-""}
SSH_USER=${2:-"root"}
REMOTE_DIR="/opt/fomo"

if [ -z "$SERVER_IP" ]; then
  echo "Usage: ./scripts/deploy.sh <server_ip> [ssh_user]"
  exit 1
fi

echo "🚀 Deploying FOMO Signaling Server to ${SSH_USER}@${SERVER_IP}:${REMOTE_DIR}"

# Ensure local build works (optional)
echo "📦 Checking package.json..."
if [ ! -f package.json ]; then
  echo "Must run from vps/ directory"
  exit 1
fi

# Create remote dir
ssh ${SSH_USER}@${SERVER_IP} "mkdir -p ${REMOTE_DIR}/vps"

# Rsync files (exclude node_modules, logs, etc)
echo "📤 Uploading files..."
rsync -avz --progress \
  --exclude 'node_modules' \
  --exclude 'logs' \
  --exclude '.git' \
  --exclude 'redis_data' \
  --exclude '.env' \
  ./ ${SSH_USER}@${SERVER_IP}:${REMOTE_DIR}/vps/

echo "🔧 Installing dependencies on remote..."
ssh ${SSH_USER}@${SERVER_IP} "
  cd ${REMOTE_DIR}/vps
  if [ ! -f .env ]; then
    echo '⚠️  No .env found on server, copying from .env.example - EDIT IT!'
    cp .env.example .env
  fi
  npm ci --only=production || npm install --only=production
"

echo "♻️ Restarting pm2..."
ssh ${SSH_USER}@${SERVER_IP} "
  cd ${REMOTE_DIR}/vps
  pm2 describe fomo-signaling > /dev/null
  if [ \$? -eq 0 ]; then
    pm2 restart ecosystem.config.js --env production
  else
    pm2 start ecosystem.config.js --env production
  fi
  pm2 save
  pm2 logs fomo-signaling --lines 50 --nostream
"

echo "✅ Deploy complete!"
echo "Check: https://${SERVER_IP}/health or pm2 status"
echo "Logs: ssh ${SSH_USER}@${SERVER_IP} 'pm2 logs fomo-signaling'"
