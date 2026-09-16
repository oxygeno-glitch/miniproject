<template>
  <div class="add-vehicle-container">
    <div class="header">
      <h1 class="page-title">신규 차량 등록</h1>
      <p class="page-subtitle">
        운행에 투입할 새로운 모빌리티 차량 정보를 입력합니다.
      </p>
    </div>

    <div class="form-card">
      <form class="form-group" @submit.prevent="submitForm">
        <div class="field">
          <label for="plateNumber">차량 번호</label>
          <input
            id="plateNumber"
            v-model.trim="form.plateNumber"
            type="text"
            placeholder="예: 12가 3456"
            required
          />
        </div>

        <div class="field">
          <label for="modelName">모델명</label>
          <input
            id="modelName"
            v-model.trim="form.modelName"
            type="text"
            placeholder="예: 카니발"
            required
          />
        </div>

        <div class="field">
          <label for="manufacturer">제조사</label>
          <input
            id="manufacturer"
            v-model.trim="form.manufacturer"
            type="text"
            placeholder="예: 기아"
            required
          />
        </div>

        <div class="field">
          <label for="modelYear">연식</label>
          <input
            id="modelYear"
            v-model.number="form.modelYear"
            type="number"
            placeholder="예: 2024"
            min="1900"
            :max="new Date().getFullYear() + 1"
            required
          />
        </div>

        <div class="actions">
          <button
            type="submit"
            class="btn-primary"
            :disabled="isSubmitting"
          >
            {{ isSubmitting ? '등록 중...' : '등록하기' }}
          </button>

          <button
            type="button"
            class="btn-secondary"
            @click="goBack"
            :disabled="isSubmitting"
          >
            취소
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import api from '../../api';

const emit = defineEmits(['back']);

const isSubmitting = ref(false);

const form = reactive({
  plateNumber: '',
  modelName: '',
  manufacturer: '',
  modelYear: new Date().getFullYear()
});

const submitForm = async () => {
  if (isSubmitting.value) return;

  try {
    isSubmitting.value = true;
    await api.post('/vehicles', form);

    alert('차량이 성공적으로 등록되었습니다.');
    emit('back');
  } catch (error) {
    console.error('차량 등록 실패:', error);
    alert('차량 등록 중 오류가 발생했습니다.');
  } finally {
    isSubmitting.value = false;
  }
};

const goBack = () => {
  emit('back');
};
</script>

<style scoped>
.add-vehicle-container {
  padding: var(--spacing-lg, 24px);
  max-width: 600px;
  margin: 0 auto;
}

.header {
  margin-bottom: var(--spacing-xl, 24px);
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary, #ffffff);
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary, #94a3b8);
  margin-top: 4px;
}

.form-card {
  background-color: var(--color-surface, #1e293b);
  border: 1px solid var(--color-border, #334155);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary, #94a3b8);
}

.field input {
  padding: 10px 14px;
  background-color: var(--color-surface-light, #0f172a);
  border: 1px solid var(--color-border, #334155);
  border-radius: 6px;
  color: var(--color-text-primary, #ffffff);
  font-size: 14px;
  outline: none;
}

.field input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 8px;
}

.btn-primary {
  background: var(--color-primary, #3b82f6);
  color: #ffffff;
  border: none;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 6px;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: var(--color-surface-light, #334155);
  color: var(--color-text-primary, #ffffff);
  border: 1px solid var(--color-border, #475569);
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.btn-secondary:hover {
  background-color: #475569;
  border-color: #64748b;
  color: #ffffff;
}
</style>