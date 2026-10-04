export type StressRelativeLevel = 'BELOW_NORMAL' | 'NORMAL' | 'ABOVE_NORMAL';

export type StressFeatureVector = {
  featureVersion: string;
  featureDate: string;
  timezone: string;
  featureNames: string[];
  features: (number | null)[];
  availableBaseFeatureCount: number;
};

export type StressPrediction = {
  stressScore: number;
  relativeLevel: StressRelativeLevel;
  scaleMinimum: number;
  scaleMaximum: number;
  confidence: number;
  modelVersion: string;
  usageNotice: string;
};

export type DatedStressPrediction = StressPrediction & {
  featureDate: string;
  featureVersion: string;
  timezone: string;
  predictedAt: string;
};

export type StoredStressPrediction = {
  id: string;
  alertLevel: 'INFORMATIONAL' | 'MONITOR' | 'ELEVATED' | 'HIGH';
  notificationRequired: boolean;
  notifiedAt: string | null;
};
