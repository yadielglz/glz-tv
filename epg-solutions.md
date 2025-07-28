# EPG CORS Solutions for GLZ TV

## 🚀 **Solution 1: Local Proxy Server (Recommended)**

### Setup:
1. Install dependencies:
   ```bash
   npm install
   ```

2. Start the proxy server:
   ```bash
   npm start
   ```

3. Access your app at: `http://localhost:3000`

### Benefits:
- ✅ No CORS issues
- ✅ Fast and reliable
- ✅ Caching support
- ✅ Full control over requests

---

## 🌐 **Solution 2: Browser Extension**

### Install CORS Unblock Extension:
1. Chrome: "CORS Unblock" or "Allow CORS"
2. Firefox: "CORS Everywhere"

### Usage:
- Enable the extension
- Refresh your app
- EPG will work without server

---

## 🔧 **Solution 3: Browser Launch with Disabled Security**

### Chrome:
```bash
chrome.exe --user-data-dir="C:/temp/chrome_dev" --disable-web-security --disable-features=VizDisplayCompositor
```

### Firefox:
```bash
firefox.exe -P "dev" --disable-web-security
```

---

## 📦 **Solution 4: Electron App**

Convert to Electron app to bypass CORS entirely:

```javascript
// main.js
const { app, BrowserWindow } = require('electron');

function createWindow() {
  const win = new BrowserWindow({
    webPreferences: {
      nodeIntegration: true,
      webSecurity: false
    }
  });
  
  win.loadFile('index.html');
}

app.whenReady().then(createWindow);
```

---

## 🌍 **Solution 5: Deploy to Server**

### Netlify/Vercel:
1. Upload files to GitHub
2. Connect to Netlify/Vercel
3. Add serverless function for EPG proxy

### Example Netlify Function:
```javascript
// functions/epg.js
const fetch = require('node-fetch');

exports.handler = async (event) => {
  const response = await fetch('https://epg.best/1eef8-shw7fc.xml.gz');
  const data = await response.text();
  
  return {
    statusCode: 200,
    headers: { 'Content-Type': 'application/xml' },
    body: data
  };
};
```

---

## 🎯 **Recommended Approach:**

1. **Development**: Use local proxy server
2. **Production**: Deploy with serverless function
3. **Testing**: Use browser extension

The local proxy server is the most reliable solution for development! 