import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'
import 'vant/lib/index.css'
import '@phosphor-icons/web/regular'
import App from './App.vue'
import router from './router'
import '@/styles/tokens.css'
import '@/styles/base.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Vant)
app.mount('#app')
