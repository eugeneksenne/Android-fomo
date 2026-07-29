# FOMO Signaling Server - Production Dockerfile
# Node.js 22 LTS Alpine, TypeScript build, non-root, healthcheck, Railway compatible

FROM node:22-alpine AS base

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

WORKDIR /app

# Copy package files for layer caching
COPY package.json tsconfig.json ./

# Install all deps (including dev for build)
RUN npm install

# Copy source
COPY src ./src

# Build TypeScript
RUN npm run build && npm prune --omit=dev && npm cache clean --force

# --- Production image ---
FROM node:22-alpine AS production

RUN apk add --no-cache dumb-init wget

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOST=0.0.0.0

# Create non-root user
RUN addgroup -S fomo && adduser -S fomo -G fomo

# Copy built files from base
COPY --from=base /app/dist ./dist
COPY --from=base /app/node_modules ./node_modules
COPY --from=base /app/package.json ./package.json

# Copy env example for reference (not used)
COPY .env.example ./.env.example

# Chown
RUN chown -R fomo:fomo /app

USER fomo

EXPOSE 3000

# Healthcheck - calls /health
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD wget -qO- http://localhost:${PORT:-3000}/health || exit 1

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

CMD ["node", "dist/server.js"]
