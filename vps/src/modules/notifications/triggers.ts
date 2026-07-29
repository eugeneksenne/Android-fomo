import { NotificationTrigger } from '../../shared/types/enums.js';

export const TriggerToPriority: Record<NotificationTrigger, 'normal' | 'high' | 'critical'> = {
  [NotificationTrigger.INCOMING_CALL]: 'critical',
  [NotificationTrigger.MISSED_CALL]: 'high',
  [NotificationTrigger.CALL_BUSY]: 'normal',
  [NotificationTrigger.CALL_REJECTED]: 'normal',
  [NotificationTrigger.GROUP_INVITATION]: 'high',
  [NotificationTrigger.NIGHTGUARD_ALERT]: 'high',
  [NotificationTrigger.EMERGENCY_ALERT]: 'critical',
  [NotificationTrigger.BUDDY_PING]: 'high',
  [NotificationTrigger.SOS_TRIGGERED]: 'critical',
};

export const TriggerToTtl: Record<NotificationTrigger, number> = {
  [NotificationTrigger.INCOMING_CALL]: 45, // 45 seconds - call timeout
  [NotificationTrigger.MISSED_CALL]: 3600,
  [NotificationTrigger.CALL_BUSY]: 300,
  [NotificationTrigger.CALL_REJECTED]: 300,
  [NotificationTrigger.GROUP_INVITATION]: 3600,
  [NotificationTrigger.NIGHTGUARD_ALERT]: 1800,
  [NotificationTrigger.EMERGENCY_ALERT]: 3600,
  [NotificationTrigger.BUDDY_PING]: 600,
  [NotificationTrigger.SOS_TRIGGERED]: 3600,
};

export function getNotificationChannel(trigger: NotificationTrigger): string {
  switch (trigger) {
    case NotificationTrigger.INCOMING_CALL:
      return 'calls';
    case NotificationTrigger.MISSED_CALL:
    case NotificationTrigger.CALL_BUSY:
    case NotificationTrigger.CALL_REJECTED:
      return 'calls';
    case NotificationTrigger.NIGHTGUARD_ALERT:
    case NotificationTrigger.EMERGENCY_ALERT:
    case NotificationTrigger.SOS_TRIGGERED:
    case NotificationTrigger.BUDDY_PING:
      return 'nightguard';
    case NotificationTrigger.GROUP_INVITATION:
      return 'groups';
    default:
      return 'general';
  }
}
