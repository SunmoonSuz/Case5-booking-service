import { apiClient } from './client';
import { CreateBookingRequest, BookingResponse, ClientBookingsResponse } from '@/types/api';

export const bookingApi = {
  async createBooking(data: CreateBookingRequest): Promise<BookingResponse> {
    const response = await apiClient.post<BookingResponse>('/api/v1/bookings', data);
    return response.data;
  },

  async getClientBookings(): Promise<ClientBookingsResponse> {
    const response = await apiClient.get<ClientBookingsResponse>('/api/v1/client/bookings');
    return response.data;
  },

  async cancelClientBooking(id: string): Promise<BookingResponse> {
    const response = await apiClient.post<BookingResponse>(`/api/v1/client/bookings/${id}/cancel`);
    return response.data;
  },
};
