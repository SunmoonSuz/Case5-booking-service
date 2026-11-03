import { apiClient } from './client';
import { ServiceDto, MasterDto, AvailableSlotDto } from '@/types/api';

export const catalogApi = {
  async getActiveServices(): Promise<ServiceDto[]> {
    const response = await apiClient.get<ServiceDto[]>('/api/v1/services');
    return response.data;
  },

  async getActiveMasters(serviceId?: string): Promise<MasterDto[]> {
    const response = await apiClient.get<MasterDto[]>('/api/v1/masters', {
      params: serviceId ? { serviceId } : undefined,
    });
    return response.data;
  },

  async getAvailability(params: {
    serviceId: string;
    date: string; // YYYY-MM-DD
    masterId?: string;
  }): Promise<AvailableSlotDto[]> {
    const response = await apiClient.get<AvailableSlotDto[]>('/api/v1/availability', {
      params: {
        serviceId: params.serviceId,
        date: params.date,
        ...(params.masterId ? { masterId: params.masterId } : {}),
      },
    });
    return response.data;
  },
};
