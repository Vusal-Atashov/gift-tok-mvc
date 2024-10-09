document.addEventListener('DOMContentLoaded', () => {
    const body = document.body;

    // Dark mode seçimini kontrol et ve uygula
    const savedMode = localStorage.getItem('mode') || 'light'; // Varsayılan light mode
    body.classList.remove('dark-mode', 'light-mode');
    body.classList.add(`${savedMode}-mode`);

    const milestones = JSON.parse(localStorage.getItem('milestones')) || [];

    // Elementleri DOM'da kontrol et
    const milestoneIndicators = document.getElementById('milestone-indicators');
    const likeIndicators = document.getElementById('like-indicators');
    const progressBar = document.getElementById('progress-bar');
    const progressBarContainer = document.getElementById('progress-bar-container');
    const resultsList = document.getElementById('results-list');
    const selectWinnerBtn = document.getElementById('select-winner-btn');

    // Eğer milestoneIndicators gibi elementler null dönerse hata ver
    if (!milestoneIndicators || !likeIndicators || !progressBar || !progressBarContainer || !resultsList || !selectWinnerBtn) {
        console.error("Gerekli DOM elementlerinden biri bulunamadı.");
        return;
    }

    const heartEmoji = '❤️';
    const maxLikes = Math.max(...milestones.map(milestone => milestone.likes));

    // Milestones ve like indicator'larını oluşturma
    milestones.forEach((milestoneData, index) => {
        const milestoneSpan = document.createElement('span');
        milestoneSpan.textContent = `$${milestoneData.milestone}`;
        milestoneIndicators.appendChild(milestoneSpan);

        const likeSpan = document.createElement('span');
        likeSpan.textContent = `${milestoneData.likes} ${heartEmoji}`;
        likeIndicators.appendChild(likeSpan);
    });

    // Progres bar ve milestones'a göre işaretçileri yerleştirme
    milestones.forEach((milestoneData, index) => {
        let likePercentage;

        // Milestone sayısına göre her bir milestone'un yüzde pozisyonunu hesapla
        if (milestones.length === 1) {
            likePercentage = 99; // Tek milestone varsa %99'de olmalı
        } else {
            likePercentage = ((index + 1) / milestones.length) * 99; // Diğer milestone'lar orantılı olarak yerleştirilecek
        }

        // Milestone işaretçisini konumlandırma
        const marker = document.createElement('div');
        marker.classList.add('bar-marker');
        marker.style.left = `${likePercentage}%`; // Dinamik yerleştirme
        progressBarContainer.appendChild(marker);

        // Like göstergesi için hizalama
        const likeSpan = likeIndicators.children[index];
        likeSpan.style.left = `${likePercentage-10}%`;

        // Milestone göstergesi için hizalama
        const milestoneSpan = milestoneIndicators.children[index];
        milestoneSpan.style.left = `${likePercentage-10}%`;
    });

    // Ödül hesaplama fonksiyonu
    function calculateAward(currentLikes) {
        let award = 0;
        milestones.forEach((milestoneData) => {
            if (currentLikes >= milestoneData.likes) {
                award = milestoneData.milestone;
            }
        });
        return award;
    }

    // Progres bar'ı milestone'lara göre güncelleme
    function updateLikes(currentLikes) {
        let maxReachedMilestoneIndex = 0; // Geçilmiş en büyük milestone'un indeksini tutar
        let likePercentage = 0;

        // Milestone'ların hepsini döngüyle kontrol et
        for (let i = 0; i < milestones.length; i++) {
            const milestoneData = milestones[i];
            const milestoneLikes = milestoneData.likes;

            // Eğer mevcut like sayısı milestone'u geçtiyse, son milestone olarak bunu kabul et
            if (currentLikes >= milestoneLikes) {
                maxReachedMilestoneIndex = i; // Geçilen milestone'un indeksini tutar
            }
        }

        // Eğer son milestone geçildiyse bar tamamen dolacak
        if (currentLikes < milestones[0].likes) {
            likePercentage = 0; // İlk milestone'a ulaşılmadıysa progress bar %0 olmalı
        } else if (currentLikes >= milestones[milestones.length - 1].likes) {
            likePercentage = 99;
        } else {
            likePercentage = ((maxReachedMilestoneIndex + 1) / milestones.length) * 99;
        }

        // Progress bar'ın genişliğini güncelle
        progressBar.style.width = `${likePercentage}%`;

        // Milestone'ları güncelle (hangi milestone aktif, hangileri pasif)
        milestones.forEach((milestoneData, index) => {
            const milestoneElement = milestoneIndicators.children[index];
            if (index <= maxReachedMilestoneIndex) {
                milestoneElement.style.color = '#06b216'; // Aktif milestone'lar
                milestoneElement.style.fontWeight = 'bold';
            } else {
                milestoneElement.style.color = '#333'; // Pasif milestone'lar
                milestoneElement.style.fontWeight = 'normal';
            }
        });

        const currentAward = calculateAward(currentLikes);
        localStorage.setItem('currentAward', currentAward); // Ödülü localStorage'a kaydediyoruz
    }

    // 10 saniyede bir backend'den totalLikes değerini al
    setInterval(() => {
        fetch('http://192.168.1.68:8080/api/v1/like')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                return response.text(); // JSON değilse text olarak oku
            })
            .then(text => {
                if (!text || text.trim() === "") {
                    throw new Error("Boş bir yanıt alındı");
                }

                try {
                    const data = JSON.parse(text); // JSON'a çevirme işlemi burada yapılıyor
                    const currentLikes = data.total_likes || 0;
                    updateLikes(currentLikes);
                } catch (e) {
                    console.error("Geçersiz JSON formatı:", e, text);
                }
            })
            .catch(error => {
                console.error("Error fetching total likes:", error);
            });
    }, 3000);

    // 20 saniyede bir backend'den winners verisini al ve UI'yı güncelle
    setInterval(() => {
        fetch('http://192.168.1.68:8080/api/v1/winners')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                return response.json();
            })
            .then(users => {
                resultsList.innerHTML = ''; // Mevcut sonuç listesini temizle

                users.forEach((user, index) => {
                    const winningChance = user.winning_chance && typeof user.winning_chance === 'number' ? user.winning_chance.toFixed(2) : 'N/A';
                    const totalGiftValue = user.total_gift_value ? `💎 ${user.total_gift_value}` : '💎 0';

                    const listItem = document.createElement('li');
                    listItem.innerHTML = `
                        <div class="avatar-container">
                            <img src="data:image/jpeg;base64,${user.picture_base64}" alt="${user.profile_name}">
                        </div>
                        <div class="user-info">
                            <span>${user.profile_name}</span>
                            <span class="chance">${winningChance}%</span>
                        </div>
                        <div class="amount">${totalGiftValue}</div>
                    `;
                    resultsList.appendChild(listItem);
                });
            })
            .catch(error => {
                console.error("Error fetching winners data:", error);
            });
    }, 800);

    // Winner seçme butonu işlevi
    if (selectWinnerBtn) {
        selectWinnerBtn.addEventListener('click', function () {
            const currentAward = localStorage.getItem('currentAward');
            fetch('http://192.168.1.68:8080/api/v1/stop-tiktok', {
                method: 'GET'
            }).then(response => {
                if (response.ok) {
                    console.log("TikTok takip işlemi başarıyla durduruldu.");
                } else {
                    console.error("TikTok takip işlemi durdurulamadı.");
                }
            }).catch(error => {
                console.error("TikTok takip işlemi durdurulurken hata oluştu:", error);
            });
            window.location.href = '../winner/winner.html'; // Winner sayfasına yönlendirme
        });
    } else {
        console.error("selectWinnerBtn bulunamadı.");
    }

    // Başlangıçta bir kez kazananları al
    fetch('http://192.168.1.68:8080/api/v1/winners')
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.json();
        })
        .then(users => {
            users.forEach((user, index) => {
                const winningChance = user.winning_chance && typeof user.winning_chance === 'number' ? user.winning_chance.toFixed(2) : 'N/A';
                const totalGiftValue = user.total_gift_value ? `💎 ${user.total_gift_value}` : '💎N/A';

                const listItem = document.createElement('li');
                listItem.innerHTML = `
                    <div class="avatar-container">
                        <img src="data:image/jpeg;base64,${user.picture_base64}" alt="${user.profile_name}">
                    </div>
                    <div class="user-info">
                        <span>${user.profile_name}</span>
                        <span class="chance">${winningChance}%</span>
                    </div>
                    <div class="amount">${totalGiftValue}</div>
                `;
                resultsList.appendChild(listItem);
            });
        })
        .catch(error => {
            console.error("Error fetching user data:", error);
        });
});
