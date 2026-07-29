/**
 * PM2 ecosystem config for FOMO Signaling Server v2 (TypeScript)
 * Usage:
 *  npm run build
 *  pm2 start ecosystem.config.js --env production
 *  pm2 save
 *  pm2 startup
 */
module.exports = {
  apps: [
    {
      name: 'fomo-signaling',
      script: 'dist/server.js',
      instances: 1, // Set to 'max' or 2-4 when REDIS_ENABLED=true for horizontal scaling
      exec_mode: 'cluster',
      watch: false,
      autorestart: true,
      max_memory_restart: '1G',
      env: {
        NODE_ENV: 'development',
        PORT: 3000,
        HOST: '0.0.0.0',
      },
      env_production: {
        NODE_ENV: 'production',
        PORT: 3000,
        HOST: '0.0.0.0',
      },
      error_file: './logs/pm2-error.log',
      out_file: './logs/pm2-out.log',
      log_file: './logs/pm2-combined.log',
      time: true,
      kill_timeout: 15000,
      wait_ready: false,
      listen_timeout: 10000,
    },
  ],
};
