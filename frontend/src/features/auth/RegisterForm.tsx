import React, { useState } from 'react';
import { useAuth } from '@/shared/context/AuthContext';
import { Button } from '@/shared/components/Button';
import { Lock, Mail, Phone, User as UserIcon } from 'lucide-react';

export interface RegisterFormProps {
  onSuccess?: () => void;
  onSwitchToLogin?: () => void;
}

export const RegisterForm: React.FC<RegisterFormProps> = ({ onSuccess, onSwitchToLogin }) => {
  const { register } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{
    name?: string;
    email?: string;
    phone?: string;
    password?: string;
  }>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = () => {
    const newErrors: {
      name?: string;
      email?: string;
      phone?: string;
      password?: string;
    } = {};

    if (!name.trim()) {
      newErrors.name = 'Имя обязательно для заполнения';
    } else if (name.trim().length > 100) {
      newErrors.name = 'Имя не должно превышать 100 символов';
    }

    if (!email.trim()) {
      newErrors.email = 'Email обязателен для заполнения';
    } else if (!/\S+@\S+\.\S+/.test(email.trim())) {
      newErrors.email = 'Некорректный формат email';
    }

    const cleanPhone = phone.trim();
    if (!cleanPhone) {
      newErrors.phone = 'Номер телефона обязателен для заполнения';
    } else if (!/^\+?[0-9]{10,15}$/.test(cleanPhone)) {
      newErrors.phone = 'Телефон должен содержать от 10 до 15 цифр (например, +79991234567)';
    }

    if (!password) {
      newErrors.password = 'Пароль обязателен для заполнения';
    } else if (password.length < 8) {
      newErrors.password = 'Пароль должен содержать не менее 8 символов';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    setErrors({});
    try {
      await register({
        name: name.trim(),
        email: email.trim().toLowerCase(),
        phone: phone.trim(),
        password,
      });
      onSuccess?.();
    } catch {
      // Toast handles server error message
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
          Ваше имя
        </label>
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <UserIcon className="w-4 h-4" />
          </div>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Иван Иванов"
            autoComplete="name"
            className={`w-full pl-10 pr-4 py-2.5 bg-slate-50 border rounded-xl text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:bg-white transition min-h-[44px] ${
              errors.name
                ? 'border-rose-300 focus:ring-rose-400'
                : 'border-slate-200 focus:ring-teal-500'
            }`}
          />
        </div>
        {errors.name && <p className="text-xs text-rose-600 mt-1">{errors.name}</p>}
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
          Email
        </label>
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <Mail className="w-4 h-4" />
          </div>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="client@example.com"
            autoComplete="email"
            className={`w-full pl-10 pr-4 py-2.5 bg-slate-50 border rounded-xl text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:bg-white transition min-h-[44px] ${
              errors.email
                ? 'border-rose-300 focus:ring-rose-400'
                : 'border-slate-200 focus:ring-teal-500'
            }`}
          />
        </div>
        {errors.email && <p className="text-xs text-rose-600 mt-1">{errors.email}</p>}
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
          Номер телефона
        </label>
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <Phone className="w-4 h-4" />
          </div>
          <input
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="+79991234567"
            autoComplete="tel"
            className={`w-full pl-10 pr-4 py-2.5 bg-slate-50 border rounded-xl text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:bg-white transition min-h-[44px] ${
              errors.phone
                ? 'border-rose-300 focus:ring-rose-400'
                : 'border-slate-200 focus:ring-teal-500'
            }`}
          />
        </div>
        {errors.phone && <p className="text-xs text-rose-600 mt-1">{errors.phone}</p>}
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
          Пароль (не менее 8 символов)
        </label>
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <Lock className="w-4 h-4" />
          </div>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            autoComplete="new-password"
            className={`w-full pl-10 pr-4 py-2.5 bg-slate-50 border rounded-xl text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:bg-white transition min-h-[44px] ${
              errors.password
                ? 'border-rose-300 focus:ring-rose-400'
                : 'border-slate-200 focus:ring-teal-500'
            }`}
          />
        </div>
        {errors.password && <p className="text-xs text-rose-600 mt-1">{errors.password}</p>}
      </div>

      <Button
        type="submit"
        variant="primary"
        className="w-full mt-2"
        isLoading={isSubmitting}
      >
        Зарегистрироваться
      </Button>

      {onSwitchToLogin && (
        <div className="text-center pt-2 text-sm text-slate-500">
          Уже есть аккаунт?{' '}
          <button
            type="button"
            onClick={onSwitchToLogin}
            className="text-teal-600 font-semibold hover:text-teal-700 hover:underline min-h-[44px] inline-flex items-center"
          >
            Войти
          </button>
        </div>
      )}
    </form>
  );
};
