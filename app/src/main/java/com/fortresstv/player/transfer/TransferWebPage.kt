package com.fortresstv.player.transfer

/**
 * Generates the embedded single-page HTML setup form served by [LocalTransferServer].
 *
 * Design goals:
 * - Works on any phone/tablet browser without JavaScript frameworks.
 * - Dark-mode, touch-friendly, legible at arm's length.
 * - Reads the session token from the URL fragment (#token=...) and includes it
 *   in the Authorization header of the POST — the fragment is never sent in
 *   the initial GET, which keeps the token out of server access logs.
 * - Falls back to manual 6-digit PIN entry when the token is absent (e.g.,
 *   user typed the URL manually).
 * - Validates required fields before submitting.
 * - Supports M3U URL and Xtream Codes source types.
 * - Handles success and error responses with clear user feedback.
 * - HTML-encodes the device name to prevent injection.
 */
object TransferWebPage {

    fun generate(deviceName: String, pin: String): String {
        val safeDevice = deviceName.htmlEscape()
        val safePin = pin.htmlEscape()
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendLine("<head>")
            appendLine("<meta charset=\"UTF-8\">")
            appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            appendLine("<title>FortressTV &#8212; Add Source</title>")
            appendLine("<style>")
            appendLine(CSS)
            appendLine("</style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("<main>")
            appendLine("<header class=\"brand\"><span class=\"brand-mark\">F</span><span>FortressTV</span></header>")
            appendLine("<section class=\"hero\"><span class=\"eyebrow\">LOCAL TV TRANSFER &middot; TRANSFERÊNCIA LOCAL</span><h1>Send securely to your TV</h1><p>Transfer a playlist or encrypted backup directly over your home Wi-Fi.<br><span lang=\"pt-PT\">Envie uma lista ou cópia encriptada diretamente pela rede Wi-Fi de casa.</span></p></section>")
            appendLine("<div class=\"connection\"><span class=\"status-dot\"></span><div><small>CONNECTED TV &middot; TV LIGADA</small><strong>$safeDevice</strong></div><span class=\"local-badge\">Local only</span></div>")
            appendLine("<div class=\"privacy\"><span class=\"lock\">&#128274;</span><span><strong>Private connection &middot; Ligação privada</strong><small>No cloud servers. Your details stay inside your local network.<br><span lang=\"pt-PT\">Sem servidores na nuvem. Os seus dados permanecem na rede local.</span></small></span></div>")
            appendLine(JS_PIN_CHECK.replace("__PIN__", safePin))
            appendLine(FORM_HTML)
            appendLine("<footer>FortressTV &middot; Direct local transfer<br><span>Transferência direta na rede local</span></footer>")
            appendLine("</main>")
            appendLine("</body>")
            appendLine("</html>")
        }
    }

    // ------------------------------------------------------------------
    // CSS
    // ------------------------------------------------------------------

