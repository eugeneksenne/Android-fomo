import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { env } from '../../config/env.js';
import { logger } from '../../core/logger/logger.js';
import { verifySupabaseJwt, SupabaseJwtPayload } from '../../core/middleware/auth.js';
import { AppError } from '../../shared/errors/appError.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { generateCorrelationId } from '../../shared/utils/id.js';

class SupabaseAuthService {
  private client: SupabaseClient | null = null;
  private adminClient: SupabaseClient | null = null;

  constructor() {
    if (env.SUPABASE_URL && env.SUPABASE_ANON_KEY) {
      this.client = createClient(env.SUPABASE_URL, env.SUPABASE_ANON_KEY);
    }
    if (env.SUPABASE_URL && env.SUPABASE_SERVICE_ROLE_KEY) {
      this.adminClient = createClient(env.SUPABASE_URL, env.SUPABASE_SERVICE_ROLE_KEY);
    }
  }

  async verifyToken(token: string): Promise<SupabaseJwtPayload> {
    return verifySupabaseJwt(token);
  }

  async getUserById(userId: string): Promise<{ id: string; email?: string; user_metadata?: unknown } | null> {
    if (!this.adminClient) {
      logger.warn('Supabase admin client not configured, skipping user fetch');
      return null;
    }
    try {
      const { data, error } = await this.adminClient.auth.admin.getUserById(userId);
      if (error) {
        logger.warn({ userId, error: error.message }, 'Supabase getUserById failed');
        return null;
      }
      return data.user as any;
    } catch (err) {
      logger.error({ err: (err as Error).message, userId }, 'Supabase getUserById exception');
      return null;
    }
  }

  async isSessionValid(userId: string, sessionId?: string): Promise<boolean> {
    if (!sessionId) return true; // if not provided, skip strict check
    // In production you would check against your sessions table in Supabase Postgres
    // Example query: SELECT * FROM user_sessions WHERE user_id = userId AND session_id = sessionId AND revoked = false
    // For signaling server stateless mode, we assume valid if JWT valid
    // But we log for audit
    logger.debug({ userId, sessionId }, 'Session validation check (stateless mode) - assumed valid');
    return true;
  }

  async revokeSession(userId: string, sessionId: string): Promise<void> {
    logger.info({ userId, sessionId }, 'Session revoke requested');
    // In production, update Supabase Postgres table
    // await supabase.from('user_sessions').update({ revoked: true }).eq('session_id', sessionId)
  }

  async handleRefresh(oldToken: string): Promise<{ newToken?: string; payload: SupabaseJwtPayload }> {
    // Supabase refresh is typically handled client-side via supabase.auth.refreshSession()
    // Server here only verifies new token if provided; old token refresh not server responsibility
    // However we support token rotation: if client sends refresh_token, we could call supabase.auth.refreshSession()
    throw new AppError({
      code: ErrorCodes.NOT_IMPLEMENTED,
      correlationId: generateCorrelationId(),
      message: 'Token refresh should be handled client-side via Supabase SDK',
      recovery: 'Use supabase.auth.refreshSession() on client and reconnect with new token',
    });
  }

  async checkLogoutPropagation(userId: string): Promise<boolean> {
    // Check if user has logged out globally - would query Supabase auth logs or custom table
    // Stateless mode: rely on JWT expiry; if you implement logout propagation, store revoked tokens in Redis with TTL = exp - iat
    return false;
  }
}

export const supabaseAuthService = new SupabaseAuthService();
