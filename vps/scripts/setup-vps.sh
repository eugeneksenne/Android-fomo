#!/bin/bash
# Initial VPS setup script for Ubuntu 22.04 / 24.04
# Run on fresh VPS as root: curl | bash or upload & run
# This installs Node.js 20, nginx, pm2, certbot, ufw

set -e

echo "=== FOMO Signaling Server - VPS Setup (Ubuntu) ==="

# Update
apt update && apt upgrade -y

# Install essentials
apt install -y curl git nginx certbot python3-certbot-nginx fail2ban ufw build-essential

# Install Node.js 20 via NodeSource
if ! command -v node &> /dev/null; then
  echo "Installing Node.js 20..."
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt install -y nodejs
fi

node -v
npm -v

# Install pm2 globally
npm install -g pm2

# Configure fail2ban + ufw
echo "Configuring firewall..."
ufw allow 22
ufw allow 80
ufw allow 443
ufw --force enable || true

# Enable nginx
systemctl enable nginx
systemctl start nginx

# Create app directory
mkdir -p /opt/fomo/vps/logs
chown -R www-data:www-data /opt/fomo || chown -R $SUDO_USER:$SUDO_USER /opt/fomo || true

# Setup logrotate for pm2 logs
cat > /etc/logrotate.d/fomo-signaling <<'EOF'
/opt/fomo/vps/logs/*.log {
  daily
  missingok
  rotate 14
  compress
  delaycompress
  notifempty
  create 0640 www-data www-data
  sharedscripts
}
EOF

echo "✅ Base setup complete!"
echo "Next steps:"
echo "1. Clone repo: git clone <repo> /opt/fomo (or deploy via deploy.sh)"
echo "2. cd /opt/fomo/vps && cp .env.example .env && nano .env (set JWT_SECRET, FIREBASE etc)"
echo "3. npm install --only=production"
echo "4. pm2 start ecosystem.config.js --env production && pm2 save && pm2 startup"
echo "5. Configure nginx: cp nginx.conf.example /etc/nginx/sites-available/fomo-signaling and edit server_name"
echo "6. Enable site and certbot: certbot --nginx -d signaling.yourdomain.com"
echo "7. pm2 logs fomo-signaling"
