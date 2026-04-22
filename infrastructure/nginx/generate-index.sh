#!/bin/sh

OUTPUT="/usr/share/nginx/html/index.html"
HAS_REPORTS=0

cat > "$OUTPUT" <<'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Intelligent DevSecOps Framework Findings Portal</title>
  <style>
    :root {
      --primary-red: #d60000;
      --primary-yellow: #f4d000;
      --primary-blue: #1837d8;
      --dark: #111111;
      --muted: #5b6470;
      --bg: linear-gradient(180deg, #fff9db 0%, #fffdf2 45%, #f7f8fc 100%);
      --card-bg: rgba(255, 255, 255, 0.96);
      --line: rgba(0, 0, 0, 0.08);
      --shadow: 0 10px 28px rgba(0, 0, 0, 0.10);
      --radius-xl: 22px;
      --radius-lg: 16px;
    }

    * {
      box-sizing: border-box;
    }

    body {
      margin: 0;
      font-family: Arial, Helvetica, sans-serif;
      background: var(--bg);
      color: var(--dark);
    }

    .topbar {
      position: relative;
      overflow: hidden;
      background:
        linear-gradient(135deg, rgba(214, 0, 0, 0.96), rgba(24, 55, 216, 0.92)),
        linear-gradient(180deg, #d60000, #1837d8);
      color: #fff;
      padding: 32px 24px 42px;
      border-bottom: 6px solid var(--primary-yellow);
      box-shadow: 0 8px 24px rgba(0,0,0,0.18);
    }

    .topbar::before,
    .topbar::after {
      content: "";
      position: absolute;
      border-radius: 50%;
      background: rgba(255,255,255,0.08);
    }

    .topbar::before {
      width: 240px;
      height: 240px;
      right: -60px;
      top: -90px;
    }

    .topbar::after {
      width: 180px;
      height: 180px;
      left: -40px;
      bottom: -80px;
    }

    .hero {
      position: relative;
      z-index: 1;
      max-width: 1180px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      gap: 24px;
      flex-wrap: wrap;
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 18px;
      flex: 1 1 100%;
      margin-bottom: 8px;
    }

    .logo-wrap {
      width: 96px;
      height: 96px;
      background: rgba(255,255,255,0.12);
      border: 3px solid rgba(255,255,255,0.30);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      backdrop-filter: blur(4px);
      box-shadow: 0 8px 20px rgba(0,0,0,0.18);
      flex: 0 0 auto;
    }

    .logo-wrap img {
      width: 76px;
      height: 76px;
      object-fit: contain;
      border-radius: 50%;
      background: #fff;
      padding: 4px;
    }

    .brand-text {
      min-width: 220px;
    }

    .brand-name {
      margin: 0;
      font-size: 30px;
      font-weight: 800;
      line-height: 1.2;
      letter-spacing: 0.3px;
    }

    .brand-subtitle {
      margin: 6px 0 0;
      font-size: 14px;
      color: rgba(255,255,255,0.88);
      line-height: 1.6;
    }

    .hero-text {
      flex: 1 1 520px;
      min-width: 280px;
    }

    .hero-kicker {
      display: inline-block;
      padding: 7px 12px;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.8px;
      text-transform: uppercase;
      color: #111;
      background: var(--primary-yellow);
      border-radius: 999px;
      margin-bottom: 12px;
    }

    .hero-title {
      margin: 0;
      font-size: 32px;
      line-height: 1.2;
      font-weight: 800;
    }

    .hero-subtitle {
      margin: 10px 0 0;
      font-size: 15px;
      line-height: 1.7;
      color: rgba(255,255,255,0.92);
      max-width: 760px;
    }

    .page {
      max-width: 1180px;
      margin: 30px auto 42px;
      padding: 0 20px;
    }

    .summary {
      background: var(--card-bg);
      border: 1px solid rgba(214,0,0,0.10);
      border-top: 5px solid var(--primary-red);
      border-radius: var(--radius-xl);
      padding: 22px 24px;
      box-shadow: var(--shadow);
      margin-bottom: 26px;
    }

    .summary-title {
      margin: 0 0 8px;
      font-size: 20px;
      font-weight: 800;
      color: #1a1a1a;
    }

    .summary-text {
      margin: 0;
      font-size: 14px;
      line-height: 1.8;
      color: var(--muted);
    }

    .section-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin: 0 0 14px;
      flex-wrap: wrap;
    }

    .section-title {
      margin: 0;
      font-size: 20px;
      font-weight: 800;
      color: #1a1a1a;
    }

    .section-badge {
      display: inline-block;
      padding: 8px 12px;
      border-radius: 999px;
      background: #fff4b8;
      color: #5f4700;
      font-size: 12px;
      font-weight: 700;
      border: 1px solid rgba(95, 71, 0, 0.12);
    }

    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(290px, 1fr));
      gap: 18px;
    }

    .card {
      position: relative;
      display: block;
      text-decoration: none;
      background: var(--card-bg);
      border: 1px solid var(--line);
      border-radius: var(--radius-lg);
      padding: 20px;
      color: inherit;
      box-shadow: var(--shadow);
      transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
      overflow: hidden;
    }

    .card::before {
      content: "";
      position: absolute;
      inset: 0 auto 0 0;
      width: 6px;
      background: linear-gradient(180deg, var(--primary-red), var(--primary-yellow), var(--primary-blue));
    }

    .card:hover {
      transform: translateY(-4px);
      box-shadow: 0 16px 32px rgba(0,0,0,0.14);
      border-color: rgba(24,55,216,0.22);
    }

    .card-top {
      display: flex;
      align-items: center;
      gap: 14px;
      margin-bottom: 14px;
    }

    .folder-icon {
      width: 56px;
      height: 56px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      background: linear-gradient(135deg, rgba(244,208,0,0.22), rgba(24,55,216,0.12));
      border: 1px solid rgba(24,55,216,0.10);
      flex: 0 0 auto;
    }

    .card-title {
      margin: 0;
      font-size: 17px;
      line-height: 1.4;
      font-weight: 800;
      color: #162033;
      word-break: break-word;
    }

    .card-desc {
      margin: 0;
      font-size: 13px;
      line-height: 1.7;
      color: var(--muted);
    }

    .card-footer {
      margin-top: 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
    }

    .tag {
      display: inline-block;
      padding: 7px 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 700;
      background: #fef3c7;
      color: #7c4a03;
      border: 1px solid rgba(124, 74, 3, 0.10);
    }

    .action {
      font-size: 13px;
      font-weight: 800;
      color: var(--primary-blue);
    }

    .empty-state {
      background: var(--card-bg);
      border: 1px dashed rgba(0,0,0,0.15);
      border-radius: var(--radius-lg);
      padding: 24px;
      box-shadow: var(--shadow);
    }

    .empty-state h3 {
      margin: 0 0 8px;
      font-size: 18px;
    }

    .empty-state p {
      margin: 0;
      color: var(--muted);
      line-height: 1.7;
      font-size: 14px;
    }

    .footer {
      max-width: 1180px;
      margin: 0 auto 28px;
      padding: 0 20px;
      color: #6b7280;
      font-size: 12px;
      text-align: center;
    }

    @media (max-width: 640px) {
      .brand {
        align-items: center;
      }

      .brand-name {
        font-size: 24px;
      }

      .hero-title {
        font-size: 26px;
      }

      .logo-wrap {
        width: 84px;
        height: 84px;
      }

      .logo-wrap img {
        width: 64px;
        height: 64px;
      }
    }
  </style>
