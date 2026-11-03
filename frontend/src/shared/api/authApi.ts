import { apiClient } from './client';
import { User, LoginRequest, RegisterRequest } from '@/types/api';

export const authApi = {
  async register(data: RegisterRequest): Promise<User> {
    const response = await apiClient.post<User>('/api/v1/auth/register', data);
    return response.data;
  },

  async login(data: LoginRequest): Promise<User> {
    const response = await apiClient.post<User>('/api/v1/auth/login', data);
    return response.data;
  },

  async logout(): Promise<void> {
    await apiClient.post('/api/v1/auth/logout');
  },

  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<User>('/api/v1/auth/me');
    return response.data;
  },
};
