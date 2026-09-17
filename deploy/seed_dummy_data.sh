#!/usr/bin/env bash
# STUDIO G 더미 데이터 생성 스크립트
# 서버 로컬(127.0.0.1:8080)의 백엔드 API를 직접 호출해서 데모용 데이터를 넣습니다.
# 주의: 빈 DB에 한 번만 실행하세요. 다시 실행하면 번호판/면허번호/전화번호/노선번호
#      중복으로 에러가 납니다 (다시 넣고 싶으면 DB를 비우고 재실행하세요).

set -e

if ! command -v jq >/dev/null 2>&1; then
  echo "jq가 없어서 설치합니다..."
  apt-get update -y && apt-get install -y jq
fi

BASE="http://127.0.0.1:8080/api"

post() {
  curl -s -X POST -H "Content-Type: application/json" -d "$2" "$BASE$1"
}

patch() {
  curl -s -X PATCH -H "Content-Type: application/json" -d "$2" "$BASE$1"
}

echo "=== 1. 정류장 생성 ==="
S1=$(post /stops '{"stopCode":"ST001","stopName":"서울역","latitude":37.5546,"longitude":126.9706}' | jq -r .id)
S2=$(post /stops '{"stopCode":"ST002","stopName":"강남역","latitude":37.4979,"longitude":127.0276}' | jq -r .id)
S3=$(post /stops '{"stopCode":"ST003","stopName":"홍대입구역","latitude":37.5563,"longitude":126.9236}' | jq -r .id)
S4=$(post /stops '{"stopCode":"ST004","stopName":"잠실역","latitude":37.5133,"longitude":127.1001}' | jq -r .id)
S5=$(post /stops '{"stopCode":"ST005","stopName":"여의도역","latitude":37.5219,"longitude":126.9245}' | jq -r .id)
echo "정류장 ID: $S1 $S2 $S3 $S4 $S5"

echo "=== 2. 노선 생성 ==="
R1=$(post /routes "{\"routeNumber\":\"101\",\"routeName\":\"서울역-강남 순환\",\"routeType\":\"CITY\",\"stops\":[{\"stopId\":$S1,\"stopSequence\":1,\"estimatedMinutes\":0},{\"stopId\":$S2,\"stopSequence\":2,\"estimatedMinutes\":35}]}" | jq -r .id)
R2=$(post /routes "{\"routeNumber\":\"202\",\"routeName\":\"홍대-잠실 급행\",\"routeType\":\"EXPRESS\",\"stops\":[{\"stopId\":$S3,\"stopSequence\":1,\"estimatedMinutes\":0},{\"stopId\":$S4,\"stopSequence\":2,\"estimatedMinutes\":40}]}" | jq -r .id)
R3=$(post /routes "{\"routeNumber\":\"303\",\"routeName\":\"여의도 셔틀\",\"routeType\":\"SHUTTLE\",\"stops\":[{\"stopId\":$S5,\"stopSequence\":1,\"estimatedMinutes\":0},{\"stopId\":$S1,\"stopSequence\":2,\"estimatedMinutes\":20}]}" | jq -r .id)
echo "노선 ID: $R1 $R2 $R3"

echo "=== 3. 차량 생성 ==="
V1=$(post /vehicles '{"plateNumber":"서울70가1234","modelName":"유니버스","manufacturer":"현대자동차","modelYear":2022}' | jq -r .id)
V2=$(post /vehicles '{"plateNumber":"서울71나5678","modelName":"BS106","manufacturer":"자일대우버스","modelYear":2021}' | jq -r .id)
V3=$(post /vehicles '{"plateNumber":"서울72다9012","modelName":"그린시티","manufacturer":"현대자동차","modelYear":2023}' | jq -r .id)
V4=$(post /vehicles '{"plateNumber":"서울73라3456","modelName":"BS090","manufacturer":"자일대우버스","modelYear":2020}' | jq -r .id)
V5=$(post /vehicles '{"plateNumber":"서울74마7890","modelName":"일렉시티","manufacturer":"현대자동차","modelYear":2023}' | jq -r .id)
V6=$(post /vehicles '{"plateNumber":"서울75바2345","modelName":"K320","manufacturer":"스카니아","modelYear":2019}' | jq -r .id)
echo "차량 ID: $V1 $V2 $V3 $V4 $V5 $V6"

