import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '20s', target: 100 },
        { duration: '30s', target: 100 },
        { duration: '20s', target: 300 },
        { duration: '30s', target: 300 },
        { duration: '20s', target: 600 },
        { duration: '30s', target: 600 },
        { duration: '20s', target: 1000 },
        { duration: '30s', target: 1000 },
        { duration: '20s', target: 0 },
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
    // niente sleep: ogni VU martella l'endpoint il più velocemente possibile
}