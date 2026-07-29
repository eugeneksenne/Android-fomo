import { logger } from '../../core/logger/logger.js';
import { DeviceType, DevicePriority } from '../../shared/types/enums.js';
import { DeviceInfo } from '../../shared/types/socket.js';
import { normalizeDeviceId, isValidDeviceId, parseDeviceType, getDevicePriority } from '../../shared/utils/device.js';
import { env } from '../../config/env.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { AppError } from '../../shared/errors/appError.js';
import { generateCorrelationId } from '../../shared/utils/id.js';

interface DeviceRecord {
  deviceId: string;
  userId: string;
  deviceType: DeviceType;
  priority: DevicePriority;
  lastSeen: number;
  pushToken?: string;
  appVersion?: string;
  ip?: string;
}

class DeviceValidatorService {
  private devices = new Map<string, Map<string, DeviceRecord>>(); // userId -> deviceId -> record

  validateDevice(device: Partial<DeviceInfo>, userId: string, ip?: string): DeviceInfo {
    const deviceId = normalizeDeviceId(device.deviceId);
    if (!isValidDeviceId(deviceId) && env.NODE_ENV === 'production') {
      throw new AppError({
        code: ErrorCodes.AUTH_DEVICE_MISMATCH,
        correlationId: generateCorrelationId(),
        message: 'Invalid deviceId format',
        meta: { userId, deviceId },
      });
    }

    const deviceType = device.deviceType || parseDeviceType(device.deviceModel);
    const priority = getDevicePriority(deviceType, true);

    const record: DeviceRecord = {
      deviceId,
      userId,
      deviceType,
      priority,
      lastSeen: Date.now(),
      pushToken: device.pushToken,
      appVersion: device.appVersion,
      ip,
    };

    // Check limit
    const userDevices = this.devices.get(userId) || new Map();
    if (userDevices.size >= env.MAX_DEVICES_PER_USER && !userDevices.has(deviceId)) {
      // Evict lowest priority oldest device? For now reject
      const sorted = Array.from(userDevices.values()).sort((a, b) => a.priority - b.priority || a.lastSeen - b.lastSeen);
      const lowest = sorted[0];
      if (lowest && lowest.priority < priority) {
        logger.info({ userId, evictedDevice: lowest.deviceId, newDevice: deviceId }, 'Evicting low priority device to allow higher priority');
        userDevices.delete(lowest.deviceId);
      } else {
        throw new AppError({
          code: ErrorCodes.AUTH_TOO_MANY_DEVICES,
          correlationId: generateCorrelationId(),
          statusCode: 403,
          meta: { userId, deviceId },
        });
      }
    }

    userDevices.set(deviceId, record);
    this.devices.set(userId, userDevices);

    return {
      deviceId,
      deviceType,
      priority,
      deviceModel: device.deviceModel,
      osVersion: device.osVersion,
      appVersion: device.appVersion,
      pushToken: device.pushToken,
      ip,
      userAgent: device.deviceModel,
    };
  }

  updateLastSeen(userId: string, deviceId: string): void {
    const userDevices = this.devices.get(userId);
    if (userDevices) {
      const rec = userDevices.get(deviceId);
      if (rec) rec.lastSeen = Date.now();
    }
  }

  removeDevice(userId: string, deviceId: string): void {
    this.devices.get(userId)?.delete(deviceId);
    logger.debug({ userId, deviceId }, 'Device removed from validator');
  }

  getUserDevices(userId: string): DeviceRecord[] {
    return Array.from(this.devices.get(userId)?.values() || []);
  }

  getDevicePriority(userId: string, deviceId: string): DevicePriority {
    return this.devices.get(userId)?.get(deviceId)?.priority || DevicePriority.SECONDARY;
  }

  isPrimaryDevice(userId: string, deviceId: string): boolean {
    const devices = this.getUserDevices(userId);
    if (devices.length === 0) return true;
    const maxPriority = Math.max(...devices.map(d => d.priority));
    const dev = devices.find(d => d.deviceId === deviceId);
    return dev ? dev.priority === maxPriority : false;
  }
}

export const deviceValidatorService = new DeviceValidatorService();
