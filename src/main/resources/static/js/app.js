'use strict';
(() => {
  document.documentElement.classList.add('js');
  const menu = document.querySelector('[data-menu]');
  const nav = document.getElementById('navigation');
  if (menu && nav) {
    menu.hidden = false;
    menu.addEventListener('click', () => {
      const ouvert = menu.getAttribute('aria-expanded') !== 'true';
      menu.setAttribute('aria-expanded', String(ouvert));
      nav.classList.toggle('nav-open', ouvert);
    });
    document.addEventListener('keydown', e => {
      if (e.key === 'Escape' && menu.getAttribute('aria-expanded') === 'true') {
        menu.setAttribute('aria-expanded', 'false'); nav.classList.remove('nav-open'); menu.focus();
      }
    });
  }
  document.querySelectorAll('form[data-import]').forEach(form => form.addEventListener('submit', () => {
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true; button.setAttribute('aria-busy', 'true'); button.textContent = 'Import en cours…';
  }));
  const quantite = document.getElementById('nombrePlaces');
  const estimation = document.querySelector('[data-estimation]');
  if (quantite && estimation) {
    const monnaie = new Intl.NumberFormat('fr-FR', {style: 'currency', currency: 'EUR'});
    const recalculer = () => {
      const places = Number(quantite.value), prix = Number(estimation.dataset.prix);
      estimation.textContent = Number.isInteger(places) && places >= 1 && places <= 8 && Number.isFinite(prix)
        ? monnaie.format(places * prix) : 'Choisissez de 1 à 8 places';
    };
    quantite.addEventListener('input', recalculer); recalculer();
  }
  const champErreur = document.querySelector('input.error, input[aria-invalid="true"]');
  if (champErreur) champErreur.focus({preventScroll: true});
})();
