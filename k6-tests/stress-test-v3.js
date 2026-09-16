import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '20s', target: 200 },
        { duration: '20s', target: 200 },
        { duration: '20s', target: 350 },
        { duration: '20s', target: 350 },
        { duration: '20s', target: 500 },
        { duration: '20s', target: 500 },
        { duration: '20s', target: 700 },
        { duration: '20s', target: 700 },
        { duration: '15s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        http_req_failed: ['rate<0.1'],
    },
};

export default function () {
    const res = http.get('http://localhost:8000/api/products');
    check(res, {
        'status 200': (r) => r.status === 200,
    });
}