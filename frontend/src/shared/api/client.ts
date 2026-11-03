import axios from 'axios';
import { ApiErrorResponse } from '@/types/api';

export const apiClient = axios.create({
  baseURL: '',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Response interceptor to catch 401 Unauthorized and notify AuthContext
apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('auth:unauthorized'));
      }
    }
    return Promise.reject(error);
  }
);

export function getApiError(error: unknown): ApiErrorResponse | null {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const res = (error as { response?: { data?: unknown } }).response;
    if (res?.data && typeof res.data === 'object') {
      return res.data as ApiErrorResponse;
    }
  }
  return null;
}

export function getErrorMessage(error: unknown, fallbackMessage = 'Произошла непредвиденная ошибка'): string {
  const apiError = getApiError(error);
  if (apiError?.message) {
    if (apiError.details && apiError.details.length > 0) {
      const fieldDetails = apiError.details.map((d) => `${d.issue}`).join(', ');
      return `${apiError.message}: ${fieldDetails}`;
    }
    return apiError.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallbackMessage;
}
