import logo from './logo.svg';
import './App.css';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import RegisterPage from './components/register/RegisterPage';
import VerifyPage from './components/register/VerifyPage';
import LoginPage from './components/login/LoginPage';
import MfaPage from './components/login/MfaPage';
import Dashboard from './components/dashboard/Dashboard';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RegisterPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/verify" element={<VerifyPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/mfa" element={<MfaPage />} />
        <Route path="/dashboard" element={<Dashboard />} />
        {/* D'autres routes pourront être ajoutées ici */}
      </Routes>
    </BrowserRouter>
  );
}

export default App;
