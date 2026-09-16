import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 10 },   // sale gradualmente a 10 utenti
        { duration: '30s', target: 10 },   // mantiene 10 utenti per 30s
        { duration: '10s', target: 50 },   // sale a 50 utenti
        { duration: '30s', target: 50 },   // mantiene 50 utenti per 30s
        { duration: '10s', target: 0 },    // scende a 0 (raffreddamento)
    ],
    thresholds: {
        http_req_duration: ['p(95)<200'], // soglia: 95% delle richieste sotto 200ms
        http_req_failed: ['rate<0.01'],   // soglia: meno dell'1% di errori
    },
};

export default function () {
    const res = http.get('http://localhost:8000/api/products');

    check(res, {
        'status 200': (r) => r.status === 200,
    });

    sleep(1);
}