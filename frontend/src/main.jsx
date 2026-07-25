import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';

const path = window.location.pathname;
const area = path === '/admin' || path.startsWith('/admin/') ? 'admin'
  : path === '/expert' || path.startsWith('/expert/') ? 'expert' : 'user';
const basename = area === 'user' ? '/' : `/${area}`;

createRoot(document.getElementById('root')).render(
  <React.StrictMode><BrowserRouter basename={basename}><App area={area}/></BrowserRouter></React.StrictMode>,
);
