import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { BookingFlow } from '@/features/booking/BookingFlow';
import { ToastProvider } from '@/shared/context/ToastContext';
import { AuthProvider } from '@/shared/context/AuthContext';
import { catalogApi } from '@/shared/api/catalogApi';
import { bookingApi } from '@/shared/api/bookingApi';
import { authApi } from '@/shared/api/authApi';

vi.mock('@/shared/api/catalogApi');
vi.mock('@/shared/api/bookingApi');
vi.mock('@/shared/api/authApi');

const mockServices = [
  {
    id: 's-1',
    name: 'Мужская стрижка',
    description: 'Классическая стрижка',
    durationMinutes: 60,
    price: 1500,
    isActive: true,
  },
  {
    id: 's-2',
    name: 'Оформление бороды',
    description: 'Моделирование бороды',
    durationMinutes: 30,
    price: 800,
    isActive: true,
  },
];

const mockMasters = [
  {
    id: 'm-1',
    name: 'Алексей Смирнов',
    description: 'Барбер эксперт',
    isActive: true,
    services: [mockServices[0]],
  },
];

const mockSlots = [
  {
    startTime: '2026-11-10T08:00:00Z',
    endTime: '2026-11-10T09:00:00Z',
    formattedTime: '11:00 – 12:00',
    masterId: 'm-1',
    masterName: 'Алексей Смирнов',
    serviceId: 's-1',
  },
  {
    startTime: '2026-11-10T09:00:00Z',
    endTime: '2026-11-10T10:00:00Z',
    formattedTime: '12:00 – 13:00',
    masterId: 'm-1',
    masterName: 'Алексей Смирнов',
    serviceId: 's-1',
  },
];

const mockUser = {
  id: 'u-1',
  name: 'Иван Тестов',
  email: 'ivan@test.ru',
  phone: '+79991234567',
  role: 'CLIENT' as const,
};

function renderWithProviders(ui: React.ReactElement, queryClient = new QueryClient()) {
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>
          <MemoryRouter>{ui}</MemoryRouter>
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  );
}

describe('BookingFlow (AC-002, AC-004)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(catalogApi.getActiveServices).mockResolvedValue(mockServices);
    vi.mocked(catalogApi.getActiveMasters).mockResolvedValue(mockMasters);
    vi.mocked(catalogApi.getAvailability).mockResolvedValue(mockSlots);
    vi.mocked(authApi.getCurrentUser).mockResolvedValue(mockUser);
  });

  it('renders services catalog and masters list', async () => {
    renderWithProviders(<BookingFlow />);

    expect(await screen.findByText('Мужская стрижка')).toBeInTheDocument();
    expect(screen.getByText('Оформление бороды')).toBeInTheDocument();

    expect(await screen.findByText('Алексей Смирнов')).toBeInTheDocument();
    expect(await screen.findByText('11:00 – 12:00')).toBeInTheDocument();
  });

  it('AC-004: intercepts HTTP 409 SLOT_ALREADY_BOOKED, displays Toast alert, and invalidates slots query cache', async () => {
    const queryClient = new QueryClient();
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    // Simulate 409 conflict error when booking
    vi.mocked(bookingApi.createBooking).mockRejectedValueOnce({
      response: {
        status: 409,
        data: {
          error: 'SLOT_ALREADY_BOOKED',
          message: 'Выбранный слот уже занят другим клиентом. Пожалуйста, выберите другое время.',
        },
      },
    });

    renderWithProviders(<BookingFlow />, queryClient);

    // Click on available slot
    const slotButton = await screen.findByText('11:00 – 12:00');
    fireEvent.click(slotButton);

    // Confirmation modal should appear
    expect(await screen.findByText('Подтверждение записи')).toBeInTheDocument();

    // Click "Записаться" inside modal
    const confirmButton = screen.getByRole('button', { name: /записаться/i });
    fireEvent.click(confirmButton);

    // Check Toast notification is shown
    expect(
      await screen.findByText(
        'Выбранный слот уже занят другим клиентом. Пожалуйста, выберите другое время.'
      )
    ).toBeInTheDocument();

    // Check queryClient.invalidateQueries was invoked for availability
    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          queryKey: expect.arrayContaining(['availability']),
        })
      );
    });
  });
});
