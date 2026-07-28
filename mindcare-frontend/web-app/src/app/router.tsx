import { createBrowserRouter } from "react-router-dom";
import { AiChatPage } from "@/features/ai-chat";
import { AssessmentLibraryPage, AssessmentOverviewPage, AssessmentProcessPage, AssessmentResultPage } from "@/features/assessment";
import { ForgotPasswordPage, LoginPage, RegisterPage, ResetPasswordPage, VerifyEmailPage } from "@/features/auth";
import { BookingPage } from "@/features/booking";
import { BookmarkPage } from "@/features/bookmark";
import { ClientRecordPage } from "@/features/client-record";
import { EmotionDiaryPage, EmotionHistoryPage } from "@/features/emotion";
import { ExpertCalendarPage } from "@/features/expert-calendar";
import { ExpertDashboardPage } from "@/features/expert-dashboard";
import { ExpertDirectoryPage, ExpertProfilePage } from "@/features/expert-directory";
import { ExpertManageProfilePage } from "@/features/expert-profile";
import { ExpertRegistrationPage } from "@/features/expert-registration";
import { HomePage } from "@/features/home";
import { PaymentPage } from "@/features/payment";
import { ProfilePage } from "@/features/profile";
import { SettingsPage } from "@/features/settings";
import { AssessmentFocusLayout, AuthLayout, ExpertLayout, PublicLayout, UserLayout } from "@/layouts";
import { NotFoundPage } from "./pages/NotFoundPage";

export const appRouter = createBrowserRouter([
  {
    path: "/",
    element: <PublicLayout />,
    children: [
      { index: true, element: <HomePage /> },
      {
        element: <UserLayout />,
        children: [
          { path: "assessments", element: <AssessmentOverviewPage /> },
          { path: "assessments/library", element: <AssessmentLibraryPage /> },
          { path: "assessments/:code/result", element: <AssessmentResultPage /> },
          { path: "emotion", element: <EmotionDiaryPage /> },
          { path: "emotion/history", element: <EmotionHistoryPage /> },
          { path: "experts", element: <ExpertDirectoryPage /> },
          { path: "experts/:id", element: <ExpertProfilePage /> },
          { path: "booking/:expertId", element: <BookingPage /> },
          { path: "payment", element: <PaymentPage /> },
          { path: "ai-chat", element: <AiChatPage /> },
          { path: "bookmarks", element: <BookmarkPage /> },
          { path: "profile", element: <ProfilePage /> },
          { path: "settings", element: <SettingsPage /> },
          { path: "expert/register", element: <ExpertRegistrationPage /> },
        ],
      },
    ],
  },
  {
    element: <AssessmentFocusLayout />,
    children: [{ path: "/assessments/:code", element: <AssessmentProcessPage /> }],
  },
  {
    element: <AuthLayout />,
    children: [
      { path: "/login", element: <LoginPage /> },
      { path: "/register", element: <RegisterPage /> },
      { path: "/verify-email", element: <VerifyEmailPage /> },
      { path: "/forgot-password", element: <ForgotPasswordPage /> },
      { path: "/reset-password", element: <ResetPasswordPage /> },
    ],
  },
  {
    path: "/expert",
    element: <ExpertLayout />,
    children: [
      { index: true, element: <ExpertDashboardPage /> },
      { path: "calendar", element: <ExpertCalendarPage /> },
      { path: "profile", element: <ExpertManageProfilePage /> },
      { path: "statistics", element: <ExpertDashboardPage /> },
      { path: "clients/:id", element: <ClientRecordPage /> },
      { path: "settings", element: <SettingsPage /> },
    ],
  },
  { path: "*", element: <NotFoundPage /> },
]);
