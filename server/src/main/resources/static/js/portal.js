// portal.js - JavaScript Logic for ClinicaSaaS Console & Landing Page

document.addEventListener('DOMContentLoaded', () => {
    // Determine which page we are on
    const onboardingWizard = document.getElementById('onboarding-wizard');
    const customerDashboard = document.getElementById('customer-dashboard');
    const superAdminDashboard = document.getElementById('super-admin-dashboard');

    if (onboardingWizard && customerDashboard) {
        initConsole();
    } else {
        initLandingPage();
    }
});

/* ============================================================================
   LANDING PAGE LOGIC
   ============================================================================ */
function initLandingPage() {
    // Fetch registered hospitals count to show live stats on landing page
    fetch('/api/tenants')
        .then(res => res.json())
        .then(tenants => {
            const countEl = document.getElementById('tenant-count');
            if (countEl && tenants && tenants.length > 0) {
                countEl.textContent = `${tenants.length}+`;
            }
        })
        .catch(err => console.log('Error fetching live stats:', err));
}

/* ============================================================================
   CONSOLE (WIZARD & DASHBOARD) LOGIC
   ============================================================================ */
let currentStep = 1;
let selectedPlan = 'premium';
let paymentMethod = 'mpesa';
let currentTenant = null;

function initConsole() {
    const params = new URLSearchParams(window.location.search);
    const registerParam = params.get('register');
    const planParam = params.get('plan');

    // Pre-select plan if passed in URL
    if (planParam) {
        selectedPlan = planParam;
        document.querySelectorAll('.plan-select-card').forEach(card => {
            card.classList.remove('selected');
            if (card.getAttribute('data-plan') === planParam) {
                card.classList.add('selected');
            }
        });
        updateInvoiceSummary();
    }

    // Toggle Wizard vs Dashboard
    if (registerParam === 'true' || !localStorage.getItem('currentTenantId')) {
        showWizard();
    } else {
        const cachedId = localStorage.getItem('currentTenantId');
        loadTenantDashboard(cachedId);
    }

    // Bind Wizard events
    initWizardEvents();

    // Bind Dashboard events
    initDashboardEvents();
}

// Show onboarding wizard
function showWizard() {
    document.getElementById('onboarding-wizard').classList.remove('hidden');
    document.getElementById('customer-dashboard').classList.add('hidden');
    document.getElementById('super-admin-dashboard').classList.add('hidden');
    currentStep = 1;
    updateWizardUI();
}

function initWizardEvents() {
    const btnNext = document.getElementById('btn-next');
    const btnPrev = document.getElementById('btn-prev');
    const btnFinish = document.getElementById('btn-finish');

    btnNext.addEventListener('click', () => {
        if (validateStep(currentStep)) {
            currentStep++;
            updateWizardUI();
        }
    });

    btnPrev.addEventListener('click', () => {
        currentStep--;
        updateWizardUI();
    });

    // Plan Selection cards
    document.querySelectorAll('.plan-select-card').forEach(card => {
        card.addEventListener('click', () => {
            document.querySelectorAll('.plan-select-card').forEach(c => c.classList.remove('selected'));
            card.classList.add('selected');
            selectedPlan = card.getAttribute('data-plan');
            updateInvoiceSummary();
        });
    });

    // Payment Tabs
    document.querySelectorAll('.pay-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.pay-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            paymentMethod = tab.getAttribute('data-method');
            
            document.querySelectorAll('.payment-form').forEach(f => f.classList.remove('active'));
            document.getElementById(`form-${paymentMethod}`).classList.add('active');
        });
    });

    // Complete Checkout Action
    btnFinish.addEventListener('click', () => {
        submitHospitalRegistration();
    });
}

function updateWizardUI() {
    // Show/Hide Steps
    document.querySelectorAll('.wizard-step-content').forEach(content => content.classList.remove('active'));
    document.getElementById(`step-content-${currentStep}`).classList.add('active');

    // Update Step Indicators
    document.querySelectorAll('.wizard-steps .step').forEach((step, idx) => {
        const stepNum = idx + 1;
        step.classList.remove('active', 'completed');
        if (stepNum === currentStep) {
            step.classList.add('active');
        } else if (stepNum < currentStep) {
            step.classList.add('completed');
        }
    });

    // Footer buttons control
    const btnNext = document.getElementById('btn-next');
    const btnPrev = document.getElementById('btn-prev');
    const btnFinish = document.getElementById('btn-finish');

    if (currentStep === 1) {
        btnPrev.classList.add('hidden');
        btnNext.classList.remove('hidden');
        btnFinish.classList.add('hidden');
    } else if (currentStep > 1 && currentStep < 4) {
        btnPrev.classList.remove('hidden');
        btnNext.classList.remove('hidden');
        btnFinish.classList.add('hidden');
    } else if (currentStep === 4) {
        btnPrev.classList.remove('hidden');
        btnNext.classList.add('hidden');
        btnFinish.classList.remove('hidden');
    }
}

