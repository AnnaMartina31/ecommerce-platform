import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '20s', target: 50 },    // sale a 50 utenti
        { duration: '30s', target: 50 },    // mantiene 50
        { duration: '20s', target: 150 },   // sale a 150 utenti
        { duration: '30s', target: 150 },   // mantiene 150
        { duration: '20s', target: 300 },   // sale a 300 utenti
        { duration: '30s', target: 300 },   // mantiene 300
        { duration: '20s', target: 0 },     // raffreddamento
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // soglia più permissiva, ci aspettiamo che degradi
        http_req_failed: ['rate<0.05'],   // tolleriamo fino al 5% di errori prima di considerarlo un fallimento
    },
};

export default function () {
    const res = http.get('http://localhost:8000/api/products');

    check(res, {
        'status 200': (r) => r.status === 200,
    });

    sleep(1);
}