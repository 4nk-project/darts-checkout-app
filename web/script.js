const boardNumbers = [
  20, 1, 18, 4, 13, 6, 10, 15, 2, 17,
  3, 19, 7, 16, 8, 11, 14, 9, 12, 5,
];

const allThrows = [
  { label: "BULL", points: 50, finish: true },
  ...Array.from({ length: 20 }, (_, i) => i + 1).flatMap((number) => [
    { label: `S${number}`, points: number, finish: false },
    { label: `D${number}`, points: number * 2, finish: true },
    { label: `T${number}`, points: number * 3, finish: true },
  ]),
];

const difficulties = {
  beginner: { label: "初級 2-80", min: 2, max: 80 },
  intermediate: { label: "中級 81-120", min: 81, max: 120 },
  advanced: { label: "上級 121-180", min: 121, max: 180 },
  all: { label: "全範囲", min: 2, max: 180 },
};

const defaultBoardColors = {
  lightSegment: "#f1ddc7",
  ringPrimary: "#1d8b4c",
  ringSecondary: "#c73b32",
};

const boardColorOptions = [
  { label: "クリーム", color: "#f1ddc7" },
  { label: "ホワイト", color: "#f7f2ea" },
  { label: "イエロー", color: "#f2c94c" },
  { label: "オレンジ", color: "#e47b35" },
  { label: "レッド", color: "#c73b32" },
  { label: "ピンク", color: "#d95c8a" },
  { label: "パープル", color: "#7b61ff" },
  { label: "ブルー", color: "#2d7ff9" },
  { label: "シアン", color: "#22a6b3" },
  { label: "グリーン", color: "#1d8b4c" },
  { label: "ライム", color: "#8abf3f" },
  { label: "グレー", color: "#6f665d" },
];

const checkoutArrangements = [
  [180, "T20 / T20 / T20"],
  [170, "T20 / T20 / BULL"],
  [167, "T20 / T19 / BULL"],
  [164, "T20 / T18 / BULL"],
  [161, "T20 / T17 / BULL"],
  [160, "T20 / T20 / D20"],
  [157, "T20 / T19 / D20"],
  [156, "T20 / T20 / D18"],
  [154, "T20 / T18 / D20"],
  [151, "T20 / T17 / D20"],
  [150, "T20 / T20 / D15"],
  [140, "T20 / T20 / D10"],
  [132, "BULL / BULL / D16"],
  [121, "T20 / T11 / D14"],
  [100, "T20 / D20"],
  [80, "T20 / D10"],
  [64, "T16 / D8"],
  [50, "BULL"],
  [40, "D20"],
  [32, "D16"],
];

const finishableScores = Array.from({ length: 179 }, (_, i) => i + 2).filter((target) => {
  for (const first of allThrows) {
    if (first.finish && first.points === target) return true;
    for (const second of allThrows) {
      if (second.finish && first.points + second.points === target) return true;
      for (const third of allThrows) {
        if (third.finish && first.points + second.points + third.points === target) return true;
      }
    }
  }
  return false;
});

const state = {
  difficulty: "all",
  target: 0,
  throws: [],
  result: "waiting",
  answerVisible: false,
  highlight: null,
  highlightStartedAt: 0,
  stats: JSON.parse(localStorage.getItem("dartsCheckoutStats") || '{"attempts":0,"correct":0}'),
  boardColors: loadBoardColors(),
};

