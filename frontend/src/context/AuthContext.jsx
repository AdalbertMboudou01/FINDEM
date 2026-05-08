import React, { createContext, useContext, useState } from 'react';
import { api } from '../lib/api';
import { saveSession, getSession, clearSession } from '../lib/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [session, setSession] = useState(getSession);

  async function login(email, password) {
    const data = await api.post('/api/auth/login', { email, password });
    saveSession(data);
    setSession(data);
    return data;
  }

  async function register(payload) {
    const data = await api.post('/api/auth/register-company-owner', payload);
    saveSession(data);
    setSession(data);
    return data;
  }

  function logout() {
    clearSession();
    setSession(null);
  }

  return (
    <AuthContext.Provider value={{ session, login, register, logout, isAuthenticated: !!session }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
