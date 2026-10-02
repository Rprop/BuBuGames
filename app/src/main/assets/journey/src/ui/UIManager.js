import { CHARACTERS,LEVELS,PHOTOS,BADGES,PHOTO_BY_ID } from '../config/game.js';
import { postcardCanvas,makeIcon } from '../art/Artwork.js';
import { downloadMemoryCard, downloadJSON, downloadFinalCard } from './ShareCard.js';
export function formatTime(ms,centiseconds=false){if(!(ms>0))return '—';const s=Math.floor(ms/1000);return `${String(Math.floor(s/60)).padStart(2,'0')}:${String(s%60).padStart(2,'0')}${centiseconds?'.'+String(Math.floor(ms%1000/10)).padStart(2,'0'):''}`;}
const brand=()=>'<div class="brand"><b>♥</b> BUBU & YIER <span> / </span> THREE LITTLE JOURNEYS</div>';

export class UIManager {
  constructor(app){
    this.app=app;this.root=document.getElementById('ui');this.thumbs={};this.scenes={};this.activeTab=0;this.previousCombo=-1;this.toastRemaining=0;
    this.iconHappy=makeIcon('happy').toDataURL();this.iconCamera=makeIcon('camera').toDataURL();
  }
  prepareArt(){for(const l of LEVELS){const chars=['bubu','yier'].map(id=>this.app.game.textures.get(`${id}-idle`).getSourceImage());this.scenes[l.id]=postcardCanvas(l.theme,[]).toDataURL('image/jpeg',.9);this.thumbs[l.id]=postcardCanvas(l.theme,chars).toDataURL('image/jpeg',.9);}}
  set(html){this.app.input.releaseAll();this.root.innerHTML=html;}
  on(selector,fn){this.root.querySelector(selector)?.addEventListener('click',fn);}
  buttonEvents(){this.on('[data-settings]',()=>this.settings());this.on('[data-back]',()=>this.app.open('title'));}
  preview(id){return this.thumbs[id];}

  title(){const a=this.app;
    this.set(`<section class="screen title-screen v3-screen"><header class="topnav">${brand()}<button class="quiet-btn" data-settings>声音与设置 ···</button></header>
      <div class="title-layout">
        <div class="title-copy">
          <span class="pill">V3 · 三段路，九个瞬间，三种节奏</span>
          <h1>布布一二<br><span>去见你的小幸福</span></h1>
          <p>城市、乐园、星空，现在终于有了各自不同的呼吸。<br>安全路可以慢慢走，高处藏着另一种风景，摄影小路则等你去发现。</p>
          <div class="actions"><button class="primary" id="begin">开始旅程 →</button><button class="secondary" id="route-map">路线图</button><button class="quiet-btn" id="title-album">幸福相册</button></div>
          <div class="title-features"><span>城市节奏</span><span>乐园连跳</span><span>星桥晚风</span><span>照片挑战更清晰</span></div>
          <div class="title-small">把去见你的路，走得更漂亮。</div>
        </div>
        <div class="title-hero-card"><img class="hero-scene-bg" src="${this.scenes[LEVELS[0].id]}" alt="晚霞城市"><div class="hero-night-wash"></div><img class="hero-pair" src="./assets/characters/reunion.png" alt="布布和一二"><div class="hero-lights">✦　✧　♥</div><div class="hero-sticker"><strong>${String(a.save.photoCount).padStart(2,'0')}</strong><span>/ 09 回忆</span></div></div>
      </div>
      <footer class="page-foot"><span>建议横屏体验；竖屏也可以继续进入。</span><div><button id="change-role-title" class="quiet-btn">当前伙伴：${CHARACTERS[a.save.data.selectedCharacter].name}</button><span>V3.0.1</span></div></footer></section>`);
    this.on('#begin',()=>a.open('characters'));this.on('#title-album',()=>a.open('album'));this.on('#route-map',()=>a.open('levels'));this.on('#change-role-title',()=>a.open('characters'));this.buttonEvents();
  }

