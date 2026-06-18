export interface OverviewResponse {
  repoId: string;
  repoOverview: string;
  directorySummaries: Record<string, string>;
  fileSummaries: Record<string, string>;
}

export interface QAResponse {
  answer: string;
  sources: string[];
}

export interface Message {
  role: 'user' | 'assistant';
  content: string;
  sources?: string[];
}