    private val CSS = """
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body {
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
          background: #0f0f13;
          color: #e8e8f0;
          min-height: 100vh;
          display: flex;
          align-items: flex-start;
          justify-content: center;
          padding: 24px 16px 48px;
        }
        main {
          width: 100%;
          max-width: 500px;
        }
        h1 { font-size: 1.6rem; font-weight: 700; margin-bottom: 4px; }
        .sub { color: #9090b0; font-size: 0.9rem; margin-bottom: 24px; }

        /* Tab bar */
        .tabs { display: flex; gap: 8px; margin-bottom: 20px; }
        .tab {
          flex: 1;
          padding: 10px 0;
          background: #1c1c28;
          border: 2px solid transparent;
          border-radius: 10px;
          color: #9090b0;
          font-size: 0.85rem;
          font-weight: 600;
          cursor: pointer;
          text-align: center;
          transition: all 0.15s;
        }
        .tab.active {
          background: #2b2b40;
          border-color: #6c63ff;
          color: #e8e8f0;
        }

        /* Form */
        .panel { display: none; flex-direction: column; gap: 14px; }
        .panel.active { display: flex; }
        label { font-size: 0.8rem; font-weight: 600; color: #9090b0; letter-spacing: 0.04em; text-transform: uppercase; display: block; margin-bottom: 4px; }
        input[type=text], input[type=password], input[type=url], input[type=number] {
          width: 100%;
          padding: 12px 14px;
          background: #1c1c28;
          border: 1.5px solid #2e2e44;
          border-radius: 10px;
          color: #e8e8f0;
          font-size: 1rem;
          outline: none;
          transition: border-color 0.15s;
        }
        input:focus { border-color: #6c63ff; }
        input::placeholder { color: #4a4a68; }
        input[type=file] { width: 100%; padding: 14px; background: #1c1c28; border: 1.5px dashed #4a4a68; border-radius: 10px; color: #e8e8f0; }

        .btn {
          padding: 14px;
          background: #6c63ff;
          border: none;
          border-radius: 12px;
          color: #fff;
          font-size: 1rem;
          font-weight: 700;
          cursor: pointer;
          width: 100%;
          margin-top: 6px;
          transition: background 0.15s;
        }
        .btn:disabled { background: #3a3a58; cursor: default; }
        .btn:not(:disabled):hover { background: #7b72ff; }
        .app-link { display: none; margin-bottom: 18px; padding: 13px; border: 1.5px solid #6c63ff; border-radius: 12px; color: #b9b5ff; text-align: center; text-decoration: none; font-weight: 700; }
        .app-link.visible { display: block; }

        /* PIN fallback */
        .pin-section {
          margin-top: 20px;
          padding: 14px;
          background: #1c1c28;
          border-radius: 12px;
          display: none;
        }
        .pin-section.visible { display: block; }
        .pin-section label { margin-bottom: 8px; }

        /* Status */
        #status {
          margin-top: 18px;
          padding: 14px;
          border-radius: 10px;
          font-size: 0.95rem;
          display: none;
        }
        #status.ok  { background: #1a3a2a; color: #60d98a; display: block; }
        #status.err { background: #3a1a1a; color: #f07070; display: block; }
        #status.sending { background: #1c2240; color: #9090ff; display: block; }

        /* FortressTV branded local-transfer shell */
        :root { color-scheme: dark; --card:#111824; --line:#263449; --text:#f5f7fb; --muted:#9ba9bd; --cyan:#53d9e8; --blue:#4778ff; --green:#4ade9a; }
        body { background:radial-gradient(circle at 50% -15%,#17345c 0,#0b1422 30%,#070b12 62%); color:var(--text); padding-top:28px; }
        main { max-width:540px; }
        .brand { display:flex; align-items:center; gap:10px; font-size:1.05rem; font-weight:800; margin-bottom:34px; letter-spacing:-.02em; }
        .brand-mark { width:34px; height:34px; display:grid; place-items:center; border-radius:10px; color:#061019; font-size:1.15rem; background:linear-gradient(145deg,var(--cyan),#88f0d0); box-shadow:0 8px 28px #53d9e833; }
        .hero { margin-bottom:22px; }
        .eyebrow { color:var(--cyan); font-size:.72rem; font-weight:800; letter-spacing:.15em; }
        h1 { font-size:clamp(1.85rem,7vw,2.45rem); line-height:1.08; letter-spacing:-.045em; margin:8px 0 10px; }
        .hero p { color:var(--muted); line-height:1.55; font-size:.98rem; }
        .connection { display:flex; align-items:center; gap:12px; padding:15px 16px; border:1px solid var(--line); border-radius:16px; background:#111a27dd; box-shadow:0 18px 55px #0005; margin-bottom:12px; }
        .status-dot { width:11px; height:11px; border-radius:50%; background:var(--green); box-shadow:0 0 0 5px #4ade9a18; flex:none; }
        .connection div { display:flex; flex-direction:column; min-width:0; flex:1; gap:2px; }
        .connection small { color:var(--muted); font-size:.64rem; font-weight:800; letter-spacing:.1em; }
        .connection strong { white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
        .local-badge { color:var(--green); background:#4ade9a13; border:1px solid #4ade9a36; padding:6px 9px; border-radius:999px; font-size:.7rem; font-weight:800; white-space:nowrap; }
        .privacy { display:flex; gap:12px; align-items:center; padding:13px 15px; color:#b8c7d9; background:#0c1722; border:1px solid #1c3443; border-radius:14px; margin-bottom:18px; }
        .privacy span:last-child { display:flex; flex-direction:column; gap:2px; font-size:.8rem; }
        .privacy small { color:#7f91a7; line-height:1.35; }
        .transfer-card { padding:18px; border:1px solid var(--line); border-radius:20px; background:linear-gradient(160deg,#121b29,#0e151f); box-shadow:0 24px 70px #0006; }
        .tabs { gap:6px; padding:5px; background:#080f18; border-radius:13px; }
        .tab { background:transparent; color:var(--muted); border-radius:9px; }
        .tab.active { background:#182334; border-color:#33445c; color:var(--text); box-shadow:0 5px 18px #0005; }
        label { color:#aebbd0; }
        input[type=text],input[type=password],input[type=url],input[type=number],input[type=file] { background:#09111b; border-color:var(--line); color:var(--text); }
        input:focus { border-color:var(--cyan); box-shadow:0 0 0 3px #53d9e815; }
        .btn { background:linear-gradient(135deg,var(--blue),#6557f5); box-shadow:0 10px 28px #4778ff36; }
        .app-link { border-color:#53d9e866; background:#53d9e80d; color:#8eeaf3; }
        footer { color:#64748a; text-align:center; font-size:.72rem; line-height:1.6; padding:24px 8px 0; }
        footer span { color:#4d5d72; }
        @media (max-width:420px) { .transfer-card { padding:14px; } .tab { font-size:.74rem; } .local-badge { display:none; } }
    """.trimIndent()