  characters(){const a=this.app,selected=a.save.data.selectedCharacter;
    this.set(`<section class="screen v3-screen"><header class="topnav">${brand()}<button class="quiet-btn" data-back>← 回到封面</button></header>
      <div class="page-head"><div><h1 class="screen-title">今天，谁来陪你出发？</h1><div class="screen-sub">怕跳不过去就选一二；想多拿分、冲连击就选布布。</div></div><span class="pill">同速不同手感 · 全内容都能通关</span></div>
      <div class="character-grid v3-characters">${Object.values(CHARACTERS).map(c=>`<button class="character-card ${c.id===selected?'selected':''}" data-role="${c.id}"><span class="check">${c.id===selected?'✓':''}</span><img src="./assets/characters/${c.id}-idle.png" alt="${c.name}"><div class="role"><h2>${c.name}</h2><span class="subtitle">${c.subtitle}</span></div><p>${c.description}</p><div class="traits">${c.id==='bubu'?'<span>Perfect 区更宽</span><span>·</span><span>连击宽限 +0.6 秒</span><span>·</span><span>适合刷高分</span>':'<span>空中再跳一次</span><span>·</span><span>更容易修正落点</span><span>·</span><span>适合新手</span>'}</div></button>`).join('')}</div>
      <div class="char-bottom"><small>角色差异是手感差异，不是强弱差异；都能拿满 9 张照片。</small><button class="primary" id="choose-role">就让${CHARACTERS[selected].name}出发 →</button></div>
      <footer class="page-foot"><span>键盘 ← → / A D 移动，空格 / ↑ 跳跃</span><span>手机支持双指移动与跳跃；按钮已加大</span></footer></section>`);
    for(const e of this.root.querySelectorAll('[data-role]'))e.onclick=()=>{a.save.selectCharacter(e.dataset.role);this.characters();};
    this.on('#choose-role',()=>a.save.levelCount===0?a.play(LEVELS[0].id):a.open('levels'));this.buttonEvents();
  }

  levels(){const a=this.app;
    this.set(`<section class="screen route-screen v3-screen"><header class="topnav">${brand()}<div><button class="quiet-btn" id="change-role">${CHARACTERS[a.save.data.selectedCharacter].name} · 换个伙伴</button><button class="quiet-btn" data-back>← 封面</button></div></header>
      <div class="page-head route-head"><div><h1 class="screen-title">去见你的小幸福路线图</h1><div class="screen-sub">每一关都有安全主路、高处奖励路和摄影小路；这一次不再是同一条走廊换皮。</div></div><div class="route-preview route-triptych">${LEVELS.map(l=>`<span><img src="${this.scenes[l.id]}" alt="${l.chapter}"><b>0${l.order}</b></span>`).join('')}</div></div>
      <div class="level-grid v3-levels">${LEVELS.map(l=>{const r=a.save.data.levels[l.id],locked=l.order>a.save.data.unlockedLevel&&!a.debug;return `<button class="level-card route-card" data-level="${l.id}" ${locked?'disabled':''}><div class="illustration cover"><img src="${this.preview(l.id)}" alt="${l.title}"><span class="number">0${l.order}</span>${locked?'<span class="lock">先走完前一段旅程</span>':''}</div><div class="body"><div class="route-title-row"><div><h2>${l.title}</h2><p class="subline">${l.subtitle}</p></div><span class="chip">${l.chapter}</span></div><div class="record-line"><span>最高小幸福</span><strong>${r.best.happiness} / ${l.totalCollectibles}</strong></div><div class="record-line"><span>照片 ${r.photos.length} / ${l.totalPhotos}</span><strong>最佳连击 ×${r.best.combo}</strong></div><div class="mini-badges">${Object.entries(BADGES).map(([k,v])=>`<span class="${r.badges[k]?'earned':''}">${r.badges[k]?'✓ ':''}${v}</span>`).join('')}</div></div></button>`;}).join('')}</div>
      <footer class="page-foot"><span>相册 ${a.save.photoCount} / 9 · 缺的照片通常藏在分岔出的摄影小路上。</span><div><button class="quiet-btn" id="open-album">幸福相册 →</button><span>${a.debug?'开发调试模式':'单人本地离线存档'}</span></div></footer></section>`);
    for(const e of this.root.querySelectorAll('[data-level]'))e.onclick=()=>a.play(e.dataset.level);
    this.on('#change-role',()=>a.open('characters'));this.on('#open-album',()=>a.open('album'));this.buttonEvents();
  }

