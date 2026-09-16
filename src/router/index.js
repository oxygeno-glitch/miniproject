import { createRouter, createWebHistory } from 'vue-router';

import Common from '../components/Common.vue';
import LoginPage from '../components/LoginPage.vue';
import SignUp from '../components/SignUp.vue';
import Dashboard from '../components/Dashboard.vue';

const routes = [
  {
    path: '/',
    name: 'Common',
    component: Common
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginPage
  },
  {
    path: '/signup',
    name: 'SignUp',
    component: SignUp
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: {
      requiresAuth: true
    }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const isLoggedIn = sessionStorage.getItem('isLoggedIn') === 'true';

  if (to.meta.requiresAuth && !isLoggedIn) {
    return '/login';
  }

  if (
    (to.path === '/login' || to.path === '/signup') &&
    isLoggedIn
  ) {
    return '/dashboard';
  }

  return true;
});

export default router;