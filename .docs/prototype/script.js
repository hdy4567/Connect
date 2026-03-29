document.addEventListener('DOMContentLoaded', () => {
    const mirrorCard = document.getElementById('mode-mirror');
    const expansionCard = document.getElementById('mode-expansion');
    const settingsTrigger = document.getElementById('settings-trigger');
    const settingsModal = document.getElementById('settings-modal');
    const closeBtn = document.querySelector('.close-btn');

    // Card Click Animations
    const handleCardClick = (mode) => {
        const body = document.body;
        body.style.transition = 'background-color 1s ease';
        
        if (mode === 'mirror') {
            body.style.backgroundColor = '#001a33';
            alert('1번 모드: 미러링 및 터치패드 엔진을 로드합니다.');
        } else {
            body.style.backgroundColor = '#1a0033';
            alert('2번 모드: 확장 디스플레이 및 연속성 엔진을 로드합니다.');
        }
        
        setTimeout(() => {
            body.style.backgroundColor = '#050505';
        }, 2000);
    };

    mirrorCard.addEventListener('click', () => handleCardClick('mirror'));
    expansionCard.addEventListener('click', () => handleCardClick('expansion'));

    // Modal Logic
    settingsTrigger.addEventListener('click', () => {
        settingsModal.classList.add('active');
    });

    closeBtn.addEventListener('click', () => {
        settingsModal.classList.remove('active');
    });

    window.addEventListener('click', (e) => {
        if (e.target === settingsModal) {
            settingsModal.classList.remove('active');
        }
    });

    // Add hover ripple effect to cards
    const cards = document.querySelectorAll('.mode-card');
    cards.forEach(card => {
        card.addEventListener('mousemove', (e) => {
            const rect = card.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            
            card.style.setProperty('--mouse-x', `${x}px`);
            card.style.setProperty('--mouse-y', `${y}px`);
        });
    });
});