  album(tab=this.activeTab){this.activeTab=tab;const a=this.app,l=LEVELS[tab],photos=PHOTOS.filter(p=>p.levelId===l.id),complete=a.save.data.levels[l.id].photos.length===l.totalPhotos;
    this.set(`<section class="screen album-screen v3-screen"><header class="topnav">${brand()}<button class="quiet-btn" data-back>← 回到封面</button></header>
      <div class="page-head album-head"><div><h1 class="screen-title">我们的小幸福相册</h1><div class="screen-sub">照片不在主线终点，而在离主路不远的摄影小路里。看到发光相机，就去试试看。</div></div><div class="album-hero album-collage">${LEVELS.map(l=>`<img src="${this.thumbs[l.id]}" alt="${l.chapter}">`).join('')}</div></div>
      <div class="album-tabs">${LEVELS.map((lv,i)=>`<button data-tab="${i}" class="${tab===i?'active':''}">0${lv.order} / ${lv.chapter}</button>`).join('')}</div>
      <div class="album-layout"><aside class="album-side"><div class="album-side-card"><img src="${this.preview(l.id)}" alt="${l.title}"><div><h3>${l.title}</h3><p>${l.subtitle}</p><b>${a.save.data.levels[l.id].photos.length} / ${l.totalPhotos} 张照片</b></div></div><div class="album-side-note">${complete?'本章三张齐全，纪念卡已解锁。':'提示：挑战失败不扣心，可以立即再试；若想跳过，向下回主路继续走。'}</div></aside>
      <div class="photo-grid v3-photo-grid">${photos.map(p=>{const found=a.save.hasPhoto(p.id);return `<article class="photo-card ${found?'':'locked'}"><img src="${found?(a.save.thumbnail(p.id)||this.preview(l.id)):this.preview(l.id)}" alt="${found?p.name:'未收集的照片'}">${found?'':'<div class="photo-lock">一个还没遇见的小瞬间</div>'}<h3>${p.name}</h3><p>${found?p.caption:p.hint}</p><span class="photo-number">0${p.index}</span></article>`;}).join('')}</div></div>
      <footer class="page-foot"><span class="${complete?'memory-ready':'empty-note'}">${complete?'本章三张齐全 · 纪念卡已解锁':'每章收齐三张照片，就能保存一张纪念卡。'}</span><div>${complete?'<button id="memory-card" class="quiet-btn">保存本章纪念卡 ↗</button>':''}${a.save.photoCount===9?'<button id="final-card" class="quiet-btn">保存最终合影 ↗</button>':''}<button id="album-play" class="quiet-btn">去这段路找找 →</button></div></footer></section>`);
    for(const e of this.root.querySelectorAll('[data-tab]'))e.onclick=()=>this.album(Number(e.dataset.tab));
    this.on('#memory-card',()=>downloadMemoryCard(a,l));this.on('#final-card',()=>downloadFinalCard(a));this.on('#album-play',()=>l.order<=a.save.data.unlockedLevel||a.debug?a.play(l.id):a.open('levels'));this.buttonEvents();
  }

