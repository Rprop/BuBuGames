import { LEVELS, PHOTOS, CHARACTERS } from '../config/game.js';
export const SAVE_KEY = 'bubu-yier-platformer:v2';
export const OLD_KEY = 'bubu-yier-platformer:v1';
const obj = x => x && typeof x === 'object' && !Array.isArray(x) ? x : {};
const num = (x, lo, hi, fallback=0) => Number.isFinite(Number(x)) ? Math.max(lo,Math.min(hi,Math.floor(Number(x)))) : fallback;
const validTime = x => Number.isFinite(Number(x)) && Number(x) > 0 ? Number(x) : null;
const minTime = (a,b) => a && b ? Math.min(a,b) : a || b || null;
const blankChar = () => ({ bestTimeMs: null, bestCombo: 0, bestHappiness: 0, bestHearts: 0, noDamage: false });
export function normalizeSave(raw) {
  const r=obj(raw), old=Number(r.version)!==2, all=obj(r.levels);
  const data={version:2, selectedCharacter:Object.hasOwn(CHARACTERS,r.selectedCharacter)?r.selectedCharacter:'bubu',
    unlockedLevel:num(r.unlockedLevel,1,3,1), settings:{muted:!!(obj(r.settings).muted ?? r.muted),
      reducedShake:!!obj(r.settings).reducedShake}, levels:{}};
  for(const level of LEVELS){
    const s=obj(all[level.id]), best=obj(s.best), badges=obj(s.badges);
    const photoIds=PHOTOS.filter(p=>p.levelId===level.id).map(p=>p.id);
    data.levels[level.id]={completed:s.completed===true,
      photos:[...new Set((Array.isArray(s.photos)?s.photos:[]).filter(x=>photoIds.includes(x)))],
      badges:{happiness:!old&&badges.happiness===true,steady:!old&&badges.steady===true,flow:!old&&badges.flow===true},
      best:{happiness:num(old?s.collectibles:best.happiness,0,level.totalCollectibles),
        combo:old?0:num(best.combo,0,10000),timeMs:old?null:validTime(best.timeMs),
        hearts:num(old?s.bestHearts:best.hearts,0,3),noDamage:!old&&best.noDamage===true},characters:{}};
    for(const name of Object.keys(CHARACTERS)){
      const c=obj(obj(s.characters)[name]); data.levels[level.id].characters[name]={...blankChar(),
        bestTimeMs:validTime(c.bestTimeMs),bestCombo:num(c.bestCombo,0,10000),
        bestHappiness:num(c.bestHappiness,0,level.totalCollectibles),bestHearts:num(c.bestHearts,0,3),noDamage:c.noDamage===true};
    }
    if(s.completed===true) data.unlockedLevel=Math.max(data.unlockedLevel,Math.min(3,level.order+1));
  }
  return data;
}
export class SaveManager {
  constructor(storage){
    this.storage=storage;this.lastError=null;this.migrated=false;let raw;
    for(const key of [SAVE_KEY,OLD_KEY]){
      try {const text=storage?.getItem(key);if(text){raw=JSON.parse(text);this.migrated=key===OLD_KEY;break;}}
      catch(e){this.lastError='存档暂时无法读取，已保留原数据';}
    }
    this.data=normalizeSave(raw); if(this.migrated||!this.storage)this.persist();
  }
  persist(){if(!this.storage){this.lastError='浏览器未开放本地存储，请导出备份；离开页面后内存记录可能丢失';return false;}try{this.storage.setItem(SAVE_KEY,JSON.stringify(this.data));this.lastError=null;return true;}catch(e){this.lastError='本地空间不足，本次记录暂存在内存中';return false;}}
  selectCharacter(id){if(Object.hasOwn(CHARACTERS,id)){this.data.selectedCharacter=id;this.persist();}}
  setSetting(key,value){if(Object.hasOwn(this.data.settings,key)){this.data.settings[key]=!!value;this.persist();}}
  get levelCount(){return Object.values(this.data.levels).filter(l=>l.completed).length;}
  get photoCount(){return Object.values(this.data.levels).reduce((n,l)=>n+l.photos.length,0);}
  hasPhoto(id){return Object.values(this.data.levels).some(l=>l.photos.includes(id));}
  addPhoto(levelId,photoId){const l=this.data.levels[levelId];if(!l||this.hasPhoto(photoId))return false;
    if(!PHOTOS.some(p=>p.id===photoId&&p.levelId===levelId))return false;
    l.photos.push(photoId);this.persist();return true;}
  saveThumbnail(id,dataURL){try{if(dataURL?.startsWith('data:image/'))this.storage?.setItem(`bubu-yier:photo:${id}`,dataURL);}catch(e){/* A photo unlock must never depend on a thumbnail quota. */}}
  thumbnail(id){try{return this.storage?.getItem(`bubu-yier:photo:${id}`)||null;}catch{return null;}}
  commitRun(level,character,run){
    const l=this.data.levels[level.id];if(!l)return null;
    const earned={happiness:run.happiness>=Math.ceil(level.totalCollectibles*.9),steady:run.damageHits===0,flow:run.maxCombo>=level.flow};
    const previous=JSON.parse(JSON.stringify(l.best));l.completed=true;
    for(const k in earned)l.badges[k] ||= earned[k];
    l.best.happiness=Math.max(l.best.happiness,run.happiness);l.best.combo=Math.max(l.best.combo,run.maxCombo);
    l.best.hearts=Math.max(l.best.hearts,run.hearts);l.best.noDamage ||= run.damageHits===0;
    l.best.timeMs=minTime(l.best.timeMs,validTime(run.elapsed));
    const c=l.characters[character];c.bestTimeMs=minTime(c.bestTimeMs,validTime(run.elapsed));
    c.bestCombo=Math.max(c.bestCombo,run.maxCombo);c.bestHappiness=Math.max(c.bestHappiness,run.happiness);
    c.bestHearts=Math.max(c.bestHearts,run.hearts);c.noDamage ||= run.damageHits===0;
    const next=LEVELS.find(item=>item.id===level.nextLevel);this.data.unlockedLevel=Math.max(this.data.unlockedLevel,Math.min(3,next?.order??level.order+1));this.persist();
    return {earned,previous,newCombo:run.maxCombo>previous.combo,newTime:!previous.timeMs||run.elapsed<previous.timeMs};
  }
  importJSON(text){const raw=JSON.parse(text);if(!raw||![1,2].includes(Number(raw.version))||!raw.levels||typeof raw.levels!=='object')throw new Error('请选择布布一二导出的 V1 / V2 存档文件');this.data=normalizeSave(raw);this.persist();return this.data;}
  exportJSON(){return JSON.stringify(this.data,null,2);}
}
