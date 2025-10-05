import logo from './logo.svg';
import './App.css';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import RegisterPage from './components/register/RegisterPage';
import VerifyPage from './components/register/VerifyPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RegisterPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/verify" element={<VerifyPage />} />
        {/* D'autres routes pourront être ajoutées ici */}
      </Routes>
    </BrowserRouter>
  );
}

export default App;