function updateInvoiceSummary() {
    const prices = { basic: 19, premium: 49, enterprise: 150 };
    const names = { basic: 'Basic Subscription Plan', premium: 'Premium Subscription Plan', enterprise: 'Enterprise Subscription Plan' };
    
    const priceStr = `$${prices[selectedPlan]}.00`;
    document.getElementById('summary-plan-name').textContent = names[selectedPlan];
    document.getElementById('summary-plan-price').textContent = priceStr + '/mo';
    document.getElementById('summary-total-price').textContent = priceStr;
}

function validateStep(step) {
    if (step === 1) {
        const name = document.getElementById('hospital-name').value.trim();
        const code = document.getElementById('hospital-code').value.trim();
        const email = document.getElementById('hospital-email').value.trim();
        if (!name || !code || !email) {
            alert('Please fill in all hospital details.');
            return false;
        }
        if (!/^[a-z0-9-]+$/.test(code)) {
            alert('Subdomain code must contain only lowercase letters, numbers, and dashes.');
            return false;
        }
    } else if (step === 2) {
        const name = document.getElementById('admin-name').value.trim();
        const username = document.getElementById('admin-username').value.trim();
        const pin = document.getElementById('admin-pin').value.trim();
        if (!name || !username || !pin) {
            alert('Please specify the system administrator credentials.');
            return false;
        }
        if (!username.startsWith('AD-')) {
            alert('Admin Staff ID must start with "AD-" (e.g. AD-010).');
            return false;
        }
        if (!/^\d{4}$/.test(pin)) {
            alert('PIN code must be exactly 4 digits.');
            return false;
        }
    }
    return true;
}

