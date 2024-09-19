document.addEventListener('DOMContentLoaded', function () {
    const darkBtn = document.getElementById('darkBtn');
    const lightBtn = document.getElementById('lightBtn');
    const body = document.body;
    const logoImages = document.querySelectorAll('.logo img'); // Select all logo images

    // Function to toggle logo visibility based on the mode
    function toggleLogo(mode) {
        if (logoImages.length >= 2) { // Ensure there are at least two logos
            if (mode === 'dark') {
                logoImages[0].style.display = 'block'; // Show dark mode logo
                logoImages[1].style.display = 'none';  // Hide light mode logo
            } else {
                logoImages[0].style.display = 'none';  // Hide dark mode logo
                logoImages[1].style.display = 'block'; // Show light mode logo
            }
        } else {
            console.error('Insufficient logos found');
        }
    }

    // Set mode in localStorage and apply it
    function setMode(mode) {
        localStorage.setItem('mode', mode); // Save mode in localStorage
        body.classList.remove('dark-mode', 'light-mode');
        body.classList.add(`${mode}-mode`);
        toggleLogo(mode);
    }

    // Dark mode button event listener
    if (darkBtn) {
        darkBtn.addEventListener('click', () => {
            setMode('dark');
        });
    }

    // Light mode button event listener
    if (lightBtn) {
        lightBtn.addEventListener('click', () => {
            setMode('light');
        });
    }

    // Check for saved mode in localStorage on page load
    const savedMode = localStorage.getItem('mode') || 'light'; // Default to light mode
    setMode(savedMode); // Apply saved mode

    const createGameBtn = document.querySelector('.create-game-btn');

    if (createGameBtn) {
        createGameBtn.addEventListener('click', (event) => {
            event.preventDefault(); // Default link behavior disabled

            // Start the fetch process asynchronously
            fetch('http://localhost:8080/api/v1/start-tiktok', {
                method: 'GET'
            })
            .then(response => response.text())
            .then(data => {
                // Handle response, though user is already redirected
                console.log(data); // You can log it in the console if needed
            })
            .catch(error => {
                console.error('Error:', error);
            });

            // Use a short delay before redirecting
            setTimeout(() => {
                window.location.href = "milestone/milestone.html";
            }, 100); // Adjust the delay as needed
        });
    } else {
        console.error('Create game button not found');
    }
});
