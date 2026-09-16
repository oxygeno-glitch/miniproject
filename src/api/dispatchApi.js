import api from './index';

export const dispatchApi = {
  // 전체 배차 목록 조회
  getAllDispatches: () => api.get('/dispatches'),

  // 특정 배차 상세 조회
  getDispatchById: (id) => api.get(`/dispatches/${id}`),

  // 배차 등록
  createDispatch: (data) => api.post('/dispatches', data),

  // 배차 상태 변경
  updateDispatchStatus: (id, statusData) => api.patch(`/dispatches/${id}/status`, statusData),
};