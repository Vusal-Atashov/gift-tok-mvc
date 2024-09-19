document.addEventListener("DOMContentLoaded", function() {
    const body = document.body;

    // Dark mode seçimini kontrol et ve uygula
    const savedMode = localStorage.getItem('mode') || 'light';
    body.classList.remove('dark-mode', 'light-mode');
    body.classList.add(`${savedMode}-mode`);

    // Ana konteyner
    const rewardCardContainer = document.getElementById('rewardCardContainer');

    // Yeni div yaradaraq kartı hazırlayırıq
    const rewardCard = document.createElement('div');
    rewardCard.classList.add('reward-card');

    // Təbriklər mesajını yaradaraq əlavə edirik
    const messageContainer = document.createElement('div');
    messageContainer.classList.add('message-container');

    const title = document.createElement('h2');
    title.textContent = 'Təbriklər!';
    messageContainer.appendChild(title);

    // Profil konteyneri yaradaraq əlavə edirik
    const profileContainer = document.createElement('div');
    profileContainer.classList.add('profile-container');

    // Avatar şəkli yaradaraq əlavə edirik
    const profileImage = document.createElement('img');
    profileImage.id = 'profileImage';
    profileImage.src = 'default-avatar.png';
    profileImage.alt = 'Profile Picture';
    profileContainer.appendChild(profileImage);

    // İstifadəçi adını yaradaraq əlavə edirik
    const username = document.createElement('p');
    username.id = 'username';
    username.classList.add('username');
    username.textContent = 'default_username';
    profileContainer.appendChild(username);

    // Təbriklər mesajını və profil konteynerini əsas kartın içinə əlavə edirik
    rewardCard.appendChild(messageContainer);
    rewardCard.appendChild(profileContainer);

    // Hazır kartı əsas konteynerə əlavə edirik
    rewardCardContainer.appendChild(rewardCard);

    // localStorage-dan məlumatları alırıq
    const savedProfileImage = localStorage.getItem('winnerProfilePic') || 'default-avatar.png';
    const savedUsername = localStorage.getItem('winnerName') || 'default_username';

    // Dinamik veri ilə profil şəklini və istifadəçi adını yeniləyirik
    profileImage.src = savedProfileImage.startsWith('data:image') ? savedProfileImage : `data:image/jpeg;base64,${savedProfileImage}`;
    username.textContent = savedUsername;

    // GSAP ilə animasiya tətbiq edirik
    gsap.fromTo(profileImage, { opacity: 0 }, { opacity: 1, duration: 1.5, delay: 0.5 });
    gsap.fromTo(username, { opacity: 0 }, { opacity: 1, duration: 1.5, delay: 1 });
});
