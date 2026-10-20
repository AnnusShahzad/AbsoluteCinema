// Shared behaviour for the admin management pages.

const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

async function apiRequest(method, url, body) {
    const headers = { [csrfHeader]: csrfToken };
    if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
    }
    const response = await fetch(url, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body)
    });
    if (!response.ok) {
        throw new Error(`${method} ${url} failed with status ${response.status}`);
    }
    return response.status === 204 ? null : response.json().catch(() => null);
}

// Hides table rows in a panel that don't contain the text typed into its search box.
function bindTableSearch(panel) {
    const input = panel.querySelector('.search-bar input');
    if (!input) {
        return;
    }
    input.addEventListener('input', () => {
        const term = input.value.trim().toLowerCase();
        panel.querySelectorAll('tbody tr[data-row]').forEach(row => {
            row.style.display = row.textContent.toLowerCase().includes(term) ? '' : 'none';
        });
    });
}

function bindTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            tabBtns.forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById(`${btn.dataset.tab}-panel`).classList.add('active');
        });
    });
}

// Buttons with data-delete-url remove their row after the server confirms the delete.
function bindDeleteButtons() {
    document.querySelectorAll('[data-delete-url]').forEach(btn => {
        btn.addEventListener('click', async () => {
            if (!confirm(`Delete ${btn.dataset.label}?`)) {
                return;
            }
            try {
                await apiRequest(btn.dataset.deleteMethod || 'DELETE', btn.dataset.deleteUrl);
                btn.closest('tr').remove();
            } catch (error) {
                console.error(error);
                alert(`Could not delete ${btn.dataset.label}. It may still be referenced by bookings.`);
            }
        });
    });
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.tab-panel').forEach(bindTableSearch);
    bindTabs();
    bindDeleteButtons();
});
