import { FastifyError, FastifyInstance } from 'fastify';
import { AppError, isAppError } from '../../shared/errors/appError.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { logger } from '../logger/logger.js';

export function registerErrorHandler(app: FastifyInstance): void {
  app.setErrorHandler((error: FastifyError, request, reply) => {
    const correlationId = (request.headers['x-correlation-id'] as string) || generateCorrelationId();

    if (isAppError(error)) {
      logger.error({ code: error.code, correlationId, userId: (request as any).user?.sub, path: request.url }, error.message);
      return reply.code(error.statusCode).send({
        success: false,
        error: error.toJSON(),
      });
    }

    // Fastify validation errors
    if (error.validation) {
      const appError = new AppError({
        code: ErrorCodes.VALIDATION_FAILED,
        correlationId,
        statusCode: 400,
        developerMessage: JSON.stringify(error.validation),
      });
      return reply.code(400).send({ success: false, error: appError.toJSON() });
    }

    // Generic
    logger.error({ err: error.message, stack: error.stack, correlationId, path: request.url }, 'Unhandled error');
    const internalError = new AppError({
      code: ErrorCodes.INTERNAL_ERROR,
      correlationId,
      statusCode: 500,
      developerMessage: error.message,
    });
    return reply.code(500).send({ success: false, error: internalError.toJSON() });
  });
}

export function socketErrorResponse(code: string, message: string, correlationId: string, recovery?: string, details?: unknown) {
  return {
    code,
    message,
    developerMessage: message,
    correlationId,
    recovery: recovery || 'Retry with corrected payload',
    timestamp: new Date().toISOString(),
    details,
  };
}
