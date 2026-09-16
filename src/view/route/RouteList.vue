<template>
  <div class="route-list-container">
    <div class="header">
      <div>
        <h1 class="page-title">노선 관리</h1>

        <p class="page-subtitle">
          등록된 노선을 조회하고 관리합니다.
        </p>
      </div>

      <button
        type="button"
        class="btn-primary"
        @click="goToAdd"
      >
        노선 추가
      </button>
    </div>

    <div class="table-card">
      <table class="route-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>노선 번호</th>
            <th>노선명</th>
            <th>노선 유형</th>
            <th>상태</th>
            <th>등록일</th>
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="route in routes"
            :key="route.id"
          >
            <td class="font-mono">
              {{ route.id }}
            </td>

            <td class="route-number">
              {{ route.routeNumber }}
            </td>

            <td>
              {{ route.routeName || '-' }}
            </td>

            <td>
              <span class="route-type">
                {{ getRouteTypeText(route.routeType) }}
              </span>
            </td>

            <td>
              <span
                :class="[
                  'status-badge',
                  getStatusClass(route.status)
                ]"
              >
                {{ getStatusText(route.status) }}
              </span>
            </td>

            <td class="font-mono">
              {{ formatDateTime(route.createdAt) }}
            </td>

            <td>
              <div class="action-buttons">
                <button
                  type="button"
                  class="btn-secondary"
                  @click="goToDetail(route.id)"
                >
                  상세보기
                </button>
              </div>
            </td>
          </tr>

          <tr v-if="routes.length === 0">
            <td
              colspan="7"
              class="empty-msg"
            >
              등록된 노선이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '../../api';

const emit = defineEmits([
  'go-add',
  'go-detail'
]);

const routes = ref([]);

const fetchRoutes = async () => {
  try {
    const response = await api.get('/routes');

    routes.value = response.data;
  } catch (error) {
    console.error('노선 목록 조회 실패:', error);

    alert('노선 목록을 불러오지 못했습니다.');
  }
};

const getRouteTypeText = (type) => {
  switch (type) {
    case 'CITY':
      return '시내';

    case 'EXPRESS':
      return '급행';

    case 'SHUTTLE':
      return '셔틀';

    default:
      return type || '-';
  }
};

const getStatusText = (status) => {
  switch (status) {
    case 'ACTIVE':
      return '운행 중';

    case 'SUSPENDED':
      return '운행 중지';

    case 'DELETED':
      return '삭제됨';

    default:
      return status || '-';
  }
};

const getStatusClass = (status) => {
  switch (status) {
    case 'ACTIVE':
      return 'status-on';

    case 'SUSPENDED':
      return 'status-danger';

    default:
      return 'status-off';
  }
};

const formatDateTime = (dateString) => {
  if (!dateString) {
    return '-';
  }

  return dateString
    .replace('T', ' ')
    .substring(0, 16);
};

const goToAdd = () => {
  emit('go-add');
};

const goToDetail = (id) => {
  emit('go-detail', id);
};

onMounted(() => {
  fetchRoutes();
});
</script>

<style scoped>
.route-list-container {
  padding: var(--spacing-lg);
  max-width: 1400px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--spacing-xl);
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-top: var(--spacing-xs);
}

.table-card {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
  overflow-x: auto;
}

.route-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.route-table th {
  padding: 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}

.route-table td {
  padding: 14px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  border-bottom: 1px solid var(--color-border);
}

.route-table tr:last-child td {
  border-bottom: none;
}

.route-number {
  color: #00a3ff !important;
  font-weight: 700;
}

.route-type {
  display: inline-block;
  padding: 5px 10px;
  border-radius: 6px;
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  font-size: 12px;
  font-weight: 600;
}

.status-badge {
  display: inline-block;
  min-width: 75px;
  padding: 5px 0;
  border-radius: 20px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
}

.status-on {
  background-color: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.status-off {
  background-color: rgba(148, 163, 184, 0.15);
  color: #94a3b8;
}

.status-danger {
  background-color: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.route-table td:nth-child(7) {
  width: 130px;
  text-align: center;
}

.action-buttons {
  display: flex;
  justify-content: center;
}

.btn-primary {
  background-color: var(--color-primary);
  color: white;
  border: none;
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-md);
  white-space: nowrap;
}

.btn-primary:hover {
  opacity: 0.9;
}

.btn-secondary {
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  padding: 9px 14px;
  min-width: 70px;
  font-size: 14px;
  cursor: pointer;
  border-radius: var(--radius-md);
  white-space: nowrap;
}

.btn-secondary:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.font-mono {
  color: var(--color-text-secondary);
  font-family: monospace;
}

.empty-msg {
  text-align: center;
  padding: 40px !important;
  color: var(--color-text-secondary) !important;
}
</style>