# 🏥 EGESA Deployment Quick Start

## Using Your GitHub Student Developer Pack

### Step 1: Get Your Free $200 DigitalOcean Credit (5 minutes)

1. **Go to** [GitHub Education Pack](https://education.github.com/pack)
2. **Sign in** with your student GitHub account
3. **Click "Get benefits"** and verify your `.edu` email
4. **Find DigitalOcean** in the benefits list → **Claim credit**
5. **Create DigitalOcean account** (link to your GitHub)
6. **Apply the promotion code** (usually automatic with GitHub linking)

**You now have $200 free credits!** ✨

---

## Step 2: Deploy Your Server (Choose One)

### **Option A: App Platform (Recommended - Easiest)**

```powershell
# Nothing to do! DigitalOcean auto-deploys from your GitHub repo
# Just follow the guide in docs/DIGITALOCEAN_DEPLOYMENT_GUIDE.md
```

**Pros:**
- Auto-builds from GitHub
- No server management
- Free database included (limited)
- Auto-scaling
- SSL certificate auto-renew

**Cons:**
- More expensive ($12+/month)
- Less control

---

### **Option B: Droplet + GitHub Actions (More Control)**

```powershell
# 1. Create a Droplet in DigitalOcean Dashboard
# 2. Get your Droplet IP address
# 3. Set environment variable and deploy:

$env:DROPLET_IP = "your.droplet.ip.address"  # Replace with your IP
.\deploy.ps1 -Environment production -DropletIP $env:DROPLET_IP
```

**Pros:**
- Cheaper ($5/month for basic droplet)
- Full control
- Easy to customize
- Great for learning

**Cons:**
- You manage everything
- Need to handle SSL/backups

---

## Step 3: Configure Your Server

### Create `.env` file or set in DigitalOcean:

```env
DATABASE_URL=postgresql://user:password@db-host:5432/egesa_clinic
JWT_SECRET=your_super_secret_key_here_change_this
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your_supabase_anon_key
ENVIRONMENT=production
PORT=8080
```

---

## Step 4: Connect Your Android App

Update `local.properties` or `gradle.properties`:

```properties
# For local development
API_BASE_URL=http://10.0.2.2:8080     # Android emulator
# or
API_BASE_URL=http://your.droplet.ip:8080

# For production
API_BASE_URL=https://your-domain.com  # Or DigitalOcean App Platform URL
```

---

## Step 5: Monitor Your Deployment

### App Platform Dashboard:
```
DigitalOcean Dashboard → Apps → Your App → Logs
```

### Droplet SSH Access:
```powershell
ssh root@your.droplet.ip.address

# View logs
journalctl -u egesa-clinic-server -f

# Check health
curl http://localhost:8080/health
```

---

## 💰 Cost Breakdown (12 months with $200 credit)

| Plan | Monthly | Covers |
|------|---------|--------|
| **App Platform** (recommended) | $12 | ~16 months |
| **Droplet (5GB)** | $5 | ~40 months |
| **PostgreSQL DB** (if separate) | $15 | ~13 months |
| **Managed DB** | $25 | ~8 months |

**Recommendation:** Start with a $5 Droplet + managed database ($15). Your $200 credit covers 5-6 months easily, giving you time to launch and iterate.

---

## 📋 Quick Deployment Checklist

- [ ] GitHub Student Pack applied at education.github.com
- [ ] DigitalOcean account created + credit applied
- [ ] Read `docs/DIGITALOCEAN_DEPLOYMENT_GUIDE.md`
- [ ] Choose deployment method (App Platform or Droplet)
- [ ] Set up environment variables
- [ ] Run `./deploy.ps1` or use DigitalOcean Dashboard
- [ ] Test API at `https://your-server.com/health`
- [ ] Update Android app API base URL
- [ ] Test login flow end-to-end

---

## 🆘 Troubleshooting

| Issue | Solution |
|-------|----------|
| Build fails | Check logs: `journalctl -u egesa-clinic-server -f` |
| Database won't connect | Verify `DATABASE_URL` format in env vars |
| API not responding | SSH to droplet, verify app is running: `systemctl status egesa-clinic-server` |
| Domain not resolving | DNS takes 24-48h. Use IP address temporarily. |
| Out of credits | Migrate to AWS free tier or use cheaper plan |

---

## 🚀 Next Steps

1. **Follow the full guide** in `docs/DIGITALOCEAN_DEPLOYMENT_GUIDE.md`
2. **Set up CI/CD** with GitHub Actions (auto-deploy on push)
3. **Add custom domain** (namecheap.com, godaddy.com, etc.)
4. **Enable SSL** (auto with App Platform, use Certbot with Droplet)
5. **Set up monitoring** (DigitalOcean Monitoring or DataDog)
6. **Back up your database** regularly

---

**Your $200 credit = 5-6 months FREE! Deploy and iterate! 🎉**

