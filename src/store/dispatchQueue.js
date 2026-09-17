import { ref, computed } from "vue";
import api from "../api";

// ---------------------------------------------------------------------------
// 배차 관리(DispatchList.vue) 화면의 배차 목록 / "배차 신청 대기" 큐 상태를
// 컴포넌트가 아니라 이 모듈(파일) 스코프에 둡니다.
//
// Dashboard.vue는 메뉴를 v-if/v-else-if로 전환하기 때문에, 다른 메뉴로
// 이동했다가 배차 관리로 돌아오면 DispatchList.vue는 매번 새로 마운트됩니다.
// 상태가 컴포넌트 안에 있으면 그때마다 초기화되어 "배차 신청 대기" 카드와
// 진행 중이던 타이머(10분 자동 만료, 자동 운행 시작)가 사라져 버립니다.
// 이 모듈은 페이지를 새로고침하지 않는 한 한 번만 로드되는 싱글턴이므로,
// 여기에 상태를 두면 화면을 오가도 그대로 유지됩니다.
// ---------------------------------------------------------------------------

export const dispatches = ref([]);
export const selectedIds = ref([]);
export const loading = ref(false);

export const pendingRequests = ref([]);
export const selectedRequestIds = ref([]);
export const generating = ref(false);
let requestSeq = 0;

// 신청별 10분 자동 만료 타이머 (requestId -> timeout id)
const requestExpiryTimers = new Map();

const REQUEST_EXPIRY_MS = 10 * 60 * 1000;

const scheduleRequestExpiry = (requestId) => {
  const timeoutId = setTimeout(() => {
    requestExpiryTimers.delete(requestId);
    expireRequestAsFailed(requestId);
  }, REQUEST_EXPIRY_MS);

  requestExpiryTimers.set(requestId, timeoutId);
};

const clearRequestExpiry = (requestId) => {
  const timeoutId = requestExpiryTimers.get(requestId);

  if (timeoutId) {
    clearTimeout(timeoutId);
    requestExpiryTimers.delete(requestId);
  }
};

// 10분 동안 승인되지 않은 신청을 "배차 실패" 상태의 카드로 바꿔 아래 배차
// 목록에 표시합니다. 실제로 배차가 생성된 것은 아니므로 백엔드에는 저장되지
// 않는, 화면 표시용 데이터입니다.
const expireRequestAsFailed = (requestId) => {
  const request = pendingRequests.value.find(
    (item) => item.requestId === requestId
  );

  if (!request) {
    return;
  }

  dispatches.value = sortByStatus([
    ...dispatches.value,
    {
      id: `failed-${request.requestId}`,
      routeId: request.routeId,
      routeNumber: request.routeNumber,
      routeName: request.routeName,
      vehicleId: request.vehicleId,
      vehiclePlateNumber: request.vehiclePlateNumber,
      driverId: request.driverId,
      driverName: request.driverName,
      plannedStartTime: request.plannedStartTime,
      plannedEndTime: request.plannedEndTime,
      dispatchStatus: "FAILED",
      isFailedRequest: true,
    },
  ]);

  removePendingRequest(requestId);
};

// ---------------------------------------------------------------------------
// 배차 대기중 -> 운행 중 자동 전환
// "배차 대기중(SCHEDULED)" 상태인 배차는 수동으로 "운행 시작" 버튼을 누르지
// 않아도, 5초 ~ 27초 사이의 랜덤한 시간이 지나면 자동으로 "운행 중"으로
// 상태가 바뀝니다. 13~17초 구간(정점 15초)에서 나올 확률이 가장 높도록
// 삼각분포(triangular distribution)를 사용합니다.
// ---------------------------------------------------------------------------

const AUTO_START_MIN_MS = 5 * 1000;
const AUTO_START_MAX_MS = 27 * 1000;
const AUTO_START_MODE_MS = 15 * 1000;

