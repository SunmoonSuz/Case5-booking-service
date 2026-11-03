import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BookingsModerationTab } from '@/features/admin/BookingsModerationTab';
import { ToastProvider } from '@/shared/context/ToastContext';
import { adminApi } from '@/shared/api/adminApi';

vi.mock('@/shared/api/adminApi');

const mockMasters = [
  {
    id: 'm-1',
    name: 'Алексей Смирнов',
    description: 'Барбер',
    isActive: true,
    services: [],
  },
];

const mockBookings = [
  {
    id: 'b-pending-1',
    clientId: 'u-1',
    clientName: 'Михаил Соколов',
    clientPhone: '+79998887766',
    clientEmail: 'mikhail@test.ru',
    masterId: 'm-1',
    masterName: 'Алексей Смирнов',
    serviceId: 's-1',
    serviceName: 'Модельная стрижка',
    servicePrice: 1500,
    startTime: '2026-11-10T11:00:00Z',
    endTime: '2026-11-10T12:00:00Z',
    status: 'PENDING' as const,
    createdAt: '2026-11-03T10:00:00Z',
    updatedAt: '2026-11-03T10:00:00Z',
  },
  {
    id: 'b-confirmed-2',
    clientId: 'u-2',
    clientName: 'Анна Васильева',
    clientPhone: '+79995554433',
    clientEmail: 'anna@test.ru',
    masterId: 'm-1',
    masterName: 'Алексей Смирнов',
    serviceId: 's-1',
    serviceName: 'Модельная стрижка',
    servicePrice: 1500,
    startTime: '2026-11-11T13:00:00Z',
    endTime: '2026-11-11T14:00:00Z',
    status: 'CONFIRMED' as const,
    createdAt: '2026-11-03T10:00:00Z',
    updatedAt: '2026-11-03T10:00:00Z',
  },
];

function renderWithProviders(ui: React.ReactElement, queryClient = new QueryClient()) {
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>{ui}</ToastProvider>
    </QueryClientProvider>
  );
}

describe('BookingsModerationTab (AC-006: Admin Moderation)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(adminApi.getAllMasters).mockResolvedValue(mockMasters);
    vi.mocked(adminApi.getBookings).mockResolvedValue(mockBookings);
  });

  it('renders bookings list with client contacts and service details', async () => {
    renderWithProviders(<BookingsModerationTab />);

    expect(await screen.findByText('Михаил Соколов')).toBeInTheDocument();
    expect(screen.getByText('+79998887766')).toBeInTheDocument();
    expect(screen.getByText('mikhail@test.ru')).toBeInTheDocument();

    expect(screen.getByText('Анна Васильева')).toBeInTheDocument();
    expect(screen.getByText('Ожидает подтверждения')).toBeInTheDocument();
    expect(screen.getByText('Подтверждено')).toBeInTheDocument();
  });

  it('allows admin to confirm a pending booking and invalidates queries cache', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    vi.mocked(adminApi.confirmBooking).mockResolvedValueOnce({
      ...mockBookings[0],
      status: 'CONFIRMED',
    });

    renderWithProviders(<BookingsModerationTab />, queryClient);

    const confirmBtn = await screen.findByRole('button', { name: /подтвердить/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(adminApi.confirmBooking).toHaveBeenCalledWith('b-pending-1');
      expect(invalidateSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          queryKey: expect.arrayContaining(['adminBookings']),
        })
      );
    });

    expect(await screen.findByText('Заявка успешно подтверждена')).toBeInTheDocument();
  });

  it('allows admin to reject a pending booking', async () => {
    vi.mocked(adminApi.rejectBooking).mockResolvedValueOnce({
      ...mockBookings[0],
      status: 'REJECTED',
    });

    renderWithProviders(<BookingsModerationTab />);

    const rejectBtn = await screen.findByRole('button', { name: /отклонить/i });
    fireEvent.click(rejectBtn);

    await waitFor(() => {
      expect(adminApi.rejectBooking).toHaveBeenCalledWith('b-pending-1');
    });

    expect(await screen.findByText('Заявка отклонена, слот освобождён')).toBeInTheDocument();
  });

  it('allows admin to complete a confirmed visit', async () => {
    vi.mocked(adminApi.completeBooking).mockResolvedValueOnce({
      ...mockBookings[1],
      status: 'COMPLETED',
    });

    renderWithProviders(<BookingsModerationTab />);

    const completeBtn = await screen.findByRole('button', { name: /завершить визит/i });
    fireEvent.click(completeBtn);

    await waitFor(() => {
      expect(adminApi.completeBooking).toHaveBeenCalledWith('b-confirmed-2');
    });

    expect(await screen.findByText('Визит успешно завершен')).toBeInTheDocument();
  });

  it('filters bookings by status when status select changes', async () => {
    renderWithProviders(<BookingsModerationTab />);

    await screen.findByText('Михаил Соколов');

    // Find status select
    const statusSelect = screen.getByLabelText('Статус');
    fireEvent.change(statusSelect, { target: { value: 'PENDING' } });

    await waitFor(() => {
      expect(adminApi.getBookings).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'PENDING',
        })
      );
    });
  });
});
