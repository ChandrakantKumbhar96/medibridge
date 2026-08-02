import { app } from './app.js';
import { env } from './config/env.js';
import { logger } from './middleware/logger.js';

app.listen(env.port, () => {
  logger.info(`gateway listening on :${env.port}, proxying to ${env.springApiUrl}`);
});
