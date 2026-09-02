export const environment = {
  production: false,
  // When running locally on localhost:4200, points to local backend.
  // In production (Vercel), update to your live Render backend URL.
  apiUrl: (typeof window !== 'undefined' && window.location.hostname === 'localhost' && window.location.port === '4200')
    ? 'http://localhost:8080/api'
    : 'https://vehicle-rental-system-backend.onrender.com/api'
};