const pickAutoStartDelayMs = () => {
  const min = AUTO_START_MIN_MS;
  const max = AUTO_START_MAX_MS;
  const mode = AUTO_START_MODE_MS;

  const u = Math.random();
  const modeRatio = (mode - min) / (max - min);

  if (u < modeRatio) {
    return min + Math.sqrt(u * (max - min) * (mode - min));
  }

  return max - Math.sqrt((1 - u) * (max - min) * (max - mode));
};

// 이미 자동 시작이 예약된 배차는 다시 예약하지 않도록 추적합니다.
const scheduledAutoStartIds = new Set();

const autoStartDispatch = async (id) => {
  const current = dispatches.value.find((dispatch) => dispatch.id === id);

  // 그 사이 취소되었거나 목록에서 사라졌으면 자동 시작하지 않습니다.
  if (!current || current.dispatchStatus !== "SCHEDULED") {
    return;
  }

  try {
    const response = await api.patch(`/dispatches/${id}/status`, {
      dispatchStatus: "IN_PROGRESS",
    });

    const index = dispatches.value.findIndex(
      (dispatch) => dispatch.id === id
    );

    if (index !== -1) {
      dispatches.value[index] = response.data;
    }
  } catch (error) {
    console.error("배차 자동 운행 시작 실패:", error);
  }
};

const ensureAutoStartScheduled = (dispatch) => {
  if (
    !dispatch ||
    dispatch.isFailedRequest ||
    dispatch.dispatchStatus !== "SCHEDULED" ||
    scheduledAutoStartIds.has(dispatch.id)
  ) {
    return;
  }

  scheduledAutoStartIds.add(dispatch.id);

  setTimeout(() => {
    scheduledAutoStartIds.delete(dispatch.id);
    autoStartDispatch(dispatch.id);
  }, pickAutoStartDelayMs());
};

const scheduleAutoStartForAll = (list) => {
  list.forEach((dispatch) => ensureAutoStartScheduled(dispatch));
};

// 1건이 나올 확률이 제일 높고, 7건이 나올 확률이 제일 낮도록 선형으로 감소하는 가중치
const REQUEST_COUNT_WEIGHTS = [7, 6, 5, 4, 3, 2, 1];

const pickRequestCount = () => {
  const total = REQUEST_COUNT_WEIGHTS.reduce((sum, weight) => sum + weight, 0);
  let roll = Math.random() * total;

  for (let i = 0; i < REQUEST_COUNT_WEIGHTS.length; i++) {
    roll -= REQUEST_COUNT_WEIGHTS[i];
    if (roll < 0) {
      return i + 1;
    }
  }

  return 1;
};

const shuffleArray = (list) => {
  const copy = [...list];

  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }

  return copy;
};

const pad2 = (n) => String(n).padStart(2, "0");

const toLocalDateTimeString = (date) => {
  return (
    `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}` +
    `T${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`
  );
};

