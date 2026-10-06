(() => {
    const input = document.getElementById('photo');
    const preview = document.getElementById('photo-preview');
    const error = document.getElementById('photo-error');
    const remove = document.getElementById('removePhoto');
    if (!input || !preview || !error) return;
    const original = preview.getAttribute('src');
    let previewUrl;

    function releasePreview() {
        if (previewUrl) URL.revokeObjectURL(previewUrl);
        previewUrl = null;
    }
    function showOriginal() {
        releasePreview();
        if (original) preview.src = original;
        else preview.removeAttribute('src');
        preview.style.display = original && !(remove && remove.checked) ? '' : 'none';
    }
    input.addEventListener('change', () => {
        releasePreview();
        error.hidden = true;
        input.setCustomValidity('');
        const file = input.files[0];
        if (!file) { showOriginal(); return; }
        if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024) {
            const message = 'Choose a JPEG or PNG photo no larger than 5 MB.';
            input.setCustomValidity(message);
            error.textContent = message;
            error.hidden = false;
            showOriginal();
            return;
        }
        if (remove) remove.checked = false;
        previewUrl = URL.createObjectURL(file);
        preview.src = previewUrl;
        preview.style.display = '';
    });
    if (remove) remove.addEventListener('change', () => {
        if (remove.checked) {
            input.value = '';
            input.setCustomValidity('');
            error.hidden = true;
        }
        showOriginal();
    });
    window.addEventListener('pagehide', releasePreview);
})();
