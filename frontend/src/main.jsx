import React from 'react';
import { createRoot } from 'react-dom/client';
import { createHashRouter, RouterProvider } from 'react-router-dom';
import './styles.css';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import DashboardPage from './pages/DashboardPage';
import OffresPage from './pages/OffresPage';
import CandidaturesPage from './pages/CandidaturesPage';
import TachesPage from './pages/TachesPage';
import EquipePage from './pages/EquipePage';
import ApplyPage from './pages/ApplyPage';
import ConversationsPage from './pages/ConversationsPage';
import WorkspacePage from './pages/WorkspacePage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import VivierPage from './pages/VivierPage';
import EntretiensPage from './pages/EntretiensPage';

const router = createHashRouter([
  { path: '/',                   element: <LoginPage />          },
  { path: '/signup',             element: <SignupPage />         },
  { path: '/forgot-password',    element: <ForgotPasswordPage /> },
  { path: '/reset-password',     element: <ResetPasswordPage />  },
  { path: '/apply/:slug',        element: <ApplyPage />          },
  {
    element: <ProtectedRoute />,
    children: [{
      element: <Layout />,
      children: [
        { path: '/dashboard',    element: <DashboardPage />    },
        { path: '/offres',       element: <OffresPage />       },
        { path: '/candidatures', element: <CandidaturesPage /> },
        { path: '/vivier',       element: <VivierPage />       },
        { path: '/entretiens',   element: <EntretiensPage />   },
        { path: '/taches',       element: <TachesPage />  },
        { path: '/equipe',       element: <EquipePage /> },
        { path: '/conversations', element: <ConversationsPage /> },
        { path: '/workspace',    element: <WorkspacePage /> },
      ],
    }],
  },
]);

createRoot(document.getElementById('root')).render(
  <AuthProvider>
    <RouterProvider router={router} />
  </AuthProvider>
);
