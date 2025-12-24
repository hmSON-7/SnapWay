import http from './http';

export const createAutoTrip = (formData) =>
  http.post('/trip/auto-create', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });

export const fetchTripDetail = (tripId) => http.get(`/trip/${tripId}`);
