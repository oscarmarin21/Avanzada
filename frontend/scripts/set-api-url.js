/**
 * Writes src/environments/environment.prod.ts with apiUrl from process.env.API_URL.
 * Use before ng build on Vercel: set API_URL to your Railway backend URL, then run
 *   node scripts/set-api-url.js && ng build --configuration=production
 */
const fs = require('fs');
const path = require('path');
const apiUrl = process.env.API_URL || '/api';
const content = `/** Production API base URL. Set via API_URL at build time (e.g. Vercel). */
export const environment = {
  production: true,
  apiUrl: '${apiUrl.replace(/'/g, "\\'")}',
};
`;
const outPath = path.join(__dirname, '..', 'src', 'environments', 'environment.prod.ts');
fs.writeFileSync(outPath, content, 'utf8');
console.log('Written environment.prod.ts with apiUrl:', apiUrl);
