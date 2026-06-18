import axios from 'axios';
import type { OverviewResponse } from '../types';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const uploadAndGetOverview = async (file: File): Promise<OverviewResponse> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axios.post<OverviewResponse>(`${API_BASE}/api/overview`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });

  return response.data;
};
