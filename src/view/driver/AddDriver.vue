<template>
  <div class="add-driver-container">
    <div class="header">
      <div>
        <h1 class="page-title">신규 기사 등록</h1>
        <p class="page-subtitle">새로운 운전 기사 정보를 등록합니다.</p>
      </div>
    </div>

    <div class="form-card">
      <form @submit.prevent="submitForm">
        <div class="form-group">
          <label for="name">이름</label>
          <input 
            id="name" 
            v-model.trim="form.name" 
            type="text" 
            placeholder="이름을 입력하세요" 
            autocomplete="off" 
            required 
          />
        </div>

        <div class="form-group">
          <label for="licenseNumber">면허 번호</label>
          <input
            id="licenseNumber"
            v-model.trim="form.licenseNumber"
            type="text"
            placeholder="예: 12-가-15312-90"
            autocomplete="off"
            required
          />
        </div>

        <!-- 커스텀 드롭다운 영역 -->
        <div class="form-group" v-click-outside="closeDropdown">
          <label>면허 종류</label>
          <div class="custom-select-wrapper" @click="toggleDropdown">
            <div :class="['custom-select-trigger', { 'is-selected': form.licenseType, 'is-open': isOpen }]">
              <span>{{ form.licenseType || '면허 종류를 선택하세요' }}</span>
              <svg class="dropdown-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
            
            <transition name="dropdown">
              <div v-if="isOpen" class="custom-options">
                <div 
                  v-for="type in licenseTypes" 
                  :key="type"
                  class="custom-option"
                  :class="{ 'selected': form.licenseType === type }"
                  @click.stop="selectLicenseType(type)"
                >
                  {{ type }}
                </div>
              </div>
            </transition>
          </div>
        </div>

        <div class="form-group">
          <label for="phone">연락처</label>
          <input
            id="phone"
            v-model="form.phoneNumber"
            type="text"
            placeholder="010-0000-0000"
            maxlength="13"
            autocomplete="off"
            @input="formatPhoneNumber"
            required
          />
        </div>

        <div class="button-group">
          <button type="submit" class="btn-primary" :disabled="isSubmitting">
            {{ isSubmitting ? '등록 중...' : '등록하기' }}
          </button>
          <button type="button" class="btn-secondary" @click="goBack">
            취소
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted } from "vue";
import api from "../../api";

const emit = defineEmits(["back"]);
const isSubmitting = ref(false);
const isOpen = ref(false);

const licenseTypes = ["1종 보통", "1종 대형", "1종 특수", "2종 보통"];

const form = reactive({
  name: "",
  licenseNumber: "",
  licenseType: "",
  phoneNumber: "",
});

const toggleDropdown = () => {
  isOpen.value = !isOpen.value;
};

const closeDropdown = () => {
  isOpen.value = false;
};

const selectLicenseType = (type) => {
  form.licenseType = type;
  isOpen.value = false;
};

// 외부 클릭 감지를 위한 커스텀 디렉티브 구현 대신 간단한 이벤트 리스너 처리
const handleClickOutside = (e) => {
  if (!e.target.closest('.custom-select-wrapper')) {
    isOpen.value = false;
  }
};

onMounted(() => {
  window.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  window.removeEventListener('click', handleClickOutside);
});

const formatPhoneNumber = (e) => {
  let value = e.target.value.replace(/[^0-9]/g, "");
  if (value.length > 11) value = value.slice(0, 11);

  if (value.length < 4) {
    form.phoneNumber = value;
  } else if (value.length < 8) {
    form.phoneNumber = `${value.slice(0, 3)}-${value.slice(3)}`;
  } else if (value.length < 11) {
    form.phoneNumber = `${value.slice(0, 3)}-${value.slice(3, 6)}-${value.slice(6)}`;
  } else {
    form.phoneNumber = `${value.slice(0, 3)}-${value.slice(3, 7)}-${value.slice(7, 11)}`;
  }
};

