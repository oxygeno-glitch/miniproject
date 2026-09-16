<template>
  <div class="add-dispatch-container">
    <div class="header">
      <h1 class="page-title">배차 추가</h1>
      <p class="page-subtitle">
        노선, 차량, 기사를 선택하여 새로운 배차를 등록합니다.
      </p>
    </div>

    <div class="form-card">
      <div class="form-group">
        <label>노선</label>

        <select v-model="form.routeId">
          <option value="">노선을 선택하세요</option>

          <option
            v-for="route in routes"
            :key="route.id"
            :value="route.id"
          >
            {{ route.routeNumber }} - {{ route.routeName || '-' }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label>차량</label>

        <select v-model="form.vehicleId">
          <option value="">차량을 선택하세요</option>

          <option
            v-for="vehicle in vehicles"
            :key="vehicle.id"
            :value="vehicle.id"
          >
            {{ vehicle.plateNumber }} - {{ vehicle.modelName }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label>기사</label>

        <select v-model="form.driverId">
          <option value="">기사를 선택하세요</option>

          <option
            v-for="driver in drivers"
            :key="driver.id"
            :value="driver.id"
          >
            {{ getDriverName(driver) }}
          </option>
        </select>
      </div>

      <div class="time-grid">
        <div class="form-group">
          <label>예정 출발 시간</label>

          <input
            v-model="form.plannedStartTime"
            type="datetime-local"
          />
        </div>

        <div class="form-group">
          <label>예정 도착 시간</label>

          <input
            v-model="form.plannedEndTime"
            type="datetime-local"
          />
        </div>
      </div>
    </div>

    <div class="actions">
      <button
        type="button"
        class="btn-secondary"
        @click="goBack"
      >
        취소
      </button>

      <button
        type="button"
        class="btn-primary"
        @click="createDispatch"
      >
        배차 등록
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import api from '../../api';

const emit = defineEmits(['back']);

const routes = ref([]);
const vehicles = ref([]);
const drivers = ref([]);

const form = reactive({
  routeId: '',
  vehicleId: '',
  driverId: '',
  plannedStartTime: '',
  plannedEndTime: ''
});

const fetchData = async () => {
  try {
    const [routeResponse, vehicleResponse, driverResponse] =
      await Promise.all([
        api.get('/routes'),
        api.get('/vehicles'),
        api.get('/drivers')
      ]);

    routes.value = routeResponse.data;
    vehicles.value = vehicleResponse.data;
    drivers.value = driverResponse.data;
  } catch (error) {
    console.error('배차 등록용 데이터 조회 실패:', error);
    alert('배차 등록에 필요한 데이터를 불러오지 못했습니다.');
  }
};

const getDriverName = (driver) => {
  return (
    driver.name ||
    driver.driverName ||
    `기사 #${driver.id}`
  );
};

const createDispatch = async () => {
  if (!form.routeId) {
    alert('노선을 선택해주세요.');
    return;
  }

  if (!form.vehicleId) {
    alert('차량을 선택해주세요.');
    return;
  }

  if (!form.driverId) {
    alert('기사를 선택해주세요.');
    return;
  }

  if (!form.plannedStartTime) {
    alert('예정 출발 시간을 입력해주세요.');
    return;
  }

  if (!form.plannedEndTime) {
    alert('예정 도착 시간을 입력해주세요.');
    return;
  }

  if (
    new Date(form.plannedEndTime) <=
    new Date(form.plannedStartTime)
  ) {
    alert('예정 도착 시간은 출발 시간보다 늦어야 합니다.');
    return;
  }

  try {
    await api.post('/dispatches', {
      routeId: Number(form.routeId),
      vehicleId: Number(form.vehicleId),
      driverId: Number(form.driverId),
      plannedStartTime: form.plannedStartTime,
      plannedEndTime: form.plannedEndTime
    });

    alert('배차가 등록되었습니다.');
    emit('back');
  } catch (error) {
    console.error('배차 등록 실패:', error);

    alert(
      error.response?.data?.message ||
      '배차 등록에 실패했습니다.'
    );
  }
};

const goBack = () => {
  emit('back');
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.add-dispatch-container {
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

.form-card {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-card);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: var(--spacing-lg);
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.form-group input,
.form-group select {
  width: 100%;
  box-sizing: border-box;
  padding: 11px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  font-size: 14px;
  outline: none;
}

.form-group input:focus,
.form-group select:focus {
  border-color: var(--color-primary);
}

.time-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-lg);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-xl);
}

.btn-primary,
.btn-secondary {
  padding: 11px 20px;
  font-size: 14px;
  cursor: pointer;
  border-radius: var(--radius-md);
  white-space: nowrap;
}

.btn-primary {
  background-color: var(--color-primary);
  color: white;
  border: none;
  font-weight: 600;
}

.btn-primary:hover {
  opacity: 0.9;
}

.btn-secondary {
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

@media (max-width: 700px) {
  .time-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>