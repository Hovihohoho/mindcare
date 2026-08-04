export interface ExpertSummary {
  expertUserId: string;
  displayName: string;
  headline: string;
  specialties: string[];
  yearsOfExperience: number;
  consultationFee: number;
  currency: string;
  averageRating: number;
  reviewCount: number;
  consultationCount: number;
  avatarUrl?: string;
  bio?: string;
  location?: string;
  education?: string;
}
