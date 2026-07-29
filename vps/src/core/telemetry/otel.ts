import { env } from '../../config/env.js';
import { logger } from '../logger/logger.js';

let sdk: { start: () => Promise<void>; shutdown: () => Promise<void> } | null = null;

export async function initTelemetry(): Promise<void> {
  if (!env.OTEL_ENABLED) {
    logger.info('OpenTelemetry disabled');
    return;
  }

  try {
    // Dynamic import to avoid mandatory dependency if disabled
    const { NodeSDK } = await import('@opentelemetry/sdk-node');
    const { getNodeAutoInstrumentations } = await import('@opentelemetry/auto-instrumentations-node');

    const sdkInstance = new NodeSDK({
      serviceName: env.OTEL_SERVICE_NAME,
      instrumentations: [getNodeAutoInstrumentations()],
    });

    await sdkInstance.start();
    sdk = sdkInstance as unknown as { start: () => Promise<void>; shutdown: () => Promise<void> };
    logger.info({ service: env.OTEL_SERVICE_NAME, endpoint: env.OTEL_EXPORTER_OTLP_ENDPOINT }, 'OpenTelemetry initialized');
  } catch (err) {
    logger.warn({ err: (err as Error).message }, 'Failed to init OpenTelemetry, continuing without tracing');
  }
}

export async function shutdownTelemetry(): Promise<void> {
  if (sdk) {
    try {
      await sdk.shutdown();
      logger.info('OpenTelemetry shut down');
    } catch (err) {
      logger.warn({ err: (err as Error).message }, 'Error shutting down OTEL');
    }
  }
}

export const trace = {
  startSpan: (name: string, fn: () => Promise<void> | void) => {
    // No-op wrapper if OTEL not enabled, keeps code simple
    return fn();
  },
};
