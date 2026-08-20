import { assessments, chatMessages, journalEntries, settings } from '@/mocks/data';
import type { DataScenario } from '@/state/mock-state';

const MOCK_LATENCY = 520;

function wait(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function mockResponse<T>(data: T[], scenario: DataScenario): Promise<T[]> {
  await wait(MOCK_LATENCY);
  if (scenario === 'error') throw new Error('API_MOCK_ERROR');
  if (scenario === 'empty') return [];
  return data;
}

// Replace this adapter with HTTP calls while keeping the screen contracts unchanged.
export const mindcareService = {
  getJournalEntries: (scenario: DataScenario) => mockResponse(journalEntries, scenario),
  getAssessments: (scenario: DataScenario) => mockResponse(assessments, scenario),
  getChatMessages: (scenario: DataScenario) => mockResponse(chatMessages, scenario),
  getSettings: (scenario: DataScenario) => mockResponse(settings, scenario),
};
