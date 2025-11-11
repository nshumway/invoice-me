# Frontend Deployment Guide - Render

This guide explains how to deploy the InvoiceMe frontend to Render.

## Prerequisites

- Render account (free tier available at https://render.com)
- GitHub repository connected to Render
- Backend API deployed and running

## Step 1: Create Static Site on Render

1. Log in to your Render dashboard
2. Click "New +" → "Static Site"
3. Connect your GitHub repository (`NShumway/invoice-me`)
4. Configure the static site:

### Basic Settings
- **Name:** `invoice-me-frontend` (or your preferred name)
- **Branch:** `master`
- **Root Directory:** `invoice-me-frontend`
- **Build Command:** `npm install && npm run build`
- **Publish Directory:** `invoice-me-frontend/dist`

### Environment Variables
You'll need to set these in the Render dashboard:

- `VITE_API_URL`: Your backend API URL (e.g., `https://invoice-me-backend.onrender.com/api`)

**Important:** Vite requires environment variables to be prefixed with `VITE_` to be accessible in the frontend.

### Advanced Settings (Optional)
- **Auto-Deploy:** Yes (recommended for automatic deployments on push to master)
- **Pull Request Previews:** Yes (optional, useful for testing PRs)

## Step 2: Configure CORS on Backend

Make sure your backend allows requests from your frontend domain:

```java
// In SecurityConfig.java or equivalent
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:5173",  // Local development
        "https://invoice-me-frontend.onrender.com"  // Production frontend
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

## Step 3: Get Deploy Hook URL

1. In your Render static site dashboard, go to **Settings**
2. Scroll down to **Deploy Hook**
3. Click "Create Deploy Hook"
4. Copy the webhook URL

## Step 4: Add GitHub Secret

1. Go to your GitHub repository settings
2. Navigate to **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add:
   - **Name:** `RENDER_FRONTEND_DEPLOY_HOOK_URL`
   - **Value:** The deploy hook URL from Step 3

## Step 5: Update Frontend API Client (if needed)

Make sure your `invoice-me-frontend/src/api/client.ts` uses the environment variable:

```typescript
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});
```

## Step 6: Deploy!

Once everything is configured:

1. Push changes to `master` branch
2. GitHub Actions will automatically:
   - Run all frontend tests
   - Build the production bundle
   - Trigger Render deployment via webhook
3. Render will:
   - Pull latest code
   - Run build command
   - Deploy to CDN

## Verification

After deployment:

1. Visit your Render static site URL (e.g., `https://invoice-me-frontend.onrender.com`)
2. Open browser DevTools → Network tab
3. Verify API calls are going to your backend URL
4. Test login/signup functionality
5. Test creating customers, invoices, and line items

## Troubleshooting

### "Failed to fetch" or CORS errors
- Check backend CORS configuration
- Verify `VITE_API_URL` environment variable is set correctly
- Ensure backend is running and accessible

### Environment variables not working
- Make sure they're prefixed with `VITE_`
- Rebuild the site after adding/changing environment variables
- Check Render build logs for errors

### Build fails
- Check Render build logs
- Verify `package.json` scripts are correct
- Ensure all dependencies are in `dependencies` not `devDependencies`

### 404 on page refresh
For a Single Page Application (SPA), you may need to add a `_redirects` file:

Create `invoice-me-frontend/public/_redirects`:
```
/*    /index.html   200
```

This ensures all routes are handled by React Router.

## Monitoring

- **Build Logs:** Available in Render dashboard
- **Deploy Status:** GitHub Actions workflow status
- **Performance:** Render provides analytics in dashboard

## Cost

Render's free tier includes:
- Static sites hosted on global CDN
- Automatic SSL certificates
- Unlimited bandwidth
- Custom domains (optional)

For production, consider upgrading to paid tier for:
- Faster builds
- More concurrent builds
- Priority support

## Alternative: Using render.yaml (Infrastructure as Code)

If you prefer to manage both backend and frontend together, create a `render.yaml`:

```yaml
services:
  # Backend service
  - type: web
    name: invoice-me-backend
    env: java
    buildCommand: mvn clean package -DskipTests
    startCommand: java -jar target/invoice-me-*.jar
    envVars:
      - key: DATABASE_URL
        fromDatabase:
          name: invoice-me-db
          property: connectionString
      - key: JWT_SECRET
        generateValue: true

  # Frontend static site
  - type: web
    name: invoice-me-frontend
    env: static
    buildCommand: cd invoice-me-frontend && npm install && npm run build
    staticPublishPath: invoice-me-frontend/dist
    envVars:
      - key: VITE_API_URL
        value: https://invoice-me-backend.onrender.com/api

databases:
  - name: invoice-me-db
    databaseName: invoiceme
    user: invoiceme
```

This allows you to deploy both services with a single configuration file.
