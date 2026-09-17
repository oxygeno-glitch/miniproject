<template>
  <div
    v-if="route"
    class="route-details-container"
  >
    <div class="header">
      <h1 class="page-title">노선 상세 정보</h1>

      <p class="page-subtitle">
        선택한 노선의 상세 정보입니다.
      </p>
    </div>

    <div class="details-card">
      <div class="detail-row">
        <span class="label">ID</span>

        <span class="value font-mono">
          {{ route.id }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">노선 번호</span>

        <span class="value highlight-text">
          {{ route.routeNumber }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">노선명</span>

        <span class="value">
          {{ route.routeName || '-' }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">노선 유형</span>

        <span class="value">
          {{ getRouteTypeText(route.routeType) }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">상태</span>

        <span
          :class="[
            'value',
            'status-badge',
            getStatusClass(route.status)
          ]"
        >
          {{ getStatusText(route.status) }}
        </span>
      </div>

      <div class="detail-row">
        <span class="label">등록일</span>

        <span class="value font-mono">
          {{ formatDateTime(route.createdAt) }}
        </span>
      </div>
    </div>

    <div class="stop-section">
      <div class="section-header">
        <div>
          <h2 class="section-title">경유 정류장</h2>

          <p class="section-subtitle">
            해당 노선의 정류장 순서와 예상 소요시간입니다.
          </p>
        </div>
      </div>

      <div
        v-if="!route.routeStops || route.routeStops.length === 0"
        class="empty-stop"
      >
        등록된 정류장이 없습니다.
      </div>

      <div
        v-else
        class="stop-card"
      >
        <table class="stop-table">
          <thead>
            <tr>
              <th>순서</th>
              <th>정류장 코드</th>
              <th>정류장명</th>
              <th>예상 소요시간</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="stop in sortedRouteStops"
              :key="stop.routeStopId"
            >
              <td>
                <span class="sequence">
                  {{ stop.stopSequence }}
                </span>
              </td>

              <td class="font-mono">
                {{ stop.stopCode || '-' }}
              </td>

              <td class="stop-name">
                {{ stop.stopName || '-' }}
              </td>

              <td>
                {{ stop.estimatedMinutes ?? 0 }}분
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="actions">
      <button
        type="button"
        class="btn-secondary"
        @click="goToList"
      >
        목록으로 돌아가기
      </button>
    </div>
  </div>

  <div
    v-else
    class="loading"
  >
    노선 정보를 불러오는 중입니다...
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import api from '../../api';

const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
});

const emit = defineEmits([
  'back'
]);

const route = ref(null);

const sortedRouteStops = computed(() => {
  if (!route.value?.routeStops) {
    return [];
  }

  return [...route.value.routeStops].sort(
    (a, b) => a.stopSequence - b.stopSequence
  );
});

const fetchRouteDetail = async () => {
  try {
    const response = await api.get(
      `/routes/${props.id}`
    );

    route.value = response.data;
  } catch (error) {
    console.error('노선 상세 정보 조회 실패:', error);

    alert('노선 정보를 불러오지 못했습니다.');
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

const formatDateTime = (dateString) => {
  if (!dateString) {
    return '-';
  }

  return dateString
    .replace('T', ' ')
    .substring(0, 16);
};

const goToList = () => {
  emit('back');
};

onMounted(() => {
  fetchRouteDetail();
});
</script>

<style scoped>
.route-details-container {
  padding: var(--spacing-lg);
  max-width: 1000px;
  margin: 0 auto;
}

.header {
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

.details-card {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
  margin-bottom: var(--spacing-xl);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) 0;
  border-bottom: 1px solid var(--color-border);
}

.detail-row:last-child {
  border-bottom: none;
}

.label {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.value {
  color: var(--color-text-primary);
  font-size: 15px;
  font-weight: 600;
}

.highlight-text {
  color: #00a3ff;
}

.font-mono {
  color: var(--color-text-secondary);
  font-family: monospace;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 6px;
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

.stop-section {
  margin-bottom: var(--spacing-xl);
}

.section-header {
  margin-bottom: var(--spacing-md);
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.section-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.stop-card {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
  overflow-x: auto;
}

.stop-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.stop-table th {
  padding: 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
}

.stop-table td {
  padding: 14px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  border-bottom: 1px solid var(--color-border);
}

.stop-table tr:last-child td {
  border-bottom: none;
}

.sequence {
  display: inline-flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  font-size: 13px;
  font-weight: 700;
}

.stop-name {
  font-weight: 600;
}

.empty-stop {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 32px;
  text-align: center;
  color: var(--color-text-secondary);
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.btn-secondary {
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  padding: var(--spacing-md) var(--spacing-lg);
  font-size: 14px;
  cursor: pointer;
  border-radius: var(--radius-md);
}

.btn-secondary:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.loading {
  text-align: center;
  padding: var(--spacing-xxl);
  color: var(--color-text-secondary);
}
</style>