// AJAX registration submit to Ktor
function submitHospitalRegistration() {
    const name = document.getElementById('hospital-name').value.trim();
    const code = document.getElementById('hospital-code').value.trim();
    const email = document.getElementById('hospital-email').value.trim();
    const adminName = document.getElementById('admin-name').value.trim();
    const adminUsername = document.getElementById('admin-username').value.trim();
    const adminPin = document.getElementById('admin-pin').value.trim();

    const payload = {
        name: name,
        tenantCode: code,
        contactEmail: email,
        plan: selectedPlan,
        adminName: adminName,
        adminUsername: adminUsername,
        adminPin: adminPin
    };

    // Show Loading
    const loader = document.getElementById('wizard-loading');
    const loaderMsg = loader.querySelector('.loading-message');
    loader.classList.remove('hidden');
    loaderMsg.textContent = "Deploying isolated database container...";

    // API Post to server
    fetch('/api/tenants/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
    .then(res => {
        if (!res.ok) throw new Error('Registration failed. Code may already be in use.');
        return res.json();
    })
    .then(tenant => {
        // Step 2 of loading: simulate payment push
        loaderMsg.textContent = paymentMethod === 'mpesa' 
            ? "Initiating billing: Sending MPesa STK Push to Safaricom..."
            : "Processing card authorization securely...";
            
        setTimeout(() => {
            loaderMsg.textContent = "Payment approved! Seeding initial administrator and configurations...";
            
            setTimeout(() => {
                loader.classList.add('hidden');
                localStorage.setItem('currentTenantId', tenant.id);
                // Clear URL parameters
                window.history.replaceState({}, document.title, window.location.pathname);
                loadTenantDashboard(tenant.id);
            }, 1500);
        }, 2000);
    })
    .catch(err => {
        loader.classList.add('hidden');
        alert(err.message || 'An error occurred during hospital registration.');
    });
}

/* ============================================================================
   DASHBOARD LOAD & RENDER LOGIC
   ============================================================================ */
function loadTenantDashboard(tenantId) {
    document.getElementById('onboarding-wizard').classList.add('hidden');
    document.getElementById('customer-dashboard').classList.remove('hidden');
    document.getElementById('super-admin-dashboard').classList.add('hidden');

    // Fetch tenant details from list
    fetch('/api/tenants')
        .then(res => res.json())
        .then(tenants => {
            currentTenant = tenants.find(t => t.id === tenantId);
            if (!currentTenant) {
                // If not found (e.g. wiped db), reset to wizard
                localStorage.removeItem('currentTenantId');
                showWizard();
                return;
            }
            renderTenantDashboard();
        })
        .catch(err => console.log('Error loading tenant details:', err));
}

function renderTenantDashboard() {
    if (!currentTenant) return;

    // Set text elements
    document.getElementById('sub-hospital-name').textContent = currentTenant.name;
    document.getElementById('sub-hospital-code').querySelector('code').textContent = currentTenant.tenantCode;
    document.getElementById('sub-hospital-email').querySelector('code').textContent = currentTenant.contactEmail;
    
    const statusPill = document.getElementById('sub-status');
    statusPill.textContent = currentTenant.billingStatus;
    statusPill.className = `status-pill ${currentTenant.billingStatus === 'active' ? 'active' : 'warning'}`;

    // Load Metrics
    loadTenantMetrics();

    // Load Staff Directory
    loadTenantStaff();

    // Render Invoices list (simulated based on billing plan)
    renderInvoicesTable();
}

function loadTenantMetrics() {
    fetch(`/api/tenants/${currentTenant.id}/metrics`)
        .then(res => res.json())
        .then(metrics => {
            document.getElementById('metric-patients').textContent = metrics.patientsCount;
            document.getElementById('metric-staff').textContent = metrics.staffCount;
            document.getElementById('metric-queue').textContent = metrics.activeQueueCount;
        })
        .catch(err => console.log('Error loading metrics:', err));
}

function loadTenantStaff() {
    // If it is the default tenant, we fetch from /auth/staff. Otherwise we query the tenant staff endpoint.
    const url = currentTenant.id === 'default' 
        ? '/auth/staff' 
        : `/api/tenants/${currentTenant.id}/staff`;

    // Fetch staff list. Since Ktor '/auth/staff' returns StaffMemberDto, we parse the results.
    // If it's a tenant custom staff endpoint, we get our custom JSON array.
    fetch(url)
        .then(res => res.json())
        .then(staffList => {
            const body = document.getElementById('staff-list-body');
            body.innerHTML = '';
            
            if (staffList.length === 0) {
                body.innerHTML = `<tr><td colspan="5" style="text-align:center;color:var(--color-text-muted)">No staff accounts created yet.</td></tr>`;
                return;
            }

            staffList.forEach(staff => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td><code>${staff.id}</code></td>
                    <td>${staff.fullName}</td>
                    <td><span class="badge" style="background:rgba(139, 92, 246, 0.1);color:var(--color-purple);border-color:rgba(139, 92, 246, 0.2)">${staff.role}</span></td>
                    <td>${staff.department || 'General Medicine'}</td>
                    <td><span style="color:var(--color-success)"><i class="fa-solid fa-circle-check"></i> Enabled</span></td>
                `;
                body.appendChild(tr);
            });
        })
        .catch(err => console.log('Error loading staff:', err));
}

function renderInvoicesTable() {
    const body = document.getElementById('invoice-list-body');
    body.innerHTML = '';

    const price = currentTenant.billingPlan === 'enterprise' ? 150 : (currentTenant.billingPlan === 'premium' ? 49 : 19);
    const dateObj = new Date(currentTenant.createdAt);
    const billingDate = dateObj.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });

    // Seed 2 billing cycles
    const invoices = [
        { date: billingDate, id: `INV-${currentTenant.tenantCode.toUpperCase()}-001`, plan: currentTenant.billingPlan, amount: `$${price}.00`, status: 'PAID' }
    ];

    invoices.forEach(inv => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${inv.date}</td>
            <td><code>${inv.id}</code></td>
            <td>${inv.plan.toUpperCase()}</td>
            <td><strong>${inv.amount}</strong></td>
            <td><span class="badge" style="background:rgba(16, 185, 129, 0.1);color:var(--color-success);border-color:rgba(16, 185, 129, 0.2)">${inv.status}</span></td>
        `;
        body.appendChild(tr);
    });
}

function initDashboardEvents() {
    // Launch HIMS Button Click: Mock Launch
    document.getElementById('btn-launch-hims').addEventListener('click', () => {
        const message = `Launching Clinica HIMS for ${currentTenant.name}.\n\nTo access the clinic dashboard:\n1. Open Android Studio/desktop App\n2. Configure backend URL to: http://localhost:8080\n3. Log in using your registered Admin credentials.\n\nRedirecting you to the mobile login portal.`;
        alert(message);
        // Redirect to scope page or health check as proof of API connection
        window.location.href = '/scope';
    });

    // Add Staff Modal Toggle
    const btnShowAdd = document.getElementById('btn-show-add-staff');
    const addStaffContainer = document.getElementById('add-staff-container');
    const btnCancelStaff = document.getElementById('btn-cancel-staff');
    const btnSaveStaff = document.getElementById('btn-save-staff');

    btnShowAdd.addEventListener('click', () => {
        addStaffContainer.classList.toggle('hidden');
    });

    btnCancelStaff.addEventListener('click', () => {
        addStaffContainer.classList.add('hidden');
        clearStaffForm();
    });

    btnSaveStaff.addEventListener('click', () => {
        submitNewStaff();
    });

    // Super Admin Switch Toggle
    const superAdminToggle = document.getElementById('super-admin-toggle');
    superAdminToggle.addEventListener('click', () => {
        const dashboard = document.getElementById('customer-dashboard');
        const superAdmin = document.getElementById('super-admin-dashboard');

        if (superAdmin.classList.contains('hidden')) {
            // Load global hospitals list first
            loadSuperAdminData();
            superAdmin.classList.remove('hidden');
            dashboard.classList.add('hidden');
            superAdminToggle.innerHTML = '<i class="fa-solid fa-hospital"></i>';
            superAdminToggle.title = "Switch back to Clinic Portal";
        } else {
            superAdmin.classList.add('hidden');
            dashboard.classList.remove('hidden');
            superAdminToggle.innerHTML = '<i class="fa-solid fa-user-shield"></i>';
            superAdminToggle.title = "Switch to Super Admin View";
        }
    });
}

function clearStaffForm() {
    document.getElementById('staff-name').value = '';
    document.getElementById('staff-pin').value = '';
    document.getElementById('staff-dept').value = '';
}

function submitNewStaff() {
    const name = document.getElementById('staff-name').value.trim();
    const role = document.getElementById('staff-role').value;
    const pin = document.getElementById('staff-pin').value.trim();
    const dept = document.getElementById('staff-dept').value.trim() || 'General Medicine';

    if (!name || !pin) {
        alert('Name and PIN are required fields.');
        return;
    }
    if (!/^\d{4}$/.test(pin)) {
        alert('PIN must be exactly 4 digits.');
        return;
    }

    const payload = {
        name: name,
        role: role,
        pin: pin,
        department: dept
    };

    fetch(`/api/tenants/${currentTenant.id}/staff`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
    .then(res => {
        if (!res.ok) throw new Error('Failed to create staff member.');
        return res.json();
    })
    .then(data => {
        alert(`Account created successfully!\n\nStaff ID: ${data.id}\nPIN: ${pin}\nRole: ${data.role}`);
        document.getElementById('add-staff-container').classList.add('hidden');
        clearStaffForm();
        loadTenantStaff();
        loadTenantMetrics(); // Update metrics count
    })
    .catch(err => {
        alert(err.message);
    });
}

/* ============================================================================
   SUPER ADMIN PORTAL DATA LOADER
   ============================================================================ */
function loadSuperAdminData() {
    fetch('/api/tenants')
        .then(res => res.json())
        .then(tenants => {
            const body = document.getElementById('global-hospitals-body');
            body.innerHTML = '';

            document.getElementById('saas-total-hospitals').textContent = tenants.length;
            
            let totalRev = 0;
            tenants.forEach(t => {
                totalRev += t.amountBilled;
                const tr = document.createElement('tr');
                
                const dateObj = new Date(t.createdAt);
                const regDate = dateObj.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });

                tr.innerHTML = `
                    <td><strong>${t.name}</strong></td>
                    <td><code>${t.tenantCode}</code></td>
                    <td><span class="badge" style="background:rgba(20, 184, 166, 0.1);color:var(--color-teal);border-color:rgba(20, 184, 166, 0.2)">${t.billingPlan.toUpperCase()}</span></td>
                    <td><span class="status-pill active">${t.billingStatus}</span></td>
                    <td><strong>$${t.amountBilled.toFixed(2)}</strong></td>
                    <td>${regDate}</td>
                    <td>
                        <button class="btn btn-secondary btn-small" onclick="adminSwitchTenant('${t.id}')">
                            <i class="fa-solid fa-folder-open"></i> Manage
                        </button>
                    </td>
                `;
                body.appendChild(tr);
            });

            document.getElementById('saas-total-revenue').textContent = `$${totalRev.toFixed(2)}`;
        })
        .catch(err => console.log('Error loading super-admin data:', err));
}

// Function triggered by clicking "Manage" in Super Admin view
window.adminSwitchTenant = function(tenantId) {
    localStorage.setItem('currentTenantId', tenantId);
    // Switch to Dashboard layout
    const dashboard = document.getElementById('customer-dashboard');
    const superAdmin = document.getElementById('super-admin-dashboard');
    const superAdminToggle = document.getElementById('super-admin-toggle');

    superAdmin.classList.add('hidden');
    dashboard.classList.remove('hidden');
    superAdminToggle.innerHTML = '<i class="fa-solid fa-user-shield"></i>';
    superAdminToggle.title = "Switch to Super Admin View";

    loadTenantDashboard(tenantId);
};
