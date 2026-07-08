# Despliegue TaskCode (Docker)

## Estructura en el servidor

```text
/opt/taskcode/
├── .env                          ← secretos (no subir a Git)
├── TaskCodeBack/                   ← git clone
├── taskcodefront/                  ← git clone TaskCodeFront
└── (usa compose desde deploy/)
```

## 1. Clonar repos

```bash
mkdir -p /opt/taskcode && cd /opt/taskcode
git clone https://github.com/DiegoFlwrs/TaskCodeBack.git
git clone https://github.com/DiegoFlwrs/TaskCodeFront.git taskcodefront
```

## 2. Crear `.env`

```bash
cp TaskCodeBack/deploy/.env.example /opt/taskcode/.env
nano /opt/taskcode/.env
```

Generar JWT:

```bash
openssl rand -base64 48
```

`POSTGRES_PASSWORD` y `DB_PASSWORD` en el compose se toman de `POSTGRES_PASSWORD` — usa la misma clave que defines ahí.

## 3. Levantar servicios

Desde `/opt/taskcode`:

```bash
docker compose -f TaskCodeBack/deploy/docker-compose.yml --env-file .env up -d --build
```

Ver logs:

```bash
docker compose -f TaskCodeBack/deploy/docker-compose.yml --env-file .env logs -f
```

## 4. Probar

- Frontend: `http://TU_IP:3000`
- Backend:  `http://TU_IP:8080`

## 5. Actualizar después de un `git push`

```bash
/opt/taskcode/TaskCodeBack/deploy/deploy.sh          # rebuild todo
/opt/taskcode/TaskCodeBack/deploy/deploy.sh backend  # solo backend
/opt/taskcode/TaskCodeBack/deploy/deploy.sh frontend
```

## 6. Auto-deploy (GitHub Actions)

Al hacer **push/merge a `main`**, GitHub entra al servidor por SSH y ejecuta `deploy.sh`.

### Secrets en cada repo (Settings → Secrets → Actions)

| Secret | Valor |
|--------|--------|
| `SERVER_HOST` | `178.156.222.11` |
| `SERVER_USER` | `root` |
| `SSH_PRIVATE_KEY` | Clave privada SSH (`cat ~/.ssh/id_ed25519`) |

### Flujo

1. Trabajas en `develop`
2. Merge `develop` → `main` en GitHub
3. Actions despliega automáticamente

### Probar en el servidor

```bash
chmod +x /opt/taskcode/TaskCodeBack/deploy/deploy.sh
/opt/taskcode/TaskCodeBack/deploy/deploy.sh
```

## Notas

- Servidor 2 GB RAM: si falla el build, escala a 4 GB o build local y sube imágenes.
- Si cambias `NEXT_PUBLIC_API_URL`, hay que **rebuild** el frontend.
- Si cambias `CORS_ORIGINS`, reinicia el backend: `docker compose ... restart backend`