export const generateDispatchRequests = async () => {
  if (generating.value) {
    return;
  }

  generating.value = true;

  try {
    const [routeResponse, vehicleResponse, driverResponse] = await Promise.all([
      api.get("/routes"),
      api.get("/vehicles"),
      api.get("/drivers"),
    ]);

    const activeRoutes = routeResponse.data.filter(
      (route) => route.status === "ACTIVE"
    );

    // 이미 대기 중인 신청이 예약하고 있는 차량/기사는 다시 뽑히지 않도록 제외
    const reservedVehicleIds = new Set(
      pendingRequests.value.map((request) => request.vehicleId)
    );
    const reservedDriverIds = new Set(
      pendingRequests.value.map((request) => request.driverId)
    );

    const availableVehicles = vehicleResponse.data.filter(
      (vehicle) =>
        vehicle.status === "INACTIVE" && !reservedVehicleIds.has(vehicle.id)
    );
    const availableDrivers = driverResponse.data.filter(
      (driver) =>
        driver.workStatus === "STANDBY" && !reservedDriverIds.has(driver.id)
    );

    if (
      activeRoutes.length === 0 ||
      availableVehicles.length === 0 ||
      availableDrivers.length === 0
    ) {
      alert("현재 배차 가능한 노선/차량/기사가 없어 신청을 생성하지 못했습니다.");
      return;
    }

    const requestedCount = pickRequestCount();
    const actualCount = Math.min(
      requestedCount,
      availableVehicles.length,
      availableDrivers.length
    );

    const pickedVehicles = shuffleArray(availableVehicles).slice(0, actualCount);
    const pickedDrivers = shuffleArray(availableDrivers).slice(0, actualCount);

    const now = new Date();
    const plannedStartTime = toLocalDateTimeString(now);
    const plannedEndTime = toLocalDateTimeString(
      new Date(now.getTime() + 60 * 60 * 1000)
    );

    for (let i = 0; i < actualCount; i++) {
      const route = activeRoutes[Math.floor(Math.random() * activeRoutes.length)];
      const vehicle = pickedVehicles[i];
      const driver = pickedDrivers[i];

      requestSeq += 1;
      const requestId = `req-${requestSeq}`;

      pendingRequests.value.push({
        requestId,
        routeId: route.id,
        routeNumber: route.routeNumber,
        routeName: route.routeName,
        vehicleId: vehicle.id,
        vehiclePlateNumber: vehicle.plateNumber,
        driverId: driver.id,
        driverName: driver.name,
        plannedStartTime,
        plannedEndTime,
        requestedAt: toLocalDateTimeString(now),
      });

      // 승인하지 않고 10분이 지나면 이 신청은 "배차 실패"로 바뀝니다.
      scheduleRequestExpiry(requestId);
    }
  } catch (error) {
    console.error("배차 신청 생성 실패:", error);
    alert("배차 신청 생성 중 오류가 발생했습니다.");
  } finally {
    generating.value = false;
  }
};

export const isAllRequestsSelected = computed(() => {
  if (pendingRequests.value.length === 0) {
    return false;
  }

  return selectedRequestIds.value.length === pendingRequests.value.length;
});

export const toggleAllRequests = (event) => {
  if (event.target.checked) {
    selectedRequestIds.value = pendingRequests.value.map(
      (request) => request.requestId
    );
  } else {
    selectedRequestIds.value = [];
  }
};

const removePendingRequest = (requestId) => {
  pendingRequests.value = pendingRequests.value.filter(
    (request) => request.requestId !== requestId
  );
  selectedRequestIds.value = selectedRequestIds.value.filter(
    (id) => id !== requestId
  );

  clearRequestExpiry(requestId);
};

export const approveRequest = async (request) => {
  try {
    const response = await api.post("/dispatches", {
      routeId: request.routeId,
      vehicleId: request.vehicleId,
      driverId: request.driverId,
      plannedStartTime: request.plannedStartTime,
      plannedEndTime: request.plannedEndTime,
    });

    dispatches.value = sortByStatus([...dispatches.value, response.data]);
    ensureAutoStartScheduled(response.data);
    removePendingRequest(request.requestId);
  } catch (error) {
    console.error(error);

    const message =
      error.response?.data?.message || "배차 승인에 실패했습니다.";

    alert(`${request.routeNumber || "-"} 배차 승인 실패: ${message}`);

    // 실패한 신청은 차량/기사 상태가 이미 바뀐 경우일 수 있어 목록에서 제거합니다.
    removePendingRequest(request.requestId);
  }
};

