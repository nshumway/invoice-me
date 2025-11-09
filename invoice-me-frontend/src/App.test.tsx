import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from './App';

describe('App', () => {
  it('renders without crashing', () => {
    render(<App />);
    expect(document.body).toBeTruthy();
  });

  it('displays the app title', () => {
    render(<App />);
    const headingElement = screen.getByRole('heading', { level: 1 });
    expect(headingElement).toBeInTheDocument();
    expect(headingElement).toHaveTextContent('InvoiceMe');
  });

  it('contains the foundation message', () => {
    render(<App />);
    expect(screen.getByText(/Foundation setup complete/i)).toBeInTheDocument();
  });
});
