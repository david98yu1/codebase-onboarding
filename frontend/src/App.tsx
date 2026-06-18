import { useState } from 'react';
import UploadSection from './components/UploadSection';
import OverviewSection from './components/OverviewSection';
import { uploadAndGetOverview } from './api';
import type { OverviewResponse } from './types';
import './App.css';

function App() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<OverviewResponse | null>(null);

  const handleUpload = async (file: File) => {
    setLoading(true);
    setError(null);
    setData(null);
    try {
      const result = await uploadAndGetOverview(file);
      setData(result);
    } catch (e: any) {
      setError(e.response?.data?.error || e.message || 'Something went wrong');
    } finally {
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
            <p style={styles.loadingText}>Analyzing your repository... this may take a moment</p>
          </div>
        )}

        {error && (
          <div style={styles.errorBox}>
            <strong>Error:</strong> {error}
          </div>
        )}

        {data && <OverviewSection data={data} />}
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
  loadingText: { color: '#555', fontSize: '14px' },
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
};

export default App;
