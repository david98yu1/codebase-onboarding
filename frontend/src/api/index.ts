import axios from 'axios';
import type { OverviewResponse, QAResponse } from '../types';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const uploadAndGetOverview = async (file: File): Promise<OverviewResponse> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axios.post<OverviewResponse>(`${API_BASE}/api/overview`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });

  return response.data;
};

export const askQuestion = async (repoId: string, question: string): Promise<QAResponse> => {
  const response = await axios.post<QAResponse>(
    `${API_BASE}/api/qa`,
    null,
    { params: { repoId, question } }
  );
  return response.data;
};
