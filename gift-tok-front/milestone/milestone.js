document.addEventListener('DOMContentLoaded', function() {
    const body = document.body;

    // Check for saved mode in localStorage on page load
    const savedMode = localStorage.getItem('mode') || 'light'; // Default to light mode
    body.classList.remove('dark-mode', 'light-mode');
    body.classList.add(`${savedMode}-mode`);

    let milestoneCount = 1;

    function addMilestone() {
        milestoneCount++;
        const milestoneContainer = document.getElementById('milestone-container');

        const milestoneDiv = document.createElement('div');
        milestoneDiv.className = 'milestone';
        milestoneDiv.id = `milestone${milestoneCount}`;

        milestoneDiv.innerHTML = `
            <label>${milestoneCount}</label>
            <input type="text" placeholder="Milestone" class="milestone-input">
            <input type="number" placeholder="Likes" class="like-input">
            <button class="remove-btn" onclick="removeMilestone(this)">x</button>
        `;

        milestoneContainer.appendChild(milestoneDiv);
    }

    function removeMilestone(element) {
        const milestoneDiv = element.parentElement;
        milestoneDiv.remove();
        renumberMilestones(); // Call renumberMilestones after removal
    }

    function renumberMilestones() {
        const milestoneContainer = document.getElementById('milestone-container');
        const milestones = milestoneContainer.getElementsByClassName('milestone');
        
        // Update the label numbers based on the current order of milestones
        Array.from(milestones).forEach((milestone, index) => {
            const label = milestone.querySelector('label');
            label.textContent = index + 1; // Update label to be 1-based
        });

        // Update milestoneCount to reflect the current number of milestones
        milestoneCount = milestones.length;
    }

    function saveMilestones() {
        const milestones = [];
        const milestoneInputs = document.querySelectorAll('.milestone-input');
        const likeInputs = document.querySelectorAll('.like-input');

        milestoneInputs.forEach((input, index) => {
            const milestone = input.value;
            const likes = likeInputs[index].value;

            if (milestone && likes) {
                milestones.push({ milestone: milestone, likes: parseInt(likes) });
            }
        });

        // Save milestones to localStorage
        localStorage.setItem('milestones', JSON.stringify(milestones));
        window.location.href = '../results/results.html';  // Navigate to results page after saving
    }

    window.addMilestone = addMilestone;
    window.removeMilestone = removeMilestone;
    window.saveMilestones = saveMilestones;
});
