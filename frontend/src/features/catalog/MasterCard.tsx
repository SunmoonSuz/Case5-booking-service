import React from 'react';
import { MasterDto } from '@/types/api';
import { Check, Scissors } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';

export interface MasterCardProps {
  master: MasterDto;
  isSelected?: boolean;
  onSelect: (master: MasterDto) => void;
  selectedServiceId?: string;
}

export const MasterCard: React.FC<MasterCardProps> = ({
  master,
  isSelected = false,
  onSelect,
  selectedServiceId,
}) => {
  const offersSelectedService = selectedServiceId
    ? master.services.some((s) => s.id === selectedServiceId)
    : true;

  return (
    <div
      onClick={() => onSelect(master)}
      className={`group cursor-pointer rounded-2xl border p-5 transition-all duration-200 flex flex-col justify-between ${
        isSelected
          ? 'bg-teal-50/60 border-teal-500 shadow-md ring-2 ring-teal-500/20'
          : 'bg-white border-slate-200 hover:border-teal-300 hover:shadow-md'
      }`}
    >
      <div>
        <div className="flex items-center gap-3.5 mb-3">
          <div className="w-12 h-12 rounded-2xl bg-teal-100 text-teal-800 flex items-center justify-center font-bold text-lg border border-teal-200/60 shadow-sm flex-shrink-0">
            {master.name.charAt(0).toUpperCase()}
          </div>
          <div>
            <h3 className="text-base sm:text-lg font-bold text-slate-900 group-hover:text-teal-700 transition">
              {master.name}
            </h3>
            <span className="text-xs text-slate-500 font-medium">Специалист</span>
          </div>
        </div>

        {master.description && (
          <p className="text-xs sm:text-sm text-slate-500 line-clamp-2 mb-3 leading-relaxed">
            {master.description}
          </p>
        )}

        {master.services.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mb-2">
            {master.services.slice(0, 3).map((service) => (
              <Badge
                key={service.id}
                size="sm"
                variant={service.id === selectedServiceId ? 'default' : 'neutral'}
              >
                {service.name}
              </Badge>
            ))}
            {master.services.length > 3 && (
              <span className="text-[11px] text-slate-400 self-center">
                +{master.services.length - 3} ещё
              </span>
            )}
          </div>
        )}
      </div>

      <div className="flex items-center justify-between pt-4 border-t border-slate-100 mt-2">
        <div className="text-xs text-slate-500">
          {offersSelectedService ? (
            <span className="text-emerald-700 font-medium flex items-center gap-1">
              <Scissors className="w-3.5 h-3.5" /> Оказывает услугу
            </span>
          ) : (
            <span className="text-slate-400">Другие услуги</span>
          )}
        </div>

        <Button
          type="button"
          size="sm"
          variant={isSelected ? 'primary' : 'outline'}
          className="min-h-[44px]"
          onClick={(e) => {
            e.stopPropagation();
            onSelect(master);
          }}
          icon={isSelected ? <Check className="w-4 h-4" /> : undefined}
        >
          {isSelected ? 'Выбран' : 'Выбрать'}
        </Button>
      </div>
    </div>
  );
};
