const fs = require('fs');
const path = require('path');

const srcDir = path.join(__dirname, 'dist', 'frontend-angular', 'browser');
const destDir = path.join(__dirname, '..', 'api-gateway', 'src', 'main', 'resources', 'static');

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
      console.log(`Copied: ${entry.name}`);
    }
  }
}

try {
  console.log('Copying build files...');
  copyDir(srcDir, destDir);
  console.log('Build files copied successfully!');
} catch (err) {
  console.error('Error during copying files:', err);
  process.exit(1);
}