</head>
<body>
  <header class="topbar">
    <div class="hero">
      <div class="brand">
        <div class="logo-wrap">
          <img src="/assets/logo.png" alt="Pamulang University Logo">
        </div>
        <div class="brand-text">
          <h1 class="brand-name">Pamulang University</h1>
          <p class="brand-subtitle">
            Intelligent DevSecOps Framework Findings Portal
          </p>
        </div>
      </div>

      <div class="hero-text">
        <div class="hero-kicker">CI/CD Security Report Portal</div>
        <h2 class="hero-title">Automated Findings Repository</h2>
        <p class="hero-subtitle">
          Centralized access to CI/CD findings reports generated by the Intelligent DevSecOps Framework
          to support secure, reliable, and accountable software delivery for Pamulang University.
        </p>
      </div>
    </div>
  </header>

  <main class="page">
    <section class="summary">
      <h2 class="summary-title">CI/CD Findings Overview</h2>
      <p class="summary-text">
        This portal provides access to automated findings reports generated from the CI/CD pipeline by the
        Intelligent DevSecOps Framework. Each report directory may contain security findings, code quality
        observations, compliance issues, and related analysis artifacts for review and follow-up.
      </p>
    </section>

    <div class="section-head">
      <h2 class="section-title">Available Findings Reports</h2>
      <div class="section-badge">Auto-discovered report directories</div>
    </div>

    <section class="grid">
EOF

for dir in /usr/share/nginx/html/*; do
  [ -d "$dir" ] || continue

  name="$(basename "$dir")"

  [ "$name" = "assets" ] && continue

  HAS_REPORTS=1

  cat >> "$OUTPUT" <<EOF
      <a class="card" href="/$name/">
        <div class="card-top">
          <div class="folder-icon">📁</div>
          <div>
            <h3 class="card-title">$name</h3>
            <p class="card-desc">CI/CD findings report generated by the Intelligent DevSecOps Framework.</p>
          </div>
        </div>
        <div class="card-footer">
          <span class="tag">Security Findings</span>
          <span class="action">Open Report</span>
        </div>
      </a>
EOF
done

if [ "$HAS_REPORTS" -eq 0 ]; then
  cat >> "$OUTPUT" <<'EOF'
      <div class="empty-state">
        <h3>No findings reports available</h3>
        <p>Please add generated report directories from the Intelligent DevSecOps Framework into the mounted report path.</p>
      </div>
EOF
fi

cat >> "$OUTPUT" <<'EOF'
    </section>
  </main>

  <div class="footer">
    Intelligent DevSecOps Framework • Pamulang University
  </div>
</body>
</html>
EOF