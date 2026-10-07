import { Route, Routes } from 'react-router';
import { NotFoundPage } from './pages/NotFoundPage';
import { RegisterPage } from './pages/RegisterPage';
import { VerifyEmailPage } from './pages/VerifyEmailPage';

/** Keep in step with SpaController, which decides which paths Spring serves this app on. */
export function App() {
  return (
    <Routes>
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
