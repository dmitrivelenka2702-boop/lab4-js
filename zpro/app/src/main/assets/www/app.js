let DATA={tasks:[],stats:{},notificationAccess:false},FILTER='all';
const esc=s=>String(s??'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));
const fmtDate=ms=>ms?new Date(ms).toLocaleDateString('uk-UA',{day:'2-digit',month:'2-digit',year:'numeric'}):'Без дати';
const fmtReceived=ms=>new Date(ms).toLocaleString('uk-UA',{day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'});
function getData(){try{DATA=JSON.parse(Android.getData())}catch(e){DATA={tasks:[],stats:{}}}}
window.refreshAll=function(){getData();renderAll()}
document.addEventListener('DOMContentLoaded',()=>{document.querySelectorAll('.tab').forEach(b=>b.onclick=()=>{document.querySelectorAll('.tab').forEach(x=>x.classList.remove('active'));b.classList.add('active');FILTER=b.dataset.f;renderTasks()});refreshAll()})
function renderAll(){let s=DATA.stats||{};stOpen.textContent=s.open||0;stWeek.textContent=s.week||0;stOver.textContent=s.overdue||0;stDone.textContent=s.done||0;access.classList.toggle('hidden',!!DATA.notificationAccess);renderTasks()}
function renderTasks(){
 let q=(search.value||'').toLowerCase().trim(),now=Date.now(),week=now+7*86400000;
 let arr=(DATA.tasks||[]).filter(t=>{
  if(q && ![t.regNo,t.topic,t.subject,t.taskText,t.note,t.sender].join(' ').toLowerCase().includes(q))return false;
  if(FILTER==='done')return t.status==='done';
  if(FILTER==='nodate')return t.status!=='done'&&!t.deadline;
  if(FILTER==='urgent')return t.status!=='done'&&t.deadline&&t.deadline<=week;
  return true;
 });
 if(!arr.length){tasks.innerHTML='<div class="empty">Нічого немає за цим фільтром.</div>';return}
 tasks.innerHTML=arr.map(t=>{
  let cls='task';if(t.status==='done')cls+=' done';else if(t.deadline&&t.deadline<now)cls+=' over';else if(t.deadline&&t.deadline<=week)cls+=' soon';
  let dl=t.deadline?fmtDate(t.deadline):'Без дедлайну';
  return '<div class="'+cls+'" onclick="openTask('+t.id+')"><div class="topline"><div><h3>'+esc(t.subject||t.taskText||'Робочий лист')+'</h3><div class="meta">'+esc(t.sender||'')+' · '+fmtReceived(t.receivedAt)+'</div></div><div class="deadline">'+dl+'</div></div><div class="chips">'+(t.regNo?'<span class="chip reg">№ '+esc(t.regNo)+'</span>':'')+(t.topic?'<span class="chip">'+esc(t.topic)+'</span>':'')+'<span class="chip">'+statusLabel(t.status)+'</span></div></div>';
 }).join('')
}
function statusLabel(s){return s==='done'?'Закрито':s==='work'?'В роботі':'Нове'}
function quickAdd(){let subject=prompt('Коротко: що за завдання?');if(subject&&subject.trim()){Android.addManual(subject.trim());setTimeout(refreshAll,120)}}
function openTask(id){
 let t=DATA.tasks.find(x=>x.id===id);if(!t)return;
 modalBody.innerHTML='<div class="form"><h2>Картка завдання</h2>'+
 '<div class="field"><label>Тема листа</label><input value="'+esc(t.subject||'')+'" disabled></div>'+
 '<div class="row"><div class="field"><label>Вхідний №</label><input id="fReg" value="'+esc(t.regNo||'')+'" placeholder="Напр. 25777/1.2/7.2-26"></div><div class="field"><label>Тематика</label><input id="fTopic" value="'+esc(t.topic||'')+'" placeholder="Звітність, захід…"></div></div>'+
 '<div class="field"><label>Що треба зробити</label><textarea id="fTask" placeholder="Коротко сформулюй завдання">'+esc(t.taskText||'')+'</textarea></div>'+
 '<div class="row"><div class="field"><label>Дедлайн</label><input id="fDeadline" type="date" value="'+dateInput(t.deadline)+'"></div><div class="field"><label>Статус</label><select id="fStatus"><option value="new" '+(t.status==='new'?'selected':'')+'>Нове</option><option value="work" '+(t.status==='work'?'selected':'')+'>В роботі</option><option value="done" '+(t.status==='done'?'selected':'')+'>Закрито</option></select></div></div>'+
 '<div class="field"><label>Примітка / результат</label><textarea id="fNote" placeholder="Що зроблено, кому відправлено…">'+esc(t.note||'')+'</textarea></div>'+
 '<div class="sourcebox"><b>Джерело:</b> '+esc(t.source||'')+(t.raw?'<br>'+esc(t.raw):'')+'</div>'+
 '<div class="actions"><button class="primary" onclick="saveTask('+id+')">Зберегти</button><button class="danger" onclick="deleteTask('+id+')">Видалити</button></div></div>';
 modal.classList.remove('hidden')
}
function dateInput(ms){if(!ms)return'';let d=new Date(ms),y=d.getFullYear(),m=String(d.getMonth()+1).padStart(2,'0'),day=String(d.getDate()).padStart(2,'0');return y+'-'+m+'-'+day}
function saveTask(id){let dl=0;if(fDeadline.value){let d=new Date(fDeadline.value+'T12:00:00');dl=d.getTime()}Android.saveTask(id,fReg.value.trim(),fTopic.value.trim(),fTask.value.trim(),dl,fStatus.value,fNote.value.trim());closeModal();setTimeout(refreshAll,120)}
function deleteTask(id){if(confirm('Видалити це завдання?')){Android.deleteTask(id);closeModal();setTimeout(refreshAll,120)}}
function closeModal(){modal.classList.add('hidden')}
