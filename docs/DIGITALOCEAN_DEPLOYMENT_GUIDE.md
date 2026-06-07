# DigitalOcean Deployment Guide for EGESA Medical Clinic System

## Step 1: Get GitHub Student Developer Pack Credits

### 1.1 Verify Your Student Status
- Go to **[GitHub Education](https://education.github.com/pack)**
- Sign in with your student GitHub account
- Click **"Get benefits"** and verify your student email
- You'll receive **$200 DigitalOcean credits** (valid for 1 year) for free

### 1.2 Create DigitalOcean Account
- Go to **[DigitalOcean](https://www.digitalocean.com)** → **Sign Up**
- Use the same email as your GitHub student account (recommended)
- Select **GitHub** or link your GitHub account to DigitalOcean
- Apply your GitHub Student credits (you should see a prompt or promotion code in the GitHub Education page)

---

## Step 2: Set Up DigitalOcean App Platform (Recommended for Kotlin/Java Server)

### 2.1 Create DigitalOcean App
1. Log into **DigitalOcean Dashboard**
2. Click **Apps** (left sidebar) → **Create App**
3. Select **GitHub** as the source
4. Authorize DigitalOcean to access your GitHub account
5. Select the **egesa-medical-clinic-mobile-app** repository
6. Choose a branch (e.g., `main` or `develop`)

### 2.2 Configure the Server Component
1. In the App Platform builder:
   - **Name your app**: `egesa-clinic-server`
   - **Region**: Choose closest to your users (e.g., `lon` for Europe, `nyc` for North America)

2. DigitalOcean will auto-detect your project structure. You may need to configure:
   - **Source Directory**: `server/` (if it asks)
   - **Build Command**: 
     ```bash
     ./gradlew build -x test
     ```
   - **Run Command**: 
     ```bash
     ./build/distributions/server/bin/server
     ```

### 2.3 Set Environment Variables
In the App Platform settings, add these environment variables:

```env
DATABASE_URL=your_database_connection_string
JWT_SECRET=your_jwt_secret_key
SUPABASE_URL=your_supabase_url
SUPABASE_ANON_KEY=your_supabase_anon_key
ENVIRONMENT=production
```

### 2.4 Add Database (PostgreSQL)
- In App Platform: Click **Create a New Database**
- Choose **PostgreSQL** (version 14+)
- Name it: `egesa-clinic-db`
- The connection string will auto-populate in `DATABASE_URL`

### 2.5 Deploy!
1. Click **Create App**
2. DigitalOcean will:
   - Clone your GitHub repo
   - Build the Gradle project
   - Deploy to a containerized environment
   - Assign a public URL (e.g., `https://egesa-clinic-server-xxxx.ondigitalocean.app`)

---

## Step 3: Alternative - Manual Deployment (Droplet)

If you prefer more control, use a Droplet:

### 3.1 Create a Droplet
1. **DigitalOcean Dashboard** → **Create** → **Droplets**
2. Choose:
   - **OS**: Ubuntu 22.04 LTS
   - **Plan**: Basic ($5/month starter is fine; student credits cover this)
   - **Region**: Closest to your users
   - **Authentication**: SSH Key (recommended)

### 3.2 SSH into Your Droplet
```bash
ssh root@your_droplet_ip
```

### 3.3 Install Dependencies
```bash
# Update system
apt update && apt upgrade -y

# Install Java
apt install -y openjdk-17-jdk

# Install PostgreSQL
apt install -y postgresql postgresql-contrib

# Install Nginx (reverse proxy)
apt install -y nginx

# Install Git
apt install -y git
```

### 3.4 Clone and Build Your Server
```bash
cd /opt
git clone https://github.com/YOUR_USERNAME/egesa-medical-clinic-mobile-app.git
cd egesa-medical-clinic-mobile-app
./gradlew build -x test
```

### 3.5 Set Up Systemd Service
Create `/etc/systemd/system/egesa-clinic-server.service`:

```ini
[Unit]
Description=EGESA Clinic Medical Server
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/egesa-medical-clinic-mobile-app
ExecStart=/opt/egesa-medical-clinic-mobile-app/build/distributions/server/bin/server
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
systemctl enable egesa-clinic-server
systemctl start egesa-clinic-server
systemctl status egesa-clinic-server
```

### 3.6 Configure Nginx as Reverse Proxy
Edit `/etc/nginx/sites-available/default`:

```nginx
server {
    listen 80;
    server_name your_domain_or_ip;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Restart Nginx:
```bash
nginx -t
systemctl restart nginx
```

---

## Step 4: Set Up GitHub Actions for CI/CD

Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy to DigitalOcean

on:
  push:
    branches:
      - main
      - develop

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build with Gradle
        run: ./gradlew :server:build -x test
      
      - name: Deploy to DigitalOcean (via App Platform webhook)
        run: |
          curl -X POST ${{ secrets.DIGITALOCEAN_WEBHOOK_URL }} \
            -H "Content-Type: application/json" \
            -d '{"ref": "main"}'
```

---

## Step 5: Custom Domain & SSL

### 5.1 Point Domain to DigitalOcean
1. Go to your domain registrar (Namecheap, GoDaddy, etc.)
2. Update **Nameservers** to DigitalOcean's:
   ```
   ns1.digitalocean.com
   ns2.digitalocean.com
   ns3.digitalocean.com
   ```

### 5.2 Add Domain in DigitalOcean
1. **Networking** → **Domains** → **Add Domain**
2. Follow the prompts to verify ownership

### 5.3 Enable Free SSL (Let's Encrypt)
- **App Platform**: Auto-enabled
- **Droplet + Nginx**: Install Certbot
  ```bash
  apt install -y certbot python3-certbot-nginx
  certbot --nginx -d your_domain.com
  ```

---

## Step 6: Monitor & Logs

### App Platform Logs
- **DigitalOcean Dashboard** → **Apps** → Your App → **Logs**

### Droplet Logs
```bash
# View Nginx logs
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log

# View app logs (if using systemd)
journalctl -u egesa-clinic-server -f
```

---

## Troubleshooting

### App won't start
- Check logs in DigitalOcean Dashboard
- Verify `DATABASE_URL` and environment variables are set
- Ensure port `8080` is exposed

### Database connection fails
- Confirm PostgreSQL is running: `systemctl status postgresql`
- Check connection string format

### Domain not resolving
- DNS can take 24-48 hours to propagate
- Use `nslookup your_domain.com` to check

---

## Quick Checklist
- [ ] GitHub Student Developer Pack applied
- [ ] DigitalOcean account created with student credits
- [ ] App Platform or Droplet deployed
- [ ] Environment variables configured
- [ ] Database created and connected
- [ ] Nginx/SSL configured (if Droplet)
- [ ] GitHub Actions workflow set up
- [ ] Domain pointed to DigitalOcean

---

## Cost Estimate (with $200 student credit)
- **App Platform**: $12/month (includes database for free tier with limits)
- **Droplet (5GB)**: $5/month
- **Extra Database (if needed)**: $15/month
- **Your $200 credit covers ~12-24 months** depending on plan

**Recommended for startups**: Start with App Platform (simpler), migrate to Droplet if you need more control.

---

Done! You should now have your EGESA server running on DigitalOcean with your student credits. 🚀

