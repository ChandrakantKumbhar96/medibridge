import express from 'express';
import { requestId } from './middleware/requestId.js';
import { corsMiddleware } from './middleware/cors.js';
import { httpLogger } from './middleware/logger.js';
import { notFoundHandler, errorHandler } from './middleware/errorHandler.js';
import { routes } from './routes/index.js';

// Phase 2: proxy routes are rate-limited and JWT-gated (see
// routes/proxy.routes.js). No body parser here on purpose — see
// proxy/streaming.js.
export const app = express();

app.use(requestId);
app.use(httpLogger);
app.use(corsMiddleware);

app.use(routes);

app.use(notFoundHandler);
app.use(errorHandler);
