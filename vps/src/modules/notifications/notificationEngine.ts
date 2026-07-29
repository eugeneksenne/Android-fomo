import { NotificationTrigger } from '../../shared/types/enums.js';
import { pushAdapter, PushPayload } from './pushAdapter.js';
import { TriggerToPriority, TriggerToTtl, getNotificationChannel } from './triggers.js';
import { logger } from '../../core/logger/logger.js';
import { metricsCollector } from '../../core/monitoring/metrics.js';
import { generateCorrelationId } from '../../shared/utils/id.js';

export interface NotificationRequest {
  trigger: NotificationTrigger;
  recipientId: string;
  title: string;
  body: string;
  data: Record<string, unknown>;
  correlationId?: string;
  priority?: 'normal' | 'high' | 'critical';
}

class NotificationEngine {
  private sentCount = 0;
  private failedCount = 0;

  async trigger(request: NotificationRequest): Promise<{ success: boolean }> {
    const correlationId = request.correlationId || generateCorrelationId();
    const priority = request.priority || TriggerToPriority[request.trigger] || 'high';
    const ttl = TriggerToTtl[request.trigger] || 3600;

    const payload: PushPayload = {
      recipientId: request.recipientId,
      trigger: request.trigger,
      title: request.title.slice(0, 100),
      body: request.body.slice(0, 300),
      data: {
        ...request.data,
        channel: getNotificationChannel(request.trigger),
        trigger: request.trigger,
      },
      correlationId,
      priority,
      ttlSeconds: ttl,
    };

    try {
      const result = await pushAdapter.send(payload);
      if (result.success) this.sentCount++;
      else this.failedCount++;

      logger.info({
        event: 'notification_trigger',
        trigger: request.trigger,
        recipientId: request.recipientId,
        correlationId,
        success: result.success,
      }, `Notification ${request.trigger} -> ${request.recipientId} success=${result.success}`);

      return result;
    } catch (err) {
      this.failedCount++;
      logger.error({ err: (err as Error).message, trigger: request.trigger, recipientId: request.recipientId, correlationId }, 'Notification trigger failed');
      metricsCollector.recordError('NOTIFICATION_FAILED');
      return { success: false };
    }
  }

  async triggerBulk(requests: NotificationRequest[]): Promise<{ success: number; failed: number }> {
    // Battery efficient: batch triggers
    let success = 0;
    let failed = 0;
    for (const req of requests) {
      const res = await this.trigger(req);
      if (res.success) success++;
      else failed++;
    }
    return { success, failed };
  }

  getStats(): { sent: number; failed: number } {
    return { sent: this.sentCount, failed: this.failedCount };
  }
}

export const notificationEngine = new NotificationEngine();
