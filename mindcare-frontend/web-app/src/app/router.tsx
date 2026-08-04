import { createBrowserRouter } from "react-router-dom";
import { AiChatPage } from "@/features/ai-chat";
import { AdminAiDocumentsPage, AdminAuditPage, AdminContentPage, AdminDashboardPage, AdminExpertsPage, AdminLayout, AdminNotificationsPage, AdminUsersPage } from "@/features/admin";
import { AssessmentLibraryPage, AssessmentOverviewPage, AssessmentProcessPage, AssessmentResultPage } from "@/features/assessment";
import { ForgotPasswordPage, LoginPage, RegisterPage, RequireRole, ResetPasswordPage, VerifyEmailPage } from "@/features/auth";
import { BookingPage } from "@/features/booking";
import { BookmarkPage } from "@/features/bookmark";
import { ClientRecordPage } from "@/features/client-record";
import { EmotionDiaryPage, EmotionHistoryPage } from "@/features/emotion";
import { ExpertCalendarPage } from "@/features/expert-calendar";
import { ChatHistoryPage, ExpertChatPage } from "@/features/expert-chat";
import { ExpertDashboardPage } from "@/features/expert-dashboard";
import { ExpertDirectoryPage, ExpertProfilePage } from "@/features/expert-directory";
import { ExpertManageProfilePage } from "@/features/expert-profile";
import { ExpertRegistrationPage } from "@/features/expert-registration";
import { HomePage } from "@/features/home";
import { NotificationsPage } from "@/features/notifications";
import { PaymentPage } from "@/features/payment";
import { ProfilePage } from "@/features/profile";
import { SettingsPage } from "@/features/settings";
import { AssessmentFocusLayout, AuthLayout, ExpertLayout, PublicLayout, UserLayout } from "@/layouts";
import { NotFoundPage } from "./pages/NotFoundPage";
import { ForbiddenPage } from "./pages/ForbiddenPage";

export const appRouter = createBrowserRouter([
  {
    path: "/",
    element: <PublicLayout />,
    children: [
      { index: true, element: <HomePage /> },
      {
        element: <RequireRole roles={["ROLE_USER"]}><UserLayout /></RequireRole>,
        children: [
          { path: "assessments", element: <AssessmentOverviewPage /> },
          { path: "assessments/library", element: <AssessmentLibraryPage /> },
          { path: "assessments/:code/result", element: <AssessmentResultPage /> },
          { path: "emotion", element: <EmotionDiaryPage /> },
          { path: "emotion/history", element: <EmotionHistoryPage /> },
          { path: "experts", element: <ExpertDirectoryPage /> },
          { path: "experts/chat-history", element: <ChatHistoryPage /> },
          { path: "experts/:id", element: <ExpertProfilePage /> },
          { path: "booking/:expertId", element: <BookingPage /> },
          { path: "chat", element: <ExpertChatPage /> },
          { path: "chat/:bookingId", element: <ExpertChatPage /> },
          { path: "payment", element: <PaymentPage /> },
          { path: "ai-chat", element: <AiChatPage /> },
          { path: "bookmarks", element: <BookmarkPage /> },
          { path: "profile", element: <ProfilePage /> },
          { path: "settings", element: <SettingsPage /> },
          { path: "notifications", element: <NotificationsPage /> },
          { path: "expert/register", element: <ExpertRegistrationPage /> },
        ],
      },
    ],
  },
  {
    element: <RequireRole roles={["ROLE_USER"]}><AssessmentFocusLayout /></RequireRole>,
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
    element: <RequireRole roles={["ROLE_EXPERT"]}><ExpertLayout /></RequireRole>,
    children: [
      { index: true, element: <ExpertDashboardPage /> },
      { path: "calendar", element: <ExpertCalendarPage /> },
      { path: "chat/:bookingId", element: <ExpertChatPage /> },
      { path: "profile", element: <ExpertManageProfilePage /> },
      { path: "statistics", element: <ExpertDashboardPage /> },
      { path: "clients/:id", element: <ClientRecordPage /> },
      { path: "settings", element: <SettingsPage /> },
    ],
  },
  {
    path: "/admin",
    element: <RequireRole roles={["ROLE_ADMIN"]}><AdminLayout /></RequireRole>,
    children: [
      { index: true, element: <AdminDashboardPage /> },
      { path: "users", element: <AdminUsersPage /> },
      { path: "experts", element: <AdminExpertsPage /> },
      { path: "content", element: <AdminContentPage /> },
      { path: "ai-documents", element: <AdminAiDocumentsPage /> },
      { path: "notifications", element: <AdminNotificationsPage /> },
      { path: "audit", element: <AdminAuditPage /> },
    ],
  },
  { path: "/forbidden", element: <ForbiddenPage /> },
  { path: "*", element: <NotFoundPage /> },
]);
