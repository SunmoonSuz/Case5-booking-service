import React, { useState, useEffect } from 'react';
import { Modal } from '@/shared/components/Modal';
import { LoginForm } from './LoginForm';
import { RegisterForm } from './RegisterForm';

export interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialTab?: 'login' | 'register';
  onSuccess?: () => void;
  title?: string;
  subtitle?: string;
}

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  initialTab = 'login',
  onSuccess,
  title,
  subtitle,
}) => {
  const [activeTab, setActiveTab] = useState<'login' | 'register'>(initialTab);

  useEffect(() => {
    setActiveTab(initialTab);
  }, [initialTab, isOpen]);

  const handleSuccess = () => {
    onSuccess?.();
    onClose();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={title || (activeTab === 'login' ? 'Вход в систему' : 'Регистрация')}
      description={
        subtitle ||
        (activeTab === 'login'
          ? 'Войдите для быстрого управления вашими записями'
          : 'Создайте аккаунт, чтобы бронировать услуги онлайн')
      }
      maxWidth="md"
    >
      <div className="flex border-b border-slate-200 mb-6">
        <button
          type="button"
          onClick={() => setActiveTab('login')}
          className={`flex-1 py-3 text-sm font-semibold border-b-2 transition min-h-[44px] ${
            activeTab === 'login'
              ? 'border-teal-600 text-teal-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          Вход
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('register')}
          className={`flex-1 py-3 text-sm font-semibold border-b-2 transition min-h-[44px] ${
            activeTab === 'register'
              ? 'border-teal-600 text-teal-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          Регистрация
        </button>
      </div>

      {activeTab === 'login' ? (
        <LoginForm
          onSuccess={handleSuccess}
          onSwitchToRegister={() => setActiveTab('register')}
        />
      ) : (
        <RegisterForm
          onSuccess={handleSuccess}
          onSwitchToLogin={() => setActiveTab('login')}
        />
      )}
    </Modal>
  );
};
