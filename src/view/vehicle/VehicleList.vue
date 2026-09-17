<template>
  <div class="vehicle-list-container">
    <div class="header">
      <div>
        <h1 class="page-title">차량 관리</h1>
        <p class="page-subtitle">
          운행 중인 모빌리티 차량 목록 및 상태를 관리합니다.
        </p>
      </div>

      <button
        type="button"
        class="btn-primary"
        @click="emit('go-add')"
      >
        + 신규 차량 등록
      </button>
    </div>

    <div class="table-card">
      <table class="vehicle-table">
        <thead>
          <tr>
            <th>차량 번호</th>
            <th>모델명</th>
            <th>제조사</th>
            <th>연식</th>
            <th>상태</th>
            <th>액션</th>
          </tr>
        </thead>

        <tbody>
          <tr v-if="vehicles.length === 0">
            <td colspan="6" class="empty-msg">
              등록된 차량 데이터가 없습니다.
            </td>
          </tr>

          <tr v-for="vehicle in pagedVehicles" :key="vehicle.id">

            <td class="highlight-text">
              {{ vehicle.plateNumber }}
            </td>

            <td>{{ vehicle.modelName }}</td>

            <td>{{ vehicle.manufacturer }}</td>

            <td>{{ vehicle.modelYear }}년식</td>

            <td>
              <span
                :class="[
                  'status-badge',
                  'clickable',
                  getStatusClass(vehicle.status)
                ]"
                @click="toggleStatus(vehicle)"
              >
                {{ getStatusText(vehicle.status) }}
              </span>
            </td>

            <td>
              <div class="action-buttons">
                <button
                  type="button"
                  class="btn-secondary"
                  @click="goToDetail(vehicle.id)"
                >
                  상세보기
                </button>

                <button
                  type="button"
                  class="btn-danger"
                  @click="deleteVehicle(vehicle.id)"
                >
                  삭제
                </button>
              </div>
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
          <span class="page-total">(전체 {{ vehicles.length }}대)</span>
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

const vehicles = ref([]);

const pageSize = 10;
const currentPage = ref(1);

const totalPages = computed(() =>
  Math.max(1, Math.ceil(vehicles.value.length / pageSize))
);

const pagedVehicles = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return vehicles.value.slice(start, start + pageSize);
});

watch(totalPages, (newTotal) => {
  if (currentPage.value > newTotal) {
    currentPage.value = newTotal;
  }
});

// 상태 우선순위: 운행 중 > 운행 대기 중 > 정비
// 목록을 새로 불러올 때(최초 진입 또는 다른 메뉴에서 돌아왔을 때)만 이 순서로
// 정렬합니다. 상태 배지를 클릭해 바로 바꿔도 그 자리에서 즉시 재정렬되지는
// 않고, 다음에 목록을 다시 불러올 때 반영됩니다.
const STATUS_ORDER = {
  ACTIVE: 0,
  INACTIVE: 1,
  MAINTENANCE: 2
};

const sortByStatus = (list) => {
  return [...list].sort((a, b) => {
    const orderA = STATUS_ORDER[a.status] ?? 99;
    const orderB = STATUS_ORDER[b.status] ?? 99;
    return orderA - orderB;
  });
};

const fetchVehicles = async () => {
  try {
    const response = await api.get('/vehicles');
    vehicles.value = sortByStatus(response.data);
  } catch (error) {
    console.error('차량 목록 조회 실패:', error);
  }
};

const getStatusText = (status) => {
  switch (status) {
    case 'ACTIVE':
      return '운행 중';

    case 'MAINTENANCE':
      return '정비';

    case 'INACTIVE':
      return '운행 대기 중';

    default:
      return status;
  }
};

const getStatusClass = (status) => {
  switch (status) {
    case 'ACTIVE':
      return 'status-on';

    case 'MAINTENANCE':
      return 'status-danger';

    case 'INACTIVE':
      return 'status-off';

    default:
      return 'status-off';
  }
};

const toggleStatus = async (vehicle) => {
  let nextStatus = 'ACTIVE';

  if (vehicle.status === 'ACTIVE') {
    nextStatus = 'MAINTENANCE';
  } else if (vehicle.status === 'MAINTENANCE') {
    nextStatus = 'INACTIVE';
  } else {
    nextStatus = 'ACTIVE';
  }

  try {
    const response = await api.patch(`/vehicles/${vehicle.id}/status`, {
      status: nextStatus
    });

    vehicle.status = response.data.status;
  } catch (error) {
    console.error('차량 상태 변경 실패:', error);
    alert(
      error.response?.data?.message ||
      '차량 상태 변경 중 오류가 발생했습니다.'
    );
  }
};

const goToDetail = (id) => {
  emit('go-detail', id);
};

const deleteVehicle = async (id) => {
  if (!confirm(`ID ${id}번 차량을 정말 삭제하시겠습니까?`)) {
    return;
  }

  try {
    await api.delete(`/vehicles/${id}`);

    alert('차량이 성공적으로 삭제되었습니다.');

    await fetchVehicles();
  } catch (error) {
    console.error('차량 삭제 실패:', error);
    alert(
      error.response?.data?.message ||
      '차량 삭제 중 오류가 발생했습니다.'
    );
  }
};

onMounted(() => {
  fetchVehicles();
});
</script>

<style scoped>
.vehicle-list-container {
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
  overflow: hidden;
}

.vehicle-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.vehicle-table th {
  background-color: var(--color-surface-light);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 600;
  padding: var(--spacing-md);
  border-bottom: 1px solid var(--color-border);
}

.vehicle-table td {
  padding: var(--spacing-md);
  border-bottom: 1px solid var(--color-border);
  font-size: 14px;
  color: var(--color-text-primary);
  vertical-align: middle;
}

.vehicle-table td:nth-child(6) {
  width: 150px;
  text-align: center;
}

.vehicle-table tbody tr:hover {
  background-color: var(--color-surface-light);
}

.highlight-text {
  font-weight: 600;
  color: var(--color-text-primary);
}

.font-mono {
  color: var(--color-text-secondary);
  font-family: monospace;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 95px;
  padding: 5px 0;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 650;
  letter-spacing: -0.02em;
  box-sizing: border-box;
}

.status-badge.clickable {
  cursor: pointer;
  user-select: none;
  transition: opacity 0.2s;
}

.status-badge.clickable:hover {
  opacity: 0.8;
}

.status-on {
  background-color: rgba(34, 197, 94, 0.15);
  color: #4ade80;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.status-off {
  background-color: rgba(148, 163, 184, 0.15);
  color: var(--color-text-secondary);
  border: 1px solid rgba(148, 163, 184, 0.3);
}

.status-danger {
  background-color: rgba(239, 68, 68, 0.15);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: var(--spacing-sm);
}

.btn-primary {
  background: var(--color-primary-gradient);
  color: #ffffff;
  padding: var(--spacing-sm) var(--spacing-md);
  border: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-primary:hover {
  opacity: 0.9;
}

.btn-secondary {
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  padding: 9px 14px;
  min-width: 58px;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.btn-secondary:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.btn-danger {
  background-color: transparent;
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.4);
  padding: 9px 14px;
  min-width: 58px;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.btn-danger:hover {
  background-color: #ef4444;
  color: #ffffff;
  border-color: #ef4444;
}

.empty-msg {
  text-align: center;
  padding: var(--spacing-xxl) !important;
  color: var(--color-text-secondary);
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