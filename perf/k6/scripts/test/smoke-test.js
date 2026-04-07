import http from "k6/http";
import { check, sleep } from "k6";

// 환경변수로 BASE_URL 주입: k6 run -e BASE_URL=http://... smoke-test.js
const BASE_URL = __ENV.BASE_URL || "http://host.docker.internal:8080";

export const options = {
  stages: [
    { duration: "30s", target: 10 },  // 워밍업
    { duration: "1m",  target: 10 },  // 유지
    { duration: "30s", target: 0  },  // 쿨다운
  ],
  thresholds: {
    http_req_duration: ["p(95)<500"],  // 95%ile < 500ms
    http_req_failed:   ["rate<0.01"],  // 에러율 < 1%
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/actuator/health`);
  check(res, {
    "status 200": (r) => r.status === 200,
  });
  sleep(1);
}