export const approveSelectedRequests = async () => {
  if (selectedRequestIds.value.length === 0) {
    alert("승인할 배차 신청을 선택해주세요.");
    return;
  }

  const targets = pendingRequests.value.filter((request) =>
    selectedRequestIds.value.includes(request.requestId)
  );

  if (!confirm(`선택한 ${targets.length}건의 배차 신청을 승인하시겠습니까?`)) {
    return;
  }

  let successCount = 0;
  let failCount = 0;
  const approvedDispatches = [];

  for (const request of targets) {
    try {
      const response = await api.post("/dispatches", {
        routeId: request.routeId,
        vehicleId: request.vehicleId,
        driverId: request.driverId,
        plannedStartTime: request.plannedStartTime,
        plannedEndTime: request.plannedEndTime,
      });

      approvedDispatches.push(response.data);
      removePendingRequest(request.requestId);
      successCount += 1;
    } catch (error) {
      console.error(error);
      removePendingRequest(request.requestId);
      failCount += 1;
    }
  }

  if (approvedDispatches.length > 0) {
    dispatches.value = sortByStatus([...dispatches.value, ...approvedDispatches]);
    approvedDispatches.forEach((dispatch) => ensureAutoStartScheduled(dispatch));
  }

  alert(
    failCount > 0
      ? `${successCount}건 승인 완료, ${failCount}건은 실패했습니다.`
      : `${successCount}건 승인 완료되었습니다.`
  );
};

// 화면 표시용으로 만든 "배차 실패" 카드는 실제 배차가 아니므로 선택/삭제
// 대상에서 제외합니다.
export const isAllSelected = computed(() => {
  const realDispatches = dispatches.value.filter(
    (dispatch) => !dispatch.isFailedRequest
  );

  if (realDispatches.length === 0) {
    return false;
  }

  return realDispatches.every((dispatch) =>
    selectedIds.value.includes(dispatch.id)
  );
});

// 상태 우선순위: 배차 대기중 > 운행 중 > 운행 종료 > 배차 취소 > 배차 실패
// 목록을 새로 불러올 때(최초 진입 또는 다른 메뉴에서 돌아왔을 때)만 이 순서로
// 정렬합니다. 상태를 바로 변경해도 그 자리에서 즉시 재정렬되지는 않고, 다음에
// 목록을 다시 불러올 때 반영됩니다.
const STATUS_ORDER = {
  SCHEDULED: 0,
  IN_PROGRESS: 1,
  COMPLETED: 2,
  CANCELED: 3,
  FAILED: 4
};

const sortByStatus = (list) => {
  return [...list].sort((a, b) => {
    const orderA = STATUS_ORDER[a.dispatchStatus] ?? 99;
    const orderB = STATUS_ORDER[b.dispatchStatus] ?? 99;
    return orderA - orderB;
  });
};

export const fetchDispatches = async () => {
  loading.value = true;

  try {
    const response = await api.get("/dispatches");

    // "배차 실패" 카드는 백엔드에 저장되지 않는 화면 표시용 데이터이므로,
    // 다시 불러올 때(다른 메뉴에 갔다가 돌아오는 경우 등) 사라지지 않도록
    // 기존에 표시되어 있던 실패 카드는 그대로 유지한 채 합칩니다.
    const failedCards = dispatches.value.filter(
      (dispatch) => dispatch.isFailedRequest
    );

    dispatches.value = sortByStatus([...response.data, ...failedCards]);
    selectedIds.value = [];
    scheduleAutoStartForAll(dispatches.value);
  } catch (error) {
    console.error(error);
    alert("배차 정보를 불러오지 못했습니다.");
  } finally {
    loading.value = false;
  }
};

export const toggleAll = (event) => {
  if (event.target.checked) {
    selectedIds.value = dispatches.value
      .filter((dispatch) => !dispatch.isFailedRequest)
      .map((dispatch) => dispatch.id);
  } else {
    selectedIds.value = [];
  }
};

