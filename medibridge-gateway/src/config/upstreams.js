import { env } from './env.js';

export const upstreams = {
  spring: { target: env.springApiUrl, pathPrefix: '/api' },
  chat: { target: env.chatServiceUrl, pathPrefix: '/api/chat' },
  notify: { target: env.notifyServiceUrl, pathPrefix: '/api/reports' },
};
