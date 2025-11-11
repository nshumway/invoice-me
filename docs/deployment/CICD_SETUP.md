# CI/CD Setup Guide

Complete guide to set up automated deployment for InvoiceMe using GitHub Actions, Render, and Vercel.

## Overview

**Architecture:**
- **Backend**: Deployed to Render (free tier)
- **Frontend**: Deployed to Vercel (free tier)
- **Database**: Neon PostgreSQL (free tier)
- **CI/CD**: GitHub Actions

**What happens on push to `master`:**
1. GitHub Actions runs tests (backend + frontend)
2. Backend builds JAR file
3. Frontend builds production bundle
4. Backend auto-deploys to Render
5. Frontend auto-deploys to Vercel

---

## Prerequisites

- GitHub account
- Render account (https://render.com - sign up with GitHub)
- Vercel account (https://vercel.com - sign up with GitHub)
- Neon account (https://neon.tech - sign up with GitHub)

---

## Step 1: Set Up Neon Database (5 minutes)

1. Go to https://neon.tech and sign in with GitHub
2. Click "Create Project"
3. Project settings:
   - Name: `invoiceme`
   - PostgreSQL version: `15`
   - Region: Choose closest to you
4. Click "Create Project"
5. **Save the connection string** - it looks like:
   ```
   postgresql://username:password@ep-xxx.us-east-2.aws.neon.tech/neondb
   ```
6. That's it! Database is ready.

---

## Step 2: Deploy Backend to Render (10 minutes)

1. Go to https://dashboard.render.com
2. Sign up/in with GitHub
3. Click "New +" → "Web Service"
4. Click "Build and deploy from a Git repository" → Next
5. Connect your GitHub repository:
   - Find `invoice-me` in the list
   - Click "Connect"
   - If you don't see it, click "Configure account" to grant access

### Configure the Service

6. Fill in the configuration:
   - **Name**: `invoiceme-backend`
   - **Region**: Choose closest to you
   - **Branch**: `master` (or `feature/phase1-foundation` if not merged)
   - **Root Directory**: Leave empty
   - **Environment**: Select **Docker**
   - **Dockerfile Path**: `Dockerfile` (auto-detected)
   - **Docker Build Context Directory**: Leave empty
   - **Docker Command**: Leave empty (uses ENTRYPOINT from Dockerfile)

7. Choose **Free** plan (sleeps after 15 min inactivity)

### Add Environment Variables

8. Click "Advanced" to expand environment variables
9. Add these variables:
   - **DATABASE_URL**: `<paste your Neon connection string>`
   - **JWT_SECRET**: Run `openssl rand -base64 32` and paste the output
   - **SPRING_PROFILES_ACTIVE**: `prod`
   - **PORT**: `8080`

10. Click "Create Web Service"

Render will build the Docker image (~10 minutes for first deploy)

### Get Deploy Hook for GitHub Actions

1. In your Render service, go to "Settings"
2. Scroll to "Deploy Hook"
3. Click "Create Deploy Hook"
4. Copy the URL (looks like: `https://api.render.com/deploy/srv-xxx?key=xxx`)
5. Save this for Step 4

---

## Step 3: Deploy Frontend to Vercel (5 minutes)

1. Go to https://vercel.com/dashboard
2. Click "Add New..." → "Project"
3. Import your `invoice-me` GitHub repository
4. Configure project:
   - **Framework Preset**: Vite
   - **Root Directory**: `invoice-me-frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`

5. Add Environment Variable:
   - Name: `VITE_API_BASE_URL`
   - Value: `https://invoiceme-backend.onrender.com` (replace with your Render URL)

6. Click "Deploy"

### Get Vercel Tokens for GitHub Actions

1. Go to Account Settings → Tokens
2. Create new token:
   - Name: `GitHub Actions`
   - Scope: Full Account
   - Copy the token - save it!

3. Get Project ID and Org ID:
   ```bash
   # In your project directory
   cd invoice-me-frontend
   npx vercel link
   # Follow prompts, then:
   cat .vercel/project.json
   ```
   Save the `projectId` and `orgId`

---

## Step 4: Configure GitHub Secrets (5 minutes)

1. Go to your GitHub repository
2. Settings → Secrets and variables → Actions
3. Click "New repository secret"

Add these secrets:

| Secret Name | Value | Where to Get It |
|-------------|-------|-----------------|
| `RENDER_DEPLOY_HOOK_URL` | `https://api.render.com/deploy/srv-...` | Render service → Settings → Deploy Hook |
| `VERCEL_TOKEN` | `vercel_xxx...` | Vercel → Account Settings → Tokens |
| `VERCEL_ORG_ID` | `team_xxx` or `user_xxx` | `.vercel/project.json` (orgId) |
| `VERCEL_PROJECT_ID` | `prj_xxx` | `.vercel/project.json` (projectId) |

---

## Step 5: Test the CI/CD Pipeline

1. Make a small change to your code
2. Commit and push to `master`:
   ```bash
   git add .
   git commit -m "Test CI/CD pipeline"
   git push origin master
   ```

3. Watch GitHub Actions:
   - Go to your repo → Actions tab
   - You'll see two workflows running:
     - "Backend CI/CD"
     - "Frontend CI/CD"

4. Check deployments:
   - **Backend**: https://invoiceme-backend.onrender.com/actuator/health
   - **Frontend**: https://your-project.vercel.app

---

## Workflow Details

### Backend CI/CD (`.github/workflows/backend-ci.yml`)

**On every push/PR:**
1. Starts PostgreSQL test database
2. Runs Maven tests
3. Builds JAR file
4. Uploads JAR as artifact

**On push to master only:**
5. Triggers Render deploy hook
6. Render pulls latest code and redeploys

### Frontend CI/CD (`.github/workflows/frontend-ci.yml`)

**On every push/PR:**
1. Installs npm dependencies
2. Runs TypeScript type checking
3. Runs ESLint
4. Checks Prettier formatting
5. Builds production bundle
6. Uploads dist as artifact

**On push to master only:**
7. Deploys to Vercel production

---

## Troubleshooting

### Backend deployment fails on Render

**Check logs:**
- Render Dashboard → Your service → Logs

**Common issues:**
- Database connection string incorrect
- Missing environment variables
- Java version mismatch (need Java 17)

### Frontend deployment fails on Vercel

**Check logs:**
- Vercel Dashboard → Your project → Deployments → View Function Logs

**Common issues:**
- Wrong root directory (should be `invoice-me-frontend`)
- Missing `VITE_API_BASE_URL` environment variable
- Build command incorrect

### GitHub Actions fails

**Check workflow logs:**
- GitHub repo → Actions → Click on failed workflow

**Common issues:**
- Missing GitHub secrets
- Test failures (fix tests first)
- NPM package-lock.json conflicts

---

## Monitoring

### Backend Health Check
```bash
curl https://invoiceme-backend.onrender.com/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### Check Deployment Status

**Render:**
- Dashboard shows deployment status
- Logs show application startup

**Vercel:**
- Deployments page shows all deploys
- Each deploy has preview URL

---

## Cost Breakdown

| Service | Free Tier Limits | Cost if Exceeded |
|---------|------------------|------------------|
| **Render** | 750 hrs/month, sleeps after 15min | $7/month for always-on |
| **Vercel** | Unlimited deployments | $20/month for team features |
| **Neon** | 0.5GB storage, 1 project | $19/month for more |
| **GitHub Actions** | 2000 min/month | $0.008/min after |

**Total**: $0/month for free tier!

---

## Next Steps

1. ✅ Set up custom domain in Vercel
2. ✅ Configure CORS in backend for your Vercel domain
3. ✅ Set up monitoring/alerts
4. ✅ Add SSL certificate (automatic with Render/Vercel)
5. ✅ Configure CDN for static assets

---

## Useful Commands

```bash
# Force redeploy on Render
curl -X POST $RENDER_DEPLOY_HOOK_URL

# Check Vercel deployment
npx vercel ls

# View Vercel logs
npx vercel logs <deployment-url>

# Test backend locally
mvn spring-boot:run

# Test frontend locally
cd invoice-me-frontend && npm run dev
```

---

## Support

- **Render Docs**: https://render.com/docs
- **Vercel Docs**: https://vercel.com/docs
- **Neon Docs**: https://neon.tech/docs
- **GitHub Actions Docs**: https://docs.github.com/en/actions

---

## Security Notes

- Never commit secrets to Git
- Rotate JWT_SECRET regularly
- Use strong database passwords
- Enable 2FA on all accounts
- Review GitHub Actions logs for exposed secrets
