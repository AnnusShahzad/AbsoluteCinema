// Theme Toggle
document.querySelectorAll('[data-bs-theme-value]').forEach(button => {
    button.addEventListener('click', () => {
        const theme = button.getAttribute('data-bs-theme-value');
        document.documentElement.setAttribute('data-bs-theme', theme);
        localStorage.setItem('theme', theme);
        document.querySelectorAll('[data-bs-theme-value]').forEach(btn => {
            btn.parentElement.classList.remove('active');
        });
        button.parentElement.classList.add('active');
    });
});

// Load saved theme
window.addEventListener('DOMContentLoaded', () => {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme) {
        document.documentElement.setAttribute('data-bs-theme', savedTheme);
        const activeBtn = document.querySelector(`[data-bs-theme-value="${savedTheme}"]`);
        if (activeBtn) activeBtn.parentElement.classList.add('active');
    }

    // Initialize animations
    document.querySelectorAll('.fadeIn').forEach(el => {
        el.style.opacity = '0';
        setTimeout(() => {
            el.style.opacity = '1';
        }, 100);
    });
});

// Search: Enter searches titles; the dropdown links filter by genre
const searchInput = document.getElementById('searchInput');
const dropdown = document.getElementById('filterDropdown');

// Not every page has the search box
if (searchInput && dropdown) {
    searchInput.addEventListener('focus', () => {
        dropdown.style.display = 'block';
    });

    searchInput.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            window.location.href = '/movies/search?query=' + encodeURIComponent(searchInput.value.trim());
        }
    });

    // Close dropdown when clicking outside
    document.addEventListener('click', (event) => {
        if (!searchInput.contains(event.target) && !dropdown.contains(event.target)) {
            dropdown.style.display = 'none';
        }
    });
}
