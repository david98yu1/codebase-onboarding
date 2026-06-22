import React from 'react';

interface Props {
  step: string;
}

const STEPS = [
  { key: 'Uploading',       label: 'Uploading file' },
  { key: 'Extracting',      label: 'Extracting files' },
  { key: 'Filtering',       label: 'Filtering irrelevant files' },
  { key: 'Chunking',        label: 'Chunking files for Q&A' },
  { key: 'skeleton',        label: 'Extracting code skeletons' },
  { key: 'Summarizing',     label: 'Summarizing files' },
  { key: 'module',          label: 'Generating module summaries' },
  { key: 'repository',      label: 'Generating repository overview' },
  { key: 'Done',            label: 'Analysis complete' },
];

function getCurrentIndex(step: string): number {
  const lower = step.toLowerCase();
  for (let i = STEPS.length - 1; i >= 0; i--) {
    if (lower.includes(STEPS[i].key.toLowerCase())) return i;
  }
  return 0;
}

const ProgressSection: React.FC<Props> = ({ step }) => {
  const currentIndex = getCurrentIndex(step);
  const progressPercent = Math.round(((currentIndex + 1) / STEPS.length) * 100);

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <span style={styles.title}>🔄 Analyzing Repository</span>
        <span style={styles.percent}>{progressPercent}%</span>
      </div>

      {/* Progress bar */}
      <div style={styles.barTrack}>
        <div style={{ ...styles.barFill, width: `${progressPercent}%` }} />
      </div>

      {/* Current step text */}
      <p style={styles.currentStep}>{step}</p>

      {/* Step list */}
      <div style={styles.stepList}>
        {STEPS.map((s, i) => {
          const isDone   = i < currentIndex;
          const isActive = i === currentIndex;

          return (
            <div key={s.key} style={styles.stepRow}>
              <div style={{
                ...styles.stepDot,
                backgroundColor: isDone ? '#4CAF50' : isActive ? '#4A90E2' : '#dde',
              }}>
                {isDone ? '✓' : isActive ? '●' : ''}
              </div>
              <span style={{
                ...styles.stepLabel,
                color: isDone ? '#4CAF50' : isActive ? '#4A90E2' : '#aaa',
                fontWeight: isActive ? 600 : 400,
              }}>
                {s.label}
              </span>
              {isActive && <span style={styles.spinner} />}
            </div>
          );
        })}
      </div>
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  container: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '24px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
    width: '100%',
    maxWidth: '600px',
    display: 'flex',
    flexDirection: 'column',
    gap: '14px',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    fontSize: '16px',
    fontWeight: 700,
    color: '#1a1a2e',
  },
  percent: {
    fontSize: '15px',
    fontWeight: 700,
    color: '#4A90E2',
  },
  barTrack: {
    width: '100%',
    height: '8px',
    backgroundColor: '#e8edf5',
    borderRadius: '99px',
    overflow: 'hidden',
  },
  barFill: {
    height: '100%',
    backgroundColor: '#4A90E2',
    borderRadius: '99px',
    transition: 'width 0.5s ease',
  },
  currentStep: {
    fontSize: '13px',
    color: '#555',
    margin: 0,
    fontStyle: 'italic',
  },
  stepList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
    marginTop: '4px',
  },
  stepRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
  },
  stepDot: {
    width: '20px',
    height: '20px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '11px',
    color: '#fff',
    flexShrink: 0,
    fontWeight: 700,
  },
  stepLabel: {
    fontSize: '13px',
    flex: 1,
  },
  spinner: {
    width: '14px',
    height: '14px',
    border: '2px solid #c0d8f8',
    borderTop: '2px solid #4A90E2',
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
    flexShrink: 0,
  },
};

export default ProgressSection;
