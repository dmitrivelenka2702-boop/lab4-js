const addButton = document.getElementById('addButton');
const addForm = document.getElementById('addForm');
const tableBody = document.getElementById('tableBody');

const groupForm = document.getElementById('groupForm');
const groupName = document.getElementById('groupName');
const groupTableBody = document.getElementById('groupTableBody');
const groupSelect = document.getElementById('group');


// Дані з localStorage
let groups = JSON.parse(localStorage.getItem('groups'));

let students = JSON.parse(localStorage.getItem('students'));


// Якщо сторінка відкривається вперше
if (groups === null) {
    groups = ['М-41', 'М-21'];
}

if (students === null) {
    students = [
        {
            id: 1,
            name: 'Олена',
            group: 'М-41',
            age: 20
        }
    ];
}


// Зберігаємо дані
function saveData() {
    localStorage.setItem('groups', JSON.stringify(groups));
    localStorage.setItem('students', JSON.stringify(students));
}


// Показуємо групи
function renderGroups() {
    groupTableBody.innerHTML = '';
    groupSelect.innerHTML = '';

    groups.forEach(function (group) {

        const option = document.createElement('option');
        option.value = group;
        option.textContent = group;

        groupSelect.appendChild(option);


        const row = document.createElement('tr');

        row.innerHTML = `
            <td>${group}</td>
            <td>
                <button class="deleteGroup" data-group="${group}">
                    ✖
                </button>
            </td>
        `;

        groupTableBody.appendChild(row);
    });
}


// Показуємо студентів
function renderStudents() {
    tableBody.innerHTML = '';

    students.forEach(function (student) {

        const row = document.createElement('tr');

        row.innerHTML = `
            <td class="editable" data-id="${student.id}">
                ${student.name}
            </td>
            <td>${student.group}</td>
            <td>${student.age}</td>
            <td>
                <button class="deleteButton" data-id="${student.id}">
                    ✖
                </button>
            </td>
        `;

        tableBody.appendChild(row);
    });
}


// Кнопка додавання студента
addButton.addEventListener('click', function () {
    addForm.style.display = 'block';
});


// Додавання студента
addForm.addEventListener('submit', function (event) {

    event.preventDefault();

    const name = document.getElementById('name').value;
    const group = document.getElementById('group').value;
    const age = document.getElementById('age').value;

    const student = {
        id: Date.now(),
        name: name,
        group: group,
        age: age
    };

    students.push(student);

    saveData();
    renderStudents();

    addForm.reset();
    addForm.style.display = 'none';
});


// Додавання нової групи
groupForm.addEventListener('submit', function (event) {

    event.preventDefault();

    const newGroup = groupName.value.trim();

    if (groups.includes(newGroup)) {
        alert('Така група вже існує.');
        return;
    }

    groups.push(newGroup);

    saveData();
    renderGroups();

    groupForm.reset();
});


// Видалення студента і редагування імені
tableBody.addEventListener('click', function (event) {

    if (event.target.classList.contains('deleteButton')) {

        const id = Number(event.target.dataset.id);

        const answer = confirm('Видалити цього студента?');

        if (answer) {
            students = students.filter(function (student) {
                return student.id !== id;
            });

            saveData();
            renderStudents();
        }
    }


    if (event.target.classList.contains('editable')) {

        const cell = event.target;

        const id = Number(cell.dataset.id);

        const student = students.find(function (student) {
            return student.id === id;
        });

        const input = document.createElement('input');

        input.type = 'text';
        input.value = student.name;

        cell.textContent = '';
        cell.appendChild(input);

        input.focus();


        function saveName() {

            student.name = input.value;

            saveData();
            renderStudents();
        }


        input.addEventListener('keydown', function (event) {

            if (event.key === 'Enter') {
                saveName();
            }
        });


        input.addEventListener('blur', function () {
            saveName();
        });
    }
});


// Видалення групи
groupTableBody.addEventListener('click', function (event) {

    if (event.target.classList.contains('deleteGroup')) {

        const group = event.target.dataset.group;

        const groupIsUsed = students.some(function (student) {
            return student.group === group;
        });

        if (groupIsUsed) {

            alert(
                'Цю групу видалити неможливо, тому що вона використовується у таблиці студентів.'
            );

            return;
        }

        groups = groups.filter(function (item) {
            return item !== group;
        });

        saveData();
        renderGroups();
    }
});


// Синхронізація між відкритими вкладками
window.addEventListener('storage', function () {

    groups = JSON.parse(localStorage.getItem('groups')) || [];
    students = JSON.parse(localStorage.getItem('students')) || [];

    renderGroups();
    renderStudents();
});


// Початкове збереження та відображення
saveData();

renderGroups();
renderStudents();