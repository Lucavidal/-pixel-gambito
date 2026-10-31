// footer.js — la línea de derechos de autor y contacto de abajo, y el panel de "Créditos".
// Los textos los traduce i18n.js (data-i18n); acá solo se arma el link del mail con brand.js y se abre/cierra el panel.
import { BRAND } from './brand.js';

const $ = (id) => document.getElementById(id);

export function createFooter() {
  const mail = $('footer-mail');
  mail.href = `mailto:${BRAND.email}`;
  mail.textContent = BRAND.email;

  // <dialog> con showModal: Esc ya lo cierra, el foco queda adentro y vuelve al link al cerrar
  const credits = $('credits');
  $('btn-credits').addEventListener('click', () => credits.showModal());
  $('btn-credits-close').addEventListener('click', () => credits.close());
  credits.addEventListener('click', (event) => {
    if (event.target === credits) credits.close();   // clic en el fondo, afuera de la tarjeta
  });
}
