import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ScheduleTab } from '@/features/admin/ScheduleTab';
import { ToastProvider } from '@/shared/context/ToastContext';
import { adminApi } from '@/shared/api/adminApi';

vi.mock('@/shared/api/adminApi');

const mockMasters = [
  {
    id: 'm-1',
    name: 'Мария Иванова',
    description: 'Мастер ногтевого сервиса',
    isActive: true,
    services: [],
  },
];

const mockSchedule = [
  {
    dayOfWeek: 1, // Monday
    startTime: '09:00',
    endTime: '13:00',
  },
  {
    dayOfWeek: 1, // Monday (lunch break afternoon shift)
    startTime: '14:00',
    endTime: '18:00',
  },
];

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>{ui}</ToastProvider>
    </QueryClientProvider>
  );
}

describe('ScheduleTab (Admin schedule management & validation)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(adminApi.getAllMasters).mockResolvedValue(mockMasters);
    vi.mocked(adminApi.getMasterSchedule).mockResolvedValue(mockSchedule);
  });

  it('renders days of week and master shifts', async () => {
    renderWithProviders(<ScheduleTab />);

    expect(await screen.findByText('Понедельник')).toBeInTheDocument();
    expect(screen.getByText('Вторник')).toBeInTheDocument();
    expect(screen.getByText('Среда')).toBeInTheDocument();

    // Monday should have 2 intervals
    expect(await screen.findByText(/Рабочий день \(2 смен\/интервалов\)/i)).toBeInTheDocument();
  });

  it('validates start < end interval constraint and blocks saving if invalid', async () => {
    renderWithProviders(<ScheduleTab />);
    expect(await screen.findByDisplayValue('09:00')).toBeInTheDocument();

    // Find time inputs
    const inputs = screen.getAllByDisplayValue(/09:00|13:00|14:00|18:00/);
    expect(inputs.length).toBeGreaterThan(0);

    // Change first start time to 15:00 while end time is 13:00 (15:00 > 13:00)
    fireEvent.change(inputs[0], { target: { value: '15:00' } });

    // Click "Сохранить график"
    const saveButton = screen.getByRole('button', { name: /сохранить график/i });
    fireEvent.click(saveButton);

    // Validation error must appear
    expect(
      await screen.findByText(/должно быть раньше окончания/i)
    ).toBeInTheDocument();

    // adminApi.setMasterSchedule should NOT be called
    expect(adminApi.setMasterSchedule).not.toHaveBeenCalled();
  });
});
