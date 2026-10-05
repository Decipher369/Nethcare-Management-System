(() => {
    const header = document.querySelector('.public-page .site-head');
    if (!header) return;
    const button = header.querySelector('.menu-toggle');
    const panel = header.querySelector('.nav-panel');
    const mobile = window.matchMedia('(max-width: 800px)');
    const setOpen = (open) => {
        button.setAttribute('aria-expanded', String(open));
        button.setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation');
        button.querySelector('.menu-label').textContent = open ? 'Close' : 'Menu';
        header.classList.toggle('menu-open', open);
    };
    header.classList.add('nav-ready');
    button.addEventListener('click', () => setOpen(button.getAttribute('aria-expanded') !== 'true'));
    header.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && header.classList.contains('menu-open')) {
            setOpen(false);
            button.focus();
        }
    });
    document.addEventListener('click', (event) => {
        if (!header.contains(event.target) && header.classList.contains('menu-open')) {
            if (panel.contains(document.activeElement)) button.focus();
            setOpen(false);
        }
    });
    panel.addEventListener('click', (event) => {
        if (event.target.closest('a') && mobile.matches) setOpen(false);
    });
    mobile.addEventListener('change', () => {
        const focus = document.activeElement;
        setOpen(false);
        if (mobile.matches && panel.contains(focus)) button.focus();
        else if (!mobile.matches && focus === button) header.querySelector('.brand').focus();
    });
})();
