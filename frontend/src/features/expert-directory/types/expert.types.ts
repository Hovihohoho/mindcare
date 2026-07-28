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
  bio?: string;
  location?: string;
  education?: string[];
}
