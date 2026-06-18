import React, { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import type { OverviewResponse } from '../types';

interface Props {
  data: OverviewResponse;
}

const OverviewSection: React.FC<Props> = ({ data }) => {
  const [expandedDir, setExpandedDir] = useState<string | null>(null);
  const [expandedFile, setExpandedFile] = useState<string | null>(null);

  return (
    <div style={styles.container}>

      {/* Repo Overview */}
      <section style={styles.card}>
        <h2 style={styles.cardTitle}>🗺️ Repository Overview</h2>
        <div style={styles.markdownBody}>
          <ReactMarkdown>{data.repoOverview}</ReactMarkdown>
        </div>
      </section>

      {/* Directory Summaries */}
      <section style={styles.card}>
        <h2 style={styles.cardTitle}>📂 Module Summaries</h2>
        {Object.entries(data.directorySummaries).map(([dir, summary]) => (
          <div key={dir} style={styles.accordionItem}>
            <button
              style={styles.accordionHeader}
              onClick={() => setExpandedDir(expandedDir === dir ? null : dir)}
            >
              <span>📁 {dir}</span>
              <span>{expandedDir === dir ? '▲' : '▼'}</span>
            </button>
            {expandedDir === dir && (
              <div style={styles.accordionBody}>
                <ReactMarkdown>{summary}</ReactMarkdown>
              </div>
            )}
          </div>
        ))}
      </section>

      {/* File Summaries */}
      <section style={styles.card}>
        <h2 style={styles.cardTitle}>📄 File Summaries</h2>
        {Object.entries(data.fileSummaries).map(([file, summary]) => (
          <div key={file} style={styles.accordionItem}>
            <button
              style={styles.accordionHeader}
              onClick={() => setExpandedFile(expandedFile === file ? null : file)}
            >
              <span style={styles.filePath}>{file}</span>
              <span>{expandedFile === file ? '▲' : '▼'}</span>
            </button>
            {expandedFile === file && (
              <div style={styles.accordionBody}>
                <ReactMarkdown>{summary}</ReactMarkdown>
              </div>
            )}
          </div>
        ))}
      </section>

    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    gap: '24px',
    width: '100%',
    maxWidth: '860px',
    margin: '0 auto',
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '24px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
  },
  cardTitle: {
    fontSize: '18px',
    fontWeight: 700,
    marginBottom: '16px',
    color: '#1a1a2e',
  },
  markdownBody: { fontSize: '14px', lineHeight: '1.7', color: '#444' },
  accordionItem: { borderBottom: '1px solid #f0f0f0', marginBottom: '4px' },
  accordionHeader: {
    width: '100%',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '12px 8px',
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: 600,
    color: '#333',
    textAlign: 'left',
  },
  accordionBody: {
    padding: '8px 16px 16px',
    fontSize: '13px',
    lineHeight: '1.7',
    color: '#555',
    backgroundColor: '#fafafa',
    borderRadius: '6px',
  },
  filePath: { fontFamily: 'monospace', fontSize: '13px', color: '#4A90E2' },
};

export default OverviewSection;
