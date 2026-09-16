import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 디자인 시스템 전역 CSS 로드
import './style.css'

const app = createApp(App)
app.use(router)
app.mount('#app')