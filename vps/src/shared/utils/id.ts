import { v4 as uuidv4 } from 'uuid';

export function generateId(prefix?: string): string {
  const id = uuidv4();
  return prefix ? `${prefix}_${id}` : id;
}

export function generateShortId(): string {
  return Math.random().toString(36).slice(2, 9) + Date.now().toString(36);
}

export function generateCorrelationId(): string {
  return `corr_${Date.now()}_${generateShortId()}`;
}

export function generateRoomId(type: string, suffix?: string): string {
  const short = uuidv4().split('-')[0];
  return suffix ? `${type}_${suffix}_${short}` : `${type}_${short}`;
}