// 배차 관리 화면에서는 "배차 대기중" 상태일 때 취소 버튼으로만 호출되므로
// CANCELED 확인 메시지만 사용합니다. (운행 시작/종료는 자동으로 처리되며
// 수동 버튼이 없습니다.)
export const updateStatus = async (id, status) => {
  const message =
    status === "CANCELED" ? "배차를 취소하시겠습니까?" : "";

  if (!confirm(message)) {
    return;
  }

  try {
    const response = await api.patch(`/dispatches/${id}/status`, {
      dispatchStatus: status,
    });

    // 목록을 다시 불러오면 정렬 순서가 바뀌어 방금 클릭한 행을 찾기 어려워지므로,
    // 해당 배차 데이터만 그 자리에서 갱신합니다. 정렬은 다음에 목록을 다시
    // 불러올 때(다른 메뉴로 이동했다가 돌아올 때 등) 반영됩니다.
    const index = dispatches.value.findIndex(
      (dispatch) => dispatch.id === id
    );

    if (index !== -1) {
      dispatches.value[index] = response.data;
    }
  } catch (error) {
    console.error(error);

    const message =
      error.response?.data?.message ||
      "배차 상태 변경에 실패했습니다.";

    alert(message);
  }
};

// 배차 삭제는 개별 행 버튼 대신, 체크박스로 선택한 뒤
// 상단의 "선택 삭제" 버튼으로 일괄 처리합니다.
export const deleteSelectedDispatches = async () => {
  if (selectedIds.value.length === 0) {
    alert("삭제할 배차를 선택해주세요.");
    return;
  }

  const selectedDispatches = dispatches.value.filter(
    (dispatch) => selectedIds.value.includes(dispatch.id)
  );

  const hasInProgress = selectedDispatches.some(
    (dispatch) => dispatch.dispatchStatus === "IN_PROGRESS"
  );

  if (hasInProgress) {
    alert("운행 중인 배차가 포함되어 있어 삭제할 수 없습니다.");
    return;
  }

  const confirmed = confirm(
    `선택한 ${selectedIds.value.length}개의 배차를 삭제하시겠습니까?`
  );

  if (!confirmed) {
    return;
  }

  try {
    await api.delete("/dispatches", {
      data: selectedIds.value,
    });

    alert("선택한 배차가 삭제되었습니다.");

    await fetchDispatches();
  } catch (error) {
    console.error(error);

    const message =
      error.response?.data?.message || "선택한 배차 삭제에 실패했습니다.";

    alert(message);
  }
};

// 예정 출발~도착 시간을 바탕으로 예상 운행 소요 시간을 계산합니다.
export const formatDuration = (startValue, endValue) => {
  if (!startValue || !endValue) {
    return "-";
  }

  const start = new Date(startValue);
  const end = new Date(endValue);
  const diffMs = end - start;

  if (Number.isNaN(diffMs) || diffMs <= 0) {
    return "-";
  }

  const totalMinutes = Math.round(diffMs / 60000);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  if (hours > 0 && minutes > 0) {
    return `${hours}시간 ${minutes}분`;
  }

  if (hours > 0) {
    return `${hours}시간`;
  }

  return `${minutes}분`;
};

export const formatDateTime = (dateString) => {
  if (!dateString) {
    return "-";
  }

  return dateString.replace("T", " ").substring(0, 16);
};

// 운행 관리(Operation.vue)·배차 상세(DispatchDetails.vue)와 동일한 상태 표기를
// 사용해 배차 상태 배지가 화면 전체에서 일관되게 보이도록 합니다.
export const getStatusText = (status) => {
  switch (status) {
    case "SCHEDULED":
      return "배차 대기중";
    case "IN_PROGRESS":
      return "운행 중";
    case "COMPLETED":
      return "운행 종료";
    case "CANCELED":
      return "배차 취소";
    case "FAILED":
      return "배차 실패";
    default:
      return status || "-";
  }
};

export const getStatusClass = (status) => {
  switch (status) {
    case "SCHEDULED":
      return "status-wait";
    case "IN_PROGRESS":
      return "status-on";
    case "COMPLETED":
      return "status-complete";
    case "CANCELED":
      return "status-danger";
    case "FAILED":
      return "status-danger";
    default:
      return "status-off";
  }
};
