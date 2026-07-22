// Small helper that resolves mock data after a short delay to emulate latency.
export const mockResolve = (data, ms = 300) =>
  new Promise((resolve) => setTimeout(() => resolve(structuredClone(data)), ms))
