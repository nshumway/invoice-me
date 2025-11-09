export interface Customer {
  // Entity ID and audit fields
  id: string;
  createdAt: string;
  createdBy: string;
  lastModifiedAt: string;
  lastModifiedBy: string;
  version: number;

  // Customer fields
  companyName: string;
  contactFirstName: string | null;
  contactLastName: string | null;
  email: string;
  phone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  zipCode: string | null;
  country: string | null;

  // Read-only computed fields
  draftInvoiceCount: number;
  sentInvoiceCount: number;
  paidInvoiceCount: number;
  totalOutstanding: string; // BigDecimal as string
}

export interface CustomerListItem {
  id: string;
  companyName: string;
  email: string;
  totalOutstanding: string;
}

export interface CreateCustomerRequest {
  companyName: string;
  contactFirstName?: string;
  contactLastName?: string;
  email: string;
  phone?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
}

export interface UpdateCustomerRequest {
  id: string;
  version: number;
  companyName: string;
  contactFirstName?: string;
  contactLastName?: string;
  email: string;
  phone?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
}

export interface DeleteCustomerRequest {
  id: string;
  version: number;
}
