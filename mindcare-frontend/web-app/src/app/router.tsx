import { lazy } from "react";
import { createBrowserRouter } from "react-router-dom";
import { RequireRole } from "@/features/auth";
import { AdminLayout } from "@/features/admin";
import { AssessmentFocusLayout, AuthLayout, PublicLayout, UserLayout } from "@/layouts";

const AiChatPage = lazy(() => import("@/features/ai-chat").then((module) => ({ default: module.AiChatPage })));
const AdminAiDocumentsPage = lazy(() => import("@/features/admin").then((module) => ({ default: module.AdminAiDocumentsPage })));
const AdminAuditPage = lazy(() => import("@/features/admin").then((module) => ({ default: module.AdminAuditPage })));
const AdminContentPage = lazy(() => import("@/features/admin").then((module) => ({ default: module.AdminContentPage })));
const AdminDashboardPage = lazy(() => import("@/features/admin").then((module) => ({ default: module.AdminDashboardPage })));
const AdminNotificationsPage = lazy(() => import("@/features/admin").then((module) => ({ default: module.AdminNotificationsPage })));
const AdminUsersPage = lazy(() => import("@/features/admin").then((module) => ({ default: module.AdminUsersPage })));
const AssessmentLibraryPage = lazy(() => import("@/features/assessment").then((module) => ({ default: module.AssessmentLibraryPage })));
const AssessmentOverviewPage = lazy(() => import("@/features/assessment").then((module) => ({ default: module.AssessmentOverviewPage })));
const AssessmentProcessPage = lazy(() => import("@/features/assessment").then((module) => ({ default: module.AssessmentProcessPage })));
const AssessmentResultPage = lazy(() => import("@/features/assessment").then((module) => ({ default: module.AssessmentResultPage })));
const ForgotPasswordPage = lazy(() => import("@/features/auth").then((module) => ({ default: module.ForgotPasswordPage })));
const LoginPage = lazy(() => import("@/features/auth").then((module) => ({ default: module.LoginPage })));
const RegisterPage = lazy(() => import("@/features/auth").then((module) => ({ default: module.RegisterPage })));
const ResetPasswordPage = lazy(() => import("@/features/auth").then((module) => ({ default: module.ResetPasswordPage })));
const VerifyEmailPage = lazy(() => import("@/features/auth").then((module) => ({ default: module.VerifyEmailPage })));
const BookmarkPage = lazy(() => import("@/features/bookmark").then((module) => ({ default: module.BookmarkPage })));
const EmotionDiaryPage = lazy(() => import("@/features/emotion").then((module) => ({ default: module.EmotionDiaryPage })));
const EmotionHistoryPage = lazy(() => import("@/features/emotion").then((module) => ({ default: module.EmotionHistoryPage })));
const HomePage = lazy(() => import("@/features/home").then((module) => ({ default: module.HomePage })));
const ProgressPage = lazy(() => import("@/features/progress").then((module) => ({ default: module.ProgressPage })));
const HealthDataPage = lazy(() => import("@/features/health").then((module) => ({ default: module.HealthDataPage })));
const NotificationsPage = lazy(() => import("@/features/notifications").then((module) => ({ default: module.NotificationsPage })));
const ProfilePage = lazy(() => import("@/features/profile").then((module) => ({ default: module.ProfilePage })));
const SettingsPage = lazy(() => import("@/features/settings").then((module) => ({ default: module.SettingsPage })));
const SelfCareLibraryPage = lazy(() => import("@/features/self-care").then((module) => ({ default: module.SelfCareLibraryPage })));
const SelfCareDetailPage = lazy(() => import("@/features/self-care").then((module) => ({ default: module.SelfCareDetailPage })));
const CarePlanPage = lazy(() => import("@/features/care-plan").then((module) => ({ default: module.CarePlanPage })));
const NotFoundPage = lazy(() => import("./pages/NotFoundPage").then((module) => ({ default: module.NotFoundPage })));
const ForbiddenPage = lazy(() => import("./pages/ForbiddenPage").then((module) => ({ default: module.ForbiddenPage })));

export const appRouter = createBrowserRouter([
  {
    path: "/",
    element: <PublicLayout />,
    children: [
      { index: true, element: <HomePage /> },
      {
        element: <RequireRole roles={["ROLE_USER"]}><UserLayout /></RequireRole>,
        children: [
          { path: "progress", element: <ProgressPage /> },
          { path: "assessments", element: <AssessmentOverviewPage /> },
          { path: "assessments/library", element: <AssessmentLibraryPage /> },
          { path: "assessments/:code/result", element: <AssessmentResultPage /> },
          { path: "emotion", element: <EmotionDiaryPage /> },
          { path: "emotion/history", element: <EmotionHistoryPage /> },
          { path: "health", element: <HealthDataPage /> },
          { path: "ai-chat", element: <AiChatPage /> },
          { path: "self-care", element: <SelfCareLibraryPage /> },
          { path: "self-care/:id", element: <SelfCareDetailPage /> },
          { path: "care-plan", element: <CarePlanPage /> },
          { path: "bookmarks", element: <BookmarkPage /> },
          { path: "profile", element: <ProfilePage /> },
          { path: "settings", element: <SettingsPage /> },
          { path: "notifications", element: <NotificationsPage /> },
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
    path: "/admin",
    element: <RequireRole roles={["ROLE_ADMIN"]}><AdminLayout /></RequireRole>,
    children: [
      { index: true, element: <AdminDashboardPage /> },
      { path: "users", element: <AdminUsersPage /> },
      { path: "content", element: <AdminContentPage /> },
      { path: "ai-documents", element: <AdminAiDocumentsPage /> },
      { path: "notifications", element: <AdminNotificationsPage /> },
      { path: "audit", element: <AdminAuditPage /> },
    ],
  },
  { path: "/forbidden", element: <ForbiddenPage /> },
  { path: "*", element: <NotFoundPage /> },
]);
