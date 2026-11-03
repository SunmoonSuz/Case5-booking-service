import React from 'react';
import { ServiceDto } from '@/types/api';
import { formatPrice } from '@/shared/utils/formatters';
import { Clock, Check } from 'lucide-react';
import { Button } from '@/shared/components/Button';

export interface ServiceCardProps {
  service: ServiceDto;
  isSelected?: boolean;
  onSelect: (service: ServiceDto) => void;
}

export const ServiceCard: React.FC<ServiceCardProps> = ({
  service,
  isSelected = false,
  onSelect,
}) => {
  return (
    <div
      onClick={() => onSelect(service)}
      className={`group cursor-pointer rounded-2xl border p-5 transition-all duration-200 flex flex-col justify-between ${
        isSelected
          ? 'bg-teal-50/60 border-teal-500 shadow-md ring-2 ring-teal-500/20'
          : 'bg-white border-slate-200 hover:border-teal-300 hover:shadow-md'
      }`}
    >
      <div>
        <div className="flex items-start justify-between gap-3 mb-2">
          <h3 className="text-base sm:text-lg font-bold text-slate-900 group-hover:text-teal-700 transition">
            {service.name}
          </h3>
          <div className="text-right flex-shrink-0">
            <span className="text-base sm:text-lg font-extrabold text-teal-700">
              {formatPrice(service.price)}
            </span>
          </div>
        </div>

        {service.description && (
          <p className="text-xs sm:text-sm text-slate-500 line-clamp-2 mb-4 leading-relaxed">
            {service.description}
          </p>
        )}
      </div>

      <div className="flex items-center justify-between pt-4 border-t border-slate-100 mt-2">
        <div className="flex items-center gap-1.5 text-xs text-slate-500 font-medium">
          <Clock className="w-3.5 h-3.5 text-slate-400" />
          <span>{service.durationMinutes} мин</span>
        </div>

        <Button
          type="button"
          size="sm"
          variant={isSelected ? 'primary' : 'outline'}
          className="min-h-[44px]"
          onClick={(e) => {
            e.stopPropagation();
            onSelect(service);
          }}
          icon={isSelected ? <Check className="w-4 h-4" /> : undefined}
        >
          {isSelected ? 'Выбрано' : 'Выбрать'}
        </Button>
      </div>
    </div>
  );
};
