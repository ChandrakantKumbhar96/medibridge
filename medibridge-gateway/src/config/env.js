import 'dotenv/config';

function required(name, fallback) {
  const value = process.env[name] ?? fallback;
  if (value === undefined) {
    throw new Error(`Missing required env var: ${name}`);
  }
  return value;
}

export const env = {
  port: Number(process.env.PORT ?? 4000),
  nodeEnv: process.env.NODE_ENV ?? 'development',
  corsOrigin: process.env.CORS_ORIGIN ?? 'http://localhost:5173',
  springApiUrl: required('SPRING_API_URL', 'http://localhost:8080'),
  chatServiceUrl: process.env.CHAT_SERVICE_URL ?? 'http://localhost:8000',
  notifyServiceUrl: process.env.NOTIFY_SERVICE_URL ?? 'http://localhost:5000',
  jwtSecret: process.env.JWT_SECRET,
  internalApiKey: process.env.INTERNAL_API_KEY,
};
