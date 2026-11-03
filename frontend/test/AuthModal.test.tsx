import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthModal } from '@/features/auth/AuthModal';
import { ToastProvider } from '@/shared/context/ToastContext';
import { AuthProvider } from '@/shared/context/AuthContext';
import { authApi } from '@/shared/api/authApi';

vi.mock('@/shared/api/authApi');

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>{ui}</AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  );
}

describe('AuthModal (AC-001: Login, Registration & Validation)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authApi.getCurrentUser).mockRejectedValue(new Error('Unauthorized'));
  });

  it('renders login form by default and allows switching to registration tab', async () => {
    renderWithProviders(<AuthModal isOpen={true} onClose={() => {}} />);

    // Login tab is active
    expect(screen.getByRole('button', { name: /^войти$/i })).toBeInTheDocument();
    expect(screen.queryByPlaceholderText('Иван Иванов')).not.toBeInTheDocument();

    // Click "Регистрация" tab
    const registerTab = screen.getByRole('button', { name: 'Регистрация' });
    fireEvent.click(registerTab);

    // Registration inputs are now visible
    expect(await screen.findByPlaceholderText('Иван Иванов')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('+79991234567')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /зарегистрироваться/i })).toBeInTheDocument();
  });

  it('validates required fields and password length (>= 8 chars) on registration', async () => {
    renderWithProviders(<AuthModal isOpen={true} initialTab="register" onClose={() => {}} />);

    const submitBtn = await screen.findByRole('button', { name: /зарегистрироваться/i });
    fireEvent.click(submitBtn);

    // Validation errors should appear
    expect(await screen.findByText('Имя обязательно для заполнения')).toBeInTheDocument();
    expect(screen.getByText('Email обязателен для заполнения')).toBeInTheDocument();
    expect(screen.getByText('Номер телефона обязателен для заполнения')).toBeInTheDocument();
    expect(screen.getByText('Пароль обязателен для заполнения')).toBeInTheDocument();

    // Fill short password (< 8 chars)
    fireEvent.change(screen.getByPlaceholderText('••••••••'), { target: { value: 'short' } });
    fireEvent.click(submitBtn);

    expect(await screen.findByText('Пароль должен содержать не менее 8 символов')).toBeInTheDocument();
  });

  it('calls authApi.register on valid registration and triggers onSuccess', async () => {
    const handleSuccess = vi.fn();
    vi.mocked(authApi.register).mockResolvedValueOnce({
      id: 'new-u-1',
      name: 'Елена Кузнецова',
      email: 'elena@example.com',
      phone: '+79997654321',
      role: 'CLIENT',
    });

    renderWithProviders(
      <AuthModal isOpen={true} initialTab="register" onClose={() => {}} onSuccess={handleSuccess} />
    );

    fireEvent.change(await screen.findByPlaceholderText('Иван Иванов'), {
      target: { value: 'Елена Кузнецова' },
    });
    fireEvent.change(screen.getByPlaceholderText('client@example.com'), {
      target: { value: 'elena@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('+79991234567'), {
      target: { value: '+79997654321' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'Secret123!' },
    });

    const submitBtn = screen.getByRole('button', { name: /зарегистрироваться/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(authApi.register).toHaveBeenCalledWith({
        name: 'Елена Кузнецова',
        email: 'elena@example.com',
        phone: '+79997654321',
        password: 'Secret123!',
      });
      expect(handleSuccess).toHaveBeenCalled();
    });
  });

  it('handles duplicate email HTTP 409 conflict error on registration', async () => {
    vi.mocked(authApi.register).mockRejectedValueOnce({
      response: {
        status: 409,
        data: {
          error: 'EMAIL_ALREADY_EXISTS',
          message: 'Пользователь с таким email уже существует.',
        },
      },
    });

    renderWithProviders(<AuthModal isOpen={true} initialTab="register" onClose={() => {}} />);

    fireEvent.change(await screen.findByPlaceholderText('Иван Иванов'), {
      target: { value: 'Тест Пользователь' },
    });
    fireEvent.change(screen.getByPlaceholderText('client@example.com'), {
      target: { value: 'duplicate@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('+79991234567'), {
      target: { value: '+79991112233' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'StrongPass123!' },
    });

    const submitBtn = screen.getByRole('button', { name: /зарегистрироваться/i });
    fireEvent.click(submitBtn);

    // Toast with error message should be displayed
    expect(
      await screen.findByText('Пользователь с таким email уже существует.')
    ).toBeInTheDocument();
  });

  it('calls authApi.login on valid login submission', async () => {
    const handleSuccess = vi.fn();
    vi.mocked(authApi.login).mockResolvedValueOnce({
      id: 'u-1',
      name: 'Иван',
      email: 'ivan@example.com',
      phone: '+79991112233',
      role: 'CLIENT',
    });

    renderWithProviders(<AuthModal isOpen={true} initialTab="login" onClose={() => {}} onSuccess={handleSuccess} />);

    fireEvent.change(screen.getByPlaceholderText('client@example.com'), {
      target: { value: 'ivan@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'MySecretPassword' },
    });

    const loginBtn = screen.getByRole('button', { name: /^войти$/i });
    fireEvent.click(loginBtn);

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({
        email: 'ivan@example.com',
        password: 'MySecretPassword',
      });
      expect(handleSuccess).toHaveBeenCalled();
    });
  });
});
