export interface OverviewResponse {
  repoOverview: string;
  directorySummaries: Record<string, string>;
  fileSummaries: Record<string, string>;
}
