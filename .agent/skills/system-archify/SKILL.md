---
name: system-archify
description: >-
  Creates interactive, verifiable HTML and SVG system architecture, workflow, sequence,
  data-flow, and lifecycle diagrams with motion and clean export capabilities.
  Use this skill when explaining system designs, module interactions, pipeline flows,
  or presenting visual architecture walkthroughs for engineering reviews and documentation.
---

# System Archify Skill (Visual & Interactive Architecture Diagrams)

Bộ kỹ năng trực quan hóa kiến trúc hệ thống, tạo sơ đồ tương tác bằng HTML/SVG tự chứa (self-contained) với chuyển động dữ liệu mượt mà, hỗ trợ xuất ảnh vector hoặc nhúng vào báo cáo kỹ thuật.

---

## 1. Các Thể loại Sơ đồ Hỗ trợ (Supported Diagram Types)

1. **Architecture & Component Pipeline (Kiến trúc phân tầng & Đường ống xử lý):**
   - Trực quan hóa các tầng kiến trúc: Presentation Layer $\rightarrow$ Service/Algorithm $\rightarrow$ External Integration $\rightarrow$ Rule Engine $\rightarrow$ AI Copilot $\rightarrow$ Persistence.
   - Thể hiện các luồng đi của dữ liệu với hiệu ứng dòng chảy (data flow animation).
2. **Interactive Sequence Flow (Luồng tuần tự tương tác):**
   - Biểu diễn từng bước trao đổi thông điệp giữa Client, Backend Controller, Solver, Weather Provider, Rule Evaluator, AI Service.
   - Cho phép click hoặc hover vào từng bước để xem payload JSON chi tiết.
3. **Finite State Machine / HITL Decision Flow (Máy trạng thái & Quyết định Người dùng):**
   - Trực quan hóa vòng đời lập lịch: Nháp (Draft) $\rightarrow$ Đang tối ưu (Optimizing) $\rightarrow$ Phát hiện xung đột (Conflict Detected) $\rightarrow$ Chờ phản hồi HITL (Apply / Edit / Dismiss) $\rightarrow$ Đã xác nhận (Confirmed).

---

## 2. Tiêu chuẩn Thiết kế Trực quan (Design Standards)

1. **Tự chứa (Self-contained):**
   - Toàn bộ HTML, CSS và SVG/JS nằm gọn trong một file/widget, không phụ thuộc vào CDN bên ngoài để đảm bảo tính an toàn và khả năng hiển thị offline.
2. **Hỗ trợ Giao diện Sáng/Tối (Dark/Light Mode Ready):**
   - Sử dụng CSS Variables cho màu nền, viền và text:
     - Nền tối: `--bg-canvas: #0f172a`, `--card-bg: #1e293b`, `--text-primary: #f8fafc`.
     - Điểm nhấn (Accents): `--accent-blue: #38bdf8`, `--accent-emerald: #34d399`, `--accent-amber: #fbbf24`, `--accent-rose: #f43f5e`.
3. **Hiệu ứng chuyển động (Motion & Micro-interactions):**
   - Dùng hiệu ứng nét đứt chuyển động (`stroke-dasharray`, `stroke-dashoffset`) để minh họa dòng dữ liệu đang chảy giữa các module.
   - Hover card: Khi di chuột vào một node (ví dụ: `HeldKarpSolver`), làm sáng node đó và hiển thị tooltip chứa thông số kỹ thuật (độ phức tạp $O(n^2 2^n)$, input, output).
4. **Hỗ trợ Xuất (Export Capabilities):**
   - Tích hợp nút `Export SVG` hoặc `Copy HTML` để người dùng dễ dàng chèn vào slide báo cáo hoặc tài liệu nghiệm thu.

---

## 3. Template Khung Cơ bản cho Widget Trực quan hóa

Khi kích hoạt skill này để minh họa luồng hệ thống, sinh widget HTML tương tác theo cấu trúc chuẩn:

```html
<div class="archify-container" style="background:#0f172a; border-radius:12px; padding:24px; color:#f8fafc; font-family:system-ui,sans-serif;">
  <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:16px;">
    <h3 style="margin:0; font-size:18px; font-weight:600;">📐 Kiến trúc Lộ trình & Phản biện AI (Tripzy Architecture Flow)</h3>
    <span style="font-size:12px; background:#334155; padding:4px 10px; border-radius:20px; color:#94a3b8;">Interactive Diagram</span>
  </div>
  
  <!-- SVG canvas với hiệu ứng flow chuyển động -->
  <svg viewBox="0 0 900 320" style="width:100%; height:auto;">
    <defs>
      <linearGradient id="flowGrad" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stop-color="#38bdf8"/>
        <stop offset="100%" stop-color="#34d399"/>
      </linearGradient>
      <style>
        .flow-line { stroke: url(#flowGrad); stroke-width: 2.5; fill: none; stroke-dasharray: 6 6; animation: dash 1.5s linear infinite; }
        @keyframes dash { to { stroke-dashoffset: -12; } }
        .node-box { fill: #1e293b; stroke: #334155; stroke-width: 1.5; rx: 8; transition: all 0.25s ease; cursor: pointer; }
        .node-box:hover { stroke: #38bdf8; fill: #1e293b; filter: drop-shadow(0 0 8px rgba(56, 189, 248, 0.4)); }
      </style>
    </defs>
    <!-- Các node và kết nối mô tả hệ thống -->
  </svg>
</div>
```

---

## 4. Danh mục Kiểm tra Tính Chính xác (Verification Checklist)

Mọi sơ đồ được tạo ra phải thỏa mãn:
- [ ] Tên các component khớp 100% với tên class/package trong codebase (ví dụ: `HeldKarpSolver`, `WeatherProviderFactory`, `RuleEvaluatorService`, `AiCopilotService`).
- [ ] Chiều mũi tên phản ánh đúng luồng dữ liệu (Data-flow) và sự phụ thuộc (Dependency Injection).
- [ ] Các trạng thái biên (Boundary/Fallback) được thể hiện rõ ràng (ví dụ: Open-Meteo API lỗi $\rightarrow$ Mock Fallback).