  hud(scene){this.scene=scene;this.previousCombo=-1;this.toastRemaining=0;const l=scene.level,c=scene.character;
    this.set(`<div class="hud ${l.theme==='starlight'?'night':''}"><div class="hud-top"><div class="hud-block hud-life"><img class="hud-avatar" src="./assets/characters/${c.id}-idle.png" alt="${c.name}"><div><div class="hud-life-label">${c.name}的小心情</div><div class="hearts" id="hearts">♥♥♥</div></div></div><div class="hud-block hud-chapter"><span class="index">0${l.order}</span><div><strong>${l.title}</strong><small id="hud-time">00:00</small></div></div><div class="hud-block hud-combo ending-fade" id="combo"><div class="combo-top"><label>幸福连击</label><b id="combo-value">×0</b></div><div class="combo-meter"><i id="combo-fill"></i></div></div><div class="hud-block hud-collect ending-fade"><div><img src="${this.iconHappy}" alt="小幸福"><span><strong id="happiness">0</strong> / ${l.totalCollectibles}</span></div><div><img src="${this.iconCamera}" alt="照片"><span id="photo-count">${scene.save.data.levels[l.id].photos.length} / ${l.totalPhotos}</span></div></div><button class="hud-block hud-pause" id="pause-button" aria-label="暂停">Ⅱ</button></div><div class="journey-line"><div class="fill" id="progress-fill"></div><i class="point" style="left:25%"></i><i class="point" style="left:50%"></i><i class="point" style="left:75%"></i><span class="end">♥</span></div><div class="hud-section" id="section-label"><b>${l.modules[0]}</b><small>今天也要，一路开心。</small></div><div class="challenge-banner" id="challenge-banner"></div><div class="controls"><div class="control-left"><button class="control" data-control="left" aria-label="向左移动">‹</button><button class="control" data-control="right" aria-label="向右移动">›</button></div><span class="key-hint">摄影小路通常在稍高一点的分岔；看到发光相机别直冲错过。</span><button class="control jump" data-control="jump" aria-label="跳跃">⌃<small>跳 跃</small></button></div><div class="toast" id="toast"></div><div class="reunion-caption" id="reunion-caption">跑向你的每一步，都是小幸福。</div><div class="photo-flash" id="photo-flash"></div></div>`);
    this.nodes=Object.fromEntries([...this.root.querySelectorAll('[id]')].map(e=>[e.id,e]));
    this.app.input.bindControls(this.root);this.on('#pause-button',()=>scene.setPaused(true));
  }
  showToast(text,duration=2600){if(!this.nodes?.toast||!this.nodes.toast.isConnected)return;this.nodes.toast.textContent=text;this.nodes.toast.classList.add('visible');this.toastRemaining=duration;}
  updateHUD(s,dt){if(!this.nodes?.hearts?.isConnected)return;const n=this.nodes;
    n.hearts.innerHTML='♥'.repeat(s.damage.hearts)+`<span class="empty">${'♥'.repeat(3-s.damage.hearts)}</span>`;
    n.happiness.textContent=s.happiness;n['photo-count'].textContent=`${s.save.data.levels[s.level.id].photos.length} / ${s.level.totalPhotos}`;
    n['hud-time'].textContent=formatTime(Math.max(1,s.simTime));n['progress-fill'].style.width=`${Math.min(100,s.actor.x/9000*100)}%`;
    n['combo-value'].textContent=`×${s.combo.count}`;n['combo-fill'].style.width=`${s.combo.ratio*100}%`;n.combo.classList.toggle('flow',s.combo.count>=10);
    if(s.combo.count!==this.previousCombo){if(s.combo.count>=5){n.combo.classList.add('pulse');this.pulseRemaining=160;}this.previousCombo=s.combo.count;}
    if(this.pulseRemaining>0){this.pulseRemaining-=dt;if(this.pulseRemaining<=0)n.combo.classList.remove('pulse');}
    if(this.toastRemaining>0){this.toastRemaining-=dt;if(this.toastRemaining<=0)n.toast.classList.remove('visible');}
    const ch=s.challenge.active;
    if(ch){const p=s.challenge.progress;const key=`${ch.def.challengeId}|${p.done}|${Math.ceil((ch.def.timeLimitMs-ch.elapsed)/1000)}`;
      n['challenge-banner'].style.display='block';if(this.challengeKey!==key){this.challengeKey=key;n['challenge-banner'].innerHTML=`<div class="line"><b>▣ ${PHOTO_BY_ID[ch.def.photoId].name}</b><span>${Math.max(0,(ch.def.timeLimitMs-ch.elapsed)/1000).toFixed(0)}s</span></div><div class="challenge-progress">${Array.from({length:p.total},(_,i)=>`<i class="${i<p.done?'done':''}"></i>`).join('')}<small>${p.done>=p.total?'完成了！去最后的摄影台':'失败不扣心，回到入口就能再试'}</small></div>`;}
    }else n['challenge-banner'].style.display='none';
    n['photo-flash'].style.opacity=Math.max(0,(s.photoFlash||0)/450);
    this.root.querySelector('.hud')?.classList.toggle('quiet',s.level.order===3&&s.actor.x>8100);
  }
  section(text,index){const n=this.nodes?.['section-label'];if(n){n.innerHTML=`<b>${text}</b><small>第 ${index+1} 段 / 8 段</small>`;}}
  pause(scene){this.closeModal();const wrap=document.createElement('div');wrap.className='modal-wrap';wrap.innerHTML=`<div class="modal"><span class="pill">让小幸福等一等</span><h2 style="margin-top:16px">歇一小会儿</h2><p>计时、连击和机关都停在原地。<br>准备好了，我们再继续。</p><button class="primary" id="resume-game">继续这段旅程 →</button><div class="actions"><button class="secondary" id="pause-settings">声音与设置</button><button class="secondary" id="restart-game">从头再走</button><button class="secondary" id="quit-game">路线图</button></div></div>`;this.root.append(wrap);
    this.on('#resume-game',()=>scene.setPaused(false));this.on('#pause-settings',()=>this.settings(scene));
    this.on('#restart-game',()=>{scene.telemetry.emit('restart');this.app.play(scene.level.id);});this.on('#quit-game',()=>{scene.telemetry.emit('quit_level');this.app.open('levels');});
  }
  closeModal(){this.root.querySelector('.modal-wrap')?.remove();}
  settings(pausedScene=null){this.closeModal();const a=this.app,d=a.save.data.settings;const w=document.createElement('div');w.className='modal-wrap';
    w.innerHTML=`<div class="modal"><h2>小小的设置</h2><p>用自己最舒服的方式，走这段路。</p><div class="toggle-row"><div>声音<span>轻音乐、脚步般的小音符与反馈音</span></div><button class="toggle ${d.muted?'':'on'}" id="toggle-audio">${d.muted?'已关闭':'已开启'}</button></div><div class="toggle-row"><div>减少画面晃动<span>保留星星与文字，关闭镜头轻震</span></div><button class="toggle ${d.reducedShake?'on':''}" id="toggle-shake">${d.reducedShake?'已开启':'已关闭'}</button></div><div class="settings-export"><button class="quiet-btn" id="export-save">导出存档备份</button><button class="quiet-btn" id="import-save">恢复存档备份</button><input type="file" id="save-file" accept=".json,application/json" hidden><button class="quiet-btn" id="export-events">导出本地游玩记录</button></div><p class="notice">照片和纪录只保存在当前浏览器，不上传服务器。<br>${a.save.lastError||'换浏览器或清理网站数据前，请保留存档备份。'}</p><button class="primary" id="settings-done">好了，回去吧</button></div>`;this.root.append(w);
    this.on('#toggle-audio',()=>{a.save.setSetting('muted',!d.muted);if(d.muted)a.audio.pause();else a.audio.resume();this.settings(pausedScene);});
    this.on('#toggle-shake',()=>{a.save.setSetting('reducedShake',!d.reducedShake);this.settings(pausedScene);});
    this.on('#export-save',()=>downloadJSON(a.save.exportJSON(),'布布一二_V3_存档.json'));
    this.on('#import-save',()=>this.root.querySelector('#save-file').click());
    this.root.querySelector('#save-file').onchange=async e=>{const f=e.target.files?.[0];if(!f)return;const notice=w.querySelector('.notice');try{if(f.size>1024*1024)throw new Error('存档文件过大，请选择导出的 JSON 备份');const text=await f.text();if(!confirm('恢复备份将替换当前浏览器的进度，是否继续？'))return;a.save.importJSON(text);a.input.releaseAll();if(pausedScene){a.open('levels');}else{a.ui.title();a.ui.settings();}}catch(error){notice.textContent=error.message||'无法读取这份存档';}};
    this.on('#export-events',()=>downloadJSON(a.telemetry.exportJSON(),'布布一二_V3_本地游玩记录.json'));
    this.on('#settings-done',()=>pausedScene?this.pause(pausedScene):this.closeModal());
  }
  result(level,run,result){const a=this.app;
    this.set(`<section class="screen result-screen v3-screen"><header class="topnav">${brand()}<button class="quiet-btn" id="result-album">去相册看看 →</button></header><div class="result-left"><div class="postcard v3-postcard"><img src="${this.preview(level.id)}" alt="旅程纪念照"><p>0${level.order} / ${level.chapter}</p></div><div class="result-tagline">${level.order===3?'把这份快乐，带到下一段路。':'不用急着长大，今天已经很棒啦。'}</div></div><div class="result-right"><span class="pill">${level.order===3?'三段小路 · 一次温柔重逢':'又走完了一段小幸福'}</span><h1>${level.title}</h1><div class="result-records"><div class="row"><span>本次小幸福</span><strong>${run.happiness} / ${level.totalCollectibles}</strong></div><div class="row"><span>最高幸福连击${result.newCombo?'<i class="record-new">新纪录</i>':''}</span><strong>×${run.maxCombo}</strong></div><div class="row"><span>本关永久照片</span><strong>${a.save.data.levels[level.id].photos.length} / ${level.totalPhotos}</strong></div><div class="row"><span>用时 / 剩余心情${result.newTime?'<i class="record-new">新纪录</i>':''}</span><strong>${formatTime(run.elapsed,true)} &nbsp; ${'♥'.repeat(run.hearts)}</strong></div></div><div class="result-badges">${Object.entries(BADGES).map(([k,v])=>`<span class="${result.earned[k]?'earned':''}">${result.earned[k]?'✓':'○'} ${v}</span>`).join('')}</div><div class="actions"><button class="secondary" id="again">再走一次</button><button class="secondary" id="results-map">路线图</button><button class="primary" id="next-level">${level.nextLevel?'下一段旅程 →':'保存纪念卡 ↗'}</button></div><p class="result-summary">满载而归：${Math.ceil(level.totalCollectibles*.9)} 个小幸福 · 一路稳稳：全程无伤 · 心流：×${level.flow}<br>${level.order===3?`整段旅程已收藏 ${a.save.photoCount} / 9 张照片。`:'照片获得后即时保存，没拿到的可以回路线图再找。'}</p></div><footer class="page-foot"><button class="quiet-btn" id="share-result">保存这次的纪念卡 ↗</button><span>${CHARACTERS[a.save.data.selectedCharacter].name} 的个人纪录已更新</span></footer></section>`);
    this.on('#again',()=>a.play(level.id));this.on('#results-map',()=>a.open('levels'));this.on('#result-album',()=>a.open('album'));
    this.on('#next-level',()=>level.nextLevel?a.play(level.nextLevel):downloadMemoryCard(a,level,run));this.on('#share-result',()=>downloadMemoryCard(a,level,run));
  }
}
