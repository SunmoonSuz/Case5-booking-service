import { apiClient } from './client';
import { AppConfig } from '@/types/api';

export const configApi = {
  async getConfig(): Promise<AppConfig> {
    const response = await apiClient.get<AppConfig>('/api/v1/config');
    return response.data;
  },
};
