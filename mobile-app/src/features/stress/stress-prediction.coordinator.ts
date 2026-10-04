import { stressApi } from './stress.api';
import { stressStorage } from './stress.storage';
import type { DatedStressPrediction } from './stress.types';

export async function predictLatestPmdataStress(
  userId: string,
  token: string,
): Promise<DatedStressPrediction | null> {
  const featureVector = await stressApi.features(token);
  if (featureVector.availableBaseFeatureCount < 1) return null;

  const prediction = await stressApi.predict(token, featureVector);
  const datedPrediction: DatedStressPrediction = {
    ...prediction,
    featureDate: featureVector.featureDate,
    featureVersion: featureVector.featureVersion,
    timezone: featureVector.timezone,
    predictedAt: new Date().toISOString(),
  };
  await stressApi.store(token, datedPrediction);
  await stressStorage.saveLatest(userId, datedPrediction);
  return datedPrediction;
}
