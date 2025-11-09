import { apiClient } from './client';
import type { ApiResponse } from '../models/ApiResponse';
import type { LoginRequest, CreateUserRequest, AuthResponse } from '../models/User';

export const authApi = {
  login: async (request: LoginRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', request);
    return response.data.data;
  },

  signup: async (request: CreateUserRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/signup', request);
    return response.data.data;
  },
};