const els = {
  target: document.querySelector("#target"),
  remaining: document.querySelector("#remaining"),
  difficultyLabel: document.querySelector("#difficultyLabel"),
  message: document.querySelector("#message"),
  throws: [
    document.querySelector("#throw0"),
    document.querySelector("#throw1"),
    document.querySelector("#throw2"),
  ],
  undoButton: document.querySelector("#undoButton"),
  clearButton: document.querySelector("#clearButton"),
  answerButton: document.querySelector("#answerButton"),
  nextButton: document.querySelector("#nextButton"),
  menuButton: document.querySelector("#menuButton"),
  menu: document.querySelector("#menu"),
  quickActions: document.querySelector(".quick-actions"),
  dialog: document.querySelector("#dialog"),
  dialogTitle: document.querySelector("#dialogTitle"),
  dialogContent: document.querySelector("#dialogContent"),
  closeDialogButton: document.querySelector("#closeDialogButton"),
  canvas: document.querySelector("#dartboard"),
};

const ctx = els.canvas.getContext("2d");

function pickTarget() {
  const difficulty = difficulties[state.difficulty];
  const candidates = finishableScores.filter((score) => score >= difficulty.min && score <= difficulty.max);
  return candidates[Math.floor(Math.random() * candidates.length)];
}

function nextProblem() {
  state.target = pickTarget();
  state.throws = [];
  state.result = "waiting";
  state.answerVisible = false;
  render();
}

function judge() {
  const total = state.throws.reduce((sum, item) => sum + item.points, 0);
  const last = state.throws[state.throws.length - 1];

  if (total > state.target) return "bust";
  if (total === state.target && last?.finish) return "correct";
  if (total === state.target) return "needsFinish";
  if (state.throws.length === 3) return "wrong";
  return "waiting";
}

function submitThrow(dartThrow, highlight) {
  if (state.result !== "waiting" || state.throws.length >= 3) return;
  state.throws.push(dartThrow);
  state.result = judge();
  state.highlight = highlight;
  state.highlightStartedAt = performance.now();

  if (state.result !== "waiting") {
    state.stats.attempts += 1;
    if (state.result === "correct") state.stats.correct += 1;
    localStorage.setItem("dartsCheckoutStats", JSON.stringify(state.stats));
  }

  render();
  requestAnimationFrame(drawBoard);
}

function recommendedRoute(target) {
  const listed = checkoutArrangements.find(([score]) => score === target);
  if (listed) return listed[1];

  for (const first of allThrows) {
    if (first.finish && first.points === target) return first.label;
  }
  for (const first of allThrows) {
    for (const second of allThrows) {
      if (second.finish && first.points + second.points === target) {
        return `${first.label} / ${second.label}`;
      }
    }
  }
  for (const first of allThrows) {
    for (const second of allThrows) {
      for (const third of allThrows) {
        if (third.finish && first.points + second.points + third.points === target) {
          return `${first.label} / ${second.label} / ${third.label}`;
        }
      }
    }
  }
  return "アレンジが見つかりません";
}

function loadBoardColors() {
  try {
    return {
      ...defaultBoardColors,
      ...JSON.parse(localStorage.getItem("dartsCheckoutBoardColors") || "{}"),
    };
  } catch {
    return { ...defaultBoardColors };
  }
}

function saveBoardColors() {
  localStorage.setItem("dartsCheckoutBoardColors", JSON.stringify(state.boardColors));
}

function isDefaultBoardColors() {
  return Object.entries(defaultBoardColors).every(([key, color]) => state.boardColors[key] === color);
}

function boardColorPicker(target, label) {
  const selectedColor = state.boardColors[target];
  const options = boardColorOptions
    .map((option) => (
      `<button class="color-swatch ${selectedColor === option.color ? "selected" : ""}" ` +
      `type="button" data-color-target="${target}" data-color="${option.color}" ` +
      `aria-label="${label}を${option.label}に変更" title="${option.label}" ` +
      `style="--swatch:${option.color}"></button>`
    ))
    .join("");

  return `<section class="color-picker" data-color-picker="${target}">
    <div class="color-picker-header">
      <span class="color-current" style="--swatch:${selectedColor}" aria-hidden="true"></span>
      <strong>${label}</strong>
    </div>
    <div class="color-palette">${options}</div>
  </section>`;
}

