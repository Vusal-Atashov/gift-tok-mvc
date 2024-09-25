document.addEventListener('DOMContentLoaded', function () {
    const darkBtn = document.getElementById('darkBtn');
    const lightBtn = document.getElementById('lightBtn');
    const createGameBtn = document.querySelector('.create-game-btn');
    const usernameInput = document.querySelector('.user-name');

    if (!darkBtn || !lightBtn || !createGameBtn || !usernameInput) {
        console.error("Some elements are missing.");
        return;
    }

    // Logo toggle funksiyası
    function toggleLogo(mode) {
        const logoImages = document.querySelectorAll('.logo img');
        if (logoImages.length >= 2) {
            if (mode === 'dark') {
                logoImages[0].style.display = 'block';
                logoImages[1].style.display = 'none';
            } else {
                logoImages[0].style.display = 'none';
                logoImages[1].style.display = 'block';
            }
        }
    }

    function setMode(mode) {
        localStorage.setItem('mode', mode);
        document.body.classList.remove('dark-mode', 'light-mode');
        document.body.classList.add(`${mode}-mode`);
        toggleLogo(mode);
    }

    darkBtn.addEventListener('click', () => setMode('dark'));
    lightBtn.addEventListener('click', () => setMode('light'));

    const savedMode = localStorage.getItem('mode') || 'light';
    setMode(savedMode);

    createGameBtn.addEventListener('click', (event) => {
        event.preventDefault();

        const username = usernameInput.value.trim();
        if (!username) {
            console.error("İstifadəçi adı boş ola bilməz!");
            return;
        }

        // Arka planda çalışacak istekler
        fetch('http://192.168.1.68:8080/api/v1/submit-username', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username: username })
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error("Kullanıcı adı gönderilirken hata oluştu.");
                }
                return response.json();
            })
            .then(submitData => {
                console.log('Kullanıcı adı başarıyla gönderildi:', submitData);

                return fetch('http://192.168.1.68:8080/api/v1/start-tiktok', {
                    method: 'GET'
                });
            })
            .then(startResponse => startResponse.text())
            .then(startData => {
                console.log('TikTok izleme başlatıldı:', startData);
            })
            .catch(error => {
                console.error('Hata oluştu:', error);
            });

        // Yönlendirme hemen gerçekleşiyor
        setTimeout(() => window.location.href = "milestone/milestone.html", 100);
    });
});
