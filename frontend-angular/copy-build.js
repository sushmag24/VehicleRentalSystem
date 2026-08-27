const fs = require('fs');
const path = require('path');

const srcDir = path.join(__dirname, 'dist', 'frontend-angular', 'browser');
const destDirGateway = path.join(__dirname, '..', 'api-gateway', 'src', 'main', 'resources', 'static');
const destDirMonolith = path.join(__dirname, '..', 'monolith', 'src', 'main', 'resources', 'static');

function copyDir(src, dest) {
  if (!fs.existsSync(dest)) {
    fs.mkdirSync(dest, { recursive: true });
  }

  const entries = fs.readdirSync(src, { withFileTypes: true });

  for (let entry of entries) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);

    if (entry.isDirectory()) {
      copyDir(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
      console.log(`Copied: ${entry.name} -> ${dest}`);
    }
  }
}

try {
  console.log('Copying build files...');
  copyDir(srcDir, destDirGateway);
  copyDir(srcDir, destDirMonolith);
  console.log('Build files copied successfully to Gateway and Monolith!');
} catch (err) {
  console.error('Error during copying files:', err);
  process.exit(1);
}
