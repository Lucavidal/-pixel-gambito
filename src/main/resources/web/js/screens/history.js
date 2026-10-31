// history.js — "Mi historial": totales y últimas rondas del torneo, como HistoryDialog (Swing).
// Los textos se arman acá con i18n.js a partir de los datos del servidor (números, códigos y la fecha ISO),
// así se ven en el idioma elegido; el servidor igual manda sus textos en español (los usa Swing).
// El filtro por dificultad se le pide al servidor, así los totales también lo respetan.
import { api } from '../api.js';
import { t, formatDate, onLangChange } from '../i18n.js';
import { difficultyName } from '../tournament-panel.js';

const $ = (id) => document.getElementById(id);

/** "Tu récord en el torneo: 3 ganadas · 1 tablas · 2 perdidas · 1 título" (o que todavía no jugó). */
export function recordText(summary) {
  if (summary.roundsPlayed === 0) return t('record.none');
  const record = t('record.text', {
    wins: t('count.wins', { count: summary.wins }),
    draws: t('count.draws', { count: summary.draws }),
    losses: t('count.losses', { count: summary.losses }),
  });
  return summary.titles === 0 ? record : t('record.withTitles', { record, titles: t('count.titles', { count: summary.titles }) });
}

/** "12 rondas jugadas: 7 ganadas, 3 perdidas y 2 tablas. Ganaste 1 torneo." */
export function summaryText(summary) {
  if (summary.roundsPlayed === 0) return t('history.summary.none');
  const text = t('history.summary', {
    rounds: t('count.roundsPlayed', { count: summary.roundsPlayed }),
    wins: t('count.wins', { count: summary.wins }),
    losses: t('count.losses', { count: summary.losses }),
    draws: t('count.draws', { count: summary.draws }),
  });
  return summary.titles === 0 ? text
    : t('history.summary.titles', { summary: text, tournaments: t('count.tournaments', { count: summary.titles }) });
}

export function createHistoryScreen() {
  const element = $('screen-history');
  let visit = 0;
  let difficulty = null;   // EASY / MEDIUM / HARD, o null = todas
  let last = null;         // lo último que mandó el servidor (para redibujar en otro idioma)
  let failed = false;

  function stat(value, label, kind) {
    const box = document.createElement('div');
    box.className = `stat ${kind}`;
    box.innerHTML = '<span class="stat-value"></span><span class="stat-label"></span>';
    box.querySelector('.stat-value').textContent = value;
    box.querySelector('.stat-label').textContent = label;
    return box;
  }

  /** "Todas · Fácil · Media · Difícil": las opciones vienen del servidor. */
  function renderFilter(options) {
    const all = [{ id: null, name: t('history.filter.all') }, ...options];
    $('history-filter').replaceChildren(...all.map((option) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = `filter-option ${(option.id ?? 'all').toLowerCase()}`;
      button.textContent = option.id ? difficultyName(option.id, option.name) : option.name;
      button.setAttribute('aria-pressed', String(option.id === difficulty));
      button.addEventListener('click', () => {
        if (option.id === difficulty) return;
        difficulty = option.id;
        load();
      });
      return button;
    }));
  }

  function render(history) {
    const { summary, rounds } = history;
    $('history-summary').textContent = summaryText(summary);
    renderFilter(history.difficulties);

    $('history-stats').replaceChildren(
      stat(summary.wins, t('stat.wins', { count: summary.wins }), 'won'),
      stat(summary.draws, t('stat.draws', { count: summary.draws }), 'drawn'),
      stat(summary.losses, t('stat.losses', { count: summary.losses }), 'lost'),
      stat(summary.titles, t('stat.titles', { count: summary.titles }), 'champion'),
    );
    $('history-stats').hidden = summary.roundsPlayed === 0;

    const rows = rounds.map((round) => {
      const tr = document.createElement('tr');
      const cells = [
        formatDate(round.playedAt),
        t('history.roundOf', { round: round.round, total: round.totalRounds }),
        round.rival,
        round.difficulty ? difficultyName(round.difficulty, round.difficultyText) : t('difficulty.none'),
        t(`outcome.${round.outcome}`),
        round.userMoves,
      ];
      cells.forEach((value, i) => {
        const td = document.createElement('td');
        td.textContent = value;
        if (i === 3) td.className = `difficulty ${(round.difficulty ?? 'none').toLowerCase()}`;
        if (i === 4) td.className = `outcome ${round.outcome.toLowerCase()}`;
        if (i === 5) td.className = 'num';
        tr.append(td);
      });
      const time = document.createElement('time');
      time.dateTime = round.playedAt;
      time.textContent = cells[0];
      tr.firstChild.replaceChildren(time);
      return tr;
    });
    $('history-rows').replaceChildren(...rows);
    $('history-table').hidden = rounds.length === 0;

    const empty = $('history-empty-text');
    empty.hidden = rounds.length > 0;
    empty.textContent = history.difficulty
      ? t('history.emptyFiltered', { difficulty: difficultyName(history.difficulty, history.difficulty) })
      : t('history.empty');
  }

  async function load() {
    const thisVisit = ++visit;
    failed = false;
    $('history-summary').textContent = t('history.loading');
    try {
      const history = await api.history(50, difficulty);
      if (thisVisit !== visit) return;
      last = history;
      render(history);
    } catch (error) {
      if (error.status !== 401 && thisVisit === visit) {
        failed = true;
        $('history-summary').textContent = t('history.loadError');
      }
    }
  }

  onLangChange(() => {
    if (element.hidden) return;
    if (last) render(last);
    else if (failed) $('history-summary').textContent = t('history.loadError');
  });

  return {
    element,
    enter() {
      difficulty = null;
      last = null;
      $('history-rows').replaceChildren();
      $('history-stats').replaceChildren();
      $('history-filter').replaceChildren();
      $('history-empty-text').hidden = true;
      return load();
    },
    leave() { visit++; },
  };
}
