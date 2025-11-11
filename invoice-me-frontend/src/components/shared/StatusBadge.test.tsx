import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatusBadge } from './StatusBadge';

describe('StatusBadge', () => {
  it('renders DRAFT status correctly', () => {
    render(<StatusBadge status="DRAFT" />);
    expect(screen.getByText('DRAFT')).toBeInTheDocument();
  });

  it('renders SENT status with primary variant', () => {
    const { container } = render(<StatusBadge status="SENT" />);
    expect(screen.getByText('SENT')).toBeInTheDocument();
    expect(container.firstChild).toHaveClass('bg-primary-600/50');
  });

  it('renders PAID status with success variant', () => {
    const { container } = render(<StatusBadge status="PAID" />);
    expect(screen.getByText('PAID')).toBeInTheDocument();
    expect(container.firstChild).toHaveClass('bg-success-600/50');
  });

  it('renders OVERDUE status with error variant', () => {
    const { container } = render(<StatusBadge status="OVERDUE" />);
    expect(screen.getByText('OVERDUE')).toBeInTheDocument();
    expect(container.firstChild).toHaveClass('bg-error-600/50');
  });

  it('renders CANCELLED status with neutral variant', () => {
    const { container } = render(<StatusBadge status="CANCELLED" />);
    expect(screen.getByText('CANCELLED')).toBeInTheDocument();
    expect(container.firstChild).toHaveClass('bg-gray-600/50');
  });

  it('applies custom className', () => {
    const { container } = render(<StatusBadge status="PAID" className="ml-4" />);
    expect(container.firstChild).toHaveClass('ml-4');
  });
});
