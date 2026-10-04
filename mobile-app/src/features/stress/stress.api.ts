import { apiRequest } from '@/services/api/api.client';
import type { DatedStressPrediction, StoredStressPrediction, StressFeatureVector, StressPrediction } from './stress.types';

export const stressApi = {
  features(token: string) {
    const query = new URLSearchParams({ timezone: 'Asia/Ho_Chi_Minh' });
    return apiRequest<StressFeatureVector>(`/api/v1/health-metrics/stress-features?${query}`, {
      token,
      responseType: 'raw',
    });
  },

  predict(token: string, featureVector: StressFeatureVector) {
    return apiRequest<StressPrediction>('/api/ai/stress-predictions', {
      body: {
        featureVersion: featureVector.featureVersion,
        features: featureVector.features,
      },
      method: 'POST',
      token,
    });
  },

  store(token: string, prediction: DatedStressPrediction) {
    return apiRequest<StoredStressPrediction>('/api/v1/health-metrics/stress-predictions', {
      body: {
        featureDate: prediction.featureDate,
        timezone: prediction.timezone,
        featureVersion: prediction.featureVersion,
        stressScore: prediction.stressScore,
        relativeLevel: prediction.relativeLevel,
        confidence: prediction.confidence,
        modelVersion: prediction.modelVersion,
      },
      method: 'POST',
      token,
      responseType: 'raw',
    });
  },
};
