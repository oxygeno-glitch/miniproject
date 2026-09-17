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
            <th>노선 번호</th>
            <th>노선명</th>
            <th>노선 유형</th>
            <th>상태</th>
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="route in pagedRoutes"
            :key="route.id"
          >
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
              <button
                type="button"
                class="status-button"
                @click="toggleStatus(route)"
              >
                <span
                  :class="[
                    'status-badge',
                    getStatusClass(route.status)
                  ]"
                >
                  {{ getStatusText(route.status) }}
                </span>
              </button>
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
              colspan="5"
              class="empty-msg"
            >
              등록된 노선이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="totalPages > 1" class="pagination">
        <button
          type="button"
          class="page-btn"
          :disabled="currentPage === 1"
          @click="currentPage--"
        >
          이전
        </button>

        <span class="page-indicator">
          {{ currentPage }} / {{ totalPages }} 페이지
          <span class="page-total">(전체 {{ routes.length }}개)</span>
        </span>

        <button
          type="button"
          class="page-btn"
          :disabled="currentPage === totalPages"
          @click="currentPage++"
        >
          다음
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import api from '../../api';

const emit = defineEmits([
  'go-add',
  'go-detail'
]);

const routes = ref([]);

const pageSize = 10;
const currentPage = ref(1);

const sortedRoutes = computed(() => {
  return [...routes.value].sort((a, b) => {
    const numberA = parseInt(a.routeNumber, 10);
    const numberB = parseInt(b.routeNumber, 10);

    if (!Number.isNaN(numberA) && !Number.isNaN(numberB)) {
      return numberA - numberB;
    }

    return String(a.routeNumber || '')
      .localeCompare(String(b.routeNumber || ''));
  });
});

const totalPages = computed(() =>
  Math.max(1, Math.ceil(sortedRoutes.value.length / pageSize))
);

const pagedRoutes = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return sortedRoutes.value.slice(start, start + pageSize);
});

watch(totalPages, (newTotal) => {
  if (currentPage.value > newTotal) {
    currentPage.value = newTotal;
  }
});

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
      return '개통';

    case 'DISCONTINUED':
      return '폐지';

    default:
      return status || '-';
  }
};

const getStatusClass = (status) => {
  switch (status) {
    case 'ACTIVE':
      return 'status-on';

    case 'DISCONTINUED':
      return 'status-danger';

    default:
      return 'status-off';
  }
};

const toggleStatus = async (route) => {
  const isActive = route.status === 'ACTIVE';
  const newStatus = isActive
    ? 'DISCONTINUED'
    : 'ACTIVE';

  const newStatusText = isActive
    ? '폐지'
    : '개통';

  const confirmed = confirm(
    `${route.routeNumber} - ${route.routeName}\n\n노선 상태를 "${newStatusText}"으로 변경하시겠습니까?`
  );

  if (!confirmed) {
    return;
  }

  try {
    const response = await api.patch(
      `/routes/${route.id}/status`,
      {
        status: newStatus
      }
    );

    const index = routes.value.findIndex(
      item => item.id === route.id
    );

    if (index !== -1) {
      routes.value[index] = response.data;
    }

    alert(`노선 상태가 "${newStatusText}"으로 변경되었습니다.`);
  } catch (error) {
    console.error('노선 상태 변경 실패:', error);

    const message =
      error.response?.data?.message ||
      '노선 상태 변경에 실패했습니다.';

    alert(message);
  }
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

.status-button {
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
}

.status-badge {
  display: inline-block;
  min-width: 75px;
  padding: 5px 0;
  border-radius: 20px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  transition: opacity 0.2s;
}

.status-button:hover .status-badge {
  opacity: 0.7;
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

.route-table td:nth-child(5) {
  width: 130px;
  text-align: center;
}

.action-buttons {
  display: flex;
  justify-content: center;
}

.btn-primary {
  background: var(--color-primary-gradient);
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

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  border-top: 1px solid var(--color-border);
}

.page-btn {
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  padding: 7px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-indicator {
  font-size: 13px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.page-total {
  color: var(--color-text-secondary);
  opacity: 0.7;
}
</style>