    // ------------------------------------------------------------------
    // JavaScript (token extraction + form submit + PIN fallback)
    // ------------------------------------------------------------------

    private val JS_PIN_CHECK = """
        <script>
        (function() {
          var token = null;
          var pinRequired = false;
          var hash = window.location.hash;
          if (hash && hash.indexOf('token=') !== -1) {
            token = decodeURIComponent(hash.split('token=')[1].split('&')[0]);
          } else {
            pinRequired = true;
          }

          window.addEventListener('DOMContentLoaded', function() {
            if (token) {
              var appLink = document.getElementById('open-app');
              var endpoint = window.location.protocol + '//' + window.location.host;
              appLink.href = 'fortresstv://send?endpoint=' + encodeURIComponent(endpoint) + '&token=' + encodeURIComponent(token);
              appLink.classList.add('visible');
            }
            if (pinRequired) {
              document.getElementById('pin-section').classList.add('visible');
            }

            var tabs = document.querySelectorAll('.tab');
            tabs.forEach(function(tab) {
              tab.addEventListener('click', function() {
                tabs.forEach(function(t) { t.classList.remove('active'); });
                document.querySelectorAll('.panel').forEach(function(p) { p.classList.remove('active'); });
                tab.classList.add('active');
                document.getElementById(tab.dataset.panel).classList.add('active');
              });
            });

            document.getElementById('form-m3u').addEventListener('submit', function(e) {
              e.preventDefault();
              send({
                protocolVersion: __PROTO_VER__,
                kind: 'm3u',
                name: document.getElementById('m3u-name').value.trim(),
                url: document.getElementById('m3u-url').value.trim(),
                epgUrl: document.getElementById('m3u-epg').value.trim() || null
              });
            });

            document.getElementById('form-xtream').addEventListener('submit', function(e) {
              e.preventDefault();
              send({
                protocolVersion: __PROTO_VER__,
                kind: 'xtream',
                name: document.getElementById('xt-name').value.trim(),
                url: document.getElementById('xt-server').value.trim(),
                username: document.getElementById('xt-user').value.trim(),
                password: document.getElementById('xt-pass').value,
                epgUrl: null
              });
            });
            document.getElementById('form-backup').addEventListener('submit', function(e) {
              e.preventDefault();
              var file = document.getElementById('backup-file').files[0];
              if (!file) return showError('Choose a FortressTV backup file.');
              if (file.size > 10 * 1024 * 1024) return showError('Backup files must be 10 MB or smaller.');
              sendBackup(file);
            });
          });

          function authHeaders() {
            var headers = {};
            var pin = pinRequired ? document.getElementById('pin-input').value.trim() : null;
            if (pinRequired && !pin) return null;
            if (token) headers['Authorization'] = 'Bearer ' + token;
            if (pin) headers['X-Fortress-Pin'] = pin;
            return headers;
          }

          function showError(message) {
            var status = document.getElementById('status');
            status.className = 'err';
            status.textContent = message;
          }

          function send(payload) {
            var status = document.getElementById('status');
            var headers = authHeaders();
            if (!headers) return showError('Please enter your 6-digit code.');
            status.className = 'sending';
            status.textContent = 'Sending\u2026';
            headers['Content-Type'] = 'application/json';
            fetch('/api/source', {
              method: 'POST',
              headers: headers,
              body: JSON.stringify(payload)
            }).then(function(r) {
              return r.json().then(function(j) { return { ok: r.ok, body: j }; });
            }).then(function(res) {
              if (res.ok) {
                status.className = 'ok';
                status.textContent = 'Done! Confirm on your TV screen.';
                document.querySelectorAll('button[type=submit]').forEach(function(b) { b.disabled = true; });
              } else {
                status.className = 'err';
                status.textContent = res.body.error || 'Error — please try again.';
              }
            }).catch(function() {
              status.className = 'err';
              status.textContent = 'Network error — check you are on the same Wi-Fi.';
            });
          }

          function sendBackup(file) {
            var headers = authHeaders();
            if (!headers) return showError('Please enter your 6-digit code.');
            headers['Content-Type'] = 'application/octet-stream';
            headers['X-Fortress-Filename'] = 'fortresstv-transfer.ftv';
            var status = document.getElementById('status');
            status.className = 'sending';
            status.textContent = 'Uploading encrypted backup\u2026';
            fetch('/api/backup', { method: 'POST', headers: headers, body: file })
              .then(function(r) { return r.json().then(function(j) { return { ok: r.ok, body: j }; }); })
              .then(function(res) {
                if (res.ok) {
                  status.className = 'ok';
                  status.textContent = 'Done! Confirm and unlock the backup on your TV.';
                  document.querySelectorAll('button[type=submit]').forEach(function(b) { b.disabled = true; });
                } else showError(res.body.error || 'Error — please try again.');
              }).catch(function() { showError('Network error — check you are on the same Wi-Fi.'); });
          }
        })();
        </script>
    """.trimIndent()
        .replace("__PROTO_VER__", TRANSFER_PROTOCOL_VERSION.toString())