function boardColorSettingsHtml() {
  return `<div class="color-settings">
    ${boardColorPicker("lightSegment", "シングル白エリア")}
    ${boardColorPicker("ringPrimary", "リングカラー 1")}
    ${boardColorPicker("ringSecondary", "リングカラー 2")}
    <button id="resetBoardColorsButton" class="secondary" type="button" ${isDefaultBoardColors() ? "disabled" : ""}>標準カラーに戻す</button>
  </div>`;
}

function render() {
  const total = state.throws.reduce((sum, item) => sum + item.points, 0);
  const remaining = state.target - total;

  els.target.textContent = state.target;
  els.remaining.textContent = `残り ${remaining}`;
  els.remaining.style.color = remaining < 0 ? "var(--red)" : "var(--muted)";
  els.difficultyLabel.textContent = difficulties[state.difficulty].label;

  els.throws.forEach((slot, index) => {
    slot.textContent = state.throws[index]?.label || "-";
  });

  els.message.className = "message";
  if (state.answerVisible) {
    els.message.textContent = `答え ${recommendedRoute(state.target)}`;
    els.message.classList.add("correct");
  } else {
    const messages = {
      waiting: "",
      correct: "正解",
      bust: "バースト",
      needsFinish: "最後はダブル・トリプル・BULLで上がってください",
      wrong: "不正解",
    };
    els.message.textContent = messages[state.result];
    if (state.result === "correct") els.message.classList.add("correct");
    if (["bust", "wrong"].includes(state.result)) els.message.classList.add("error");
    if (state.result === "needsFinish") els.message.classList.add("warn");
  }

  els.undoButton.disabled = state.throws.length === 0;
  els.clearButton.disabled = state.throws.length === 0;
  drawBoard();
}

function drawBoard() {
  const canvas = els.canvas;
  const rect = canvas.getBoundingClientRect();
  const ratio = window.devicePixelRatio || 1;
  const size = Math.max(1, Math.floor(Math.min(rect.width, rect.height) * ratio));
  if (canvas.width !== size || canvas.height !== size) {
    canvas.width = size;
    canvas.height = size;
  }

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  const center = { x: canvas.width / 2, y: canvas.height / 2 };
  const radius = canvas.width * 0.48;
  const scoringRadius = radius * 0.84;

  ctx.fillStyle = "#151515";
  circle(center, radius);

  boardNumbers.forEach((_, index) => {
    const start = index * 18 - 99;
    const dark = index % 2 === 0;
    ringSector(center, scoringRadius * 0.12, scoringRadius * 0.49, start, 18, dark ? "#1f1e1a" : state.boardColors.lightSegment);
    ringSector(center, scoringRadius * 0.50, scoringRadius * 0.58, start, 18, dark ? state.boardColors.ringPrimary : state.boardColors.ringSecondary);
    ringSector(center, scoringRadius * 0.59, scoringRadius * 0.85, start, 18, dark ? "#1f1e1a" : state.boardColors.lightSegment);
    ringSector(center, scoringRadius * 0.86, scoringRadius * 0.98, start, 18, dark ? state.boardColors.ringPrimary : state.boardColors.ringSecondary);
  });

  ctx.fillStyle = state.boardColors.ringPrimary;
  circle(center, scoringRadius * 0.11);
  ctx.fillStyle = state.boardColors.ringSecondary;
  circle(center, scoringRadius * 0.05);

  drawHighlight(center, scoringRadius);

  ctx.strokeStyle = "#efe8dd";
  ctx.lineWidth = Math.max(2, canvas.width * 0.003);
  ctx.beginPath();
  ctx.arc(center.x, center.y, scoringRadius * 0.98, 0, Math.PI * 2);
  ctx.stroke();

  drawNumbers(center, radius);

  if (state.highlight && performance.now() - state.highlightStartedAt < 450) {
    requestAnimationFrame(drawBoard);
  }
}

