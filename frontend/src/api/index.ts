import axios from 'axios';
import type { OverviewResponse, QAResponse } from '../types';

const API_BASE = import.meta.env.DEV ? 'http://localhost:8080' : '';

// Start analysis job, returns jobId immediately
export const startOverview = async (file: File): Promise<string> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await axios.post<{ jobId: string }>(`${API_BASE}/api/overview`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data.jobId;
};

// Poll job status
export interface JobStatusResponse {
  status: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';
  step: string;
  result?: OverviewResponse;
  error?: string;
}

export const getJobStatus = async (jobId: string): Promise<JobStatusResponse> => {
  const response = await axios.get<JobStatusResponse>(`${API_BASE}/api/overview/status/${jobId}`);
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
