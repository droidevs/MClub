# QR check-in + club-admin membership actions

This document explains how the QR code check‑in flow and the club-admin membership management UI work in this project.

## 1) Club admins: approve/reject applications + kick members

### UI
- Template: `src/main/resources/templates/manage-members.html`
- The page lists `members` (a `List<Membership>`).
- For each non-ADMIN member:
  - If `member.status == 'PENDING'`, it shows:
    - **Approve** button → POST `/club-admin/memberships/{membershipId}/status` with `status=APPROVED`
    - **Reject** button → POST `/club-admin/memberships/{membershipId}/status` with `status=REJECTED`
  - **Kick out** button → POST `/club-admin/memberships/{membershipId}/kick`

### Backend
- Controller: `src/main/java/io/droidevs/mclub/controller/WebClubAdminController.java`

Endpoints:
- `POST /club-admin/memberships/{membershipId}/status`
  - Checks that the currently-authenticated user can manage the membership’s club.
  - Updates `membership.status`.
- `POST /club-admin/memberships/{membershipId}/kick`
  - Checks the same permission.
  - Deletes the membership.

Permission model:
- `canManageClub(clubId, userId)` returns true when the user has a `Membership` in that club with role `ADMIN` or `STAFF`.

## 2) Attendance management: QR generation, fullscreen modal, and Print QR

### Template
- `src/main/resources/templates/manage-attendance.html`

What the UI does:
- Shows the current `qrToken` (copiable text).
- If `qrToken != null`, it renders:
  - A small QR preview.
  - Buttons:
    - **Full screen** → opens a Bootstrap fullscreen modal containing a large QR.
    - **Print QR** → opens a new window with a print-friendly page that renders the QR and triggers the browser print dialog.

### Why full screen + print can break
In Thymeleaf layouts, scripts are often moved/fragmented, and browsers will *terminate the outer page’s `<script>` tag* if the string `</script>` appears inside a JavaScript template literal.

### Fix implemented
- The fullscreen modal uses Bootstrap’s JS API and renders the large QR only after the modal fires `shown.bs.modal`.
- `Print QR` does **not** embed a literal `</script>` sequence inside the outer page script.
  - We use `<\\/script>` in the printed HTML.
  - The print window loads `qrcode.min.js`, then draws the QR and calls `window.print()`.

### QR rendering
- Library: `src/main/resources/static/js/qrcode.min.js` (qrcodejs)
- The QR content is the raw `qrToken`.
- Rendering uses:
  - `new QRCode(targetEl, { text: token, width, height, correctLevel: QRCode.CorrectLevel.H })`

## 3) Noisy “User not found” errors in JWT filter

If a browser keeps an old `jwtToken` cookie but the user was deleted, the JWT filter attempts to resolve the user and throws `UsernameNotFoundException`.

Fix:
- `JwtAuthenticationFilter` now treats `UsernameNotFoundException` as an expected case:
  - clears the security context
  - continues the filter chain
  - avoids spamming logs with stack traces

---

## Quick sanity checks
- Open `Manage Members` as a club admin:
  - pending members should show Approve/Reject
  - non-admin members should show Kick out
- Open `Manage Attendance`:
  - rotating/generating token should render QR
  - Full screen should show a large QR
  - Print QR should open a new window and trigger printing