function drawHighlight(center, scoringRadius) {
  if (!state.highlight) return;
  const elapsed = performance.now() - state.highlightStartedAt;
  const alpha = Math.max(0, 1 - elapsed / 450);
  if (alpha <= 0) return;

  const color = `rgba(255, 241, 118, ${0.78 * alpha})`;
  if (state.highlight.area === "bull") {
    ctx.fillStyle = color;
    circle(center, scoringRadius * 0.11);
    return;
  }

  const start = state.highlight.index * 18 - 99;
  const ranges = {
    singleInner: [0.12, 0.49],
    triple: [0.50, 0.58],
    singleOuter: [0.59, 0.85],
    double: [0.86, 0.98],
  };
  const [inner, outer] = ranges[state.highlight.area];
  ringSector(center, scoringRadius * inner, scoringRadius * outer, start, 18, color);
}

function drawNumbers(center, radius) {
  ctx.fillStyle = "#fff";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.font = `800 ${radius * 0.08}px system-ui, sans-serif`;
  const numberRadius = radius * 0.92;
  boardNumbers.forEach((number, index) => {
    const angle = (index * 18 * Math.PI) / 180;
    const x = center.x + Math.sin(angle) * numberRadius;
    const y = center.y - Math.cos(angle) * numberRadius;
    ctx.fillText(String(number), x, y);
  });
}

function circle(center, radius) {
  ctx.beginPath();
  ctx.arc(center.x, center.y, radius, 0, Math.PI * 2);
  ctx.fill();
}

function ringSector(center, innerRadius, outerRadius, startDeg, sweepDeg, color) {
  const start = (startDeg * Math.PI) / 180;
  const end = ((startDeg + sweepDeg) * Math.PI) / 180;
  ctx.fillStyle = color;
  ctx.beginPath();
  ctx.arc(center.x, center.y, outerRadius, start, end);
  ctx.arc(center.x, center.y, innerRadius, end, start, true);
  ctx.closePath();
  ctx.fill();
}

function hitTest(event) {
  const rect = els.canvas.getBoundingClientRect();
  const scale = els.canvas.width / rect.width;
  const x = (event.clientX - rect.left) * scale;
  const y = (event.clientY - rect.top) * scale;
  const center = { x: els.canvas.width / 2, y: els.canvas.height / 2 };
  const radius = els.canvas.width * 0.48 * 0.84;
  const dx = x - center.x;
  const dy = y - center.y;
  const normalized = Math.hypot(dx, dy) / radius;

  if (normalized > 1) return null;
  if (normalized <= 0.11) {
    return {
      dartThrow: { label: "BULL", points: 50, finish: true },
      highlight: { area: "bull" },
    };
  }

  const degrees = ((Math.atan2(dx, -dy) * 180) / Math.PI + 360) % 360;
  const index = Math.floor((degrees + 9) / 18) % 20;
  const number = boardNumbers[index];

  if (normalized >= 0.46 && normalized <= 0.62) {
    return {
      dartThrow: { label: `T${number}`, points: number * 3, finish: true },
      highlight: { index, area: "triple" },
    };
  }
  if (normalized >= 0.82) {
    return {
      dartThrow: { label: `D${number}`, points: number * 2, finish: true },
      highlight: { index, area: "double" },
    };
  }

  return {
    dartThrow: { label: `S${number}`, points: number, finish: false },
    highlight: { index, area: normalized < 0.59 ? "singleInner" : "singleOuter" },
  };
}

