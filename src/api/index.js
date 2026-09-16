import axios from 'axios';

const api = axios.create({
  baseURL: '/api', // Vite 프록시를 통해 스프링 부트(8080)로 연결
  headers: {
    'Content-Type': 'application/json',
  },
});

export default api;