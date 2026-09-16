<template>
  <div class="add-route-container">
    <div class="header">
      <h1 class="page-title">노선 추가</h1>

      <p class="page-subtitle">
        새로운 노선과 경유 정류장을 등록합니다.
      </p>
    </div>

    <div class="form-card">
      <div class="form-group">
        <label>노선 번호</label>

        <input
          v-model="form.routeNumber"
          type="text"
          placeholder="예: 101"
          maxlength="20"
        />
      </div>

      <div class="form-group">
        <label>노선명</label>

        <input
          v-model="form.routeName"
          type="text"
          placeholder="예: 서울역 - 강남"
          maxlength="100"
        />
      </div>

      <div class="form-group">
        <label>노선 유형</label>

        <select v-model="form.routeType">
          <option value="">선택하세요</option>
          <option value="CITY">시내</option>
          <option value="EXPRESS">급행</option>
          <option value="SHUTTLE">셔틀</option>
        </select>
      </div>
    </div>

    <div class="stop-section">
      <div class="section-header">
        <div>
          <h2 class="section-title">경유 정류장</h2>

          <p class="section-subtitle">
            노선에 포함될 정류장과 순서를 설정합니다.
          </p>
        </div>

        <button
          type="button"
          class="btn-add-stop"
          @click="addStop"
        >
          정류장 추가
        </button>
      </div>

      <div
        v-if="form.stops.length === 0"
        class="empty-stop"
      >
        등록할 정류장을 추가해주세요.
      </div>

      <div
        v-else
        class="stop-card"
      >
        <div
          v-for="(routeStop, index) in form.stops"
          :key="index"
          class="stop-row"
        >
          <div class="sequence">
            {{ index + 1 }}
          </div>

          <div class="stop-select">
            <select
              v-model="routeStop.stopId"
            >
              <option value="">
                정류장 선택
              </option>

              <option
                v-for="stop in stops"
                :key="stop.id"
                :value="stop.id"
              >
                {{ stop.stopName }} ({{ stop.stopCode }})
              </option>
            </select>
          </div>

          <div class="minutes-input">
            <input
              v-model.number="routeStop.estimatedMinutes"
              type="number"
              min="0"
              placeholder="0"
            />

            <span>분</span>
          </div>

          <button
            type="button"
            class="btn-remove"
            @click="removeStop(index)"
          >
            삭제
          </button>
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
        @click="createRoute"
      >
        노선 등록
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import api from '../../api';

const emit = defineEmits([
  'back'
]);

const stops = ref([]);

const form = reactive({
  routeNumber: '',
  routeName: '',
  routeType: '',
  stops: []
});

const fetchStops = async () => {
  try {
    const response = await api.get('/stops');

    stops.value = response.data;
  } catch (error) {
    console.error('정류장 목록 조회 실패:', error);

    alert('정류장 목록을 불러오지 못했습니다.');
  }
};

const addStop = () => {
  form.stops.push({
    stopId: '',
    stopSequence: form.stops.length + 1,
    estimatedMinutes: 0
  });
};

const removeStop = (index) => {
  form.stops.splice(index, 1);

  form.stops.forEach((stop, index) => {
    stop.stopSequence = index + 1;
  });
};

const createRoute = async () => {
  if (!form.routeNumber.trim()) {
    alert('노선 번호를 입력해주세요.');
    return;
  }

  if (!form.routeType) {
    alert('노선 유형을 선택해주세요.');
    return;
  }

  const invalidStop = form.stops.some(
    stop => !stop.stopId
  );

  if (invalidStop) {
    alert('모든 정류장을 선택해주세요.');
    return;
  }

  try {
    await api.post('/routes', {
      routeNumber: form.routeNumber,
      routeName: form.routeName,
      routeType: form.routeType,
      stops: form.stops.map(stop => ({
        stopId: Number(stop.stopId),
        stopSequence: stop.stopSequence,
        estimatedMinutes: Number(stop.estimatedMinutes) || 0
      }))
    });

    alert('노선이 등록되었습니다.');

    emit('back');
  } catch (error) {
    console.error('노선 등록 실패:', error);

    alert(
      error.response?.data?.message ||
      '노선 등록에 실패했습니다.'
    );
  }
};

const goBack = () => {
  emit('back');
};

onMounted(() => {
  fetchStops();
});
</script>

<style scoped>
.add-route-container {
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
  margin-bottom: var(--spacing-xl);
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
.form-group select,
.stop-select select,
.minutes-input input {
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
.form-group select:focus,
.stop-select select:focus,
.minutes-input input:focus {
  border-color: var(--color-primary);
}

.stop-section {
  margin-bottom: var(--spacing-xl);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
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

.btn-add-stop {
  background-color: var(--color-surface-light);
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  padding: 9px 14px;
  font-size: 13px;
  font-weight: 600;
  border-radius: var(--radius-md);
  cursor: pointer;
  white-space: nowrap;
}

.btn-add-stop:hover {
  background-color: var(--color-primary);
  color: white;
}

.stop-card {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
}

.stop-row {
  display: grid;
  grid-template-columns: 42px 1fr 130px 60px;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
}

.stop-row:last-child {
  border-bottom: none;
}

.sequence {
  width: 32px;
  height: 32px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  font-size: 13px;
  font-weight: 700;
}

.minutes-input {
  display: flex;
  align-items: center;
  gap: 6px;
}

.minutes-input span {
  font-size: 13px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.btn-remove {
  background: none;
  border: 1px solid var(--color-border);
  color: #ef4444;
  padding: 8px 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  white-space: nowrap;
}

.btn-remove:hover {
  border-color: #ef4444;
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
  gap: var(--spacing-sm);
}

.btn-primary {
  background-color: var(--color-primary);
  color: white;
  border: none;
  padding: 11px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-md);
}

.btn-primary:hover {
  opacity: 0.9;
}

.btn-secondary {
  background-color: var(--color-surface-light);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  padding: 11px 20px;
  font-size: 14px;
  cursor: pointer;
  border-radius: var(--radius-md);
}

.btn-secondary:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
</style>