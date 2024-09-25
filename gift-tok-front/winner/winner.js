document.addEventListener('DOMContentLoaded', function() {
    const body = document.body;

    // Dark mode seçimini kontrol et ve uygula
    const savedMode = localStorage.getItem('mode') || 'light';
    body.classList.remove('dark-mode', 'light-mode');
    body.classList.add(`${savedMode}-mode`);

    const prizeElement = document.getElementById('prize');
    const profileImageElement = document.getElementById('profile-image');
    const usernameElement = document.getElementById('username');
    const giftBoxDiv = document.querySelector('.giftbox');

    const winnerEndpoint = "http://192.168.1.68:8080/api/v1/select-winner";

    // Kazananı API'den çek ve dinamik verileri ekle
    function fetchWinner() {
        fetch(winnerEndpoint)
            .then(response => response.json())
            .then(data => {
                const currentAward = localStorage.getItem('currentAward') || 0;

                // Dinamik verileri HTML'e ekle
                prizeElement.innerHTML = `$${currentAward}`;
                profileImageElement.src = `data:image/jpeg;base64,${data.picture_base64}`;
                usernameElement.textContent = data.username;

                // Kazanan bilgilerini localStorage'a kaydet
                localStorage.setItem('winnerProfilePic', data.picture_base64);
                localStorage.setItem('winnerName', data.username);
                localStorage.setItem('currentAward', currentAward);

                // GSAP animasyonunu başlat
                gsap.fromTo(giftBoxDiv, { scale: 0, opacity: 0 }, { scale: 1, opacity: 1, duration: 1 });
                gsap.to(prizeElement, { y: -45, opacity: 1, duration: 1.5, delay: 1 });
            })
            .catch(error => {
                console.error("Kazanan getirilirken hata oluştu, varsayılan veri kullanılıyor:", error);

                // Hata durumunda varsayılan verilerle
                prizeElement.innerHTML = "$ 0";
                profileImageElement.src = "../image/default-profile.png";
                usernameElement.textContent = "Varsayılan Kullanıcı";

                gsap.fromTo(giftBoxDiv, { scale: 0, opacity: 0 }, { scale: 1, opacity: 1, duration: 1 });
                gsap.to(prizeElement, { y: -45, opacity: 1, duration: 1.5, delay: 1 });
            });
    }

    // Kazananı al ve ödül kartını yarat
    fetchWinner();

    // Yönlendirme animasyonu (sayfa 5 saniye sonra yönlenir)
    setTimeout(function() {
        gsap.to('body', { opacity: 0, duration: 2, onComplete: function() {
                window.location.href = "../congratulations/congratulations.html";
            }});
    }, 5000);
});
