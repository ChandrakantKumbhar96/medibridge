import pino from 'pino';
import pinoHttp from 'pino-http';
import { env } from '../config/env.js';

export const logger = pino({ level: env.nodeEnv === 'production' ? 'info' : 'debug' });

export const httpLogger = pinoHttp({
  logger,
  genReqId: (req) => req.id,
});
