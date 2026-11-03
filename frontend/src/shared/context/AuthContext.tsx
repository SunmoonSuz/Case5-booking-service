import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { User, LoginRequest, RegisterRequest } from '@/types/api';
import { authApi } from '@/shared/api/authApi';
import { useToast } from './ToastContext';
import { getErrorMessage } from '@/shared/api/client';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (data: LoginRequest) => Promise<User>;
  register: (data: RegisterRequest) => Promise<User>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const { success, error: toastError } = useToast();

  const refreshUser = useCallback(async () => {
    try {
      const currentUser = await authApi.getCurrentUser();
      setUser(currentUser);
    } catch {
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  // Subscribe to 401 unauthorized events to clear stale session state
  useEffect(() => {
    const handleUnauthorized = () => {
      setUser(null);
    };
    if (typeof window !== 'undefined') {
      window.addEventListener('auth:unauthorized', handleUnauthorized);
      return () => {
        window.removeEventListener('auth:unauthorized', handleUnauthorized);
      };
    }
  }, []);

  const login = useCallback(
    async (data: LoginRequest): Promise<User> => {
      try {
        const loggedInUser = await authApi.login(data);
        setUser(loggedInUser);
        success(`Добро пожаловать, ${loggedInUser.name}!`);
        return loggedInUser;
      } catch (err) {
        const msg = getErrorMessage(err, 'Ошибка при авторизации');
        toastError(msg);
        throw err;
      }
    },
    [success, toastError]
  );

  const register = useCallback(
    async (data: RegisterRequest): Promise<User> => {
      try {
        const newUser = await authApi.register(data);
        setUser(newUser);
        success(`Регистрация успешна! Добро пожаловать, ${newUser.name}!`);
        return newUser;
      } catch (err) {
        const msg = getErrorMessage(err, 'Ошибка при регистрации');
        toastError(msg);
        throw err;
      }
    },
    [success, toastError]
  );

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
      setUser(null);
      success('Вы успешно вышли из системы');
    } catch (err) {
      const msg = getErrorMessage(err, 'Ошибка при выходе из системы');
      toastError(msg);
    }
  }, [success, toastError]);

  const isAuthenticated = !!user;
  const isAdmin = user?.role === 'ADMIN';

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        isAuthenticated,
        isAdmin,
        login,
        register,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