    // ------------------------------------------------------------------
    // HTML form body
    // ------------------------------------------------------------------

    private val FORM_HTML = """
        <a id="open-app" class="app-link" href="#">Open FortressTV app / Abrir a app FortressTV</a>
        <section class="transfer-card">
        <div class="tabs">
          <button class="tab active" data-panel="form-m3u">M3U Playlist</button>
          <button class="tab" data-panel="panel-xtream">Xtream Codes</button>
          <button class="tab" data-panel="panel-backup">Backup</button>
        </div>

        <form id="form-m3u" class="panel active">
          <div>
            <label for="m3u-name">Playlist name</label>
            <input id="m3u-name" type="text" placeholder="My Playlist" autocomplete="off">
          </div>
          <div>
            <label for="m3u-url">M3U address</label>
            <input id="m3u-url" type="url" placeholder="http://example.com/playlist.m3u" required autocomplete="off">
          </div>
          <div>
            <label for="m3u-epg">EPG (XMLTV) address <span style="font-weight:400;text-transform:none">(optional)</span></label>
            <input id="m3u-epg" type="url" placeholder="http://example.com/epg.xml" autocomplete="off">
          </div>
          <button class="btn" type="submit">Add to TV</button>
        </form>

        <div class="panel" id="panel-xtream">
          <form id="form-xtream" style="display:contents">
            <div>
              <label for="xt-name">Source name</label>
              <input id="xt-name" type="text" placeholder="My Provider" autocomplete="off">
            </div>
            <div>
              <label for="xt-server">Server address</label>
              <input id="xt-server" type="url" placeholder="http://myserver.com" required autocomplete="off">
            </div>
            <div>
              <label for="xt-user">Username</label>
              <input id="xt-user" type="text" required autocomplete="username">
            </div>
            <div>
              <label for="xt-pass">Password</label>
              <input id="xt-pass" type="password" required autocomplete="current-password">
            </div>
            <button class="btn" type="submit">Add to TV</button>
          </form>
        </div>

        <div class="panel" id="panel-backup">
          <form id="form-backup" style="display:contents">
            <div>
              <label for="backup-file">Encrypted FortressTV backup (.ftv)</label>
              <input id="backup-file" type="file" accept=".ftv,application/octet-stream" required>
            </div>
            <button class="btn" type="submit">Send backup to TV</button>
          </form>
        </div>

        <div class="pin-section" id="pin-section">
          <label for="pin-input">Enter the 6-digit code shown on your TV</label>
          <input id="pin-input" type="number" placeholder="123456" maxlength="6" autocomplete="off">
        </div>

        <div id="status"></div>
        </section>
    """.trimIndent()

    private fun String.htmlEscape(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
