export type Role = 'CLIENT' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  name: string;
  phone: string;
  role: Role;
  createdAt?: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  phone: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ServiceDto {
  id: string;
  name: string;
  description?: string | null;
  durationMinutes: number;
  price: number;
  isActive: boolean;
  createdAt?: string;
}

export interface CreateServiceRequest {
  name: string;
  description?: string | null;
  durationMinutes: number;
  price: number;
}

export interface UpdateServiceRequest {
  name: string;
  description?: string | null;
  durationMinutes: number;
  price: number;
  isActive?: boolean | null;
}

export interface MasterDto {
  id: string;
  name: string;
  description?: string | null;
  isActive: boolean;
  services: ServiceDto[];
  createdAt?: string;
}

export interface CreateMasterRequest {
  name: string;
  description?: string | null;
}

export interface UpdateMasterRequest {
  name: string;
  description?: string | null;
  isActive?: boolean | null;
}

export interface AssignServicesRequest {
  serviceIds: string[];
}

export interface ScheduleIntervalDto {
  dayOfWeek: number; // 1 (Mon) .. 7 (Sun)
  startTime: string; // HH:mm e.g. "09:00"
  endTime: string;   // HH:mm e.g. "13:00"
}

export interface SetScheduleRequest {
  intervals: ScheduleIntervalDto[];
}

export interface AvailableSlotDto {
  startTime: string; // ISO Instant
  endTime: string;   // ISO Instant
  formattedTime: string;
  masterId: string;
  masterName: string;
  serviceId: string;
}

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED';

export interface CreateBookingRequest {
  serviceId: string;
  masterId: string;
  startTime: string;
}

export interface BookingResponse {
  id: string;
  clientId: string;
  clientName: string;
  clientPhone: string;
  clientEmail: string;
  masterId: string;
  masterName: string;
  serviceId: string;
  serviceName: string;
  servicePrice: number;
  startTime: string;
  endTime: string;
  status: BookingStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ClientBookingsResponse {
  active: BookingResponse[];
  archive: BookingResponse[];
}

export interface ApiFieldError {
  field: string;
  issue: string;
}

export interface ApiErrorResponse {
  error: string;
  message: string;
  details?: ApiFieldError[];
  timestamp?: string;
}

export interface AppConfig {
  timezone: string;
  currency: string;
}