const submitForm = async () => {
  if (!form.licenseType) {
    alert("면허 종류를 선택해주세요.");
    return;
  }

  if (isSubmitting.value) return;

  try {
    isSubmitting.value = true;
    await api.post("/drivers", form);

    alert("기사가 성공적으로 등록되었습니다.");
    emit("back");
  } catch (error) {
    console.error("기사 등록 실패:", error);
    alert("기사 등록 중 오류가 발생했습니다.");
  } finally {
    isSubmitting.value = false;
  }
};

const goBack = () => {
  emit("back");
};
</script>

<style scoped>
.add-driver-container {
  padding: var(--spacing-xl, 32px) var(--spacing-lg, 24px);
  max-width: 800px;
  margin: 0 auto;
}

.header {
  margin-bottom: var(--spacing-xl, 24px);
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text-primary, #ffffff);
  letter-spacing: -0.02em;
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary, #94a3b8);
  margin-top: 6px;
}

.form-card {
  background-color: var(--color-surface, #1e293b);
  border: 1px solid var(--color-border, #334155);
  border-radius: var(--radius-lg, 12px);
  padding: 36px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.form-group {
  margin-bottom: 24px;
  position: relative;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary, #94a3b8);
  margin-bottom: 8px;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  background-color: var(--color-surface-light, #0f172a);
  border: 1px solid var(--color-border, #334155);
  border-radius: var(--radius-sm, 6px);
  color: var(--color-text-primary, #ffffff);
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.form-group input::placeholder {
  color: rgba(148, 163, 184, 0.4);
}

/* 커스텀 드롭다운 스타일 */
.custom-select-wrapper {
  position: relative;
  width: 100%;
  cursor: pointer;
}

.custom-select-trigger {
  width: 100%;
  padding: 12px 16px;
  background-color: var(--color-surface-light, #0f172a);
  border: 1px solid var(--color-border, #334155);
  border-radius: var(--radius-sm, 6px);
  color: rgba(148, 163, 184, 0.4); /* 기본 placeholder 색상 */
  font-size: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-sizing: border-box;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.custom-select-trigger.is-selected {
  color: var(--color-text-primary, #ffffff);
}

.custom-select-trigger.is-open {
  border-color: var(--color-primary, #3b82f6);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
  background-color: rgba(15, 23, 42, 0.8);
}

.dropdown-arrow {
  width: 16px;
  height: 16px;
  stroke: #94a3b8;
  transition: transform 0.25s ease;
}

.custom-select-trigger.is-open .dropdown-arrow {
  transform: rotate(180deg);
}

.custom-options {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  width: 100%;
  background-color: #0f172a;
  border: 1px solid var(--color-border, #334155);
  border-radius: var(--radius-sm, 6px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
  z-index: 100;
  overflow: hidden;
}

.custom-option {
  padding: 12px 16px;
  color: #94a3b8;
  font-size: 14px;
  transition: all 0.2s ease;
}

.custom-option:hover {
  background-color: #334155;
  color: #ffffff;
}

.custom-option.selected {
  background-color: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
  font-weight: 600;
}

/* 드롭다운 애니메이션 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.form-group input:-webkit-autofill,
.form-group input:-webkit-autofill:hover,
.form-group input:-webkit-autofill:focus,
.form-group input:-webkit-autofill:active {
  -webkit-box-shadow: 0 0 0 30px var(--color-surface-light, #0f172a) inset !important;
  -webkit-text-fill-color: var(--color-text-primary, #ffffff) !important;
  transition: background-color 5000s ease-in-out 0s;
}

.form-group input:focus {
  border-color: var(--color-primary, #3b82f6);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
  background-color: rgba(15, 23, 42, 0.8);
}

.button-group {
  display: flex;
  gap: 12px;
  margin-top: 36px;
  justify-content: flex-end;
}

.btn-primary {
  padding: 12px 28px;
  background: var(--color-primary-gradient, #3b82f6);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-sm, 6px);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 12px 24px;
  background-color: transparent;
  color: var(--color-text-secondary, #94a3b8);
  border: 1px solid var(--color-border, #334155);
  border-radius: var(--radius-sm, 6px);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
}

.btn-secondary:hover {
  background-color: var(--color-surface-light, #0f172a);
  color: var(--color-text-primary, #ffffff);
  border-color: var(--color-text-secondary, #94a3b8);
}
</style>