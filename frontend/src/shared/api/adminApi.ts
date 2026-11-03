import { apiClient } from './client';
import {
  BookingResponse,
  BookingStatus,
  ServiceDto,
  CreateServiceRequest,
  UpdateServiceRequest,
  MasterDto,
  CreateMasterRequest,
  UpdateMasterRequest,
  ScheduleIntervalDto,
  SetScheduleRequest,
} from '@/types/api';

export const adminApi = {
  // Bookings moderation
  async getBookings(params?: {
    status?: BookingStatus;
    masterId?: string;
    date?: string; // YYYY-MM-DD
  }): Promise<BookingResponse[]> {
    const response = await apiClient.get<BookingResponse[]>('/api/v1/admin/bookings', {
      params,
    });
    return response.data;
  },

  async confirmBooking(id: string): Promise<BookingResponse> {
    const response = await apiClient.post<BookingResponse>(`/api/v1/admin/bookings/${id}/confirm`);
    return response.data;
  },

  async rejectBooking(id: string): Promise<BookingResponse> {
    const response = await apiClient.post<BookingResponse>(`/api/v1/admin/bookings/${id}/reject`);
    return response.data;
  },

  async cancelBooking(id: string): Promise<BookingResponse> {
    const response = await apiClient.post<BookingResponse>(`/api/v1/admin/bookings/${id}/cancel`);
    return response.data;
  },

  async completeBooking(id: string): Promise<BookingResponse> {
    const response = await apiClient.post<BookingResponse>(`/api/v1/admin/bookings/${id}/complete`);
    return response.data;
  },

  // Services CRUD
  async getAllServices(): Promise<ServiceDto[]> {
    const response = await apiClient.get<ServiceDto[]>('/api/v1/admin/services');
    return response.data;
  },

  async createService(data: CreateServiceRequest): Promise<ServiceDto> {
    const response = await apiClient.post<ServiceDto>('/api/v1/admin/services', data);
    return response.data;
  },

  async updateService(id: string, data: UpdateServiceRequest): Promise<ServiceDto> {
    const response = await apiClient.put<ServiceDto>(`/api/v1/admin/services/${id}`, data);
    return response.data;
  },

  async deleteService(id: string): Promise<void> {
    await apiClient.delete(`/api/v1/admin/services/${id}`);
  },

  // Masters CRUD
  async getAllMasters(): Promise<MasterDto[]> {
    const response = await apiClient.get<MasterDto[]>('/api/v1/admin/masters');
    return response.data;
  },

  async createMaster(data: CreateMasterRequest): Promise<MasterDto> {
    const response = await apiClient.post<MasterDto>('/api/v1/admin/masters', data);
    return response.data;
  },

  async updateMaster(id: string, data: UpdateMasterRequest): Promise<MasterDto> {
    const response = await apiClient.put<MasterDto>(`/api/v1/admin/masters/${id}`, data);
    return response.data;
  },

  async deleteMaster(id: string): Promise<void> {
    await apiClient.delete(`/api/v1/admin/masters/${id}`);
  },

  async assignServices(masterId: string, serviceIds: string[]): Promise<MasterDto> {
    const response = await apiClient.put<MasterDto>(`/api/v1/admin/masters/${masterId}/services`, {
      serviceIds,
    });
    return response.data;
  },

  // Schedule
  async getMasterSchedule(masterId: string): Promise<ScheduleIntervalDto[]> {
    const response = await apiClient.get<ScheduleIntervalDto[]>(`/api/v1/admin/masters/${masterId}/schedule`);
    return response.data;
  },

  async setMasterSchedule(masterId: string, data: SetScheduleRequest): Promise<ScheduleIntervalDto[]> {
    const response = await apiClient.put<ScheduleIntervalDto[]>(`/api/v1/admin/masters/${masterId}/schedule`, data);
    return response.data;
  },
};
