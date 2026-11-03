import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { ClientCabinetPage } from '@/features/cabinet/ClientCabinetPage';
import { ToastProvider } from '@/shared/context/ToastContext';
import { bookingApi } from '@/shared/api/bookingApi';

vi.mock('@/shared/api/bookingApi');

const futureDate = new Date(Date.now() + 24 * 3600 * 1000).toISOString();
const pastDate = new Date(Date.now() - 24 * 3600 * 1000).toISOString();

const mockBookingsData = {
  active: [
    {
      id: 'b-active-1',
      clientId: 'u-1',
      clientName: 'Иван',
      clientPhone: '+79991112233',
      clientEmail: 'ivan@test.ru',
      masterId: 'm-1',
      masterName: 'Алексей Смирнов',
      serviceId: 's-1',
      serviceName: 'Мужская стрижка',
      servicePrice: 1500,
      startTime: futureDate,
      endTime: futureDate,
      status: 'PENDING' as const,
      createdAt: pastDate,
      updatedAt: pastDate,
    },
  ],
  archive: [
    {
      id: 'b-arch-1',
      clientId: 'u-1',
      clientName: 'Иван',
      clientPhone: '+79991112233',
      clientEmail: 'ivan@test.ru',
      masterId: 'm-1',
      masterName: 'Алексей Смирнов',
      serviceId: 's-1',
      serviceName: 'Мужская стрижка',
      servicePrice: 1500,
      startTime: pastDate,
      endTime: pastDate,
      status: 'COMPLETED' as const,
      createdAt: pastDate,
      updatedAt: pastDate,
    },
  ],
};

function renderWithProviders(ui: React.ReactElement, queryClient = new QueryClient()) {
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <MemoryRouter>{ui}</MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>
  );
}

describe('ClientCabinetPage (AC-005)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(bookingApi.getClientBookings).mockResolvedValue(mockBookingsData);
  });

  it('renders active visits and allows canceling an upcoming booking', async () => {
    vi.mocked(bookingApi.cancelClientBooking).mockResolvedValueOnce({
      ...mockBookingsData.active[0],
      status: 'CANCELLED',
    });

    renderWithProviders(<ClientCabinetPage />);

    expect(await screen.findByText('Мужская стрижка')).toBeInTheDocument();
    expect(screen.getByText('Ожидает подтверждения')).toBeInTheDocument();

    // Click "Отменить запись"
    const cancelBtn = screen.getByRole('button', { name: /отменить запись/i });
    fireEvent.click(cancelBtn);

    // Modal appears
    expect(await screen.findByText(/Вы уверены, что хотите отменить запись\?/i)).toBeInTheDocument();

    // Confirm cancel
    const confirmBtn = screen.getByRole('button', { name: /да, отменить/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(bookingApi.cancelClientBooking).toHaveBeenCalledWith('b-active-1');
    });
  });

  it('switches to archive tab and displays past visits', async () => {
    renderWithProviders(<ClientCabinetPage />);

    const archiveTab = await screen.findByRole('button', { name: /история визитов/i });
    fireEvent.click(archiveTab);

    expect(await screen.findByText('Завершено')).toBeInTheDocument();
  });
});
