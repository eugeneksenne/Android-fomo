import { DeviceType, DevicePriority } from '../types/enums.js';

export function parseDeviceType(userAgent?: string, explicit?: string): DeviceType {
  if (explicit) {
    const upper = explicit.toUpperCase();
    if (upper.includes('ANDROID')) return DeviceType.ANDROID;
    if (upper.includes('IOS') || upper.includes('IPHONE') || upper.includes('IPAD')) return DeviceType.IOS;
    if (upper.includes('WEB')) return DeviceType.WEB;
  }
  if (!userAgent) return DeviceType.UNKNOWN;
  const ua = userAgent.toLowerCase();
  if (ua.includes('android')) return DeviceType.ANDROID;
  if (ua.includes('iphone') || ua.includes('ipad')) return DeviceType.IOS;
  if (ua.includes('fomo-android')) return DeviceType.ANDROID;
  return DeviceType.WEB;
}

export function getDevicePriority(deviceType: DeviceType, isForeground = true): DevicePriority {
  if (!isForeground) return DevicePriority.BACKGROUND;
  if (deviceType === DeviceType.ANDROID || deviceType === DeviceType.IOS) return DevicePriority.PRIMARY;
  return DevicePriority.SECONDARY;
}

export function isValidDeviceId(deviceId: unknown): boolean {
  return typeof deviceId === 'string' && deviceId.length >= 8 && deviceId.length <= 128;
}

export function normalizeDeviceId(deviceId?: string, socketId?: string): string {
  if (deviceId && isValidDeviceId(deviceId)) return deviceId;
  return socketId ? `temp_${socketId.slice(0, 12)}` : `temp_${Math.random().toString(36).slice(2, 10)}`;
}