function openDialog(type) {
  const content = {
    about: [
      "このアプリについて",
      "<p>表示された残り点を、ソフトダーツのチェックアウトとして3投以内で上がる練習アプリです。盤面をタップすると入力され、合計点と最後のダブル・トリプル・BULL条件で判定します。</p>",
    ],
    arrange: [
      "ダーツのアレンジ表",
      checkoutArrangements
        .map(([score, route]) => `<div class="arrange-row"><strong>${score}</strong><span>${route}</span></div>`)
        .join(""),
    ],
    difficulty: [
      "難易度を選択",
      `<div class="difficulty-options">${Object.entries(difficulties)
        .map(([key, item]) => `<button class="option-button ${state.difficulty === key ? "selected" : ""}" data-difficulty="${key}" type="button">${item.label}</button>`)
        .join("")}</div>`,
    ],
    boardColors: [
      "盤面カラー設定",
      boardColorSettingsHtml(),
    ],
    stats: [
      "成績",
      `<div class="stat-card"><strong>挑戦数</strong><span>${state.stats.attempts}</span></div>
       <div class="stat-card"><strong>正解数</strong><span>${state.stats.correct}</span></div>
       <div class="stat-card"><strong>正答率</strong><span>${state.stats.attempts ? Math.floor((state.stats.correct * 100) / state.stats.attempts) : 0}%</span></div>
       <button id="resetStatsButton" class="secondary" type="button">成績をリセット</button>`,
    ],
    privacy: [
      "プライバシー",
      "<p>このアプリは個人情報を収集しません。練習中の成績は端末内のブラウザに保存され、外部サーバーへ送信されません。問い合わせメールを送る場合は、メールアプリ側で入力した内容が送信されます。</p>",
    ],
    licenses: [
      "ライセンス",
      "<p>このWeb版はHTML、CSS、JavaScriptで作成しています。外部ライブラリは使用していません。</p>",
    ],
  }[type];

  els.dialogTitle.textContent = content[0];
  els.dialogContent.innerHTML = content[1];
  if (!els.dialog.open) {
    els.dialog.showModal();
  }
}

els.canvas.addEventListener("click", (event) => {
  const hit = hitTest(event);
  if (hit) submitThrow(hit.dartThrow, hit.highlight);
});

els.undoButton.addEventListener("click", () => {
  state.throws.pop();
  state.result = "waiting";
  state.answerVisible = false;
  render();
});

els.clearButton.addEventListener("click", () => {
  state.throws = [];
  state.result = "waiting";
  state.answerVisible = false;
  render();
});

els.answerButton.addEventListener("click", () => {
  state.answerVisible = true;
  render();
});

els.nextButton.addEventListener("click", nextProblem);

els.menuButton.addEventListener("click", () => {
  els.menu.hidden = !els.menu.hidden;
});

els.menu.addEventListener("click", (event) => {
  const button = event.target.closest("[data-dialog]");
  if (!button) return;
  els.menu.hidden = true;
  openDialog(button.dataset.dialog);
});

els.quickActions.addEventListener("click", (event) => {
  const button = event.target.closest("[data-dialog]");
  if (!button) return;
  els.menu.hidden = true;
  openDialog(button.dataset.dialog);
});

els.closeDialogButton.addEventListener("click", () => els.dialog.close());

els.dialogContent.addEventListener("click", (event) => {
  const difficultyButton = event.target.closest("[data-difficulty]");
  if (difficultyButton) {
    state.difficulty = difficultyButton.dataset.difficulty;
    els.dialog.close();
    nextProblem();
  }

  if (event.target.id === "resetStatsButton") {
    state.stats = { attempts: 0, correct: 0 };
    localStorage.setItem("dartsCheckoutStats", JSON.stringify(state.stats));
    els.dialog.close();
    render();
  }

  const colorButton = event.target.closest("[data-color-target][data-color]");
  if (colorButton) {
    const target = colorButton.dataset.colorTarget;
    state.boardColors[target] = colorButton.dataset.color;
    saveBoardColors();
    drawBoard();
    openDialog("boardColors");
  }

  if (event.target.id === "resetBoardColorsButton") {
    state.boardColors = { ...defaultBoardColors };
    saveBoardColors();
    drawBoard();
    openDialog("boardColors");
  }
});

window.addEventListener("resize", drawBoard);

nextProblem();
