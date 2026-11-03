import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Calendar, Shield, LogOut, Menu, X, Sparkles } from 'lucide-react';
import { useAuth } from '@/shared/context/AuthContext';
import { Button } from './Button';

export interface NavbarProps {
  onOpenAuth?: (tab: 'login' | 'register') => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onOpenAuth }) => {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const isActive = (path: string) => {
    if (path === '/') return location.pathname === '/';
    return location.pathname.startsWith(path);
  };

  const navLinks = [
    { name: 'Каталог услуг', path: '/' },
    ...(isAuthenticated ? [{ name: 'Мои визиты', path: '/cabinet' }] : []),
    ...(isAdmin ? [{ name: 'Администрирование', path: '/admin', icon: Shield }] : []),
  ];

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link
            to="/"
            className="flex items-center gap-2.5 group min-h-[44px] min-w-[44px] py-1 text-slate-900 focus:outline-none"
          >
            <div className="w-10 h-10 rounded-xl bg-teal-600 group-hover:bg-teal-700 transition flex items-center justify-center text-white shadow-sm">
              <Calendar className="w-5 h-5" />
            </div>
            <div>
              <span className="text-lg font-bold tracking-tight block leading-tight group-hover:text-teal-700 transition">
                Сервис Записи
              </span>
              <span className="text-xs text-slate-500 font-normal block leading-none">
                Онлайн-бронирование услуг
              </span>
            </div>
          </Link>

          {/* Desktop Nav */}
          <nav className="hidden md:flex items-center gap-1">
            {navLinks.map((link) => {
              const active = isActive(link.path);
              const Icon = link.icon;
              return (
                <Link
                  key={link.path}
                  to={link.path}
                  className={`inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium transition min-h-[44px] ${
                    active
                      ? 'bg-teal-50 text-teal-700 font-semibold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  {Icon && <Icon className="w-4 h-4 text-teal-600" />}
                  {link.name}
                </Link>
              );
            })}
          </nav>

          {/* Desktop Auth Actions */}
          <div className="hidden md:flex items-center gap-3">
            {isAuthenticated && user ? (
              <div className="flex items-center gap-3">
                <Link
                  to="/cabinet"
                  className="flex items-center gap-2.5 px-3 py-1.5 rounded-xl hover:bg-slate-50 border border-slate-200/80 transition min-h-[44px]"
                >
                  <div className="w-8 h-8 rounded-lg bg-teal-100 text-teal-800 flex items-center justify-center font-bold text-xs">
                    {user.name.charAt(0).toUpperCase()}
                  </div>
                  <div className="text-left">
                    <div className="text-xs font-semibold text-slate-800 leading-tight">
                      {user.name}
                    </div>
                    <div className="text-[10px] text-slate-500 uppercase tracking-wider">
                      {user.role}
                    </div>
                  </div>
                </Link>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={logout}
                  icon={<LogOut className="w-4 h-4" />}
                  aria-label="Выйти из аккаунта"
                >
                  Выйти
                </Button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => (onOpenAuth ? onOpenAuth('login') : (window.location.href = '/login'))}
                >
                  Войти
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  icon={<Sparkles className="w-4 h-4" />}
                  onClick={() => (onOpenAuth ? onOpenAuth('register') : (window.location.href = '/login?tab=register'))}
                >
                  Регистрация
                </Button>
              </div>
            )}
          </div>

          {/* Mobile Menu Button */}
          <div className="flex md:hidden">
            <button
              type="button"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 rounded-xl text-slate-600 hover:text-slate-900 hover:bg-slate-100 min-h-[44px] min-w-[44px] flex items-center justify-center focus:outline-none"
              aria-label="Открыть главное меню"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu Dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-slate-100 bg-white px-4 pt-3 pb-5 space-y-3 shadow-lg">
          <nav className="flex flex-col space-y-1">
            {navLinks.map((link) => {
              const active = isActive(link.path);
              const Icon = link.icon;
              return (
                <Link
                  key={link.path}
                  to={link.path}
                  onClick={() => setMobileMenuOpen(false)}
                  className={`flex items-center gap-2 px-4 py-3 rounded-xl text-base font-medium min-h-[44px] ${
                    active
                      ? 'bg-teal-50 text-teal-700 font-semibold'
                      : 'text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  {Icon && <Icon className="w-5 h-5 text-teal-600" />}
                  {link.name}
                </Link>
              );
            })}
          </nav>

          <div className="pt-2 border-t border-slate-100">
            {isAuthenticated && user ? (
              <div className="space-y-3">
                <div className="flex items-center gap-3 px-3 py-2 bg-slate-50 rounded-xl">
                  <div className="w-10 h-10 rounded-lg bg-teal-100 text-teal-800 flex items-center justify-center font-bold text-sm">
                    {user.name.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <div className="text-sm font-semibold text-slate-900">{user.name}</div>
                    <div className="text-xs text-slate-500">{user.email} • {user.role}</div>
                  </div>
                </div>
                <Button
                  variant="outline"
                  size="md"
                  className="w-full"
                  onClick={() => {
                    setMobileMenuOpen(false);
                    logout();
                  }}
                  icon={<LogOut className="w-4 h-4" />}
                >
                  Выйти из системы
                </Button>
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-2">
                <Button
                  variant="outline"
                  size="md"
                  onClick={() => {
                    setMobileMenuOpen(false);
                    if (onOpenAuth) onOpenAuth('login');
                    else window.location.href = '/login';
                  }}
                >
                  Войти
                </Button>
                <Button
                  variant="primary"
                  size="md"
                  onClick={() => {
                    setMobileMenuOpen(false);
                    if (onOpenAuth) onOpenAuth('register');
                    else window.location.href = '/login?tab=register';
                  }}
                >
                  Регистрация
                </Button>
              </div>
            )}
          </div>
        </div>
      )}
    </header>
  );
};
