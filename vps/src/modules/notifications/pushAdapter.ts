/**
 * Push adapter - platform independent push notification triggering
 * This server does NOT send FCM directly (to remain stateless and not handle media/biz logic heavy),
 * but it triggers Supabase functions or queue that eventually sends push.
 * For Railway deploy, we log trigger and optionally call Supabase Edge Function if configured.
 */
import { logger } from '../../core/logger/logger.js';
import { env } from '../../config/env.js';
import { NotificationTrigger } from '../../shared/types/enums.js';

export interface PushPayload {
  recipientId: string;
  trigger: NotificationTrigger;
  title: string;
  body: string;
  data: Record<string, unknown>;
  correlationId: string;
  priority?: 'normal' | 'high' | 'critical';
  ttlSeconds?: number;
}

export interface PushAdapter {
  send(payload: PushPayload): Promise<{ success: boolean; providerResponse?: unknown }>;
}

class LoggingPushAdapter implements PushAdapter {
  async send(payload: PushPayload): Promise<{ success: boolean }> {
    // In production, replace with actual FCM / Supabase Edge Function call
    // Example: await supabase.functions.invoke('send-push', { body: payload })
    // Or publish to Redis queue that a worker consumes
    logger.info({
      event: 'push_trigger',
      recipientId: payload.recipientId,
      trigger: payload.trigger,
      title: payload.title,
      correlationId: payload.correlationId,
      dataKeys: Object.keys(payload.data),
      priority: payload.priority,
    }, `Push triggered ${payload.trigger} -> ${payload.recipientId}`);

    // Simulate platform-independent trigger
    // If SUPABASE_URL and SERVICE_ROLE_KEY configured, we could insert into notifications table
    if (env.SUPABASE_URL && env.SUPABASE_SERVICE_ROLE_KEY) {
      try {
        // Lazy import to avoid heavy dependency if not configured
        const { createClient } = await import('@supabase/supabase-js');
        const supabase = createClient(env.SUPABASE_URL, env.SUPABASE_SERVICE_ROLE_KEY);
        // Insert into a notifications or push_queue table (if exists) - best effort
        const { error } = await supabase.from('push_notifications').insert({
          recipient_id: payload.recipientId,
          trigger: payload.trigger,
          title: payload.title,
          body: payload.body,
          data: payload.data,
          correlation_id: payload.correlationId,
          priority: payload.priority || 'high',
          created_at: new Date().toISOString(),
        });
        if (error) {
          logger.debug({ error: error.message, recipientId: payload.recipientId }, 'Push insert to Supabase table failed (table may not exist, using log only)');
        }
      } catch (err) {
        logger.debug({ err: (err as Error).message }, 'Push Supabase insert skipped');
      }
    }

    return { success: true };
  }
}

export const pushAdapter: PushAdapter = new LoggingPushAdapter();
