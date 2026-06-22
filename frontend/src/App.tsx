import { useState, useEffect, useRef } from 'react';
import UploadSection from './components/UploadSection';
import OverviewSection from './components/OverviewSection';
import QASection from './components/QASection';
import { startOverview, getJobStatus } from './api';
import type { OverviewResponse } from './types';
import './App.css';

function App() {
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<OverviewResponse | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'qa'>('overview');
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const stopPolling = () => {
    if (pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }
  };

  useEffect(() => () => stopPolling(), []);

  const handleUpload = async (file: File) => {
    setLoading(true);
    setError(null);
    setData(null);
    setStep('Uploading...');

    try {
      const jobId = await startOverview(file);

      // Poll every 3 seconds
      pollRef.current = setInterval(async () => {
        try {
          const status = await getJobStatus(jobId);
          setStep(status.step);

          if (status.status === 'DONE' && status.result) {
            stopPolling();
            setData(status.result);
            setLoading(false);
            setActiveTab('overview');
          } else if (status.status === 'FAILED') {
            stopPolling();
            setError(status.error || 'Analysis failed');
            setLoading(false);
          }
        } catch (e: any) {
          stopPolling();
          setError('Lost connection to server');
          setLoading(false);
        }
      }, 3000);

    } catch (e: any) {
      setError(e.response?.data?.error || e.message || 'Upload failed');
      setLoading(false);
    }
  };

  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <h1 style={styles.title}>🔍 Codebase Onboarding Assistant</h1>
        <p style={styles.subtitle}>Upload a repository ZIP to get an AI-powered overview</p>
      </header>

      <main style={styles.main}>
        <UploadSection onUpload={handleUpload} loading={loading} />

        {loading && (
          <div style={styles.loadingBox}>
            <div style={styles.spinner} />
            <p style={styles.loadingText}>{step}</p>
          </div>
        )}

        {error && (
          <div style={styles.errorBox}>
            <strong>Error:</strong> {error}
          </div>
        )}

        {data && (
          <>
            <div style={styles.tabs}>
              <button
                style={{ ...styles.tab, ...(activeTab === 'overview' ? styles.tabActive : {}) }}
                onClick={() => setActiveTab('overview')}
              >
                🗺️ Overview
              </button>
              <button
                style={{ ...styles.tab, ...(activeTab === 'qa' ? styles.tabActive : {}) }}
                onClick={() => setActiveTab('qa')}
              >
                💬 Ask Questions
              </button>
            </div>

            {activeTab === 'overview' && <OverviewSection data={data} />}
            {activeTab === 'qa' && <QASection repoId={data.repoId} />}
          </>
        )}
      </main>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh',
    backgroundColor: '#f0f4ff',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
  },
  header: {
    backgroundColor: '#1a1a2e',
    color: '#fff',
    padding: '32px 24px',
    textAlign: 'center',
  },
  title: { margin: '0 0 8px 0', fontSize: '28px', fontWeight: 700 },
  subtitle: { margin: 0, fontSize: '15px', color: '#aab4cc' },
  main: {
    maxWidth: '900px',
    margin: '40px auto',
    padding: '0 24px',
    display: 'flex',
    flexDirection: 'column',
    gap: '32px',
    alignItems: 'center',
  },
  loadingBox: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '16px',
    padding: '32px',
  },
  spinner: {
    width: '40px',
    height: '40px',
    border: '4px solid #d0d9f0',
    borderTop: '4px solid #4A90E2',
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
  },
  loadingText: { color: '#555', fontSize: '14px', fontWeight: 500 },
  errorBox: {
    backgroundColor: '#fff0f0',
    border: '1px solid #f5c6cb',
    borderRadius: '8px',
    padding: '16px 24px',
    color: '#c0392b',
    fontSize: '14px',
    width: '100%',
    maxWidth: '600px',
  },
  tabs: {
    display: 'flex',
    gap: '8px',
    width: '100%',
  },
  tab: {
    padding: '10px 24px',
    borderRadius: '8px',
    border: '2px solid #dde',
    background: '#fff',
    fontSize: '14px',
    fontWeight: 600,
    cursor: 'pointer',
    color: '#666',
  },
  tabActive: {
    backgroundColor: '#4A90E2',
    borderColor: '#4A90E2',
    color: '#fff',
  },
};

export default App;