echo "=== 4. 기사 생성 ==="
D1=$(post /drivers '{"name":"김민준","gender":"남","age":45,"address":"서울시 강남구","phoneNumber":"010-1111-2222","licenseNumber":"11-22-333344-55","licenseType":"1종대형"}' | jq -r .id)
D2=$(post /drivers '{"name":"이서연","gender":"여","age":38,"address":"서울시 마포구","phoneNumber":"010-2222-3333","licenseNumber":"11-22-334455-66","licenseType":"1종대형"}' | jq -r .id)
D3=$(post /drivers '{"name":"박도윤","gender":"남","age":52,"address":"서울시 송파구","phoneNumber":"010-3333-4444","licenseNumber":"11-22-335566-77","licenseType":"1종대형"}' | jq -r .id)
D4=$(post /drivers '{"name":"최지우","gender":"여","age":29,"address":"서울시 영등포구","phoneNumber":"010-4444-5555","licenseNumber":"11-22-336677-88","licenseType":"1종대형"}' | jq -r .id)
D5=$(post /drivers '{"name":"정하준","gender":"남","age":41,"address":"서울시 종로구","phoneNumber":"010-5555-6666","licenseNumber":"11-22-337788-99","licenseType":"1종대형"}' | jq -r .id)
D6=$(post /drivers '{"name":"강수아","gender":"여","age":33,"address":"서울시 서대문구","phoneNumber":"010-6666-7777","licenseNumber":"11-22-338899-00","licenseType":"1종대형"}' | jq -r .id)
echo "기사 ID: $D1 $D2 $D3 $D4 $D5 $D6"

echo "=== 5. 기사/차량 상태 조정 ==="
patch /drivers/$D1/status '{"workStatus":"STANDBY"}' > /dev/null
patch /drivers/$D2/status '{"workStatus":"STANDBY"}' > /dev/null
patch /drivers/$D3/status '{"workStatus":"STANDBY"}' > /dev/null
patch /drivers/$D4/status '{"workStatus":"STANDBY"}' > /dev/null
patch /drivers/$D5/status '{"workStatus":"RETIRED"}' > /dev/null
# D6는 OFF_DUTY(출근 전) 상태로 둡니다.

patch /vehicles/$V6/status '{"status":"MAINTENANCE"}' > /dev/null
# V1~V5는 INACTIVE(운행 대기) 상태로 둡니다.

echo "=== 6. 배차/운행 생성 ==="
H_M3=$(date -d "-3 hours" +%Y-%m-%dT%H:%M:%S)
H_M2=$(date -d "-2 hours" +%Y-%m-%dT%H:%M:%S)
H_M1=$(date -d "-1 hours" +%Y-%m-%dT%H:%M:%S)
H_P1=$(date -d "+1 hours" +%Y-%m-%dT%H:%M:%S)
H_P2=$(date -d "+2 hours" +%Y-%m-%dT%H:%M:%S)
H_P3=$(date -d "+3 hours" +%Y-%m-%dT%H:%M:%S)
H_P4=$(date -d "+4 hours" +%Y-%m-%dT%H:%M:%S)

# A: 이미 완료된 운행
DA=$(post /dispatches "{\"routeId\":$R1,\"vehicleId\":$V1,\"driverId\":$D1,\"plannedStartTime\":\"$H_M3\",\"plannedEndTime\":\"$H_M2\"}" | jq -r .id)
patch /dispatches/$DA/status '{"dispatchStatus":"IN_PROGRESS"}' > /dev/null
patch /dispatches/$DA/status '{"dispatchStatus":"COMPLETED"}' > /dev/null

# B: 현재 운행 중
DB=$(post /dispatches "{\"routeId\":$R2,\"vehicleId\":$V2,\"driverId\":$D2,\"plannedStartTime\":\"$H_M1\",\"plannedEndTime\":\"$H_P1\"}" | jq -r .id)
patch /dispatches/$DB/status '{"dispatchStatus":"IN_PROGRESS"}' > /dev/null

# C: 예정된 운행 (미래, 정상)
DC=$(post /dispatches "{\"routeId\":$R3,\"vehicleId\":$V3,\"driverId\":$D3,\"plannedStartTime\":\"$H_P3\",\"plannedEndTime\":\"$H_P4\"}" | jq -r .id)

# D: 지연된 운행 (예정 시간이 이미 지났는데 아직 대기 상태)
DD=$(post /dispatches "{\"routeId\":$R1,\"vehicleId\":$V4,\"driverId\":$D4,\"plannedStartTime\":\"$H_M2\",\"plannedEndTime\":\"$H_M1\"}" | jq -r .id)

# E: 취소된 운행 (A가 끝나서 다시 대기 상태가 된 차량1/기사1 재사용)
DE=$(post /dispatches "{\"routeId\":$R2,\"vehicleId\":$V1,\"driverId\":$D1,\"plannedStartTime\":\"$H_P1\",\"plannedEndTime\":\"$H_P2\"}" | jq -r .id)
patch /dispatches/$DE/status '{"dispatchStatus":"CANCELED"}' > /dev/null

echo "배차 ID: 완료=$DA 진행중=$DB 예정=$DC 지연=$DD 취소=$DE"

echo "=== 7. 로그인 테스트 계정 생성 ==="
post /auth/signup '{"name":"데모계정","account":"demo","password":"demo1234"}' > /dev/null

echo ""
echo "더미 데이터 생성 완료!"
echo "로그인 계정: demo / demo1234"
